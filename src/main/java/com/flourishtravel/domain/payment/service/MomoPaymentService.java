package com.flourishtravel.domain.payment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flourishtravel.common.exception.BadRequestException;
import com.flourishtravel.common.util.UrlUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Khởi tạo thanh toán MoMo Payment Gateway (sandbox: test-payment.momo.vn).
 * Tài liệu: https://developers.momo.vn/v3/docs/payment/api/payment-api/init/
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MomoPaymentService {

    /** Kết quả tra cứu giao dịch /v2/gateway/api/query (resultCode=0 = thành công). */
    public record MomoGatewayQueryResult(int resultCode, String transId, String message) {}

    private final ObjectMapper objectMapper;

    @Value("${app.momo.partner-code:}")
    private String partnerCode;

    @Value("${app.momo.access-key:}")
    private String accessKey;

    @Value("${app.momo.secret-key:}")
    private String secretKey;

    @Value("${app.momo.endpoint:https://test-payment.momo.vn}")
    private String endpoint;

    @Value("${app.momo.ipn-url:}")
    private String ipnUrl;

    @Value("${app.momo.return-url:}")
    private String returnUrl;

    @Value("${app.momo.lang:vi}")
    private String lang;

    @Value("${app.momo.partner-name:FlourishTravel}")
    private String partnerName;

    @Value("${app.momo.store-id:MomoTestStore}")
    private String storeId;

    private static final String CREATE_PATH = "/v2/gateway/api/create";
    private static final String QUERY_PATH = "/v2/gateway/api/query";
    private static final String REQUEST_TYPE = "captureWallet";

    public boolean isConfigured() {
        return partnerCode != null && !partnerCode.isBlank()
                && accessKey != null && !accessKey.isBlank()
                && secretKey != null && !secretKey.isBlank();
    }

    /**
     * Gọi API create của MoMo và trả về payUrl (trang thanh toán sandbox / production).
     *
     * @param orderId   trùng với {@link com.flourishtravel.domain.payment.entity.Payment#getOrderId()}
     * @param amountVnd số tiền VND (nguyên)
     * @param orderInfo mô tả đơn (ASCII an toàn cho cổng)
     * @param requestId mã mỗi lần gọi (UUID)
     */
    public String createPaymentUrl(String orderId, long amountVnd, String orderInfo, String requestId) {
        if (!isConfigured()) {
            throw new IllegalStateException("MoMo is not configured");
        }
        if (amountVnd <= 0) {
            throw new BadRequestException("Số tiền thanh toán không hợp lệ");
        }

        String extraData = "";
        String amountStr = String.valueOf(amountVnd);
        String safeOrderInfo = orderInfo != null && !orderInfo.isBlank() ? orderInfo.trim() : "Thanh toan don hang";

        String redirectUrl = UrlUtils.squashDuplicateSlashesExceptScheme(
                returnUrl == null ? "" : returnUrl.trim());
        String notifyUrl = UrlUtils.squashDuplicateSlashesExceptScheme(
                ipnUrl == null ? "" : ipnUrl.trim());

        String signature = signCreateHash(accessKey, amountStr, extraData, notifyUrl, orderId, safeOrderInfo,
                partnerCode, redirectUrl, requestId, REQUEST_TYPE);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("partnerCode", partnerCode);
        body.put("partnerName", partnerName);
        body.put("storeId", storeId);
        body.put("requestId", requestId);
        body.put("amount", amountStr);
        body.put("orderId", orderId);
        body.put("orderInfo", safeOrderInfo);
        body.put("redirectUrl", redirectUrl);
        body.put("ipnUrl", notifyUrl);
        body.put("lang", lang);
        body.put("extraData", extraData);
        body.put("requestType", REQUEST_TYPE);
        body.put("signature", signature);

        String url = trimTrailingSlash(endpoint) + CREATE_PATH;
        try {
            String json = objectMapper.writeValueAsString(body);
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (res.statusCode() < 200 || res.statusCode() >= 300) {
                log.warn("MoMo HTTP {}: {}", res.statusCode(), res.body());
                throw new BadRequestException("Không kết nối được cổng thanh toán MoMo (HTTP " + res.statusCode() + ")");
            }
            JsonNode root = objectMapper.readTree(res.body());
            int resultCode = root.path("resultCode").asInt(-1);
            String message = root.path("message").asText("Lỗi không xác định từ MoMo");
            if (resultCode != 0) {
                log.warn("MoMo create failed: resultCode={}, body={}", resultCode, res.body());
                throw new BadRequestException("MoMo: " + message);
            }
            String payUrl = root.path("payUrl").asText(null);
            if (payUrl == null || payUrl.isBlank()) {
                log.warn("MoMo missing payUrl: {}", res.body());
                throw new BadRequestException("MoMo không trả về liên kết thanh toán");
            }
            return payUrl;
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("MoMo create error", e);
            throw new BadRequestException("Lỗi khi tạo giao dịch MoMo: " + e.getMessage());
        }
    }

    /**
     * Tra cứu trạng thái đơn trên MoMo (khi IPN không tới được server, ví dụ IPN trỏ localhost).
     * @see <a href="https://developers.momo.vn/v3/docs/payment/api/payment-api/query/">MoMo Query API</a>
     */
    public MomoGatewayQueryResult queryTransactionStatus(String orderId) {
        if (!isConfigured()) {
            throw new IllegalStateException("MoMo is not configured");
        }
        if (orderId == null || orderId.isBlank()) {
            throw new BadRequestException("orderId không hợp lệ");
        }
        String oid = orderId.trim();
        String requestId = UUID.randomUUID().toString();
        String sig = signQueryHash(accessKey, oid, partnerCode, requestId);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("partnerCode", partnerCode);
        body.put("requestId", requestId);
        body.put("orderId", oid);
        body.put("signature", sig);
        body.put("lang", lang);

        String url = trimTrailingSlash(endpoint) + QUERY_PATH;
        try {
            String json = objectMapper.writeValueAsString(body);
            HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20)).build();
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(35))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> res = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (res.statusCode() < 200 || res.statusCode() >= 300) {
                log.warn("MoMo query HTTP {}: {}", res.statusCode(), res.body());
                throw new BadRequestException("Không tra cứu được MoMo (HTTP " + res.statusCode() + ")");
            }
            JsonNode root = objectMapper.readTree(res.body());
            int resultCode = root.path("resultCode").asInt(-999);
            String message = root.path("message").asText("");
            JsonNode transNode = root.get("transId");
            String transId = null;
            if (transNode != null && !transNode.isNull()) {
                if (transNode.isNumber()) {
                    transId = String.valueOf(transNode.asLong());
                } else {
                    transId = transNode.asText("").trim();
                    if (transId.isEmpty()) {
                        transId = null;
                    }
                }
            }
            return new MomoGatewayQueryResult(resultCode, transId, message);
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("MoMo query error", e);
            throw new BadRequestException("Lỗi tra cứu MoMo: " + e.getMessage());
        }
    }

    /**
     * Chuỗi ký query: accessKey&orderId&partnerCode&requestId (theo tài liệu MoMo).
     */
    private static String signQueryHash(String accessKey, String orderId, String partnerCode, String requestId, String secretKey) {
        String rawHash = "accessKey=" + accessKey
                + "&orderId=" + orderId
                + "&partnerCode=" + partnerCode
                + "&requestId=" + requestId;
        return hmacSha256Hex(rawHash, secretKey);
    }

    private String signQueryHash(String accessKey, String orderId, String partnerCode, String requestId) {
        return signQueryHash(accessKey, orderId, partnerCode, requestId, secretKey);
    }

    /**
     * Chuỗi ký theo tài liệu MoMo (HMAC SHA256, hex).
     */
    private String signCreateHash(String accessKey, String amount, String extraData, String ipnUrl,
                                  String orderId, String orderInfo, String partnerCode, String redirectUrl,
                                  String requestId, String requestType) {
        String rawHash = "accessKey=" + accessKey
                + "&amount=" + amount
                + "&extraData=" + extraData
                + "&ipnUrl=" + ipnUrl
                + "&orderId=" + orderId
                + "&orderInfo=" + orderInfo
                + "&partnerCode=" + partnerCode
                + "&redirectUrl=" + redirectUrl
                + "&requestId=" + requestId
                + "&requestType=" + requestType;
        return hmacSha256Hex(rawHash, secretKey);
    }

    private static String hmacSha256Hex(String rawHash, String secretKey) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(rawHash.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot sign MoMo request", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static String trimTrailingSlash(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }
}
