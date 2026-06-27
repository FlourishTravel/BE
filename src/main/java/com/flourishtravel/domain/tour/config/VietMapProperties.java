package com.flourishtravel.domain.tour.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.vietmap")
public class VietMapProperties {

    /** API key từ https://maps.vietmap.vn/console — dùng Search v4 / Place v4 geocoding. */
    private String apiKey = "";
}
