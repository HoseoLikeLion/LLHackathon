package com.hackathon.skinroutine.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS — 프론트/백이 레포·도메인 분리라 필수.
 * 기본은 * (개발 편의), 제출 전에 CORS_ALLOWED_ORIGINS 환경변수로 프론트 도메인만 남긴다.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AppProperties props;

    public WebConfig(AppProperties props) {
        this.props = props;
    }

    @Override
    public void addCorsMappings(@NonNull CorsRegistry registry) {
        String raw = (props.cors() == null || props.cors().allowedOrigins() == null
                || props.cors().allowedOrigins().isBlank()) ? "*" : props.cors().allowedOrigins();
        String[] origins = raw.split("\\s*,\\s*");
        registry.addMapping("/api/**")
                .allowedOriginPatterns(origins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(false) // 쿠키 안 씀 (X-User-Id 헤더 방식)
                .maxAge(3600);
    }
}
