package com.flourishtravel.common.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Mã hóa dữ liệu nhạy cảm (CCCD/CMND) khi lưu DB. Giải mã khi đọc.
 * Dùng AES-256-GCM; key được set từ PiiEncryptionConfig (app.encryption.pii-key, base64 32 bytes).
 */
@Converter
@Slf4j
public class PiiEncryptionConverter implements AttributeConverter<String, String> {

    private static final String PREFIX = "ENC:";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final String ALGORITHM = "AES/GCM/NoPadding";

    private static volatile byte[] keyBytes;

    public static void setKeyFromBase64(String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            keyBytes = null;
            return;
        }
        try {
            byte[] key = Base64.getDecoder().decode(base64Key.trim());
            if (key.length != 32) {
                log.warn("app.encryption.pii-key must be 32 bytes (base64)");
                keyBytes = null;
                return;
            }
            keyBytes = key;
        } catch (Exception e) {
            log.warn("Invalid app.encryption.pii-key", e);
            keyBytes = null;
        }
    }

    private static byte[] getKey() {
        return keyBytes;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null || attribute.isBlank()) return attribute;
        byte[] key = PiiEncryptionConverter.getKey();
        if (key == null) return attribute;
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), spec);
            byte[] encrypted = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            log.error("PII encryption failed", e);
            return attribute;
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank() || !dbData.startsWith(PREFIX)) return dbData;
        byte[] key = PiiEncryptionConverter.getKey();
        if (key == null) return dbData;
        try {
            byte[] combined = Base64.getDecoder().decode(dbData.substring(PREFIX.length()));
            if (combined.length < GCM_IV_LENGTH) return dbData;
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH);
            byte[] encrypted = new byte[combined.length - GCM_IV_LENGTH];
            System.arraycopy(combined, GCM_IV_LENGTH, encrypted, 0, encrypted.length);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), spec);
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("PII decryption failed", e);
            return dbData;
        }
    }
}
