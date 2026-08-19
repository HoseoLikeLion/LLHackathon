package com.hackathon.skinroutine.controller;

import com.hackathon.skinroutine.dto.DemoDtos;
import com.hackathon.skinroutine.seed.DemoSeedService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** #11 데모 세션용 API */
@RestController
@RequestMapping("/api")
public class DemoController {

    private final DemoSeedService demoSeedService;

    public DemoController(DemoSeedService demoSeedService) {
        this.demoSeedService = demoSeedService;
    }

    @PostMapping("/demo/session")
    public DemoDtos.SessionResponse createSession() {
        return DemoDtos.SessionResponse.from(demoSeedService.createDemoSession());
    }
}
