package com.hackathon.skinroutine.config;

import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * 외부 HTTP 클라이언트 2개 (OpenAI · Supabase Storage).
 * 타임아웃 명시가 핵심 — #3 기록 API의 응답 예산이 5~10초라, 외부 호출이 무한정 매달리면 안 된다.
 * 타임아웃·오류는 서비스 레이어에서 폴백으로 흡수한다.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient openAiRestClient(AppProperties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(25)); // 비전 호출 여유분 — 초과 시 폴백이 받는다
        return RestClient.builder()
                .baseUrl(props.openai().baseUrl())
                .requestFactory(factory)
                .build();
    }

    @Bean
    public RestClient supabaseRestClient(AppProperties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(15));
        // 미설정이면 이 빈은 실제로 호출되지 않는다 (SupabaseStorageService가 설정 여부를 먼저 확인)
        String baseUrl = (props.supabase().url() == null || props.supabase().url().isBlank())
                ? "http://localhost" : props.supabase().url();
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }
}
