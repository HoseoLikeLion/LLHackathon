package com.hackathon.skinroutine.controller;

import com.hackathon.skinroutine.common.ApiException;
import com.hackathon.skinroutine.common.KoreaTime;
import com.hackathon.skinroutine.domain.DailyRecord;
import com.hackathon.skinroutine.domain.Routine;
import com.hackathon.skinroutine.domain.User;
import com.hackathon.skinroutine.dto.RoutineDtos;
import com.hackathon.skinroutine.repository.DailyRecordRepository;
import com.hackathon.skinroutine.repository.RoutineRepository;
import com.hackathon.skinroutine.service.RoutineGenerationService;
import com.hackathon.skinroutine.service.StreakService;
import com.hackathon.skinroutine.service.UserService;

import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController
public class RoutineController {

    private final UserService userService;
    private final DailyRecordRepository dailyRecordRepository;
    private final RoutineRepository routineRepository;
    private final StreakService streakService;
    private final RoutineGenerationService routineGenerationService;

    public RoutineController(UserService userService,
                             DailyRecordRepository dailyRecordRepository,
                             RoutineRepository routineRepository,
                             StreakService streakService,
                             RoutineGenerationService routineGenerationService) {
        this.userService = userService;
        this.dailyRecordRepository = dailyRecordRepository;
        this.routineRepository = routineRepository;
        this.streakService = streakService;
        this.routineGenerationService = routineGenerationService;
    }

    private Routine getTodayRoutine(UUID userId) {
        DailyRecord todayRecord = dailyRecordRepository.findByUserIdAndRecordDate(userId, KoreaTime.today())
                .orElseThrow(() -> ApiException.notFound("NO_ROUTINE_TODAY", "오늘 루틴이 아직 없어요. 먼저 기록을 남겨 주세요."));

        return routineRepository.findTopByRecordIdOrderByGenerationDesc(todayRecord.getId())
                .orElseThrow(() -> ApiException.notFound("NO_ROUTINE_TODAY", "오늘 루틴이 아직 없어요. 먼저 기록을 남겨 주세요."));
    }

    @GetMapping("/api/routines/today")
    public RoutineDtos.RoutineResponse getTodayRoutineApi(@RequestHeader("X-User-Id") UUID userId) {
        userService.requireUser(userId);
        Routine routine = getTodayRoutine(userId);
        return RoutineDtos.RoutineResponse.from(routine);
    }

    @PostMapping("/api/routines/today/complete")
    public RoutineDtos.ActionResponse completeTodayRoutine(@RequestHeader("X-User-Id") UUID userId) {
        User user = userService.requireUser(userId);
        Routine routine = getTodayRoutine(userId);

        routine.markCompleted();
        routineRepository.save(routine);

        int streakDays = streakService.currentStreak(user.getId());
        return RoutineDtos.ActionResponse.from(routine, streakDays);
    }

    @PostMapping("/api/routines/today/defer")
    public RoutineDtos.ActionResponse deferTodayRoutine(@RequestHeader("X-User-Id") UUID userId) {
        User user = userService.requireUser(userId);
        Routine routine = getTodayRoutine(userId);

        routine.markDeferred();
        routineRepository.save(routine);

        int streakDays = streakService.currentStreak(user.getId());
        return RoutineDtos.ActionResponse.from(routine, streakDays);
    }

    @PostMapping("/api/routines/today/alternative")
    public RoutineDtos.RoutineResponse generateAlternativeRoutine(@RequestHeader("X-User-Id") UUID userId) {
        User user = userService.requireUser(userId);
        Routine alternativeRoutine = routineGenerationService.generateAlternative(user);
        return RoutineDtos.RoutineResponse.from(alternativeRoutine);
    }
}