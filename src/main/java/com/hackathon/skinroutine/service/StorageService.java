package com.hackathon.skinroutine.service;

import java.time.LocalDate;
import java.util.UUID;

/**
 * 사진 저장소. 운영 = SupabaseStorageService, local 프로필 = LocalStorageService.
 * 계약: 실패해도 예외를 던지지 않고 null을 반환한다 —
 * 사진 저장이 죽어도 기록·분석은 반드시 살린다 (시연 중 절대 빈손으로 응답하지 않기, docs/05).
 */
public interface StorageService {

    /** 리사이즈된 JPEG 업로드 후 접근 가능한 URL 반환. 실패 시 null. */
    String uploadJpeg(byte[] jpegBytes, UUID userId, LocalDate date);
}
