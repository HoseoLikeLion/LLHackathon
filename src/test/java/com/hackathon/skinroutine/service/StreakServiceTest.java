package com.hackathon.skinroutine.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.hackathon.skinroutine.common.KoreaTime;
import com.hackathon.skinroutine.domain.DailyRecord;
import com.hackathon.skinroutine.domain.User;
import com.hackathon.skinroutine.repository.DailyRecordRepository;
import com.hackathon.skinroutine.repository.UserRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** 스트릭 규칙: 오늘 있으면 오늘부터, 없으면 어제부터 / 하루라도 비면 끊김 */
@SpringBootTest
@ActiveProfiles("local")
class StreakServiceTest {

    @Autowired
    StreakService streakService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    DailyRecordRepository recordRepository;

    private User newUser() {
        return userRepository.save(new User("스트릭테스트", false));
    }

    private void addRecord(User user, LocalDate date) {
        recordRepository.save(new DailyRecord(user, date, null, 7.0, false, 1));
    }

    @Test
    void threeConsecutiveDaysIncludingToday() {
        User user = newUser();
        LocalDate today = KoreaTime.today();
        addRecord(user, today);
        addRecord(user, today.minusDays(1));
        addRecord(user, today.minusDays(2));
        assertEquals(3, streakService.currentStreak(user.getId()));
    }

    @Test
    void countsFromYesterdayWhenTodayNotRecorded() {
        User user = newUser();
        LocalDate today = KoreaTime.today();
        addRecord(user, today.minusDays(1));
        addRecord(user, today.minusDays(2));
        assertEquals(2, streakService.currentStreak(user.getId()));
    }

    @Test
    void zeroWhenLastRecordTwoDaysAgo() {
        User user = newUser();
        LocalDate today = KoreaTime.today();
        addRecord(user, today.minusDays(2));
        addRecord(user, today.minusDays(3));
        assertEquals(0, streakService.currentStreak(user.getId()));
    }

    @Test
    void zeroWhenNoRecords() {
        User user = newUser();
        assertEquals(0, streakService.currentStreak(user.getId()));
    }

    @Test
    void gapBreaksStreak() {
        User user = newUser();
        LocalDate today = KoreaTime.today();
        addRecord(user, today);
        addRecord(user, today.minusDays(1));
        addRecord(user, today.minusDays(3)); // 하루 공백
        assertEquals(2, streakService.currentStreak(user.getId()));
    }
}
