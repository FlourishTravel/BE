package com.flourishtravel.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@ConditionalOnProperty(name = "app.upload.storage", havingValue = "s3")
public class S3ClientConfig {

    @Bean
    public S3Client s3Client(
            @Value("${app.s3.region:ap-southeast-1}") String region,
            @Value("${app.s3.upload-access-key-id:}") String uploadAccessKeyId,
            @Value("${app.s3.upload-secret-access-key:}") String uploadSecretAccessKey) {
        AwsCredentialsProvider credentials = resolveCredentials(uploadAccessKeyId, uploadSecretAccessKey);
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(credentials)
                .build();
    }

    private static AwsCredentialsProvider resolveCredentials(String accessKeyId, String secretAccessKey) {
        if (accessKeyId != null && !accessKeyId.isBlank()
                && secretAccessKey != null && !secretAccessKey.isBlank()) {
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKeyId.trim(), secretAccessKey.trim()));
        }
        return DefaultCredentialsProvider.create();
    }
}
