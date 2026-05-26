package com.flourishtravel.config;

import com.flourishtravel.security.JwtAuthenticationFilter;
import com.flourishtravel.security.JwtEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtEntryPoint jwtEntryPoint;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(cs -> cs.disable())
            .cors(cors -> {})
            .headers(h -> h.frameOptions(f -> f.sameOrigin()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(e -> e.authenticationEntryPoint(jwtEntryPoint))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/chatbot-demo.html", "/upload-test.html").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/destinations", "/destinations/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/destinations/flora-match").permitAll()
                .requestMatchers(HttpMethod.POST, "/planner/generate", "/planner/calculate-budget").permitAll()
                .requestMatchers(HttpMethod.GET, "/planner/suggestions").permitAll()
                .requestMatchers(HttpMethod.GET, "/tours", "/tours/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/catalog", "/catalog/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/catalog/flora-recommend").permitAll()
                .requestMatchers(HttpMethod.GET, "/categories").permitAll()
                .requestMatchers("/chatbot/message").permitAll()
                .requestMatchers("/chatbot/config", "/chatbot/config/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/chatbot/nearby-places", "/chatbot/weather-forecast").permitAll()
                .requestMatchers("/payments/momo/ipn").permitAll()
                .requestMatchers(HttpMethod.POST, "/bookings/validate-session").permitAll()
                .requestMatchers("/contact-requests").permitAll()
                .requestMatchers("/uploads/**").permitAll()
                .requestMatchers("/ws/**").authenticated()
                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers("/guide/**").hasAnyRole("ADMIN", "TOUR_GUIDE")
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
