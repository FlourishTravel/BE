package com.flourishtravel.domain.upload.controller;

import com.flourishtravel.common.dto.ApiResponse;
import com.flourishtravel.domain.upload.service.UploadStorageService;
import com.flourishtravel.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/upload")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Upload", description = "Tải ảnh hoặc video, nhận URL công khai")
public class UploadController {

    private final UploadStorageService uploadStorageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
            summary = "Upload ảnh / video",
            description = "multipart field `file`. Trả về URL (local `/uploads/...` hoặc S3/CDN nếu bật). Nhấn **Authorize** và dán JWT trước khi thử.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    public ResponseEntity<ApiResponse<String>> upload(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(
                    description = "Chọn tệp ảnh (image/*) hoặc video (video/*)",
                    required = true,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_OCTET_STREAM_VALUE,
                            schema = @Schema(type = "string", format = "binary")
                    )
            )
            @RequestParam("file") MultipartFile file) {
        if (principal == null) {
            return ResponseEntity.status(401).build();
        }
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Chọn file để tải lên"));
        }
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.startsWith("image/") && !contentType.startsWith("video/"))) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Chỉ chấp nhận ảnh hoặc video"));
        }
        try {
            String url = uploadStorageService.store(file);
            return ResponseEntity.ok(ApiResponse.ok("Tải lên thành công", url));
        } catch (IOException e) {
            log.error("Upload failed", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Lỗi tải file"));
        }
    }
}
