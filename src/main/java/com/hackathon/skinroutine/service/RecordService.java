package com.hackathon.skinroutine.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackathon.skinroutine.common.ApiException;
import com.hackathon.skinroutine.common.KoreaTime;
import com.hackathon.skinroutine.domain.Analysis;
import com.hackathon.skinroutine.domain.DailyRecord;
import com.hackathon.skinroutine.domain.Routine;
import com.hackathon.skinroutine.domain.User;
import com.hackathon.skinroutine.dto.RecordDtos;
import com.hackathon.skinroutine.dto.RoutineDtos;
import com.hackathon.skinroutine.repository.AnalysisRepository;
import com.hackathon.skinroutine.repository.DailyRecordRepository;
import com.hackathon.skinroutine.repository.RoutineRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * #3 · #4 · #5 — 기록·분석 코어 파이프라인 (팀장 A, 최대 난이도 구간).
 *
 * #3 순서(docs/05): 검증 → 리사이즈 → 사진 업로드 → 기록 저장 → AI 분석·루틴 생성 → 응답 조립.
 * AI가 어떤 이유로든 실패하면 룰 기반 폴백으로 응답한다(isFallback=true) — 절대 빈손으로 응답하지 않는다.
 *
 * 참고: 메서드 전체에 @Transactional을 걸지 않은 건 의도 — AI 호출이 5~10초라
 * 그동안 DB 커넥션을 물고 있지 않기 위해 저장 단위별로 트랜잭션을 짧게 끊는다.
 */
@Service
public class RecordService {

    private static final Logger log = LoggerFactory.getLogger(RecordService.class);
    private static final int RAW_MAX_LENGTH = 20000;

    private final UserService userService;
    private final ImageService imageService;
    private final StorageService storageService;
    private final VisionService visionService;
    private final RoutineGenerationService routineGenerationService;
    private final DailyRecordRepository recordRepository;
    private final AnalysisRepository analysisRepository;
    private final RoutineRepository routineRepository;
    private final ObjectMapper objectMapper;

    public RecordService(UserService userService, ImageService imageService,
                         StorageService storageService, VisionService visionService,
                         RoutineGenerationService routineGenerationService,
                         DailyRecordRepository recordRepository,
                         AnalysisRepository analysisRepository,
                         RoutineRepository routineRepository, ObjectMapper objectMapper) {
        this.userService = userService;
        this.imageService = imageService;
        this.storageService = storageService;
        this.visionService = visionService;
        this.routineGenerationService = routineGenerationService;
        this.recordRepository = recordRepository;
        this.analysisRepository = analysisRepository;
        this.routineRepository = routineRepository;
        this.objectMapper = objectMapper;
    }

    /** #3 오늘 기록 만들기 — 같은 날 두 번째 요청은 409 ALREADY_RECORDED */
    public RecordDtos.DetailResponse create(UUID userId, MultipartFile photo,
                                            double sleepHours, boolean hadDrinkOrSnack, int stressLevel) {
        User user = userService.requireUser(userId);
        LocalDate today = KoreaTime.today();
        if (recordRepository.existsByUserIdAndRecordDate(userId, today)) {
            throw ApiException.conflict("ALREADY_RECORDED",
                    "오늘은 이미 기록했어요. 오늘 결과는 GET /api/records/today 로 볼 수 있어요.");
        }

        byte[] jpeg = imageService.toResizedJpeg(photo);
        String photoUrl = storageService.uploadJpeg(jpeg, userId, today); // 실패 시 null (기록은 살림)

        DailyRecord record;
        try {
            record = recordRepository.save(
                    new DailyRecord(user, today, photoUrl, sleepHours, hadDrinkOrSnack, stressLevel));
        } catch (DataIntegrityViolationException e) {
            // 더블클릭 등 동시 요청 경쟁 — UNIQUE(user_id, record_date)가 최종 방어
            throw ApiException.conflict("ALREADY_RECORDED", "오늘은 이미 기록했어요.");
        }

        Analysis previous = latestAnalysisBefore(userId, today);
        Analysis analysis;
        Routine routine;
        try {
            VisionService.Context ctx = new VisionService.Context(sleepHours, hadDrinkOrSnack,
                    stressLevel, previous, recentRecords(userId, today), recentRoutineTitles(userId));
            VisionService.AiOutcome ai = visionService.analyze(jpeg, ctx);
            analysis = analysisRepository.save(new Analysis(record, ai.score(), ai.redness(),
                    ai.moisture(), ai.oil(), toJson(ai.labels()), ai.insight(), false,
                    truncateRaw(ai.raw())));
            routine = routineGenerationService.saveRoutine(user, record, ai.routine().title(),
                    ai.routine().reason(), ai.routine().method(), ai.routine().expectedMinutes(), 1);
        } catch (Exception e) {
            log.warn("AI 분석 실패 → 룰 기반 폴백으로 응답 (isFallback=true): {}", e.toString());
            RoutineGenerationService.FallbackOutcome fb =
                    routineGenerationService.fallback(record, previous, List.of());
            analysis = analysisRepository.save(new Analysis(record, fb.score(), fb.redness(),
                    fb.moisture(), fb.oil(), toJson(fb.labels()), fb.insight(), true, null));
            routine = routineGenerationService.saveRoutine(user, record, fb.routine().title(),
                    fb.routine().reason(), fb.routine().method(), fb.routine().minutes(), 1);
        }
        return assemble(record, analysis, routine);
    }

