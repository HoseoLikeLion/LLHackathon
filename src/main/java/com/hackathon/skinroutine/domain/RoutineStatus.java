package com.hackathon.skinroutine.domain;

/** 루틴 상태 — DB에는 대문자(STRING), 응답 JSON은 소문자 계약("suggested") */
public enum RoutineStatus {
    SUGGESTED, COMPLETED, DEFERRED;

    /** DTO 변환 시 이걸 쓴다 (계약: docs/05 응답 예시) */
    public String toJson() {
        return name().toLowerCase();
    }
}
