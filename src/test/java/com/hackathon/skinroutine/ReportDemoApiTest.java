package com.hackathon.skinroutine;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * #10 리포트 · #11 데모 세션 API 통합 테스트.
 * 심사위원 시연 경로(데모 계정 발급 → 리포트 화면)가 실제로 채워지는지 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class ReportDemoApiTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    private String createDemoUser() throws Exception {
        String body = mockMvc.perform(post("/api/demo/session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").isNotEmpty())
                .andExpect(jsonPath("$.nickname").value("데모 체험"))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(body).path("userId").asText();
    }

    @Test
    void demoSessionFillsReportScreen() throws Exception {
        String userId = createDemoUser();

        mockMvc.perform(get("/api/reports/summary").param("days", "14").header("X-User-Id", userId))
                .andExpect(status().isOk())
                // 심사 화면의 핵심 문구 — 데모 계정은 반드시 '좋아지는 중'으로 보여야 한다
                .andExpect(jsonPath("$.latestVsPrevious.redness").value("down"))
                .andExpect(jsonPath("$.latestVsPrevious.moisture").value("up"))
                .andExpect(jsonPath("$.latestVsPrevious.oil").isNotEmpty())
                .andExpect(jsonPath("$.trends").isArray())
                .andExpect(jsonPath("$.trends[0].date").isNotEmpty())
                .andExpect(jsonPath("$.trends[0].score").isNumber())
                .andExpect(jsonPath("$.routineEffects").isArray())
                .andExpect(jsonPath("$.routineEffects[0].title").isNotEmpty())
                .andExpect(jsonPath("$.routineEffects[0].executedCount").isNumber())
                .andExpect(jsonPath("$.routineEffects[0].note").isNotEmpty());
    }

    @Test
    void demoSessionsAreIndependent() throws Exception {
        // 심사위원 여러 명이 동시에 눌러도 서로 데이터가 섞이면 안 된다
        String first = createDemoUser();
        String second = createDemoUser();
        org.junit.jupiter.api.Assertions.assertNotEquals(first, second);
    }

    @Test
    void demoUserHomeShowsStreakAndTodayNotRecorded() throws Exception {
        // 오늘은 비워 둔 시드 → 심사위원이 직접 기록하는 라이브 시연이 가능해야 한다
        String userId = createDemoUser();
        mockMvc.perform(get("/api/users/me/home").header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todayRecorded").value(false))
                .andExpect(jsonPath("$.streakDays").value(20));
    }

    @Test
    void reportForNewUserIsEmptyButValid() throws Exception {
        // 데이터가 없어도 200 + 빈 배열 (프론트 빈 상태 처리용)
        String body = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String userId = objectMapper.readTree(body).path("userId").asText();

        mockMvc.perform(get("/api/reports/summary").header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trends").isEmpty())
                .andExpect(jsonPath("$.routineEffects").isEmpty())
                .andExpect(jsonPath("$.latestVsPrevious.redness").value("same"));
    }

    @Test
    void reportRequiresUserHeader() throws Exception {
        mockMvc.perform(get("/api/reports/summary"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("MISSING_USER_ID"));
    }
}
