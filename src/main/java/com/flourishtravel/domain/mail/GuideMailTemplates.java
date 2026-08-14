package com.flourishtravel.domain.mail;

import org.springframework.util.StringUtils;

/** HTML mail phân công HDV — bảng + CSS inline để Gmail/Outlook đọc được. */
public final class GuideMailTemplates {

    public static final String LOGO_CID = "flourish-logo";

    private GuideMailTemplates() {}

    public record Contact(
            String website,
            String portalUrl,
            String helpUrl,
            String supportEmail,
            String hotline
    ) {}

    public static String assigned(String guideName, String tourTitle, String dateRange, String note, Contact contact) {
        String noteBlock = StringUtils.hasText(note)
                ? row("Ghi chú điều hành", note)
                : "";
        String inner = ""
                + "<p style=\"margin:0 0 16px;font-size:16px;color:#0f172a;\">Xin chào <strong>"
                + MailService.escape(guideName) + "</strong>,</p>"
                + "<p style=\"margin:0 0 20px;font-size:15px;line-height:1.6;color:#334155;\">"
                + "Điều hành Flourish vừa phân công bạn dẫn đoàn dưới đây. Vui lòng xác nhận trên portal HDV "
                + "và chuẩn bị danh sách khách, lịch trình, điểm tập trung trước ngày khởi hành.</p>"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"background:#f8fafc;border:1px solid #e2e8f0;border-radius:12px;margin:0 0 20px;\">"
                + "<tr><td style=\"padding:18px 20px;\">"
                + "<p style=\"margin:0 0 4px;font-size:12px;letter-spacing:.08em;color:#059669;font-weight:700;\">LỊCH ĐƯỢC PHÂN CÔNG</p>"
                + "<p style=\"margin:0 0 12px;font-size:18px;font-weight:700;color:#0f172a;\">"
                + MailService.escape(tourTitle) + "</p>"
                + row("Ngày khởi hành", dateRange)
                + noteBlock
                + "</td></tr></table>"
                + "<p style=\"margin:0 0 8px;font-size:15px;font-weight:700;color:#0f172a;\">Việc cần làm</p>"
                + "<ul style=\"margin:0 0 22px;padding-left:20px;color:#334155;font-size:14px;line-height:1.7;\">"
                + "<li>Mở portal HDV để xem danh sách khách và trạng thái thanh toán.</li>"
                + "<li>Đối chiếu lịch trình từng ngày, điểm tập trung và ghi chú điều hành.</li>"
                + "<li>Chuẩn bị kênh chat đoàn / liên lạc khẩn nếu tour yêu cầu.</li>"
                + "<li>Báo điều hành ngay nếu trùng lịch hoặc chưa nhận đủ thông tin.</li>"
                + "</ul>"
                + cta(contact.portalUrl(), "Mở portal HDV");
        return wrap("Bạn được phân công dẫn tour", inner, contact);
    }

    public static String unassigned(String guideName, String tourTitle, String dateRange, Contact contact) {
        String inner = ""
                + "<p style=\"margin:0 0 16px;font-size:16px;color:#0f172a;\">Xin chào <strong>"
                + MailService.escape(guideName) + "</strong>,</p>"
                + "<p style=\"margin:0 0 20px;font-size:15px;line-height:1.6;color:#334155;\">"
                + "Điều hành đã chuyển lịch dẫn tour này sang HDV khác. Bạn không còn phụ trách đoàn dưới đây.</p>"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"background:#fff7ed;border:1px solid #fed7aa;border-radius:12px;margin:0 0 20px;\">"
                + "<tr><td style=\"padding:18px 20px;\">"
                + "<p style=\"margin:0 0 4px;font-size:12px;letter-spacing:.08em;color:#c2410c;font-weight:700;\">LỊCH ĐÃ ĐỔI HDV</p>"
                + "<p style=\"margin:0 0 12px;font-size:18px;font-weight:700;color:#0f172a;\">"
                + MailService.escape(tourTitle) + "</p>"
                + row("Ngày khởi hành", dateRange)
                + "</td></tr></table>"
                + "<p style=\"margin:0 0 22px;font-size:14px;line-height:1.6;color:#334155;\">"
                + "Nếu đây là nhầm lẫn, gọi hotline điều hành hoặc trả lời email này trong ngày.</p>"
                + cta(contact.portalUrl(), "Xem lịch trên portal");
        return wrap("Lịch dẫn tour đã chuyển người khác", inner, contact);
    }

