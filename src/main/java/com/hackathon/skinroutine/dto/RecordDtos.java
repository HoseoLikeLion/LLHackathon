package com.hackathon.skinroutine.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hackathon.skinroutine.domain.Analysis;
import com.hackathon.skinroutine.domain.DailyRecord;
import java.time.LocalDate;
import java.util.List;

/** #3 · #4 · #5 기록 API 응답 (계약: docs/05 "응답 예시 — POST /api/records") */
public final class RecordDtos {

    /** #3 · #4 응답 전체 — {"record","analysis","routine"} */
    public record DetailResponse(RecordPart record, AnalysisPart analysis,
                                 RoutineDtos.RoutineResponse routine) {}

    public record RecordPart(Long id, LocalDate recordDate, String photoUrl,
                             Double sleepHours, Boolean hadDrinkOrSnack, Integer stressLevel) {
        public static RecordPart from(DailyRecord r) {
            return new RecordPart(r.getId(), r.getRecordDate(), r.getPhotoUrl(),
                    r.getSleepHours(), r.getHadDrinkOrSnack(), r.getStressLevel());
        }
    }

    /**
     * analysis 파트. ⚠️ @JsonProperty("isFallback") 필수 —
     * boolean record 컴포넌트가 is로 시작하면 Jackson이 "fallback"으로 이름을 바꿔버린다.
     */
    public record AnalysisPart(Integer score, Levels levels, List<String> labels, String insight,
                               @JsonProperty("isFallback") boolean isFallback) {
        public static AnalysisPart of(Analysis a, List<String> labels) {
            return new AnalysisPart(a.getScore(),
                    new Levels(a.getRedness(), a.getMoisture(), a.getOil()),
                    labels, a.getInsightText(), a.isFallback());
        }
    }

    public record Levels(Integer redness, Integer moisture, Integer oil) {}

    /** #5 목록 응답 — {"records":[...]} */
    public record ListResponse(List<SummaryItem> records) {}

    public record SummaryItem(Long id, LocalDate recordDate, String photoUrl, Integer score,
                              List<String> labels, @JsonProperty("isFallback") Boolean isFallback) {}

    private RecordDtos() {}
}
