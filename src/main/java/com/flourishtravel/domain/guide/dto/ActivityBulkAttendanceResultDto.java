package com.flourishtravel.domain.guide.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ActivityBulkAttendanceResultDto {
    private UUID activityId;
    /** Số người vừa được cập nhật trong lần gọi này. */
    private int updated;
    /** Số người đã ở đúng trạng thái, bỏ qua. */
    private int skippedAlready;
    private int totalParticipants;
    private int checkedInAtStopCount;
}