    private static String wrap(String eyebrow, String inner, Contact contact) {
        String site = MailService.escape(blankToDash(contact.website()));
        String mail = MailService.escape(blankToDash(contact.supportEmail()));
        String phone = StringUtils.hasText(contact.hotline())
                ? MailService.escape(contact.hotline())
                : "Xem trên portal HDV";
        String help = MailService.escape(blankToDash(contact.helpUrl()));
        String portal = MailService.escape(blankToDash(contact.portalUrl()));
        return "<!DOCTYPE html><html lang=\"vi\"><body style=\"margin:0;padding:0;background:#ecfdf5;font-family:Arial,Helvetica,sans-serif;\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#ecfdf5;padding:24px 12px;\">"
                + "<tr><td align=\"center\">"
                + "<table role=\"presentation\" width=\"600\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:600px;width:100%;background:#ffffff;border-radius:16px;overflow:hidden;border:1px solid #d1fae5;\">"
                + "<tr><td style=\"background:linear-gradient(135deg,#059669,#0d9488);padding:28px 28px 22px;text-align:center;\">"
                + "<img src=\"cid:" + LOGO_CID + "\" alt=\"Flourish Travel\" width=\"96\" "
                + "style=\"display:inline-block;border:0;border-radius:18px;background:#fff;padding:6px;\">"
                + "<p style=\"margin:14px 0 0;font-size:22px;font-weight:700;color:#ffffff;letter-spacing:.02em;\">Flourish Travel</p>"
                + "<p style=\"margin:4px 0 0;font-size:13px;color:#d1fae5;\">" + MailService.escape(eyebrow) + "</p>"
                + "</td></tr>"
                + "<tr><td style=\"padding:28px 28px 8px;\">" + inner + "</td></tr>"
                + "<tr><td style=\"padding:8px 28px 28px;\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"background:#f0fdf4;border:1px solid #bbf7d0;border-radius:12px;\">"
                + "<tr><td style=\"padding:18px 20px;\">"
                + "<p style=\"margin:0 0 10px;font-size:13px;font-weight:700;color:#047857;\">Liên hệ Flourish Travel</p>"
                + contactLine("Website", site)
                + contactLine("Email điều hành", mail)
                + contactLine("Hotline", phone)
                + contactLine("Portal HDV", portal)
                + contactLine("Trợ giúp", help)
                + "<p style=\"margin:12px 0 0;font-size:12px;line-height:1.5;color:#64748b;\">"
                + "Ứng dụng web: " + site + " · Portal HDV: " + portal
                + "</p>"
                + "</td></tr></table>"
                + "</td></tr>"
                + "<tr><td style=\"padding:16px 28px 24px;text-align:center;background:#f8fafc;border-top:1px solid #e2e8f0;\">"
                + "<p style=\"margin:0;font-size:12px;color:#94a3b8;\">© 2026 Flourish Travel · Mail tự động từ điều hành tour</p>"
                + "</td></tr>"
                + "</table></td></tr></table></body></html>";
    }

    private static String row(String label, String value) {
        return "<p style=\"margin:0 0 8px;font-size:14px;color:#475569;\"><span style=\"color:#64748b;\">"
                + MailService.escape(label) + ": </span><strong style=\"color:#0f172a;\">"
                + MailService.escape(value) + "</strong></p>";
    }

    private static String contactLine(String label, String value) {
        return "<p style=\"margin:0 0 6px;font-size:13px;color:#334155;\"><strong>" + label + ":</strong> " + value + "</p>";
    }

    private static String cta(String href, String label) {
        String safeHref = MailService.escape(blankToDash(href));
        return "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin:0 0 8px;\"><tr><td "
                + "style=\"background:#059669;border-radius:10px;\">"
                + "<a href=\"" + safeHref + "\" style=\"display:inline-block;padding:12px 22px;color:#ffffff;"
                + "text-decoration:none;font-weight:700;font-size:14px;\">" + MailService.escape(label) + "</a>"
                + "</td></tr></table>";
    }

    private static String blankToDash(String value) {
        return StringUtils.hasText(value) ? value : "—";
    }
}
