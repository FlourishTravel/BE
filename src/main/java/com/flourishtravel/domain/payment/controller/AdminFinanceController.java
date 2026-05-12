package com.flourishtravel.domain.payment.controller;

import com.flourishtravel.common.dto.ApiResponse;
import com.flourishtravel.domain.payment.dto.FinanceOverviewDto;
import com.flourishtravel.domain.payment.dto.TransactionDetailDto;
import com.flourishtravel.domain.payment.dto.TransactionRowDto;
import com.flourishtravel.domain.payment.dto.UpdatePaymentRequest;
import com.flourishtravel.domain.payment.service.AdminFinanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * REST endpoints chỉ-admin cho trang Tài Chính.
 *
 * Đường dẫn: /finance/admin/**
 *  - GET   /overview                     → stat cards + chart + top tours
 *  - GET   /transactions                 → danh sách giao dịch (payment + refund) có lọc
 *  - GET   /transactions/{kind}/{id}     → chi tiết
 *  - PATCH /payments/{id}                → update note/status/fee/failureReason
 *  - GET   /export                       → CSV xuất báo cáo
 */
@RestController
@RequestMapping("/finance/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminFinanceController {

    private final AdminFinanceService adminFinanceService;

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<FinanceOverviewDto>> overview() {
        return ResponseEntity.ok(ApiResponse.ok(adminFinanceService.overview()));
    }

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<Page<TransactionRowDto>>> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String kind,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100));
        Page<TransactionRowDto> rows = adminFinanceService.listTransactions(q, kind, status, provider, from, to, pageable);
        return ResponseEntity.ok(ApiResponse.ok(rows));
    }

    @GetMapping("/transactions/{kind}/{id}")
    public ResponseEntity<ApiResponse<TransactionDetailDto>> detail(
            @PathVariable String kind, @PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(adminFinanceService.detail(kind, id)));
    }

    @PatchMapping("/payments/{id}")
    public ResponseEntity<ApiResponse<TransactionDetailDto>> updatePayment(
            @PathVariable UUID id,
            @Valid @RequestBody UpdatePaymentRequest req) {
        TransactionDetailDto detail = adminFinanceService.updatePayment(id, req);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật giao dịch thành công", detail));
    }

    /**
     * Xuất CSV báo cáo giao dịch theo bộ lọc. Trả về Content-Disposition: attachment.
     * Bộ lọc trùng với /transactions, không phân trang (lấy tối đa 5000 dòng).
     */
    @GetMapping(value = "/export", produces = "text/csv; charset=UTF-8")
    public ResponseEntity<String> exportCsv(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String kind,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String provider,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        Pageable pageable = PageRequest.of(0, 5000);
        Page<TransactionRowDto> rows = adminFinanceService.listTransactions(q, kind, status, provider, from, to, pageable);

        StringBuilder sb = new StringBuilder();
        // BOM cho Excel mở UTF-8 đúng tiếng Việt.
        sb.append('\uFEFF');
        sb.append("Loai,Ma giao dich,Booking,Khach hang,Email,Tour,Cong thanh toan,So tien,Phi,Net,Trang thai,Ngay tao,Ngay xu ly\n");
        for (TransactionRowDto r : rows.getContent()) {
            sb.append(csv("payment".equalsIgnoreCase(r.getKind()) ? "Thanh toan" : "Hoan tien")).append(',');
            sb.append(csv(r.getCode())).append(',');
            sb.append(csv(r.getBookingCode())).append(',');
            sb.append(csv(r.getCustomerName())).append(',');
            sb.append(csv(r.getCustomerEmail())).append(',');
            sb.append(csv(r.getTourTitle())).append(',');
            sb.append(csv(r.getProvider())).append(',');
            sb.append(csv(r.getAmount() == null ? "0" : r.getAmount().toPlainString())).append(',');
            sb.append(csv(r.getFeeAmount() == null ? "0" : r.getFeeAmount().toPlainString())).append(',');
            sb.append(csv(r.getNetAmount() == null ? "0" : r.getNetAmount().toPlainString())).append(',');
            sb.append(csv(r.getStatus())).append(',');
            sb.append(csv(r.getCreatedAt() == null ? "" : r.getCreatedAt().toString())).append(',');
            sb.append(csv(r.getPaidAt() == null ? "" : r.getPaidAt().toString())).append('\n');
        }

        String fileName = String.format(Locale.ROOT, "transactions-%s.csv", Instant.now().toString().replace(":", "-"));
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.setContentDispositionFormData("attachment", fileName);
        return new ResponseEntity<>(sb.toString(), headers, 200);
    }

    private static String csv(Object value) {
        if (value == null) return "";
        String s = value.toString();
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            s = "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