    /** #4 오늘 결과 다시 보기 — 없으면 404 NO_RECORD_TODAY */
    @Transactional(readOnly = true)
    public RecordDtos.DetailResponse today(UUID userId) {
        userService.requireUser(userId);
        DailyRecord record = recordRepository.findByUserIdAndRecordDate(userId, KoreaTime.today())
                .orElseThrow(() -> ApiException.notFound("NO_RECORD_TODAY", "오늘 기록이 아직 없어요."));
        Analysis analysis = analysisRepository.findByRecordId(record.getId()).orElse(null);
        Routine routine = routineRepository.findTopByRecordIdOrderByGenerationDesc(record.getId())
                .orElse(null);
        return assemble(record, analysis, routine);
    }

    /** #5 지난 기록 목록 — limit 1~100 (기본 30) */
    @Transactional(readOnly = true)
    public RecordDtos.ListResponse list(UUID userId, int limit) {
        userService.requireUser(userId);
        int size = Math.max(1, Math.min(limit, 100));
        List<DailyRecord> records =
                recordRepository.findByUserIdOrderByRecordDateDesc(userId, PageRequest.of(0, size));
        // 기록당 분석 1건 추가 조회 — 최대 100건 규모라 해커톤 범위에선 단순함이 이득
        List<RecordDtos.SummaryItem> items = records.stream().map(r -> {
            Analysis a = analysisRepository.findByRecordId(r.getId()).orElse(null);
            return new RecordDtos.SummaryItem(r.getId(), r.getRecordDate(), r.getPhotoUrl(),
                    a == null ? null : a.getScore(),
                    a == null ? List.<String>of() : parseLabels(a.getLabelsJson()),
                    a == null ? null : a.isFallback());
        }).toList();
        return new RecordDtos.ListResponse(items);
    }

    /** #4 · #3 응답 조립 — 분석/루틴이 비어 있어도(중간 장애 흔적) 기록만이라도 내려준다 */
    private RecordDtos.DetailResponse assemble(DailyRecord record, Analysis analysis, Routine routine) {
        RecordDtos.AnalysisPart analysisPart = analysis == null ? null
                : RecordDtos.AnalysisPart.of(analysis, parseLabels(analysis.getLabelsJson()));
        RoutineDtos.RoutineResponse routinePart =
                routine == null ? null : RoutineDtos.RoutineResponse.from(routine);
        return new RecordDtos.DetailResponse(RecordDtos.RecordPart.from(record), analysisPart, routinePart);
    }

    private Analysis latestAnalysisBefore(UUID userId, LocalDate before) {
        List<Analysis> found = analysisRepository.findLatestBefore(userId, before, PageRequest.of(0, 1));
        return found.isEmpty() ? null : found.get(0);
    }

    /** 프롬프트용 최근 7일 상태 — 방금 만든 오늘 기록은 제외 */
    private List<DailyRecord> recentRecords(UUID userId, LocalDate today) {
        return recordRepository.findByUserIdOrderByRecordDateDesc(userId, PageRequest.of(0, 8))
                .stream().filter(r -> !today.equals(r.getRecordDate())).limit(7).toList();
    }

    private List<String> recentRoutineTitles(UUID userId) {
        return routineRepository.findTop10ByUserIdOrderByIdDesc(userId)
                .stream().map(Routine::getTitle).distinct().limit(5).toList();
    }

    private String toJson(List<String> labels) {
        try {
            return objectMapper.writeValueAsString(labels == null ? List.of() : labels);
        } catch (Exception e) {
            log.warn("labels 직렬화 실패: {}", e.toString());
            return null;
        }
    }

    private List<String> parseLabels(String labelsJson) {
        if (labelsJson == null || labelsJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(labelsJson, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("labels 파싱 실패: {}", e.toString());
            return List.of();
        }
    }

    private String truncateRaw(String raw) {
        if (raw == null) {
            return null;
        }
        return raw.length() > RAW_MAX_LENGTH ? raw.substring(0, RAW_MAX_LENGTH) : raw;
    }
}
