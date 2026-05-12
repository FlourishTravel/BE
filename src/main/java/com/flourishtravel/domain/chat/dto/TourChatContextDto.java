package com.flourishtravel.domain.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Thông tin phòng chat theo đơn đặt tour — dùng cho màn hình chat của khách.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TourChatContextDto {

    private UUID bookingId;
    private UUID sessionId;
    /** null nếu chưa có phòng (lịch cũ trước khi có chat) */
    private UUID roomId;
    private String roomName;
    private String tourTitle;
    private LocalDate sessionStartDate;
    private LocalDate sessionEndDate;
    private String bookingStatus;
    private String guideName;

    /** true khi khách được vào phòng (đã đặt thành công + đã tham gia room). */
    private boolean canChat;
    /** Lý do không mở chat (hiển thị cho user). */
    private String denyReason;
}
