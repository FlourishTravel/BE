package com.flourishtravel.domain.upload.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.upload.storage", havingValue = "local", matchIfMissing = true)
public class LocalUploadStorageService implements UploadStorageService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${app.api-base-url:http://localhost:8080}")
    private String apiBaseUrl;

    @Override
    public String store(MultipartFile file) throws IOException {
        Path dir = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(dir);
        String contentType = file.getContentType();
        String ext = contentType != null && contentType.contains("/")
                ? contentType.split("/")[1]
                : "bin";
        if ("jpeg".equals(ext)) {
            ext = "jpg";
        }
        String filename = UUID.randomUUID() + "." + ext;
        Path target = dir.resolve(filename);
        Files.copy(file.getInputStream(), target);
        String base = apiBaseUrl.endsWith("/") ? apiBaseUrl.substring(0, apiBaseUrl.length() - 1) : apiBaseUrl;
        return base + "/uploads/" + filename;
    }
}
