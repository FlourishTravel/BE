package com.flourishtravel.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Spring Boot không đọc file .env — nạp vào {@link System#setProperty} trước khi chạy app
 * để {@code ${MOMO_PARTNER_CODE}} v.v. có giá trị khi dùng BE/.env.
 * Không ghi đè biến môi trường đã set (Docker, IDE Env).
 * <p>
 * Trùng key trong cùng file: giá trị <strong>cuối cùng</strong> được dùng (dotenv-java 3.x ném
 * {@link IllegalStateException} nên không dùng thư viện đó nữa).
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
            try {
                Map<String, String> entries = parseEnvFile(envFile);
                entries.forEach(DotEnvBootstrap::applyEntry);
            } catch (IOException e) {
                throw new IllegalStateException("Không đọc được .env: " + envFile.toAbsolutePath(), e);
            }
            return;
        }
    }

    /**
     * Dòng trùng key: {@link LinkedHashMap#put} ghi đè → giữ giá trị cuối trong file.
     */
    private static Map<String, String> parseEnvFile(Path path) throws IOException {
        Map<String, String> map = new LinkedHashMap<>();
        for (String rawLine : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (line.startsWith("export ")) {
                line = line.substring(7).trim();
            }
            int eq = line.indexOf('=');
            if (eq <= 0) {
                continue;
            }
            String key = line.substring(0, eq).trim();
            if (key.isEmpty() || key.startsWith("#")) {
                continue;
            }
            String value = unquote(line.substring(eq + 1).trim());
            map.put(key, value);
        }
        return map;
    }

    private static String unquote(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        if (value.length() >= 2 && value.startsWith("'") && value.endsWith("'")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static void applyEntry(String key, String val) {
        if (key == null || key.isBlank()) {
            return;
        }
        if (System.getenv(key) != null) {
            return;
        }
        if (System.getProperty(key) != null) {
            return;
        }
        if (val != null) {
            System.setProperty(key, val);
            mirrorAwsCredentialsToSdkSystemProperties(key, val);
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
