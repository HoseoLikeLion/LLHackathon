package com.hackathon.skinroutine.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yml 의 app.* 설정 바인딩.
 * 실제 값의 원본은 전부 환경변수 — 어떤 키가 필요한지는 .env.example 참고.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Openai openai, Supabase supabase, Cors cors, Storage storage, Demo demo) {

    public record Openai(String apiKey, String model, String baseUrl) {
        /** 키가 없으면 AI 호출을 시도조차 하지 않고 폴백으로 간다 */
        public boolean hasKey() {
            return apiKey != null && !apiKey.isBlank();
        }
    }

    public record Supabase(String url, String serviceKey, String bucket) {
        public boolean configured() {
            return url != null && !url.isBlank() && serviceKey != null && !serviceKey.isBlank();
        }
    }

    public record Cors(String allowedOrigins) {}

    public record Storage(String localDir) {}

    public record Demo(String photoUrl) {}
}
