package com.flourishtravel.domain.upload.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.UUID;

@Service
@ConditionalOnProperty(name = "app.upload.storage", havingValue = "s3")
public class S3UploadStorageService implements UploadStorageService {

    private final S3Client s3Client;

    @Value("${app.s3.bucket}")
    private String bucket;

    @Value("${app.s3.region:ap-southeast-1}")
    private String region;

    /**
     * Tuỳ chọn: URL gốc công khai (CloudFront hoặc custom domain trỏ bucket).
     * Để trống thì dùng URL virtual-hosted mặc định của S3.
     */
    @Value("${app.s3.public-url-prefix:}")
    private String publicUrlPrefix;

    public S3UploadStorageService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public String store(MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }
        String ext = contentType.contains("/") ? contentType.split("/")[1] : "bin";
        if ("jpeg".equals(ext)) {
            ext = "jpg";
        }
        String key = "uploads/" + UUID.randomUUID() + "." + ext;

        PutObjectRequest put = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();
        s3Client.putObject(put, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        if (publicUrlPrefix != null && !publicUrlPrefix.isBlank()) {
            String base = publicUrlPrefix.endsWith("/")
                    ? publicUrlPrefix.substring(0, publicUrlPrefix.length() - 1)
                    : publicUrlPrefix;
            return base + "/" + key;
        }
        return s3Client.utilities()
                .getUrl(GetUrlRequest.builder()
                        .bucket(bucket)
                        .key(key)
                        .region(Region.of(region))
                        .build())
                .toExternalForm();
    }
}
