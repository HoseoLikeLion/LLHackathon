package com.hackathon.skinroutine.dto;

import java.time.LocalDate;
import java.util.List;

/** #10 GET /api/reports/summary 응답 (계약: docs/05) */
public final class ReportDtos {

    public record SummaryResponse(LatestVsPrevious latestVsPrevious, List<TrendPoint> trends,
                                  List<RoutineEffect> routineEffects) {}

    /** 최신 분석 vs 직전 분석 — 값은 "up" | "down" | "same" (비교 데이터 부족 시 전부 "same") */
    public record LatestVsPrevious(String redness, String moisture, String oil) {}

    /** 차트용 시계열 점 */
    public record TrendPoint(LocalDate date, Integer score, Integer redness,
                             Integer moisture, Integer oil) {}

    /** 완료한 루틴별 효과 — note는 인과 단정이 아니라 "함께 나타났어요" 상관 표현(팀 확정 원칙) */
    public record RoutineEffect(String title, int executedCount, String note) {}

    private ReportDtos() {}
}
