package com.flourishtravel.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;

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

    @Bean("vietMapRestClient")
    public RestClient vietMapRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(8).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(15).toMillis());
        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }
}
