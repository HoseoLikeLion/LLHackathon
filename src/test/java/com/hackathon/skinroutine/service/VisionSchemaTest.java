package com.hackathon.skinroutine.service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

/**
 * OpenAI response_format 스키마의 직렬화 형태 확인.
 * (실제 호출은 키가 있어야 하므로, 요청 본문이 strict 스키마 규칙을 지키는지만 정적으로 검증)
 */
class VisionSchemaTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void analysisSchemaSerializesWithStrictRules() throws Exception {
        String json = objectMapper.writeValueAsString(VisionService.ANALYSIS_RESPONSE_FORMAT);
        assertTrue(json.contains("\"type\":\"json_schema\""));
        assertTrue(json.contains("\"strict\":true"));
        assertTrue(json.contains("\"additionalProperties\":false"));
        assertTrue(json.contains("\"skin_analysis\""));
        // strict 필수 규칙: 모든 최상위 키가 required에 있어야 함
        for (String key : new String[]{"score", "redness", "moisture", "oil", "labels", "insight", "routine"}) {
            assertTrue(json.contains("\"" + key + "\""), key + " 누락");
        }
    }

    @Test
    void routineSchemaSerializesWithStrictRules() throws Exception {
        String json = objectMapper.writeValueAsString(VisionService.ROUTINE_RESPONSE_FORMAT);
        assertTrue(json.contains("\"skin_routine\""));
        assertTrue(json.contains("\"strict\":true"));
        assertTrue(json.contains("\"expectedMinutes\""));
    }
}
