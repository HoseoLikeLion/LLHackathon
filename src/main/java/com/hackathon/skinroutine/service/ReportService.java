package com.hackathon.skinroutine.service;

import com.hackathon.skinroutine.common.KoreaTime;
import com.hackathon.skinroutine.domain.Analysis;
import com.hackathon.skinroutine.domain.Routine;
import com.hackathon.skinroutine.domain.RoutineStatus;
import com.hackathon.skinroutine.dto.ReportDtos;
import com.hackathon.skinroutine.repository.AnalysisRepository;
import com.hackathon.skinroutine.repository.RoutineRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * #10 리포트 집계 (팀장 A) — C의 ReportController는 summary() 호출만 하면 된다.
 * 별도 테이블 없이 analyses·routines에서 계산한다 (docs/05: 해커톤 범위 — 테이블 늘리지 않기).
 * note 문구는 인과 단정 대신 "함께 나타났어요" 상관 표현 — 6-2 근거 문장 원칙 준수.
 */
@Service
public class ReportService {

    private final UserService userService;
    private final AnalysisRepository analysisRepository;
    private final RoutineRepository routineRepository;

    public ReportService(UserService userService, AnalysisRepository analysisRepository,
                         RoutineRepository routineRepository) {
        this.userService = userService;
        this.analysisRepository = analysisRepository;
        this.routineRepository = routineRepository;
    }

    /** days 1~90 (기본 14) */
    @Transactional(readOnly = true)
    public ReportDtos.SummaryResponse summary(UUID userId, int days) {
        userService.requireUser(userId);
        int range = Math.max(1, Math.min(days, 90));
        LocalDate from = KoreaTime.today().minusDays(range - 1L);

        List<Analysis> analyses = analysisRepository.findInRangeWithRecord(userId, from);

        List<ReportDtos.TrendPoint> trends = analyses.stream()
                .map(a -> new ReportDtos.TrendPoint(a.getRecord().getRecordDate(), a.getScore(),
                        a.getRedness(), a.getMoisture(), a.getOil()))
                .toList();

        ReportDtos.LatestVsPrevious latestVsPrevious;
        if (analyses.size() >= 2) {
            Analysis latest = analyses.get(analyses.size() - 1);
            Analysis previous = analyses.get(analyses.size() - 2);
            latestVsPrevious = new ReportDtos.LatestVsPrevious(
                    direction(latest.getRedness(), previous.getRedness()),
                    direction(latest.getMoisture(), previous.getMoisture()),
                    direction(latest.getOil(), previous.getOil()));
        } else {
            latestVsPrevious = new ReportDtos.LatestVsPrevious("same", "same", "same"); // 비교 데이터 부족
        }

        return new ReportDtos.SummaryResponse(latestVsPrevious, trends,
                routineEffects(userId, from, analyses));
    }

    private String direction(Integer latest, Integer previous) {
        if (latest == null || previous == null || latest.equals(previous)) {
            return "same";
        }
        return latest > previous ? "up" : "down";
    }

    /** 완료된 루틴별: 실행 횟수 + "실천한 다음 날 3축이 어떻게 움직였나" 룰 문구 */
    private List<ReportDtos.RoutineEffect> routineEffects(UUID userId, LocalDate from,
                                                          List<Analysis> analyses) {
        Map<LocalDate, Analysis> byDate = analyses.stream()
                .collect(Collectors.toMap(a -> a.getRecord().getRecordDate(), a -> a, (a, b) -> a));
        List<Routine> completed =
                routineRepository.findByStatusInRange(userId, RoutineStatus.COMPLETED, from);

        Map<String, List<Routine>> byTitle = completed.stream()
                .collect(Collectors.groupingBy(Routine::getTitle, LinkedHashMap::new, Collectors.toList()));

        List<ReportDtos.RoutineEffect> effects = new ArrayList<>();
        for (Map.Entry<String, List<Routine>> entry : byTitle.entrySet()) {
            double rednessDelta = 0;
            double moistureDelta = 0;
            int samples = 0;
            for (Routine routine : entry.getValue()) {
                LocalDate day = routine.getRecord().getRecordDate();
                Analysis dayAnalysis = byDate.get(day);
                Analysis nextAnalysis = byDate.get(day.plusDays(1));
                if (dayAnalysis != null && nextAnalysis != null) {
                    rednessDelta += nextAnalysis.getRedness() - dayAnalysis.getRedness();
                    moistureDelta += nextAnalysis.getMoisture() - dayAnalysis.getMoisture();
                    samples++;
                }
            }
            String note;
            if (samples == 0) {
                note = "효과 확인까지 데이터가 조금 더 필요해요";
            } else {
                double avgRedness = rednessDelta / samples;
                double avgMoisture = moistureDelta / samples;
                if (avgRedness <= -0.3 && Math.abs(avgRedness) >= Math.abs(avgMoisture)) {
                    note = "실천한 다음 날 붉음 감소가 함께 나타났어요";
                } else if (avgMoisture >= 0.3) {
                    note = "실천한 다음 날 수분 상승이 함께 나타났어요";
                } else if (avgRedness >= 0.5) {
                    note = "아직 뚜렷한 변화가 없어요 — 조금 더 지켜봐요";
                } else {
                    note = "큰 변화 없이 컨디션을 유지 중이에요";
                }
            }
            effects.add(new ReportDtos.RoutineEffect(entry.getKey(), entry.getValue().size(), note));
        }
        return effects.stream()
                .sorted(Comparator.comparingInt(ReportDtos.RoutineEffect::executedCount).reversed())
                .limit(5)
                .toList();
    }
}
