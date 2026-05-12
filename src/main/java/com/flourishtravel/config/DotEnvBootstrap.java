package com.flourishtravel.config;

import io.github.cdimascio.dotenv.Dotenv;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Spring Boot không đọc file .env — nạp vào {@link System#setProperty} trước khi chạy app
 * để {@code ${MOMO_PARTNER_CODE}} v.v. có giá trị khi dùng BE/.env.
 * Không ghi đè biến môi trường đã set (Docker, IDE Env).
 */
public final class DotEnvBootstrap {

    private DotEnvBootstrap() {}

    public static void load() {
        Path cwd = Paths.get(System.getProperty("user.dir", ".")).normalize();
        Path[] candidates = {
                cwd.resolve(".env"),
                cwd.resolve("BE").resolve(".env"),
                cwd.getParent() != null ? cwd.getParent().resolve("BE").resolve(".env") : null,
        };
        for (Path envFile : candidates) {
            if (envFile == null || !Files.isRegularFile(envFile)) {
                continue;
            }
            Path dir = envFile.getParent();
            if (dir == null) {
                continue;
            }
            Dotenv dotenv = Dotenv.configure().directory(dir.toString()).load();
            dotenv.entries().forEach(e -> {
                String key = e.getKey();
                if (key == null || key.isBlank()) {
                    return;
                }
                if (System.getenv(key) != null) {
                    return;
                }
                if (System.getProperty(key) != null) {
                    return;
                }
                String val = e.getValue();
                if (val != null) {
                    System.setProperty(key, val);
                    mirrorAwsCredentialsToSdkSystemProperties(key, val);
                }
            });
            return;
        }
    }

    /**
     * {@link software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider} đọc env
     * {@code AWS_ACCESS_KEY_ID} hoặc system property {@code aws.accessKeyId} (không đọc
     * {@code AWS_ACCESS_KEY_ID} dạng property). File .env chỉ nạp qua {@link System#setProperty}
     * nên cần map sang tên SDK dùng. {@code S3_UPLOAD_*} cũng map tương tự khi dùng default chain.
     */
    private static void mirrorAwsCredentialsToSdkSystemProperties(String key, String val) {
        if (val.isBlank()) {
            return;
        }
        switch (key) {
            case "AWS_ACCESS_KEY_ID" -> {
                if (System.getProperty("aws.accessKeyId") == null) {
                    System.setProperty("aws.accessKeyId", val);
                }
            }
            case "AWS_SECRET_ACCESS_KEY" -> {
                if (System.getProperty("aws.secretAccessKey") == null) {
                    System.setProperty("aws.secretAccessKey", val);
                }
            }
            case "AWS_SESSION_TOKEN" -> {
                if (System.getProperty("aws.sessionToken") == null) {
                    System.setProperty("aws.sessionToken", val);
                }
            }
            case "S3_UPLOAD_ACCESS_KEY_ID" -> {
                if (System.getProperty("aws.accessKeyId") == null) {
                    System.setProperty("aws.accessKeyId", val);
                }
            }
            case "S3_UPLOAD_SECRET_ACCESS_KEY" -> {
                if (System.getProperty("aws.secretAccessKey") == null) {
                    System.setProperty("aws.secretAccessKey", val);
                }
            }
            default -> {
                // no-op
            }
        }
    }
}
