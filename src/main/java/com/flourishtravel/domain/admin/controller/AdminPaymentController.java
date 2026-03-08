package com.flourishtravel.domain.admin.controller;

import com.flourishtravel.common.dto.ApiResponse;
import com.flourishtravel.domain.payment.entity.Payment;
import com.flourishtravel.domain.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminPaymentController {

    private final PaymentRepository paymentRepository;

    @GetMapping("/payments")
    public ResponseEntity<ApiResponse<Page<Payment>>> list(
            @RequestParam(required = false) String orderId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageRequest pr = PageRequest.of(page, size);
        if (orderId != null && !orderId.isBlank()) {
            var opt = paymentRepository.findByOrderId(orderId);
            if (opt.isPresent()) {
                Page<Payment> single = new PageImpl<>(List.of(opt.get()), pr, 1);
                return ResponseEntity.ok(ApiResponse.ok(single));
            }
            return ResponseEntity.ok(ApiResponse.ok(Page.empty(pr)));
        }
        if (status != null && !status.isBlank()) {
            Page<Payment> byStatus = paymentRepository.findByStatusOrderByCreatedAtDesc(status, pr);
            return ResponseEntity.ok(ApiResponse.ok(byStatus));
        }
        Page<Payment> all = paymentRepository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ResponseEntity.ok(ApiResponse.ok(all));
    }
}
