package com.flourishtravel.domain.mail;

import org.springframework.util.StringUtils;

import java.util.List;

/**
 * Hóa đơn / phiếu xác nhận đặt tour — HTML inline để Gmail/Outlook đọc được.
 * Nội dung cố ý dài: khách giữ mail làm giấy tờ xuất trình khi tập trung.
 */
public final class BookingInvoiceMailTemplates {

    private BookingInvoiceMailTemplates() {
    }

    public record GuestLine(String fullName, String dateOfBirth, String document, String nationality) {}

    public record DayLine(String heading, String detail) {}

    public record InvoiceSnapshot(
            String customerName,
            String customerEmail,
            String customerPhone,
            String bookingCode,
            String bookingId,
            String issuedAt,
            boolean paid,
            String paymentStatusLabel,
            String paymentProviderLabel,
            String paymentOrderId,
            String paymentTransId,
            String paymentUrl,
            String tourTitle,
            String destination,
            String duration,
            String category,
            String marketSegment,
            String dateRange,
            String startDate,
            String endDate,
            int guestCount,
            String unitPrice,
            String subtotal,
            String discount,
            String total,
            String paidAmount,
            String remaining,
            String promoCode,
            String contactPhone,
            String pickupAddress,
            String specialRequests,
            String emergencyName,
            String emergencyPhone,
            String guideName,
            String bookingUrl,
            String cancellationPolicyUrl,
            String website,
            String supportEmail,
            String hotline,
            List<GuestLine> guests,
            List<DayLine> itineraryDays,
            String highlights,
            String includes,
            String excludes
    ) {}

    public static String build(InvoiceSnapshot d) {
        boolean paid = d.paid();
        String statusColor = paid ? "#047857" : "#b45309";
        String statusBg = paid ? "#ecfdf5" : "#fffbeb";
        String statusBorder = paid ? "#a7f3d0" : "#fde68a";
        String statusLabel = paid ? "ĐÃ THANH TOÁN — HÓA ĐƠN XÁC NHẬN" : "CHỜ THANH TOÁN — PHIẾU ĐẶT CHỖ";
        String eyebrow = paid
                ? "Hóa đơn xác nhận đặt tour · giấy tờ xuất trình"
                : "Phiếu xác nhận đặt tour · hóa đơn tạm thời";

        String payCta = !paid && StringUtils.hasText(d.paymentUrl())
                ? cta(d.paymentUrl(), "Thanh toán đơn này ngay")
                : "";
        String openBooking = StringUtils.hasText(d.bookingUrl())
                ? cta(d.bookingUrl(), "Xem chi tiết đơn trên Flourish Travel")
                : "";

        String inner = greeting(d)
                + intro(d, paid)
                + statusBanner(statusLabel, statusBg, statusBorder, statusColor, d)
                + invoiceMeta(d, paid)
                + tourBlock(d)
                + guestsBlock(d)
                + moneyBlock(d, paid)
                + itineraryBlock(d)
                + serviceBlock(d)
                + presentBlock(d, paid)
                + policyBlock(d)
                + packingBlock(d)
                + supportBlock(d)
                + legalBlock(d, paid)
                + payCta
                + openBooking;

        return wrap(eyebrow, inner, d);
    }

    private static String greeting(InvoiceSnapshot d) {
        return "<p style=\"margin:0 0 16px;font-size:16px;color:#0f172a;\">Kính gửi <strong>"
                + MailService.escape(blank(d.customerName(), "Quý khách"))
                + "</strong>,</p>";
    }

