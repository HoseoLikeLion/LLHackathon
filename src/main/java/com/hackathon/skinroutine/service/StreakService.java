package com.hackathon.skinroutine.service;

import com.hackathon.skinroutine.common.KoreaTime;
import com.hackathon.skinroutine.repository.DailyRecordRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 연속 기록(스트릭) 계산 — #2 홈, #7 완료 응답에서 공용 (B는 currentStreak 호출만 하면 됨) */
@Service
public class StreakService {

    private final DailyRecordRepository recordRepository;

    public StreakService(DailyRecordRepository recordRepository) {
        this.recordRepository = recordRepository;
    }

    /**
     * 오늘 기록이 있으면 오늘부터, 없으면 어제부터 거꾸로 연속된 날 수를 센다.
     * (오늘 아직 기록 전이라고 어제까지 쌓은 스트릭을 0으로 보여주지 않기 위함)
     */
    @Transactional(readOnly = true)
    public int currentStreak(UUID userId) {
        List<LocalDate> dates = recordRepository.findRecordDatesDesc(userId, PageRequest.of(0, 366));
        if (dates.isEmpty()) {
            return 0;
        }
        LocalDate today = KoreaTime.today();
        LocalDate anchor = dates.get(0);
        if (anchor.isBefore(today.minusDays(1))) {
            return 0; // 마지막 기록이 그저께 이전 — 스트릭 끊김
        }
        int streak = 0;
        LocalDate expected = anchor;
        for (LocalDate date : dates) {
            if (date.equals(expected)) {
                streak++;
                expected = expected.minusDays(1);
            } else {
                break; // (user_id, record_date) UNIQUE라 중복은 없다 — 다르면 공백
            }
        }
        return streak;
    }
}
