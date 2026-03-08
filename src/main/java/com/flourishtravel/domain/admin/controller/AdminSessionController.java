package com.flourishtravel.domain.admin.controller;

import com.flourishtravel.common.dto.ApiResponse;
import com.flourishtravel.common.exception.BadRequestException;
import com.flourishtravel.common.exception.ResourceNotFoundException;
import com.flourishtravel.domain.chat.entity.ChatRoom;
import com.flourishtravel.domain.chat.repository.ChatRoomRepository;
import com.flourishtravel.domain.tour.entity.Tour;
import com.flourishtravel.domain.tour.entity.TourSession;
import com.flourishtravel.domain.tour.repository.TourRepository;
import com.flourishtravel.domain.tour.repository.TourSessionRepository;
import com.flourishtravel.domain.user.entity.User;
import com.flourishtravel.domain.user.repository.UserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminSessionController {

    private final TourSessionRepository sessionRepository;
    private final TourRepository tourRepository;
    private final UserRepository userRepository;
    private final ChatRoomRepository chatRoomRepository;

    @PostMapping("/sessions")
    public ResponseEntity<ApiResponse<TourSession>> create(@RequestBody AdminSessionDto dto) {
        Tour tour = tourRepository.findById(dto.getTourId()).orElseThrow(() -> new ResourceNotFoundException("Tour", dto.getTourId()));
        User guide = dto.getTourGuideId() != null ? userRepository.findById(dto.getTourGuideId()).orElse(null) : null;
        if (dto.getStartDate() == null || dto.getEndDate() == null) {
            throw new BadRequestException("start_date và end_date là bắt buộc");
        }
        if (!dto.getEndDate().isAfter(dto.getStartDate()) && !dto.getEndDate().equals(dto.getStartDate())) {
            throw new BadRequestException("end_date phải sau hoặc bằng start_date");
        }
        TourSession session = TourSession.builder()
                .tour(tour)
                .startDate(dto.getStartDate())
                .endDate(dto.getEndDate())
                .maxParticipants(dto.getMaxParticipants() != null ? dto.getMaxParticipants() : 20)
                .currentParticipants(0)
                .tourGuide(guide)
                .status("scheduled")
                .build();
        session = sessionRepository.save(session);
        String roomName = tour.getTitle() + " - " + dto.getStartDate();
        ChatRoom room = ChatRoom.builder()
                .session(session)
                .roomName(roomName)
                .isActive(true)
                .build();
        chatRoomRepository.save(room);
        return ResponseEntity.ok(ApiResponse.ok("Đã tạo lịch khởi hành và phòng chat", session));
    }

    @PutMapping("/sessions/{id}")
    public ResponseEntity<ApiResponse<TourSession>> update(@PathVariable UUID id, @RequestBody AdminSessionDto dto) {
        TourSession session = sessionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Session", id));
        if (session.getCurrentParticipants() > 0) {
            throw new BadRequestException("Không thể sửa lịch đã có khách đặt");
        }
        if (dto.getStartDate() != null) session.setStartDate(dto.getStartDate());
        if (dto.getEndDate() != null) session.setEndDate(dto.getEndDate());
        if (dto.getMaxParticipants() != null) session.setMaxParticipants(dto.getMaxParticipants());
        if (dto.getTourGuideId() != null) {
            session.setTourGuide(userRepository.findById(dto.getTourGuideId()).orElse(null));
        }
        session = sessionRepository.save(session);
        return ResponseEntity.ok(ApiResponse.ok("Đã cập nhật lịch", session));
    }

    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        TourSession session = sessionRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Session", id));
        if (session.getCurrentParticipants() > 0) {
            throw new BadRequestException("Không thể xóa lịch đã có khách đặt");
        }
        chatRoomRepository.findBySession_Id(id).ifPresent(chatRoomRepository::delete);
        sessionRepository.delete(session);
        return ResponseEntity.ok(ApiResponse.ok("Đã xóa lịch", null));
    }

    @Data
    public static class AdminSessionDto {
        private UUID tourId;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate startDate;
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        private LocalDate endDate;
        private Integer maxParticipants;
        private UUID tourGuideId;
    }
}
