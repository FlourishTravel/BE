package com.flourishtravel.config;

import com.flourishtravel.common.converter.PiiEncryptionConverter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

@Configuration
@Slf4j
public class PiiEncryptionConfig {

    @Value("${app.encryption.pii-key:}")
    private String piiKey;

    @PostConstruct
    public void init() {
        PiiEncryptionConverter.setKeyFromBase64(piiKey);
        if (piiKey == null || piiKey.isBlank()) {
            log.info("PII encryption key not set – CCCD/CMND will be stored in plaintext. Set app.encryption.pii-key for encryption.");
        } else {
            log.info("PII encryption enabled for sensitive fields (CCCD/CMND).");
        }
    }
}
