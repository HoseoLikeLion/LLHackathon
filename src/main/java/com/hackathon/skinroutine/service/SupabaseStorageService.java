package com.hackathon.skinroutine.service;

import com.hackathon.skinroutine.config.AppProperties;
import java.time.LocalDate;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Supabase Storage REST 업로드 (공식 Java SDK가 없어 RestClient 직접 호출 — docs/05).
 * 업로드: POST {SUPABASE_URL}/storage/v1/object/{bucket}/{path} + service key
 * 조회 URL: {SUPABASE_URL}/storage/v1/object/public/{bucket}/{path} (버킷을 Public으로 만들 것)
 */
@Service
@Profile("!local")
public class SupabaseStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(SupabaseStorageService.class);

    private final RestClient supabaseRestClient;
    private final AppProperties props;

    public SupabaseStorageService(@Qualifier("supabaseRestClient") RestClient supabaseRestClient,
                                  AppProperties props) {
        this.supabaseRestClient = supabaseRestClient;
        this.props = props;
    }

    @Override
    public String uploadJpeg(byte[] jpegBytes, UUID userId, LocalDate date) {
        if (!props.supabase().configured()) {
            log.warn("Supabase 설정(SUPABASE_URL/SUPABASE_SERVICE_KEY) 없음 — 사진 저장 생략");
            return null;
        }
        String bucket = props.supabase().bucket();
        // 경로 구성 문자는 전부 URL-safe (uuid·날짜·hex) — 인코딩 이슈 없음
        String path = "records/" + userId + "/" + date + "-"
                + UUID.randomUUID().toString().substring(0, 8) + ".jpg";
        try {
            supabaseRestClient.post()
                    .uri("/storage/v1/object/" + bucket + "/" + path)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + props.supabase().serviceKey())
                    .header("apikey", props.supabase().serviceKey())
                    .header("x-upsert", "true")
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(jpegBytes)
                    .retrieve()
                    .toBodilessEntity();
            return props.supabase().url() + "/storage/v1/object/public/" + bucket + "/" + path;
        } catch (Exception e) {
            log.error("Supabase Storage 업로드 실패 — photoUrl 없이 진행: {}", e.toString());
            return null;
        }
    }
}
