package com.flourishtravel.domain.upload.controller;

import com.flourishtravel.common.dto.ApiResponse;
import com.flourishtravel.security.UserPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@RestController
@RequestMapping("/upload")
@Slf4j
public class UploadController {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.api-base-url:http://localhost:8080/api}")
    private String apiBaseUrl;

    @PostMapping
    public ResponseEntity<ApiResponse<String>> upload(
            @AuthenticationPrincipal UserPrincipal principal,
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
            Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            String ext = contentType.split("/")[1];
            if (ext.equals("jpeg")) ext = "jpg";
            String filename = UUID.randomUUID().toString() + "." + ext;
            Path target = dir.resolve(filename);
            Files.copy(file.getInputStream(), target);
            String url = apiBaseUrl + "/uploads/" + filename;
            return ResponseEntity.ok(ApiResponse.ok("Tải lên thành công", url));
        } catch (IOException e) {
            log.error("Upload failed", e);
            return ResponseEntity.internalServerError().body(ApiResponse.error("Lỗi tải file"));
        }
    }
}
