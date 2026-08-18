package com.hackathon.skinroutine.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.hackathon.skinroutine.domain.User;
import com.hackathon.skinroutine.dto.ReportDtos;
import com.hackathon.skinroutine.seed.DemoSeedService;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** #10 리포트 집계 + #11 데모 시드(틀)가 서로 맞물리는지 — 심사 시연의 핵심 경로 */
@SpringBootTest
@ActiveProfiles("local")
class ReportAndSeedTest {

    @Autowired
    DemoSeedService demoSeedService;

    @Autowired
    ReportService reportService;

    @Autowired
    StreakService streakService;

    @Test
    void demoSeedFeedsReportAndStreak() {
        User demo = demoSeedService.createDemoSession();
        assertTrue(demo.isDemo());

        ReportDtos.SummaryResponse summary = reportService.summary(demo.getId(), 14);

        // 최근 14일 구간 → 시드가 채운 날이 13일 이상
        assertTrue(summary.trends().size() >= 10, "trends 크기: " + summary.trends().size());
        Set<String> allowed = Set.of("up", "down", "same");
        assertTrue(allowed.contains(summary.latestVsPrevious().redness()));
        assertTrue(allowed.contains(summary.latestVsPrevious().moisture()));
        assertTrue(allowed.contains(summary.latestVsPrevious().oil()));

        // 완료된 루틴 효과 집계가 나와야 리포트 화면이 채워진다
        assertFalse(summary.routineEffects().isEmpty());
        assertTrue(summary.routineEffects().get(0).executedCount() >= 1);
        assertNotNull(summary.routineEffects().get(0).note());

        // 시드는 D-20 ~ D-1 연속 → 스트릭 20 (어제 기준 앵커)
        assertEquals(20, streakService.currentStreak(demo.getId()));
    }
}
