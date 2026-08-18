package com.hackathon.skinroutine.dto;

import com.hackathon.skinroutine.domain.Routine;
import java.time.Instant;

/** 루틴 관련 응답 (계약: docs/05) — #3의 routine 파트, #6, #7, #8, #9 공용 */
public final class RoutineDtos {

    /** 루틴 카드 1장 — status는 소문자 계약("suggested" | "completed" | "deferred") */
    public record RoutineResponse(Long id, String title, String reason, String method,
                                  Integer expectedMinutes, String status, Integer generation) {
        public static RoutineResponse from(Routine r) {
            return new RoutineResponse(r.getId(), r.getTitle(), r.getReasonText(), r.getMethodText(),
                    r.getExpectedMinutes(), r.getStatus().toJson(), r.getGeneration());
        }
    }

    /** #7 완료 / #8 연기 응답 — {"status","completedAt","streakDays"} (B가 사용) */
    public record ActionResponse(String status, Instant completedAt, int streakDays) {
        public static ActionResponse from(Routine r, int streakDays) {
            return new ActionResponse(r.getStatus().toJson(), r.getCompletedAt(), streakDays);
        }
    }

    private RoutineDtos() {}
}
