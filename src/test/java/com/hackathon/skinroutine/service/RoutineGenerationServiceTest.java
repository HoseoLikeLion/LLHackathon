package com.hackathon.skinroutine.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.hackathon.skinroutine.common.ApiException;
import com.hackathon.skinroutine.common.KoreaTime;
import com.hackathon.skinroutine.domain.DailyRecord;
import com.hackathon.skinroutine.domain.Routine;
import com.hackathon.skinroutine.domain.User;
import com.hackathon.skinroutine.repository.DailyRecordRepository;
import com.hackathon.skinroutine.repository.RoutineRepository;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

/** #9 재추천 로직 검증 — B의 RoutineController가 호출할 generateAlternative()의 계약 확인 */
@SpringBootTest
@ActiveProfiles("local")
class RoutineGenerationServiceTest {

    @Autowired
    RoutineGenerationService routineGenerationService;

    @Autowired
    RecordService recordService;

    @Autowired
    UserService userService;

    @Autowired
    DailyRecordRepository recordRepository;

    @Autowired
    RoutineRepository routineRepository;

    private static MockMultipartFile photo() throws IOException {
        BufferedImage image = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(new Color(200, 170, 150));
        g.fillRect(0, 0, 64, 64);
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", out);
        return new MockMultipartFile("photo", "selfie.jpg", MediaType.IMAGE_JPEG_VALUE, out.toByteArray());
    }

    @Test
    void alternativeCreatesNextGenerationWithDifferentTitle() throws Exception {
        User user = userService.create("김멋사");
        recordService.create(user.getId(), photo(), 5.0, true, 3); // 폴백 경로로 1세대 생성

        Routine alternative = routineGenerationService.generateAlternative(user);

        assertEquals(2, alternative.getGeneration());
        DailyRecord record = recordRepository
                .findByUserIdAndRecordDate(user.getId(), KoreaTime.today()).orElseThrow();
        Routine top = routineRepository
                .findTopByRecordIdOrderByGenerationDesc(record.getId()).orElseThrow();
        assertEquals(alternative.getId(), top.getId()); // "오늘의 루틴"은 항상 최신 세대

        List<Routine> all = routineRepository.findByRecordId(record.getId());
        assertEquals(2, all.size());
        assertNotEquals(all.get(0).getTitle(), all.get(1).getTitle()); // 이전 제목 회피
    }

    @Test
    void alternativeWithoutTodayRecordThrows404() {
        User user = userService.create(null);
        ApiException exception = assertThrows(ApiException.class,
                () -> routineGenerationService.generateAlternative(user));
        assertEquals("NO_RECORD_TODAY", exception.getCode());
    }
}
