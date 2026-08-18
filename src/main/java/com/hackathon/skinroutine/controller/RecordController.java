package com.hackathon.skinroutine.controller;

import com.hackathon.skinroutine.dto.RecordDtos;
import com.hackathon.skinroutine.service.RecordService;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** #3 · #4 · #5 기록 API (담당 A — 코어 파이프라인) */
@RestController
@RequestMapping("/api/records")
public class RecordController {

    private final RecordService recordService;

    public RecordController(RecordService recordService) {
        this.recordService = recordService;
    }

    /**
     * #3 오늘 기록 만들기 (multipart/form-data)
     * - 파트: photo(파일) + sleepHours + hadDrinkOrSnack + stressLevel
     * - AI 분석까지 한 번에 돌아 5~10초 걸림 → 프론트는 로딩 화면 필수
     * - 같은 날 두 번째 호출은 409 ALREADY_RECORDED
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RecordDtos.DetailResponse create(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestPart("photo") MultipartFile photo,
            @RequestParam("sleepHours")
            @DecimalMin(value = "0", message = "수면 시간은 0 이상이어야 해요")
            @DecimalMax(value = "24", message = "수면 시간은 24 이하여야 해요") double sleepHours,
            @RequestParam("hadDrinkOrSnack") boolean hadDrinkOrSnack,
            @RequestParam("stressLevel")
            @Min(value = 1, message = "스트레스 레벨은 1~3이에요")
            @Max(value = 3, message = "스트레스 레벨은 1~3이에요") int stressLevel) {
        return recordService.create(userId, photo, sleepHours, hadDrinkOrSnack, stressLevel);
    }

    /** #4 오늘 결과 다시 보기 — 없으면 404 NO_RECORD_TODAY */
    @GetMapping("/today")
    public RecordDtos.DetailResponse today(@RequestHeader("X-User-Id") UUID userId) {
        return recordService.today(userId);
    }

    /** #5 지난 기록 목록 — ?limit=30 (1~100) */
    @GetMapping
    public RecordDtos.ListResponse list(@RequestHeader("X-User-Id") UUID userId,
                                        @RequestParam(name = "limit", defaultValue = "30") int limit) {
        return recordService.list(userId, limit);
    }
}
