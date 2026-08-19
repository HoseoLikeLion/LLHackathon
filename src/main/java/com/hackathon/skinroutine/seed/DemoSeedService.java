package com.hackathon.skinroutine.seed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackathon.skinroutine.common.KoreaTime;
import com.hackathon.skinroutine.config.AppProperties;
import com.hackathon.skinroutine.domain.Analysis;
import com.hackathon.skinroutine.domain.DailyRecord;
import com.hackathon.skinroutine.domain.Routine;
import com.hackathon.skinroutine.domain.User;
import com.hackathon.skinroutine.repository.AnalysisRepository;
import com.hackathon.skinroutine.repository.DailyRecordRepository;
import com.hackathon.skinroutine.repository.RoutineRepository;
import com.hackathon.skinroutine.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * #11 데모 세션 시드 (틀 = 팀장 A / 시나리오 값 다듬기 = C).
 *
 * 호출할 때마다 "새" 데모 계정을 만들어 과거 20일치 데이터를 채운다.
 * - 심사위원 여러 명이 동시에 써도 서로 안 섞인다
 * - 오늘은 비워 둔다 → 심사위원이 직접 기록하는 라이브 시연 플로우가 가능하다
 * - 스토리: 첫 주는 생활 습관 나쁨(수면 부족·음주) → 루틴 실천 → 점수·3축이 좋아지는 흐름
 *   (⑤ 리포트 화면이 "뭐가 효과였는지"를 보여줄 수 있는 데이터 모양)
 *
 * C의 DemoController(#11)는 createDemoSession() 호출 → DemoDtos.SessionResponse로 감싸면 끝.
 */
@Service
public class DemoSeedService {

    private static final Logger log = LoggerFactory.getLogger(DemoSeedService.class);
    private static final int SEED_DAYS = 20;

    // ── C가 다듬는 부분: 루틴 카드 문구 (index를 맞춰서 수정할 것) ──────────────
    private static final String[] TITLES = {
            "수분 보충 케어", "저자극 진정 케어", "취침 30분 앞당기기", "물 500ml 먼저 마시기"};
    private static final String[] REASONS = {
            "수분 부족 신호가 이어지고 있어서예요",
            "붉음 신호를 가라앉히는 게 우선이에요",
            "수면 부족이 피부 컨디션을 끌어내리고 있어요",
            "속수분부터 채우면 당김이 줄어요"};
    private static final String[] METHODS = {
            "세안 직후 3분 안에 수분 크림을 한 번 더 덧발라 주세요",
            "미온수 세안 후 차가운 수건을 1분간 볼에 올려 주세요",
            "오늘은 평소보다 30분 일찍 눕고, 휴대폰은 침대 밖에 두세요",
            "지금 물 한 컵을 마시고 오늘 1L를 목표로 해요"};
    private static final int[] MINUTES = {2, 3, 1, 1};

    private final UserRepository userRepository;
    private final DailyRecordRepository recordRepository;
    private final AnalysisRepository analysisRepository;
    private final RoutineRepository routineRepository;
    private final AppProperties props;
    private final ObjectMapper objectMapper;

    public DemoSeedService(UserRepository userRepository, DailyRecordRepository recordRepository,
                           AnalysisRepository analysisRepository, RoutineRepository routineRepository,
                           AppProperties props, ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.recordRepository = recordRepository;
        this.analysisRepository = analysisRepository;
        this.routineRepository = routineRepository;
        this.props = props;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public User createDemoSession() {
        User demo = userRepository.save(new User("데모 체험", true));
        LocalDate today = KoreaTime.today();
        String photoUrl = (props.demo() != null && props.demo().photoUrl() != null
                && !props.demo().photoUrl().isBlank()) ? props.demo().photoUrl() : null;

        for (int i = 0; i < SEED_DAYS; i++) {
            LocalDate date = today.minusDays(SEED_DAYS - (long) i); // D-20 ... D-1
            boolean earlyPhase = i < 7; // 첫 주: 습관 나쁨 → 피부 나쁨

            double sleep = (earlyPhase ? 5.0 : 6.5) + (i % 3) * 0.5;
            boolean drink = earlyPhase ? (i % 2 == 0) : (i % 5 == 0);
            int stress = earlyPhase ? (i % 2 == 0 ? 3 : 2) : (i % 2 == 0 ? 1 : 2);

            int redness = clamp(4 - i / 6, 1, 5);   // 4 → 1 로 개선
            int moisture = clamp(2 + i / 6, 1, 5);  // 2 → 5 로 개선
            int oil = (i % 7 == 0) ? 4 : 3;
            if (i == SEED_DAYS - 2) {
                // 마지막 날 직전은 한 단계 나쁘게 둔다 —
                // ⑤ 리포트의 "최근 vs 직전" 비교가 '붉음 감소·수분 상승'으로 보이게 하는 시연 장치
                redness = clamp(redness + 1, 1, 5);
                moisture = clamp(moisture - 1, 1, 5);
            }
            int score = Math.min(92, clamp(58 + i * 2 - (redness - 2) * 3 + (moisture - 3) * 2, 0, 100));

            List<String> labels = earlyPhase ? List.of("붉음 주의", "수분 부족")
                    : (i < 14 ? List.of("붉음 감소", "수분 상승") : List.of("컨디션 좋음", "수분 안정"));
            String insight = earlyPhase
                    ? "수면이 부족한 날 붉음이 함께 올라오는 패턴이 보여요."
                    : "루틴을 실천한 다음 날 수분 지표가 좋아지는 흐름이에요.";

            DailyRecord record = recordRepository.save(
                    new DailyRecord(demo, date, photoUrl, sleep, drink, stress));
            analysisRepository.save(new Analysis(record, score, redness, moisture, oil,
                    toJson(labels), insight, false, "seed"));

            int t = i % TITLES.length;
            Routine routine = new Routine(demo, record, TITLES[t], REASONS[t], METHODS[t], MINUTES[t], 1);
            if (i % 5 != 3) { // 대부분 완료, 가끔 미실천 — 리포트가 현실적으로 보이게
                routine.markCompleted(date.atTime(21, 0).atZone(KoreaTime.ZONE).toInstant());
            } else {
                routine.markDeferred();
            }
            routineRepository.save(routine);
        }
        log.info("데모 세션 생성: userId={} ({}일치 시드)", demo.getId(), SEED_DAYS);
        return demo;
    }

    private String toJson(List<String> labels) {
        try {
            return objectMapper.writeValueAsString(labels);
        } catch (Exception e) {
            return null;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
