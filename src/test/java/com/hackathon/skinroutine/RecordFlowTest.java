package com.hackathon.skinroutine;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackathon.skinroutine.common.KoreaTime;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
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
 * #1~#5 코어 플로우 통합 테스트 (local 프로필: H2 + 로컬 사진 저장).
 * OPENAI_API_KEY가 없으므로 #3은 룰 기반 폴백 경로(isFallback=true)를 타야 정상이다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class RecordFlowTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    /** 64x64 살구색 정사각형 JPEG — 이미지 파이프라인(리사이즈·저장)을 실제로 태운다 */
    private static byte[] tinyJpeg() throws IOException {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(220, 180, 160));
        g.fillRect(0, 0, 64, 64);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", out);
        return out.toByteArray();
    }

    private static MockMultipartFile photo() throws IOException {
        return new MockMultipartFile("photo", "selfie.jpg", MediaType.IMAGE_JPEG_VALUE, tinyJpeg());
    }

    private String createUser() throws Exception {
        String response = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nickname\":\"테스트\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").exists())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(response).path("userId").asText();
    }

    @Test
    void fullRecordFlow() throws Exception {
        String userId = createUser();

        // 홈: 아직 기록 없음
        mockMvc.perform(get("/api/users/me/home").header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nickname").value("테스트"))
                .andExpect(jsonPath("$.todayRecorded").value(false))
                .andExpect(jsonPath("$.streakDays").value(0));

        // #3 기록 생성 — 키 없음 → 폴백 경로, 계약 JSON 모양 그대로인지 확인
        mockMvc.perform(multipart("/api/records").file(photo())
                        .param("sleepHours", "5.5")
                        .param("hadDrinkOrSnack", "true")
                        .param("stressLevel", "2")
                        .header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.record.recordDate").value(KoreaTime.today().toString()))
                .andExpect(jsonPath("$.record.photoUrl").isNotEmpty())
                .andExpect(jsonPath("$.record.sleepHours").value(5.5))
                .andExpect(jsonPath("$.analysis.isFallback").value(true))
                .andExpect(jsonPath("$.analysis.score").isNumber())
                .andExpect(jsonPath("$.analysis.levels.redness").isNumber())
                .andExpect(jsonPath("$.analysis.labels").isArray())
                .andExpect(jsonPath("$.analysis.insight").isNotEmpty())
                .andExpect(jsonPath("$.routine.title").isNotEmpty())
                .andExpect(jsonPath("$.routine.status").value("suggested"))
                .andExpect(jsonPath("$.routine.generation").value(1));

        // #4 오늘 결과 재조회
        mockMvc.perform(get("/api/records/today").header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.routine.title").isNotEmpty());

        // 홈: 기록 완료 + 스트릭 1
        mockMvc.perform(get("/api/users/me/home").header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.todayRecorded").value(true))
                .andExpect(jsonPath("$.streakDays").value(1));

        // #3 중복 — 하루 1기록 규칙
        mockMvc.perform(multipart("/api/records").file(photo())
                        .param("sleepHours", "7")
                        .param("hadDrinkOrSnack", "false")
                        .param("stressLevel", "1")
                        .header("X-User-Id", userId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("ALREADY_RECORDED"));

        // #5 목록
        mockMvc.perform(get("/api/records").header("X-User-Id", userId).param("limit", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.records.length()").value(1))
                .andExpect(jsonPath("$.records[0].score").isNumber());
    }

    @Test
    void missingHeaderIs401() throws Exception {
        mockMvc.perform(get("/api/users/me/home"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("MISSING_USER_ID"));
    }

    @Test
    void unknownUserIs401() throws Exception {
        mockMvc.perform(get("/api/users/me/home").header("X-User-Id", UUID.randomUUID().toString()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_USER_ID"));
    }

    @Test
    void malformedUserIdIs401() throws Exception {
        mockMvc.perform(get("/api/users/me/home").header("X-User-Id", "not-a-uuid"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("INVALID_USER_ID"));
    }

    @Test
    void invalidStressLevelIs400() throws Exception {
        String userId = createUser();
        mockMvc.perform(multipart("/api/records").file(photo())
                        .param("sleepHours", "7")
                        .param("hadDrinkOrSnack", "false")
                        .param("stressLevel", "9")
                        .header("X-User-Id", userId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    void missingPhotoIs400() throws Exception {
        String userId = createUser();
        mockMvc.perform(multipart("/api/records")
                        .param("sleepHours", "7")
                        .param("hadDrinkOrSnack", "false")
                        .param("stressLevel", "1")
                        .header("X-User-Id", userId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("MISSING_PART"));
    }

    @Test
    void todayWithoutRecordIs404() throws Exception {
        String userId = createUser();
        mockMvc.perform(get("/api/records/today").header("X-User-Id", userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NO_RECORD_TODAY"));
    }
}
