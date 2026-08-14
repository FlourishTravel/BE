package com.flourishtravel.domain.chat;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Flora chỉ trả lời trong chat đoàn khi được gọi tên, hoặc khi câu hỏi
 * khớp chủ đề chatbot đã có (chính sách, sự cố, lịch trình…).
 */
public final class FloraGroupChatTrigger {

    public static final String FLORA_EMAIL = "flora@flourishtravel.internal";
    public static final String FLORA_ROLE = "FLORA";
    public static final String FLORA_NAME = "Flora";
    /** Ảnh nhân vật Flora trên website (public/flora/Flora-AI.png). */
    public static final String FLORA_AVATAR_URL = "/flora/Flora-AI.png";

    private static final Pattern MENTION = Pattern.compile(
            "(?i)(?:^|\\s)(?:@flora\\b|flora\\s+ơi\\b|/flora\\b|hỏi\\s+flora\\b|flora\\s+giúp\\b|này\\s+flora\\b|flora\\s+ơi)");

    private static final Pattern SOCIAL_ONLY = Pattern.compile(
            "(?i)^[\\s.,!?]*"
                    + "(?:ok|okay|oke|okela|ừ+|uh+|ừm+|alo+|hi+|hello+|hey+|haha+|hihi+|hehe+"
                    + "|thanks|thank\\s*you|cảm ơn|cam on|yes|no|yeah|yep|👍+|❤️+|😂+)"
                    + "[\\s.,!?]*$");

    private static final Pattern QUESTION = Pattern.compile(
            "[?]|"
                    + "(?i)(?:^|\\s)(?:gì|sao|thế\\s+nào|như\\s+thế\\s+nào|khi\\s+nào|ở\\s+đâu"
                    + "|được\\s+không|có\\s+được|phải\\s+không|bao\\s+giờ|mấy\\s+giờ|làm\\s+sao"
                    + "|giúp|hỏi|cần\\s+hỗ\\s+trợ)|không\\s*[?!.]*$");

    private static final String[] EXISTING_TOPICS = {
            "hủy", "huỷ", "hoàn tiền", "hoàn vé", "đổi tour", "đổi lịch",
            "bảo hiểm", "visa", "hộ chiếu", "passport",
            "cấp cứu", "cấp cứu", "ngộ độc", "say sóng", "say xe", "bệnh",
            "bão", "mưa", "thời tiết", "hoãn", "hủy chuyến",
            "lịch trình", "tập trung", "điểm hẹn", "meeting", "hotline",
            "mất ví", "mất đồ", "mất passport",
            "ổ cắm", "đổi tiền", "atm", "sim",
            "trẻ em", "trẻ nhỏ", "giá tour", "thanh toán", "voucher",
            "khiếu nại", "khiếu nại hdv", "hdv",
            "hành lý", "hành trang", "khẩn cấp", "sự cố"
    };

    private FloraGroupChatTrigger() {
    }

    public static boolean shouldReply(String content) {
        if (content == null || content.isBlank()) {
            return false;
        }
        String trimmed = content.trim();
        if (trimmed.length() < 2) {
            return false;
        }
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (SOCIAL_ONLY.matcher(trimmed).matches()) {
            return false;
        }
        if (mentionsFlora(trimmed)) {
            return true;
        }
        return looksLikeQuestion(lower) && matchesExistingTopic(lower);
    }

    public static String stripMention(String content) {
        if (content == null) {
            return "";
        }
        String stripped = content.replaceAll("(?i)@flora\\b", " ");
        stripped = stripped.replaceAll("(?i)/flora\\b", " ");
        stripped = stripped.replaceAll("(?i)\\bflora\\s+ơi\\b", " ");
        stripped = stripped.replaceAll("(?i)\\bhỏi\\s+flora\\b", " ");
        stripped = stripped.replaceAll("(?i)\\bflora\\s+giúp\\b", " ");
        stripped = stripped.replaceAll("(?i)\\bnày\\s+flora\\b", " ");
        return stripped.replaceAll("\\s+", " ").trim();
    }

    public static boolean isFloraEmail(String email) {
        return email != null && FLORA_EMAIL.equalsIgnoreCase(email);
    }

    static boolean mentionsFlora(String content) {
        return MENTION.matcher(content).find()
                || content.toLowerCase(Locale.ROOT).startsWith("flora ");
    }

    static boolean looksLikeQuestion(String lower) {
        return QUESTION.matcher(lower).find();
    }

    static boolean matchesExistingTopic(String lower) {
        for (String topic : EXISTING_TOPICS) {
            if (lower.contains(topic)) {
                return true;
            }
        }
        return false;
    }
}
