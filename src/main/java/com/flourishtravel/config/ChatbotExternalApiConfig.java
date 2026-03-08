package com.flourishtravel.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * WebClient cho API bên ngoài phục vụ chatbot: Open-Meteo (thời tiết), Overpass (địa điểm).
 */
@Configuration
public class ChatbotExternalApiConfig {

    @Bean("openMeteoWebClient")
    public WebClient openMeteoWebClient(WebClient.Builder builder) {
        return builder.build();
    }

    @Bean("overpassWebClient")
    public WebClient overpassWebClient(WebClient.Builder builder) {
        return builder.build();
    }
}