    private static String intro(InvoiceSnapshot d, boolean paid) {
        String lead = paid
                ? "Flourish Travel xin gửi hóa đơn / phiếu xác nhận đặt tour chính thức sau khi hệ thống đã ghi nhận thanh toán thành công. "
                + "Email này là giấy tờ điện tử để Quý khách lưu trên điện thoại hoặc in ra, xuất trình cho hướng dẫn viên, nhân viên điều hành, "
                + "bộ phận đón tiễn và cơ sở lưu trú khi tập trung khởi hành."
                : "Flourish Travel đã nhận đơn đặt tour của Quý khách và xuất phiếu xác nhận / hóa đơn tạm thời ngay sau khi đặt chỗ. "
                + "Email này giúp Quý khách nắm toàn bộ thông tin đoàn, lịch trình, số tiền và hướng dẫn xuất trình giấy tờ. "
                + "Chỗ ngồi được giữ theo chính sách; vui lòng hoàn tất thanh toán để hóa đơn chuyển sang trạng thái đã thanh toán.";
        return "<p style=\"margin:0 0 12px;font-size:15px;line-height:1.75;color:#334155;\">" + lead + "</p>"
                + "<p style=\"margin:0 0 20px;font-size:15px;line-height:1.75;color:#334155;\">"
                + "Mã đơn <strong>" + MailService.escape(d.bookingCode()) + "</strong> gắn với tài khoản "
                + MailService.escape(blank(d.customerEmail(), "—"))
                + ". Khi làm thủ tục, chỉ cần mở email này (hoặc ảnh chụp màn hình phần mã đơn) kèm CCCD/hộ chiếu bản gốc của từng khách. "
                + "Không cần in màu; bản PDF/ảnh trên điện thoại được chấp nhận tại điểm tập trung Flourish Travel.</p>";
    }

    private static String statusBanner(String label, String bg, String border, String color, InvoiceSnapshot d) {
        return "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin:0 0 20px;"
                + "background:" + bg + ";border:1px solid " + border + ";border-radius:12px;\">"
                + "<tr><td style=\"padding:16px 18px;text-align:center;\">"
                + "<p style=\"margin:0 0 6px;font-size:11px;letter-spacing:.12em;font-weight:700;color:" + color + ";\">"
                + MailService.escape(label) + "</p>"
                + "<p style=\"margin:0;font-size:28px;font-weight:800;letter-spacing:.08em;color:#0f172a;\">"
                + MailService.escape(d.bookingCode()) + "</p>"
                + "<p style=\"margin:8px 0 0;font-size:13px;color:#475569;\">Xuất trình mã này khi tập trung · "
                + MailService.escape(d.issuedAt()) + "</p>"
                + "</td></tr></table>";
    }

    private static String invoiceMeta(InvoiceSnapshot d, boolean paid) {
        return sectionTitle("1. Thông tin hóa đơn")
                + "<p style=\"margin:0 0 12px;font-size:14px;line-height:1.7;color:#334155;\">"
                + "Đây là chứng từ đặt chỗ do hệ thống Flourish Travel phát hành tự động. "
                + "Hóa đơn giá trị gia tăng (VAT) giấy, nếu Quý khách cần xuất cho công ty, vui lòng trả lời email này trong 7 ngày "
                + "kèm mã số thuế, tên đơn vị và địa chỉ xuất hóa đơn.</p>"
                + box(
                        row("Số chứng từ", d.bookingCode())
                                + row("Mã hệ thống", shortId(d.bookingId()))
                                + row("Ngày lập", d.issuedAt())
                                + row("Trạng thái thanh toán", d.paymentStatusLabel())
                                + row("Cổng thanh toán", d.paymentProviderLabel())
                                + row("Mã giao dịch", blank(d.paymentOrderId(), "—"))
                                + row("Mã tham chiếu cổng", blank(d.paymentTransId(), paid ? "Đang đối soát" : "Chưa phát sinh"))
                                + row("Người đặt", blank(d.customerName(), "—"))
                                + row("Email nhận hóa đơn", blank(d.customerEmail(), "—"))
                                + row("Điện thoại tài khoản", blank(d.customerPhone(), "—")));
    }

