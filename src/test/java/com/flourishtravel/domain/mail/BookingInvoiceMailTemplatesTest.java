package com.flourishtravel.domain.mail;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class BookingInvoiceMailTemplatesTest {

    @Test
    void pendingInvoiceIsLongAndHasPresentationInfo() {
        String html = BookingInvoiceMailTemplates.build(sample(false));
        assertTrue(html.length() > 8000, "mail phải đủ dài, actual=" + html.length());
        assertTrue(html.contains("cid:flourish-logo"));
        assertTrue(html.contains("FT-A1B2C3D4"));
        assertTrue(html.contains("CHỜ THANH TOÁN"));
        assertTrue(html.contains("Nguyễn Văn A"));
        assertTrue(html.contains("Bangkok"));
        assertTrue(html.contains("Xuất trình"));
        assertTrue(html.contains("Trước 30 ngày khởi hành"));
        assertTrue(html.contains("Thanh toán đơn này ngay"));
        assertTrue(html.contains("Hành trình độc bản"));
        assertTrue(html.contains("CCCD ***1234"));
    }

    @Test
    void paidInvoiceMarksPaidAndDropsPayCta() {
        String html = BookingInvoiceMailTemplates.build(sample(true));
        assertTrue(html.length() > 8000);
        assertTrue(html.contains("ĐÃ THANH TOÁN"));
        assertTrue(html.contains("Hóa đơn xác nhận"));
        assertTrue(!html.contains("Thanh toán đơn này ngay"));
        assertTrue(html.contains("Xem chi tiết đơn trên Flourish Travel"));
    }

    private static BookingInvoiceMailTemplates.InvoiceSnapshot sample(boolean paid) {
        return new BookingInvoiceMailTemplates.InvoiceSnapshot(
                "Lan Anh",
                "lananh@example.com",
                "0901234567",
                "FT-A1B2C3D4",
                "a1b2c3d4-0000-0000-0000-000000000001",
                "15/08/2026 10:30 (GMT+7)",
                paid,
                paid ? "Đã thanh toán đủ" : "Chờ thanh toán",
                "PayOS",
                "FT-A1B2C3D4",
                paid ? "TX-99" : null,
                "https://pay.payos.vn/web/demo",
                "Bangkok – Hành trình độc bản",
                "Bangkok",
                "4 ngày / 3 đêm",
                "Châu Á",
                "Tour quốc tế",
                "30/08/2026 – 02/09/2026",
                "30/08/2026",
                "02/09/2026",
                2,
                "6.900.000 VND",
                "13.800.000 VND",
                "0 VND",
                "13.800.000 VND",
                paid ? "13.800.000 VND" : "0 VND",
                paid ? "0 VND" : "13.800.000 VND",
                null,
                "0901234567",
                "Sân bay Tân Sơn Nhất, cổng 2",
                "Phòng không hút thuốc",
                "Nguyễn Văn B",
                "0912345678",
                "Hướng dẫn viên Mai",
                "https://flourishtravelapp.khanhtn45.id.vn/my-journey/booking/a1b2c3d4-0000-0000-0000-000000000001",
                "https://flourishtravelapp.khanhtn45.id.vn/cancellation-policy",
                "https://flourishtravelapp.khanhtn45.id.vn",
                "support@flourish.vn",
                "0901 234 567",
                List.of(new BookingInvoiceMailTemplates.GuestLine("Nguyễn Văn A", "12/03/1990", "CCCD ***1234", "Việt Nam"),
                        new BookingInvoiceMailTemplates.GuestLine("Nguyễn Thị C", "01/01/2018", "Hộ chiếu ***8888", "Việt Nam")),
                List.of(new BookingInvoiceMailTemplates.DayLine("Ngày 1 · Đến Bangkok", "Đón sân bay, nhận phòng, chợ đêm."),
                        new BookingInvoiceMailTemplates.DayLine("Ngày 2 · Ayutthaya", "Cố đô, chùa, thuyền.")),
                "Temple, chợ, ẩm thực đường phố",
                "Khách sạn 4*, xe, HDV, một phần ăn",
                "Visa, tip, đồ uống"
        );
    }
}
