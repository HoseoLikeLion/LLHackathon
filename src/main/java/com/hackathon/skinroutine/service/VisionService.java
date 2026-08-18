package com.hackathon.skinroutine.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hackathon.skinroutine.config.AppProperties;
import com.hackathon.skinroutine.domain.Analysis;
import com.hackathon.skinroutine.domain.DailyRecord;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * OpenAI Chat Completions 직접 호출 (RestClient + response_format: json_schema — docs/05 스택 확정).
 *
 * 호출 왕복은 1번으로 합쳤다: 분석(score·3축·라벨) + 인사이트 + 오늘의 루틴을 한 스키마로 강제.
 * (docs/05 파이프라인 2~4단계와 논리 순서는 같고, 왕복만 합쳐 지연·실패 지점·비용을 줄임.
 *  #3 응답 예산이 5~10초라 왕복 3번은 위험하다)
 *
 * 실패는 전부 AiUnavailableException — RecordService/RoutineGenerationService가 폴백으로 흡수한다.
 */
@Service
public class VisionService {

    /** AI가 만든 루틴 1개 */
    public record AiRoutine(String title, String reason, String method, Integer expectedMinutes) {}

    /** #3 분석 호출의 전체 결과 (raw = OpenAI 원본 응답, 디버깅 저장용) */
    public record AiOutcome(int score, int redness, int moisture, int oil,
                            List<String> labels, String insight, AiRoutine routine, String raw) {}

    /** AI 호출에 함께 주는 맥락 — RecordService가 조립 */
    public record Context(double sleepHours, boolean hadDrinkOrSnack, int stressLevel,
                          Analysis previous, List<DailyRecord> recentRecords,
                          List<String> recentRoutineTitles) {}

    /** 키 없음·타임아웃·파싱 실패 등 모든 AI 쪽 실패 — 받는 쪽은 폴백으로 진행 */
    public static class AiUnavailableException extends RuntimeException {
        public AiUnavailableException(String message) {
            super(message);
        }

        public AiUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private static final String SYSTEM_PROMPT = """
            너는 피부 웰니스 코치다. 셀피 사진과 오늘의 상태 입력(수면·음주야식·스트레스)을 보고
            피부 컨디션을 분석하고, 오늘 실천할 생활 루틴을 딱 1개만 제안한다.
            규칙:
            - 의료 행위가 아니다. '진단', '치료', '질환', '병원' 같은 의료 단어를 절대 쓰지 않는다.
            - 제품 구매 권유 금지. 생활 루틴(수분 섭취·세안·수면·진정 등)만 제안한다.
            - 모든 문장은 한국어 존댓말로 짧게. insight는 1문장 40자 내외로, 상태 입력과 피부 변화를 잇는 관찰이어야 한다.
            - 인과를 단정하지 말고 "~와 함께 나타났어요", "~일 수 있어요"처럼 관찰로 표현한다.
            - 루틴은 20대 대학생이 오늘 1~10분 안에 바로 할 수 있어야 한다.
            - 직전 분석 정보가 주어지면 labels는 '붉음 증가', '수분 상승'처럼 변화 중심으로 쓴다. 없으면 상태 표현으로 쓴다.
            """;

    // ⚠️ strict 모드 스키마: required에 모든 키, additionalProperties:false 필수.
    //    minimum/maximum은 지원 안 됨 → 범위는 description으로 지시하고 코드에서 clamp.
    static final Map<String, Object> ROUTINE_OBJECT_SCHEMA = Map.of(
            "type", "object",
            "additionalProperties", false,
            "required", List.of("title", "reason", "method", "expectedMinutes"),
            "properties", Map.of(
                    "title", Map.of("type", "string", "description", "루틴 이름, 15자 이내"),
                    "reason", Map.of("type", "string", "description", "오늘 이 루틴을 제안하는 이유 한 문장"),
                    "method", Map.of("type", "string", "description", "실행 방법 한두 문장"),
                    "expectedMinutes", Map.of("type", "integer", "description", "예상 소요 시간(분), 1~10")
            )
    );

    static final Map<String, Object> ANALYSIS_RESPONSE_FORMAT = Map.of(
            "type", "json_schema",
            "json_schema", Map.of(
                    "name", "skin_analysis",
                    "strict", true,
                    "schema", Map.of(
                            "type", "object",
                            "additionalProperties", false,
                            "required", List.of("score", "redness", "moisture", "oil", "labels", "insight", "routine"),
                            "properties", Map.of(
                                    "score", Map.of("type", "integer", "description", "피부 컨디션 점수 0~100"),
                                    "redness", Map.of("type", "integer", "description", "붉음 레벨 1~5, 높을수록 심함"),
                                    "moisture", Map.of("type", "integer", "description", "수분 레벨 1~5, 높을수록 좋음"),
                                    "oil", Map.of("type", "integer", "description", "유분 레벨 1~5, 높을수록 많음"),
                                    "labels", Map.of("type", "array", "items", Map.of("type", "string"),
                                            "description", "짧은 라벨 1~3개, 직전 분석이 있으면 변화 표현('붉음 증가')"),
                                    "insight", Map.of("type", "string", "description", "상태 입력과 피부 변화를 잇는 한 줄 코멘트"),
                                    "routine", ROUTINE_OBJECT_SCHEMA
                            )
                    )
            )
    );

    static final Map<String, Object> ROUTINE_RESPONSE_FORMAT = Map.of(
            "type", "json_schema",
            "json_schema", Map.of(
                    "name", "skin_routine",
                    "strict", true,
                    "schema", ROUTINE_OBJECT_SCHEMA
            )
    );

    private final RestClient openAiRestClient;
    private final AppProperties props;
    private final ObjectMapper objectMapper;

    public VisionService(@Qualifier("openAiRestClient") RestClient openAiRestClient,
                         AppProperties props, ObjectMapper objectMapper) {
        this.openAiRestClient = openAiRestClient;
        this.props = props;
        this.objectMapper = objectMapper;
    }

    /** #3 — 사진 + 맥락 → 분석·인사이트·루틴 한 번에 (JSON 강제) */
    public AiOutcome analyze(byte[] jpegBytes, Context ctx) {
        requireKey();
        String dataUrl = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(jpegBytes);
        List<Map<String, Object>> userContent = List.of(
                Map.of("type", "text", "text", buildAnalysisPrompt(ctx)),
                Map.of("type", "image_url", "image_url", Map.of("url", dataUrl))
        );
        Map<String, Object> body = Map.of(
                "model", props.openai().model(),
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", userContent)
                ),
                "response_format", ANALYSIS_RESPONSE_FORMAT,
                "max_tokens", 800,
                "temperature", 0.5
        );
        CallResult result = call(body);
        return toOutcome(result.json(), result.raw());
    }

    /** #9 — 오늘 분석 결과 기반 재추천 (텍스트만, 이전 제목 제외) */
    public AiRoutine regenerateRoutine(Analysis todayAnalysis, DailyRecord todayRecord,
                                       List<String> excludeTitles) {
        requireKey();
        Map<String, Object> body = Map.of(
                "model", props.openai().model(),
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content",
                                buildAlternativePrompt(todayAnalysis, todayRecord, excludeTitles))
                ),
                "response_format", ROUTINE_RESPONSE_FORMAT,
                "max_tokens", 300,
                "temperature", 0.9
        );
        CallResult result = call(body);
        AiRoutine routine = toRoutine(result.json());
        if (routine.title() == null || routine.title().isBlank()) {
            throw new AiUnavailableException("AI 재추천 응답에 제목이 없음");
        }
        return routine;
    }

    private void requireKey() {
        if (!props.openai().hasKey()) {
            throw new AiUnavailableException("OPENAI_API_KEY 미설정 — 폴백으로 진행");
        }
    }

    private record CallResult(JsonNode json, String raw) {}

    private CallResult call(Map<String, Object> body) {
        String response;
        try {
            response = openAiRestClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + props.openai().apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            // 4xx/5xx·타임아웃·연결 실패 전부 여기로
            throw new AiUnavailableException("OpenAI 호출 실패: " + e.getMessage(), e);
        }
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode message = root.path("choices").path(0).path("message");
            JsonNode refusal = message.path("refusal");
            if (!refusal.isMissingNode() && !refusal.isNull()) {
                throw new AiUnavailableException("OpenAI가 응답을 거부함: " + refusal.asText());
            }
            String content = message.path("content").asText(null);
            if (content == null || content.isBlank()) {
                throw new AiUnavailableException("OpenAI 응답에 content가 없음");
            }
            return new CallResult(objectMapper.readTree(content), response);
        } catch (AiUnavailableException e) {
            throw e;
        } catch (Exception e) {
            throw new AiUnavailableException("OpenAI 응답 파싱 실패: " + e.getMessage(), e);
        }
    }

    private AiOutcome toOutcome(JsonNode json, String raw) {
        AiRoutine routine = toRoutine(json.path("routine"));
        if (routine.title() == null || routine.title().isBlank()) {
            throw new AiUnavailableException("AI 응답에 루틴 제목이 없음");
        }
        List<String> labels = new ArrayList<>();
        json.path("labels").forEach(node -> {
            if (labels.size() < 3 && node.asText() != null && !node.asText().isBlank()) {
                labels.add(node.asText());
            }
        });
        return new AiOutcome(
                clamp(json.path("score").asInt(60), 0, 100),
                clamp(json.path("redness").asInt(3), 1, 5),
                clamp(json.path("moisture").asInt(3), 1, 5),
                clamp(json.path("oil").asInt(3), 1, 5),
                labels,
                json.path("insight").asText(""),
                routine,
                raw);
    }

    private AiRoutine toRoutine(JsonNode node) {
        return new AiRoutine(
                node.path("title").asText(null),
                node.path("reason").asText(""),
                node.path("method").asText(""),
                clamp(node.path("expectedMinutes").asInt(1), 1, 10));
    }

    private String buildAnalysisPrompt(Context ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("오늘 상태 입력:\n");
        sb.append("- 수면: ").append(ctx.sleepHours()).append("시간\n");
        sb.append("- 음주/야식: ").append(ctx.hadDrinkOrSnack() ? "있음" : "없음").append("\n");
        sb.append("- 스트레스: ").append(ctx.stressLevel()).append("/3\n");
        if (ctx.previous() != null) {
            Analysis p = ctx.previous();
            sb.append("직전 분석(").append(p.getRecord().getRecordDate()).append("): 점수 ")
                    .append(p.getScore()).append(", 붉음 ").append(p.getRedness())
                    .append("/5, 수분 ").append(p.getMoisture())
                    .append("/5, 유분 ").append(p.getOil()).append("/5\n");
        }
        if (ctx.recentRecords() != null && !ctx.recentRecords().isEmpty()) {
            sb.append("최근 상태 입력(최신순):\n");
            for (DailyRecord r : ctx.recentRecords()) {
                sb.append("- ").append(r.getRecordDate()).append(": 수면 ").append(r.getSleepHours())
                        .append("h, 음주/야식 ").append(Boolean.TRUE.equals(r.getHadDrinkOrSnack()) ? "O" : "X")
                        .append(", 스트레스 ").append(r.getStressLevel()).append("/3\n");
            }
        }
        if (ctx.recentRoutineTitles() != null && !ctx.recentRoutineTitles().isEmpty()) {
            sb.append("최근 제안했던 루틴(되도록 다른 제안): ")
                    .append(String.join(", ", ctx.recentRoutineTitles())).append("\n");
        }
        sb.append("이 셀피와 위 정보를 바탕으로, 스키마에 맞춰 분석과 오늘의 루틴 1개를 만들어 주세요.");
        return sb.toString();
    }

    private String buildAlternativePrompt(Analysis analysis, DailyRecord record,
                                          List<String> excludeTitles) {
        StringBuilder sb = new StringBuilder();
        if (analysis != null) {
            sb.append("오늘 분석 결과: 점수 ").append(analysis.getScore())
                    .append(", 붉음 ").append(analysis.getRedness())
                    .append("/5, 수분 ").append(analysis.getMoisture())
                    .append("/5, 유분 ").append(analysis.getOil()).append("/5\n");
        }
        sb.append("오늘 상태: 수면 ").append(record.getSleepHours()).append("시간, 음주/야식 ")
                .append(Boolean.TRUE.equals(record.getHadDrinkOrSnack()) ? "있음" : "없음")
                .append(", 스트레스 ").append(record.getStressLevel()).append("/3\n");
        sb.append("사용자가 '다른 루틴 보기'를 눌렀어요. ");
        if (excludeTitles != null && !excludeTitles.isEmpty()) {
            sb.append("다음 제목과 겹치지 않는 ");
        }
        sb.append("새로운 오늘의 루틴 1개를 만들어 주세요.");
        if (excludeTitles != null && !excludeTitles.isEmpty()) {
            sb.append(" 제외할 제목: ").append(String.join(", ", excludeTitles));
        }
        return sb.toString();
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