    private static String tourBlock(InvoiceSnapshot d) {
        return sectionTitle("2. Thông tin tour và lịch khởi hành")
                + "<p style=\"margin:0 0 12px;font-size:14px;line-height:1.7;color:#334155;\">"
                + "Vui lòng đối chiếu tên tour, ngày đi – ngày về và điểm đến với lịch làm việc / nghỉ phép của cả đoàn. "
                + "Giờ tập trung chi tiết sẽ được gửi lại trên phòng chat đoàn và ứng dụng trước ngày khởi hành 24–48 giờ.</p>"
                + box(
                        row("Tên tour", d.tourTitle())
                                + row("Điểm đến", blank(d.destination(), "Theo chương trình tour"))
                                + row("Phân khúc", blank(d.marketSegment(), "Tour Flourish Travel"))
                                + row("Nhóm tour", blank(d.category(), "—"))
                                + row("Thời lượng", blank(d.duration(), "—"))
                                + row("Ngày khởi hành", blank(d.startDate(), "—"))
                                + row("Ngày kết thúc", blank(d.endDate(), "—"))
                                + row("Khoảng thời gian", blank(d.dateRange(), "—"))
                                + row("Số khách trên đơn", d.guestCount() + " người")
                                + row("Hướng dẫn viên", blank(d.guideName(), "Sẽ phân công trước ngày đi"))
                                + row("Điện thoại liên hệ chuyến", blank(d.contactPhone(), d.customerPhone()))
                                + row("Điểm đón / tập trung", blank(d.pickupAddress(), "Sẽ thông báo trên đơn và chat đoàn"))
                                + row("Ghi chú đặc biệt", blank(d.specialRequests(), "Không có"))
                                + row("Liên hệ khẩn cấp", emergency(d)));
    }

