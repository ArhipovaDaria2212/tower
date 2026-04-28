package ru.arkhipova.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(
                List.of("http://localhost:3000", "http://localhost:63342", "http://127.0.0.1:3000", "null"));

        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        configuration.setAllowedHeaders(List.of("*"));

        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }

    //    @Bean
    //    public CorsFilter corsFilter() {
    //        CorsConfiguration config = new CorsConfiguration();
    //        config.setAllowCredentials(true);
    //
    //        config.addAllowedOrigin("*");
    //        config.addAllowedHeader("*");
    //        config.addAllowedMethod("*");
    //
    //        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    //
    //        source.registerCorsConfiguration("/**", config);
    //        return new CorsFilter(source);
    //    }
}
