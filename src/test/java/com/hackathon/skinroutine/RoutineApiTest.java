package com.hackathon.skinroutine;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * #6~#9 루틴 API 통합 테스트 (local 프로필).
 * 특히 #7·#8은 "상태 변경이 DB에 실제로 저장되는지"를 재조회로 확인한다 —
 * 컨트롤러가 트랜잭션 밖에서 detached 엔티티를 save(merge)하는 구조라 검증이 필요하다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class RoutineApiTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    private static MockMultipartFile photo() throws IOException {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(215, 175, 155));
        g.fillRect(0, 0, 64, 64);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", out);
        return new MockMultipartFile("photo", "selfie.jpg", MediaType.IMAGE_JPEG_VALUE, out.toByteArray());
    }

    private String createUser() throws Exception {
        String body = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"루틴테스트\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(body).path("userId").asText();
    }

    private String userWithTodayRecord() throws Exception {
        String userId = createUser();
        mockMvc.perform(multipart("/api/records").file(photo())
                        .param("sleepHours", "5.0")
                        .param("hadDrinkOrSnack", "true")
                        .param("stressLevel", "3")
                        .header("X-User-Id", userId))
                .andExpect(status().isOk());
        return userId;
    }

    @Test
    void getTodayRoutineReturnsContract() throws Exception {
        String userId = userWithTodayRecord();
        mockMvc.perform(get("/api/routines/today").header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").isNotEmpty())
                .andExpect(jsonPath("$.reason").isNotEmpty())
                .andExpect(jsonPath("$.method").isNotEmpty())
                .andExpect(jsonPath("$.expectedMinutes").isNumber())
                .andExpect(jsonPath("$.status").value("suggested"))
                .andExpect(jsonPath("$.generation").value(1));
    }

    @Test
    void completePersistsAndReturnsStreak() throws Exception {
        String userId = userWithTodayRecord();
        mockMvc.perform(post("/api/routines/today/complete").header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("completed"))
                .andExpect(jsonPath("$.completedAt").isNotEmpty())
                .andExpect(jsonPath("$.streakDays").value(1));

        // 재조회 — 상태 변경이 DB에 실제로 반영됐는지 (merge 동작 확인)
        mockMvc.perform(get("/api/routines/today").header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("completed"));
    }

    @Test
    void deferPersistsAndClearsCompletedAt() throws Exception {
        String userId = userWithTodayRecord();
        mockMvc.perform(post("/api/routines/today/complete").header("X-User-Id", userId))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/routines/today/defer").header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("deferred"))
                .andExpect(jsonPath("$.completedAt").doesNotExist());

        mockMvc.perform(get("/api/routines/today").header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("deferred"));
    }

    @Test
    void alternativeCreatesNextGenerationAndBecomesTodayRoutine() throws Exception {
        String userId = userWithTodayRecord();
        String first = mockMvc.perform(get("/api/routines/today").header("X-User-Id", userId))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        String firstTitle = objectMapper.readTree(first).path("title").asText();

        mockMvc.perform(post("/api/routines/today/alternative").header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generation").value(2))
                .andExpect(jsonPath("$.status").value("suggested"))
                .andExpect(jsonPath("$.title").value(org.hamcrest.Matchers.not(firstTitle)));

        // #6은 항상 최신 세대를 돌려줘야 한다
        mockMvc.perform(get("/api/routines/today").header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.generation").value(2));
    }

    @Test
    void routineWithoutTodayRecordIs404() throws Exception {
        String userId = createUser();
        mockMvc.perform(get("/api/routines/today").header("X-User-Id", userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NO_ROUTINE_TODAY"));
        mockMvc.perform(post("/api/routines/today/complete").header("X-User-Id", userId))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/routines/today/alternative").header("X-User-Id", userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NO_RECORD_TODAY"));
    }

    @Test
    void routineApisRequireUserHeader() throws Exception {
        mockMvc.perform(get("/api/routines/today"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("MISSING_USER_ID"));
        mockMvc.perform(post("/api/routines/today/complete"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/routines/today/defer"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/routines/today/alternative"))
                .andExpect(status().isUnauthorized());
    }
}