    private static String guestsBlock(InvoiceSnapshot d) {
        StringBuilder table = new StringBuilder();
        table.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" ")
                .append("style=\"border-collapse:collapse;margin:0 0 12px;font-size:13px;\">")
                .append("<tr style=\"background:#f8fafc;\">")
                .append(th("STT")).append(th("Họ và tên")).append(th("Ngày sinh"))
                .append(th("Giấy tờ")).append(th("Quốc tịch")).append("</tr>");
        List<GuestLine> guests = d.guests() == null ? List.of() : d.guests();
        if (guests.isEmpty()) {
            table.append("<tr>").append(td("1")).append(td(blank(d.customerName(), "Khách đặt đơn")))
                    .append(td("—")).append(td("Theo hồ sơ tài khoản")).append(td("—")).append("</tr>");
        } else {
            int i = 1;
            for (GuestLine g : guests) {
                table.append("<tr>")
                        .append(td(String.valueOf(i++)))
                        .append(td(blank(g.fullName(), "—")))
                        .append(td(blank(g.dateOfBirth(), "—")))
                        .append(td(blank(g.document(), "Chưa cung cấp / sẽ đối chiếu bản gốc")))
                        .append(td(blank(g.nationality(), "—")))
                        .append("</tr>");
            }
        }
        table.append("</table>");
        return sectionTitle("3. Danh sách khách trên hóa đơn")
                + "<p style=\"margin:0 0 12px;font-size:14px;line-height:1.7;color:#334155;\">"
                + "Tên trên danh sách phải khớp giấy tờ tùy thân khi làm thủ tục. CCCD/hộ chiếu trên mail chỉ hiện dạng che số "
                + "để bảo mật; khi xuất trình hãy mang bản gốc. Trẻ em và em bé vẫn phải có tên trên danh sách này.</p>"
                + table
                + "<p style=\"margin:0 0 20px;font-size:13px;line-height:1.65;color:#64748b;\">"
                + "Nếu phát hiện sai chính tả tên, ngày sinh hoặc số giấy tờ, hãy vào chi tiết đơn trên website hoặc báo hotline "
                + "trước ngày khởi hành ít nhất 48 giờ để điều hành chỉnh hồ sơ bảo hiểm và vé.</p>";
    }

    private static String moneyBlock(InvoiceSnapshot d, boolean paid) {
        return sectionTitle("4. Bảng kê thanh toán")
                + "<p style=\"margin:0 0 12px;font-size:14px;line-height:1.7;color:#334155;\">"
                + "Số tiền dưới đây đã chốt theo đơn giá tour tại thời điểm đặt, đã trừ khuyến mãi (nếu có). "
                + (paid
                ? "Hệ thống đã ghi nhận thanh toán đủ theo hóa đơn này. Quý khách không cần chuyển thêm trừ khi phát sinh dịch vụ lẻ trên tour."
                : "Đơn chưa thanh toán xong. Vui lòng dùng nút thanh toán ở cuối email hoặc mở lại đơn trên tài khoản. "
                + "Chỗ có thể được giải phóng nếu quá hạn thanh toán.")
                + "</p>"
                + box(
                        row("Đơn giá / khách", d.unitPrice())
                                + row("Số khách tính tiền", d.guestCount() + " người")
                                + row("Tạm tính", d.subtotal())
                                + row("Khuyến mãi" + (StringUtils.hasText(d.promoCode()) ? " (" + d.promoCode() + ")" : ""), d.discount())
                                + row("Tổng phải trả", d.total())
                                + row("Đã thanh toán", d.paidAmount())
                                + row("Còn lại", d.remaining())
                                + row("Loại tiền", "Việt Nam Đồng (VND)")
                                + row("Hình thức", d.paymentProviderLabel()))
                + "<p style=\"margin:0 0 20px;font-size:13px;line-height:1.65;color:#64748b;\">"
                + "Phí phát sinh ngoài chương trình (đồ uống, vé tham quan tự chọn, hành lý quá cước, phụ thu phòng đơn…) "
                + "không nằm trong hóa đơn này và được thu riêng nếu Quý khách sử dụng.</p>";
    }

    private static String itineraryBlock(InvoiceSnapshot d) {
        StringBuilder days = new StringBuilder();
        List<DayLine> list = d.itineraryDays() == null ? List.of() : d.itineraryDays();
        if (list.isEmpty()) {
            days.append("<p style=\"margin:0 0 8px;font-size:14px;color:#334155;\">Lịch trình chi tiết theo ngày sẽ được cập nhật trên trang tour và phòng chat đoàn. ")
                    .append("Quý khách vẫn giữ hóa đơn này để đối chiếu ngày đi – ngày về.</p>");
        } else {
            for (DayLine day : list) {
                days.append("<p style=\"margin:0 0 10px;font-size:14px;line-height:1.65;color:#334155;\"><strong>")
                        .append(MailService.escape(blank(day.heading(), "Ngày")))
                        .append(".</strong> ")
                        .append(MailService.escape(blank(day.detail(), "Theo điều hành đoàn.")))
                        .append("</p>");
            }
        }
        return sectionTitle("5. Tóm tắt lịch trình để xuất trình")
                + "<p style=\"margin:0 0 12px;font-size:14px;line-height:1.7;color:#334155;\">"
                + "Đoạn dưới đây là bản tóm tắt chương trình để khách và gia đình nắm lịch. "
                + "Thứ tự điểm đến có thể đảo nhẹ vì giao thông, thời tiết hoặc yêu cầu an toàn của địa phương — "
                + "hướng dẫn viên sẽ thông báo trong chat đoàn.</p>"
                + days
                + "<p style=\"margin:12px 0 20px;font-size:13px;line-height:1.65;color:#64748b;\">"
                + "In hoặc lưu mục này khi đi cửa khẩu / khách sạn nếu được hỏi lịch trình lưu trú.</p>";
    }

    private static String serviceBlock(InvoiceSnapshot d) {
        return sectionTitle("6. Dịch vụ đã gồm và chưa gồm")
                + "<p style=\"margin:0 0 10px;font-size:14px;line-height:1.7;color:#334155;\"><strong>Điểm nổi bật:</strong> "
                + MailService.escape(blank(d.highlights(), "Theo mô tả tour trên website Flourish Travel."))
                + "</p>"
                + "<p style=\"margin:0 0 10px;font-size:14px;line-height:1.7;color:#334155;\"><strong>Đã bao gồm:</strong> "
                + MailService.escape(blank(d.includes(),
                        "Xe / vận chuyển theo chương trình, khách sạn hoặc tàu theo hạng tour, một phần ăn, vé thắng cảnh trong lịch trình, hướng dẫn viên, bảo hiểm du lịch cơ bản (nếu tour công bố)."))
                + "</p>"
                + "<p style=\"margin:0 0 20px;font-size:14px;line-height:1.7;color:#334155;\"><strong>Chưa bao gồm:</strong> "
                + MailService.escape(blank(d.excludes(),
                        "Hộ chiếu / visa, chi phí cá nhân, tip HDV và tài xế (nếu đoàn thống nhất), hành lý quá cước, phụ thu phòng đơn, đồ uống, và các mục ghi rõ ở trang tour."))
                + "</p>";
    }

    private static String presentBlock(InvoiceSnapshot d, boolean paid) {
        String paidNote = paid
                ? "Hóa đơn này đã đóng dấu điện tử ĐÃ THANH TOÁN. Khi tập trung, xuất trình cùng giấy tờ tùy thân là đủ để HDV điểm danh."
                : "Phiếu này xác nhận đã giữ chỗ. Nếu chưa thanh toán, nhân viên có thể yêu cầu hoàn tất trên app trước khi lên xe.";
        return sectionTitle("7. Hướng dẫn xuất trình giấy tờ")
                + "<p style=\"margin:0 0 12px;font-size:14px;line-height:1.7;color:#334155;\">" + paidNote + "</p>"
                + "<p style=\"margin:0 0 8px;font-size:14px;font-weight:700;color:#0f172a;\">Mang theo khi tập trung</p>"
                + "<ul style=\"margin:0 0 14px;padding-left:20px;color:#334155;font-size:14px;line-height:1.8;\">"
                + "<li>Email này (màn hình điện thoại) hoặc bản in, thấy rõ mã <strong>"
                + MailService.escape(d.bookingCode()) + "</strong>.</li>"
                + "<li>CCCD/CMND còn hạn đối với tour nội địa (người lớn). Trẻ em theo giấy khai sinh hoặc giấy tờ tùy thân.</li>"
                + "<li>Hộ chiếu còn hạn ít nhất 6 tháng sau ngày về đối với tour quốc tế, kèm visa nếu chương trình yêu cầu.</li>"
                + "<li>Thẻ bảo hiểm y tế / bảo hiểm du lịch (nếu tự mua thêm) và danh sách thuốc đang dùng.</li>"
                + "<li>Số điện thoại liên hệ chuyến: " + MailService.escape(blank(d.contactPhone(), d.customerPhone())) + ".</li>"
                + "</ul>"
                + "<p style=\"margin:0 0 8px;font-size:14px;font-weight:700;color:#0f172a;\">Không cần mang</p>"
                + "<ul style=\"margin:0 0 20px;padding-left:20px;color:#334155;font-size:14px;line-height:1.8;\">"
                + "<li>Biên lai chuyển khoản giấy nếu cổng PayOS/MoMo đã báo thành công trên hệ thống.</li>"
                + "<li>Hóa đơn VAT (trừ khi công ty Quý khách đã yêu cầu xuất trước).</li>"
                + "<li>Toàn bộ lịch trình in màu — HDV có bản điều hành.</li>"
                + "</ul>";
    }

    private static String policyBlock(InvoiceSnapshot d) {
        String policyUrl = StringUtils.hasText(d.cancellationPolicyUrl()) ? d.cancellationPolicyUrl() : d.website();
        return sectionTitle("8. Chính sách hủy, đổi ngày và hoàn tiền")
                + "<p style=\"margin:0 0 12px;font-size:14px;line-height:1.7;color:#334155;\">"
                + "Mức hoàn phụ thuộc thời điểm gửi yêu cầu so với ngày khởi hành "
                + MailService.escape(blank(d.startDate(), "(xem trên đơn)"))
                + ". Yêu cầu hủy đơn đã thanh toán phải có lý do rõ ràng; admin Flourish Travel xác nhận rồi hệ thống hoàn qua cổng đã thu (PayOS chi hộ khi đủ điều kiện).</p>"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"border-collapse:collapse;margin:0 0 12px;font-size:13px;\">"
                + "<tr style=\"background:#f8fafc;\">" + th("Thời điểm hủy") + th("Mức xử lý") + "</tr>"
                + "<tr>" + td("Trước 30 ngày khởi hành") + td("Hoàn tối đa 100% tiền đủ điều kiện, có thể trừ phí dịch vụ nhỏ") + "</tr>"
                + "<tr>" + td("Từ 15–29 ngày trước khởi hành") + td("Hoàn khoảng 50% phần đủ điều kiện; phí hủy 50%") + "</tr>"
                + "<tr>" + td("Dưới 15 ngày trước khởi hành") + td("Không hoàn tiền cọc/dịch vụ đã đặt khách sạn – xe – vé") + "</tr>"
                + "<tr>" + td("Bất khả kháng (bão, dịch, cấm bay…)") + td("Hỗ trợ hoàn 100% hoặc bảo lưu 6–12 tháng, theo hồ sơ") + "</tr>"
                + "</table>"
                + "<p style=\"margin:0 0 12px;font-size:14px;line-height:1.7;color:#334155;\">"
                + "Đổi ngày khởi hành: gửi trước ít nhất 7 ngày, còn chỗ ở lịch mới, chênh giá tour (nếu có) được thu thêm. "
                + "Đơn chờ thanh toán Quý khách tự hủy trên tài khoản. Đơn đã trả: vào chi tiết đơn chọn yêu cầu hoàn tiền.</p>"
                + "<p style=\"margin:0 0 20px;font-size:13px;line-height:1.65;color:#64748b;\">Chi tiết đầy đủ: "
                + "<a href=\"" + MailService.escape(blank(policyUrl, "#")) + "\" style=\"color:#047857;\">Chính sách hủy / hoàn Flourish Travel</a>.</p>";
    }

    private static String packingBlock(InvoiceSnapshot d) {
        return sectionTitle("9. Chuẩn bị hành lý, sức khỏe và an toàn")
                + "<p style=\"margin:0 0 12px;font-size:14px;line-height:1.7;color:#334155;\">"
                + "Hãy chuẩn bị trước để buổi tập trung gọn, không chậm giờ xe. "
                + "Thời lượng tour: " + MailService.escape(blank(d.duration(), "theo chương trình"))
                + ", điểm đến " + MailService.escape(blank(d.destination(), "theo tour")) + ".</p>"
                + "<ul style=\"margin:0 0 20px;padding-left:20px;color:#334155;font-size:14px;line-height:1.8;\">"
                + "<li>Vali/balo gọn, hành lý xách tay có giấy tờ, sạc điện thoại, thuốc uống hằng ngày.</li>"
                + "<li>Trang phục lịch sự khi vào đền chùa / cửa khẩu; giày đi bộ nếu lịch có trekking nhẹ.</li>"
                + "<li>Thông báo HDV nếu có dị ứng thực phẩm, bệnh nền, thai kỳ — ghi chú đặc biệt trên đơn: "
                + MailService.escape(blank(d.specialRequests(), "không có")) + ".</li>"
                + "<li>Không mang vật phẩm cấm theo luật Việt Nam và nước đến (nếu tour quốc tế).</li>"
                + "<li>Giữ số liên hệ khẩn cấp: " + MailService.escape(emergency(d)) + ".</li>"
                + "<li>Phòng chat đoàn trên website/app (Flora và HDV) mở sau khi đơn được thanh toán.</li>"
                + "</ul>";
    }

    private static String supportBlock(InvoiceSnapshot d) {
        return sectionTitle("10. Hỗ trợ trước và trong chuyến đi")
                + "<p style=\"margin:0 0 12px;font-size:14px;line-height:1.7;color:#334155;\">"
                + "Bộ phận điều hành đọc email này cùng mã đơn của Quý khách. Khi gọi hotline, đọc giúp mã "
                + "<strong>" + MailService.escape(d.bookingCode()) + "</strong> và ngày khởi hành "
                + MailService.escape(blank(d.startDate(), "trên hóa đơn")) + " để khỏi mất thời gian đối soát.</p>"
                + "<ul style=\"margin:0 0 20px;padding-left:20px;color:#334155;font-size:14px;line-height:1.8;\">"
                + "<li>Email: " + MailService.escape(blank(d.supportEmail(), "theo website")) + "</li>"
                + "<li>Hotline: " + MailService.escape(blank(d.hotline(), "Xem trên website Flourish Travel")) + "</li>"
                + "<li>Website: " + MailService.escape(blank(d.website(), "https://flourishtravelapp.khanhtn45.id.vn")) + "</li>"
                + "<li>Trang đơn của bạn: " + MailService.escape(blank(d.bookingUrl(), "Đăng nhập mục Chuyến đi của tôi")) + "</li>"
                + "</ul>";
    }

    private static String legalBlock(InvoiceSnapshot d, boolean paid) {
        return sectionTitle("11. Cam kết và lưu ý pháp lý")
                + "<p style=\"margin:0 0 12px;font-size:14px;line-height:1.75;color:#334155;\">"
                + "Bằng việc hoàn tất đặt chỗ, Quý khách xác nhận thông tin khách trên hóa đơn là đúng, đã đọc chính sách hủy hoàn, "
                + "và đồng ý để Flourish Travel dùng dữ liệu liên hệ / giấy tờ (đã mã hóa trên hệ thống) phục vụ đặt dịch vụ, bảo hiểm và xuất trình với đối tác vận chuyển – lưu trú. "
                + "Chúng tôi không gửi CCCD/hộ chiếu đầy đủ qua email.</p>"
                + "<p style=\"margin:0 0 12px;font-size:14px;line-height:1.75;color:#334155;\">"
                + (paid
                ? "Chứng từ này xác nhận nghĩa vụ thanh toán của hóa đơn đặt tour đã được thực hiện. "
                : "Chứng từ này xác nhận Flourish Travel đã ghi nhận yêu cầu đặt chỗ; nghĩa vụ thanh toán vẫn còn cho đến khi cổng thanh toán báo thành công. ")
                + "Mọi chỉnh sửa danh sách khách sau khi xuất hóa đơn phải được điều hành chấp nhận bằng văn bản (email hoặc ghi chú trên đơn).</p>"
                + "<p style=\"margin:0 0 20px;font-size:13px;line-height:1.7;color:#64748b;\">"
                + "Flourish Travel — thương hiệu du lịch. Email tự động, vui lòng không trả lời với mã độc / link lạ. "
                + "Nếu không phải người đặt " + MailService.escape(blank(d.customerEmail(), ""))
                + ", hãy bỏ qua và thông báo cho chúng tôi. © 2026 Flourish Travel.</p>";
    }

    private static String wrap(String eyebrow, String inner, InvoiceSnapshot d) {
        String site = MailService.escape(blank(d.website(), "—"));
        String mail = MailService.escape(blank(d.supportEmail(), "—"));
        String phone = MailService.escape(blank(d.hotline(), "Xem trên website"));
        return "<!DOCTYPE html><html lang=\"vi\"><body style=\"margin:0;padding:0;background:#ecfdf5;font-family:Arial,Helvetica,sans-serif;\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"background:#ecfdf5;padding:24px 12px;\">"
                + "<tr><td align=\"center\">"
                + "<table role=\"presentation\" width=\"640\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:640px;width:100%;background:#ffffff;border-radius:16px;overflow:hidden;border:1px solid #d1fae5;\">"
                + "<tr><td style=\"background:linear-gradient(135deg,#059669,#0d9488);padding:28px 28px 22px;text-align:center;\">"
                + "<img src=\"cid:" + GuideMailTemplates.LOGO_CID + "\" alt=\"Flourish Travel\" width=\"96\" "
                + "style=\"display:inline-block;border:0;border-radius:18px;background:#fff;padding:6px;\">"
                + "<p style=\"margin:14px 0 0;font-size:22px;font-weight:700;color:#ffffff;\">Flourish Travel</p>"
                + "<p style=\"margin:4px 0 0;font-size:13px;color:#d1fae5;\">" + MailService.escape(eyebrow) + "</p>"
                + "</td></tr>"
                + "<tr><td style=\"padding:28px 28px 8px;\">" + inner + "</td></tr>"
                + "<tr><td style=\"padding:8px 28px 28px;\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"background:#f0fdf4;border:1px solid #bbf7d0;border-radius:12px;\">"
                + "<tr><td style=\"padding:18px 20px;\">"
                + "<p style=\"margin:0 0 10px;font-size:13px;font-weight:700;color:#047857;\">Liên hệ Flourish Travel</p>"
                + contactLine("Website", site)
                + contactLine("Email", mail)
                + contactLine("Hotline", phone)
                + "<p style=\"margin:12px 0 0;font-size:12px;line-height:1.5;color:#64748b;\">Giữ email này đến hết chuyến đi.</p>"
                + "</td></tr></table></td></tr>"
                + "<tr><td style=\"padding:16px 28px 24px;text-align:center;background:#f8fafc;border-top:1px solid #e2e8f0;\">"
                + "<p style=\"margin:0;font-size:12px;color:#94a3b8;\">© 2026 Flourish Travel · Hóa đơn / phiếu xác nhận đặt tour gửi tự động</p>"
                + "</td></tr></table></td></tr></table></body></html>";
    }

    private static String sectionTitle(String title) {
        return "<p style=\"margin:22px 0 10px;font-size:16px;font-weight:800;color:#0f172a;\">"
                + MailService.escape(title) + "</p>";
    }

    private static String box(String rows) {
        return "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"background:#f8fafc;border:1px solid #e2e8f0;border-radius:12px;margin:0 0 8px;\">"
                + "<tr><td style=\"padding:16px 18px;\">" + rows + "</td></tr></table>";
    }

    private static String row(String label, String value) {
        return "<p style=\"margin:0 0 8px;font-size:14px;color:#475569;\"><span style=\"color:#64748b;\">"
                + MailService.escape(label) + ": </span><strong style=\"color:#0f172a;\">"
                + MailService.escape(blank(value, "—")) + "</strong></p>";
    }

    private static String th(String t) {
        return "<th align=\"left\" style=\"padding:8px 6px;border-bottom:1px solid #e2e8f0;color:#64748b;font-weight:700;\">"
                + MailService.escape(t) + "</th>";
    }

    private static String td(String t) {
        return "<td style=\"padding:8px 6px;border-bottom:1px solid #f1f5f9;color:#0f172a;vertical-align:top;\">"
                + MailService.escape(blank(t, "—")) + "</td>";
    }

    private static String contactLine(String label, String value) {
        return "<p style=\"margin:0 0 6px;font-size:13px;color:#334155;\"><strong>" + label + ":</strong> " + value + "</p>";
    }

    private static String cta(String href, String label) {
        String safeHref = MailService.escape(blank(href, "#"));
        return "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin:8px 0 16px;\"><tr><td "
                + "style=\"background:#059669;border-radius:10px;\">"
                + "<a href=\"" + safeHref + "\" style=\"display:inline-block;padding:12px 22px;color:#ffffff;"
                + "text-decoration:none;font-weight:700;font-size:14px;\">" + MailService.escape(label) + "</a>"
                + "</td></tr></table>";
    }

    private static String emergency(InvoiceSnapshot d) {
        String name = blank(d.emergencyName(), "");
        String phone = blank(d.emergencyPhone(), "");
        if (!StringUtils.hasText(name) && !StringUtils.hasText(phone)) {
            return "Chưa cung cấp";
        }
        if (StringUtils.hasText(name) && StringUtils.hasText(phone)) {
            return name + " · " + phone;
        }
        return StringUtils.hasText(name) ? name : phone;
    }

    private static String shortId(String id) {
        if (id == null || id.isBlank()) return "—";
        return id.length() <= 8 ? id : id.substring(0, 8);
    }

    private static String blank(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }
}
