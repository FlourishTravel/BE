package com.flourishtravel.domain.flora.recommendation;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parse in-store gift asks ("đang ở Big C… mua quà cho mẹ 500 baht") so Flora
 * can advise aisle-level buys instead of searching tours or nearby restaurants.
 */
public final class FloraGiftShoppingAdvice {

    private static final Pattern VENUE_AT = Pattern.compile(
            "(?:đang\\s+(?:đứng\\s+)?(?:ở|tại)|đứng\\s+(?:ở|tại)|ở\\s+tại|tại\\s+(?:siêu\\s+thị|mall|cửa\\s+hàng)|trong\\s+(?:siêu\\s+thị|mall|cửa\\s+hàng))\\s+(.+?)(?=\\s+và\\b|\\s+hỏi\\b|\\s+mua\\b|\\s+muốn\\b|\\s+trong\\s+ngân|\\s*,|\\s*\\.|$)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern KNOWN_CHAIN = Pattern.compile(
            "\\b(big\\s*c|tesco(?:\\s+lotus)?|lotus(?:'?s)?|7-?eleven|7\\s*eleven|family\\s*mart|ministop|mini\\s*stop|"
                    + "tops(?:\\s+market)?|villa(?:\\s+market)?|iconsiam|icon\\s*siam|central(?:\\s*world)?|"
                    + "robinson|gourmet\\s+market|foodland|maxvalu|makro|emporium|emquartier|siam\\s+paragon|"
                    + "mbk|terminal\\s*21)\\b([^\\n,?]{0,48})",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern BUDGET = Pattern.compile(
            "(\\d{2,7}(?:[.,]\\d{3})?)\\s*(baht|bạt|thb|฿|บาท)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private static final Pattern RECIPIENT = Pattern.compile(
            "(?:cho|tặng)\\s+(mẹ|má|ba\\b|bố mẹ|ba mẹ|bố|bạn gái|bạn trai|chồng|vợ|em gái|em trai|"
                    + "anh trai|chị gái|bạn thân|đồng nghiệp|sếp|con gái|con trai|con|bạn)",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    private FloraGiftShoppingAdvice() {}

    public record GiftAsk(
            boolean giftIntent,
            boolean inStore,
            boolean wantsNearbyStores,
            String venue,
            String recipient,
            Integer budgetAmount,
            String budgetCurrency) {}

    public static GiftAsk parse(String content) {
        if (content == null || content.isBlank()) {
            return new GiftAsk(false, false, false, null, null, null, null);
        }
        String raw = content.trim();
        String lower = raw.toLowerCase(Locale.ROOT);
        boolean giftIntent = looksLikeGiftIntent(lower);
        if (!giftIntent) {
            return new GiftAsk(false, false, false, null, null, null, null);
        }

        String venue = extractVenue(raw);
        String recipient = extractRecipient(raw);
        Integer amount = null;
        String currency = null;
        Matcher budget = BUDGET.matcher(raw);
        if (budget.find()) {
            amount = parseAmount(budget.group(1));
            currency = "THB";
        }

        boolean namedOrStanding = venue != null
                || lower.contains("đang ở")
                || lower.contains("đang tại")
                || lower.contains("đang đứng")
                || lower.contains("trong siêu thị")
                || lower.contains("trong mall")
                || lower.contains("trong cửa hàng");
        boolean nearby = lower.contains("gần đây")
                || lower.contains("siêu thị gần")
                || lower.contains("mall gần")
                || lower.contains("chỗ mua")
                || lower.contains("nơi mua")
                || (lower.contains("mua quà") && lower.contains("gần"));
        boolean inStore = namedOrStanding && !nearby;
        boolean wantsNearbyStores = nearby && !inStore;

        return new GiftAsk(true, inStore, wantsNearbyStores, venue, recipient, amount, currency);
    }

    public static boolean looksLikeGiftIntent(String lower) {
        if (lower == null || lower.isBlank()) return false;
        return lower.contains("mua quà")
                || lower.contains("mua qua")
                || lower.contains("quà cho")
                || lower.contains("quà tặng")
                || lower.contains("tặng phẩm")
                || lower.contains("chọn quà")
                || lower.contains("gợi ý quà")
                || lower.contains("mua đồ tặng")
                || lower.contains("souvenir")
                || lower.contains("gift for")
                || lower.contains("buy a gift")
                || lower.contains("tặng mẹ")
                || lower.contains("tặng ba")
                || lower.contains("tặng bố");
    }

    public static String coerceIntent(String intent, GiftAsk ask) {
        if (ask == null || !ask.giftIntent()) return intent;
        if ("search_tour".equals(intent) || "trip_planner".equals(intent)) {
            return "travel_tips";
        }
        return intent;
    }

    public static void stripTourBudgetSlots(Map<String, Object> slots, GiftAsk ask) {
        if (slots == null || ask == null || !ask.giftIntent()) return;
        slots.remove("budget_min");
        slots.remove("budget_max");
        slots.remove("budget");
    }

    public static String buildLlmHint(GiftAsk ask, String gpsVenueName) {
        if (ask == null || !ask.giftIntent()) return "";
        String venue = firstNonBlank(ask.venue(), gpsVenueName);

        StringBuilder sb = new StringBuilder();
        sb.append("Khách đang hỏi MUA QUÀ / SHOPPING LOCAL — không phải tìm tour.\n");
        if (venue != null) {
            sb.append("- Địa điểm đang đứng: ").append(venue)
                    .append(". Gợi ý món mua NGAY trong cửa hàng/siêu thị/mall này; đừng bảo đi chỗ khác trừ khi họ hỏi.\n");
        } else if (ask.inStore()) {
            sb.append("- Khách nói đang ở cửa hàng nhưng chưa rõ tên. Hỏi đang đứng siêu thị/mall nào.\n");
        } else if (ask.wantsNearbyStores()) {
            sb.append("- Khách chưa nói đang ở đâu; gợi ý siêu thị/mall/souvenir GẦN vị trí (nếu có GPS) để mua quà.\n");
        } else {
            sb.append("- Chưa rõ cửa hàng. Hỏi khách đang đứng siêu thị/mall nào, hoặc gợi ý loại quà phổ biến ở siêu thị Thái.\n");
        }
        if (ask.recipient() != null) {
            sb.append("- Người nhận: ").append(ask.recipient()).append(".\n");
        }
        if (ask.budgetAmount() != null) {
            sb.append("- Ngân sách: ").append(ask.budgetAmount()).append(" ")
                    .append(ask.budgetCurrency() != null ? ask.budgetCurrency() : "THB")
                    .append(". Đây là baht Thái, KHÔNG phải triệu VND, cấm điền budget_min/budget_max tour.\n");
        }
        sb.append("- Gợi ý 3 loại quà thực tế (gian hàng): snack Thái, trái cây, sữa, trà, khăn, son drugstore… ")
                .append("Không bịa giá từng SKU / mã vạch vì không có catalog cửa hàng.\n");
        sb.append("- Nếu ngữ cảnh chuyến đi có giờ tập trung, nhắc còn bao lâu để kịp về điểm hẹn.\n");
        sb.append("- Intent: travel_tips hoặc general_question, không search_tour.\n");
        return sb.toString();
    }

    public static List<String> defaultQuickReplyLabels() {
        return List.of("Snack / bánh Thái", "Đồ dùng nhà / khăn", "Mỹ phẩm drugstore");
    }

    static String extractVenue(String raw) {
        Matcher at = VENUE_AT.matcher(raw);
        if (at.find()) {
            String cleaned = cleanVenue(at.group(1));
            if (cleaned != null) return cleaned;
        }
        Matcher chain = KNOWN_CHAIN.matcher(raw);
        if (chain.find()) {
            String rest = chain.group(2) != null ? chain.group(2) : "";
            return cleanVenue(chain.group(1) + rest);
        }
        return null;
    }

    private static String extractRecipient(String raw) {
        Matcher m = RECIPIENT.matcher(raw);
        if (!m.find()) return null;
        return m.group(1).trim().toLowerCase(Locale.ROOT);
    }

    private static String cleanVenue(String raw) {
        if (raw == null) return null;
        String v = raw.replaceAll("(?i)\\bflora\\b", " ")
                .replaceAll("\\s+", " ")
                .replaceAll("[.?!]+$", "")
                .trim();
        if (v.length() < 3 || v.length() > 80) return null;
        if (v.equalsIgnoreCase("đây") || v.equalsIgnoreCase("chỗ này")) return null;
        return v;
    }

    private static Integer parseAmount(String raw) {
        if (raw == null) return null;
        String digits = raw.replace(".", "").replace(",", "");
        try {
            int n = Integer.parseInt(digits);
            return n > 0 && n <= 9_999_999 ? n : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        if (b != null && !b.isBlank()) return b;
        return null;
    }
}
