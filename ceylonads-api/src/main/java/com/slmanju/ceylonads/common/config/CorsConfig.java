package com.slmanju.ceylonads.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/api/**", configuration);

        return source;
    }

    // @Bean
    // CorsConfigurationSource corsConfigurationSource(
    //         @Value("${ceylonads.cors.allowed-origins}") List<String> allowedOrigins) {
    //     CorsConfiguration configuration = new CorsConfiguration();
    //     // Patterns (not setAllowedOrigins) so entries like "http://localhost:*" can match
    //     // whatever port Vite actually binds to when its default port is already taken.
    //     configuration.setAllowedOriginPatterns(allowedOrigins);
    //     configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    //     configuration.setAllowedHeaders(List.of("Content-Type", "Authorization"));
    //     configuration.setAllowCredentials(true);

    //     UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    //     source.registerCorsConfiguration("/api/**", configuration);
    //     return source;
    // }
}
