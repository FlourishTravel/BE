package com.flourishtravel.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.n8n")
public class N8nProperties {

    /** Production Webhook URL từ n8n workflow. Ví dụ: https://khanhtn45.app.n8n.cloud/webhook/abc123 */
    private String webhookUrl;

    /** Timeout (giây) cho HTTP call đến n8n */
    private int timeoutSeconds = 15;

    /** true = dùng n8n; false = fallback về logic Gemini/OpenAI cũ */
    private boolean enabled = true;
}
