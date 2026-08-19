package com.hackathon.skinroutine.controller;

import com.hackathon.skinroutine.dto.ReportDtos;
import com.hackathon.skinroutine.service.ReportService;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** #10 리포트 요약 API */
@RestController
@RequestMapping("/api")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/reports/summary")
    public ReportDtos.SummaryResponse summary(@RequestHeader("X-User-Id") UUID userId,
                                             @RequestParam(name = "days", defaultValue = "14") int days) {
        return reportService.summary(userId, days);
    }
}
