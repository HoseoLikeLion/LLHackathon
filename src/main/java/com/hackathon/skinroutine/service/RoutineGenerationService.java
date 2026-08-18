package com.hackathon.skinroutine.service;

import com.hackathon.skinroutine.common.ApiException;
import com.hackathon.skinroutine.common.KoreaTime;
import com.hackathon.skinroutine.domain.Analysis;
import com.hackathon.skinroutine.domain.DailyRecord;
import com.hackathon.skinroutine.domain.Routine;
import com.hackathon.skinroutine.domain.User;
import com.hackathon.skinroutine.repository.AnalysisRepository;
import com.hackathon.skinroutine.repository.DailyRecordRepository;
import com.hackathon.skinroutine.repository.RoutineRepository;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 루틴 생성(#3 내부) · 룰 기반 폴백 · #9 재추천 — 전부 팀장(A) 소관.
 * B의 RoutineController #9는 generateAlternative() 호출만 하면 된다.
 */
@Service
public class RoutineGenerationService {

    private static final Logger log = LoggerFactory.getLogger(RoutineGenerationService.class);

    /** 폴백 루틴 카드 한 장 */
    public record Template(String axis, String title, String reason, String method, int minutes) {}

    /** AI 실패 시의 룰 기반 분석+루틴 묶음 — isFallback=true로 저장된다 */
    public record FallbackOutcome(int score, int redness, int moisture, int oil,
                                  List<String> labels, String insight, Template routine) {}

    /**
     * 룰 기반 폴백 풀 (docs/05 5단계: "최악 축 기준 매핑").
     * AI가 죽어도 절대 빈손으로 응답하지 않기 위한 테이블 — 시연 안전장치.
     */
    private static final List<Template> FALLBACK_POOL = List.of(
            new Template("redness", "저자극 진정 케어", "오늘은 피부 붉음 신호가 커 보여요",
                    "미온수로 가볍게 세안하고, 차가운 수건을 1분간 볼에 올려 진정시켜 주세요", 3),
            new Template("redness", "뜨거운 물 세안 쉬기", "자극을 줄이면 붉음 회복이 빨라져요",
                    "오늘 세안은 미온수로만 하고, 스크럽·필링은 하루 쉬어 주세요", 1),
            new Template("moisture", "수분 보충 케어", "수분 부족 신호가 보여요",
                    "세안 직후 3분 안에 수분 크림을 평소보다 한 번 더 덧발라 주세요", 2),
            new Template("moisture", "물 500ml 먼저 마시기", "속수분부터 채우면 피부 당김이 줄어요",
                    "지금 물 한 컵을 마시고, 오늘 총 1L 이상을 목표로 해요", 1),
            new Template("oil", "가벼운 유분 관리", "유분이 많아 보이는 날이에요",
                    "저녁 세안을 꼼꼼히 하고, 오늘은 크림을 얇게 발라 주세요", 2),
            new Template("sleep", "취침 30분 앞당기기", "수면 부족이 피부 컨디션을 끌어내리고 있어요",
                    "오늘은 평소보다 30분 일찍 눕고, 자기 전 휴대폰은 침대 밖에 두세요", 1),
            new Template("general", "자기 전 1분 마무리 루틴", "꾸준한 기본기가 내일 피부를 만들어요",
                    "자기 전 세안 후 보습 한 번, 물 한 컵으로 하루를 마무리해요", 1)
    );

    private final DailyRecordRepository recordRepository;
    private final RoutineRepository routineRepository;
    private final AnalysisRepository analysisRepository;
    private final VisionService visionService;

    public RoutineGenerationService(DailyRecordRepository recordRepository,
                                    RoutineRepository routineRepository,
                                    AnalysisRepository analysisRepository,
                                    VisionService visionService) {
        this.recordRepository = recordRepository;
        this.routineRepository = routineRepository;
        this.analysisRepository = analysisRepository;
        this.visionService = visionService;
    }

    /** 루틴 저장 (#3 파이프라인·#9 공용) */
    @Transactional
    public Routine saveRoutine(User user, DailyRecord record, String title, String reason,
                               String method, int minutes, int generation) {
        return routineRepository.save(
                new Routine(user, record, title, reason, method, minutes, generation));
    }

    /** AI 실패 시 룰 기반 분석·루틴 만들기 — RecordService의 폴백 경로에서 호출 */
    public FallbackOutcome fallback(DailyRecord record, Analysis previous, List<String> excludeTitles) {
        double sleep = record.getSleepHours() == null ? 7.0 : record.getSleepHours();
        boolean drink = Boolean.TRUE.equals(record.getHadDrinkOrSnack());
        int stress = record.getStressLevel() == null ? 1 : record.getStressLevel();

        // 직전 분석이 있으면 이어받고, 없으면 중간값에서 시작해 오늘 상태로 보정
        int redness = previous != null ? previous.getRedness() : 3;
        int moisture = previous != null ? previous.getMoisture() : 3;
        int oil = previous != null ? previous.getOil() : 3;
        if (sleep < 6) redness = clamp(redness + 1, 1, 5);
        if (drink) moisture = clamp(moisture - 1, 1, 5);
        if (stress >= 3) redness = clamp(redness + 1, 1, 5);

        int score = clamp(78 - (redness - 3) * 8 - (3 - moisture) * 6 - Math.abs(oil - 3) * 4
                - (stress - 1) * 3 - (sleep < 6 ? 5 : 0), 0, 100);

        List<String> labels = new ArrayList<>();
        if (sleep < 6) labels.add("수면 부족");
        if (drink) labels.add("음주·야식 영향");
        if (stress >= 3) labels.add("스트레스 높음");
        if (labels.isEmpty()) labels.add("컨디션 무난");
        if (labels.size() > 3) labels = new ArrayList<>(labels.subList(0, 3));

        String axis = pickAxis(record, previous);
        String insight = switch (axis) {
            case "sleep" -> "수면이 부족한 날은 피부 회복력이 떨어지기 쉬워요.";
            case "redness" -> "오늘은 자극을 줄이고 진정에 집중하면 좋아요.";
            case "moisture" -> "수분 보충이 필요한 신호가 보여요.";
            case "oil" -> "유분 밸런스를 가볍게 잡아주는 게 좋겠어요.";
            default -> "오늘도 기본 루틴을 꾸준히 이어가면 충분해요.";
        };

        Template template = pickTemplate(axis, excludeTitles);
        return new FallbackOutcome(score, redness, moisture, oil, labels, insight, template);
    }

    /**
     * #9 "다른 루틴 보기" — 새 루틴을 generation+1로 만들고 이전 것은 보관.
     * AI 시도 → 실패하면 폴백 풀에서 아직 안 쓴 제목으로. B는 이 메서드 호출만 하면 됨.
     */
    @Transactional
    public Routine generateAlternative(User user) {
        DailyRecord record = recordRepository.findByUserIdAndRecordDate(user.getId(), KoreaTime.today())
                .orElseThrow(() -> ApiException.notFound("NO_RECORD_TODAY",
                        "오늘 기록이 아직 없어요. 먼저 기록을 남겨 주세요."));
        List<Routine> existing = routineRepository.findByRecordId(record.getId());
        List<String> usedTitles = existing.stream().map(Routine::getTitle).toList();
        int nextGeneration = existing.stream().mapToInt(Routine::getGeneration).max().orElse(0) + 1;
        Analysis analysis = analysisRepository.findByRecordId(record.getId()).orElse(null);

        String title;
        String reason;
        String method;
        int minutes;
        try {
            VisionService.AiRoutine ai = visionService.regenerateRoutine(analysis, record, usedTitles);
            title = ai.title();
            reason = ai.reason();
            method = ai.method();
            minutes = ai.expectedMinutes();
        } catch (Exception e) {
            log.warn("재추천 AI 호출 실패 → 폴백 풀 사용: {}", e.toString());
            Template t = pickTemplate(pickAxis(record, analysis), usedTitles);
            title = t.title();
            reason = t.reason();
            method = t.method();
            minutes = t.minutes();
        }
        return routineRepository.save(
                new Routine(user, record, title, reason, method, minutes, nextGeneration));
    }

    /** 오늘 상태·직전 분석에서 가장 손봐야 할 축 고르기 */
    private String pickAxis(DailyRecord record, Analysis previous) {
        if (record.getSleepHours() != null && record.getSleepHours() < 6) {
            return "sleep";
        }
        if (previous != null) {
            int rednessBad = previous.getRedness();      // 높을수록 나쁨
            int moistureBad = 6 - previous.getMoisture(); // 낮을수록 나쁨 → 뒤집기
            int oilBad = previous.getOil();
            if (rednessBad >= 4 && rednessBad >= moistureBad && rednessBad >= oilBad) return "redness";
            if (moistureBad >= 4 && moistureBad >= oilBad) return "moisture";
            if (oilBad >= 4) return "oil";
        }
        if (Boolean.TRUE.equals(record.getHadDrinkOrSnack())) {
            return "moisture"; // 음주·야식 다음날은 수분 보충 우선
        }
        if (record.getStressLevel() != null && record.getStressLevel() >= 3) {
            return "redness"; // 스트레스 높은 날은 진정 우선
        }
        return "general";
    }

    /** 축에 맞는 폴백 카드 중 아직 안 쓴 제목 고르기 — 어떤 경우에도 null을 반환하지 않는다 */
    private Template pickTemplate(String axis, List<String> excludeTitles) {
        List<String> exclude = excludeTitles == null ? List.of() : excludeTitles;
        return FALLBACK_POOL.stream()
                .filter(t -> t.axis().equals(axis) && !exclude.contains(t.title()))
                .findFirst()
                .or(() -> FALLBACK_POOL.stream()
                        .filter(t -> !exclude.contains(t.title())).findFirst())
                .orElse(FALLBACK_POOL.get(FALLBACK_POOL.size() - 1)); // 전부 소진돼도 마지막 카드로
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
