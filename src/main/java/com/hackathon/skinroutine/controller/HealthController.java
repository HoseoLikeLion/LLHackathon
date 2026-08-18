package com.hackathon.skinroutine.controller;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * #12 GET /api/health — 배포 확인 + UptimeRobot 10분 핑 대상 (담당 C · 견본으로 완성해 둠)
 *
 * [B·C 온보딩] 컨트롤러 읽는 순서: 이 파일 → UserController → RecordController
 * (쉬움 → 어려움). 새 컨트롤러도 같은 패턴으로 만들면 된다. 자세한 가이드는 README.md.
 */
@RestController
public class HealthController {

    @GetMapping("/api/health")
    public Map<String, Boolean> health() {
        return Map.of("ok", true);
    }
}
