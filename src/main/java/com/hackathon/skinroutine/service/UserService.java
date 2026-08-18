package com.hackathon.skinroutine.service;

import com.hackathon.skinroutine.common.ApiException;
import com.hackathon.skinroutine.common.KoreaTime;
import com.hackathon.skinroutine.domain.User;
import com.hackathon.skinroutine.dto.UserDtos;
import com.hackathon.skinroutine.repository.DailyRecordRepository;
import com.hackathon.skinroutine.repository.UserRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** #1 · #2 사용자 서비스 — requireUser는 모든 API의 인증 관문이기도 하다 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final DailyRecordRepository recordRepository;
    private final StreakService streakService;

    public UserService(UserRepository userRepository, DailyRecordRepository recordRepository,
                       StreakService streakService) {
        this.userRepository = userRepository;
        this.recordRepository = recordRepository;
        this.streakService = streakService;
    }

    /** #1 익명 사용자 생성 */
    @Transactional
    public User create(String nickname) {
        String clean = (nickname == null || nickname.isBlank()) ? null : nickname.trim();
        if (clean != null && clean.length() > 30) {
            clean = clean.substring(0, 30);
        }
        return userRepository.save(new User(clean, false));
    }

    /**
     * X-User-Id 검증 — 등록 안 된 ID는 401.
     * B·C 패턴: 사용자 확인이 필요한 서비스 메서드는 맨 앞에서 이걸 호출하면 된다.
     */
    @Transactional(readOnly = true)
    public User requireUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("INVALID_USER_ID",
                        "등록되지 않은 사용자예요. POST /api/users 로 ID를 발급받아 주세요."));
    }

    /** #2 홈 화면 데이터 */
    @Transactional(readOnly = true)
    public UserDtos.HomeResponse home(UUID userId) {
        User user = requireUser(userId);
        boolean todayRecorded = recordRepository.existsByUserIdAndRecordDate(userId, KoreaTime.today());
        int streakDays = streakService.currentStreak(userId);
        return new UserDtos.HomeResponse(user.getNickname(), todayRecorded, streakDays);
    }
}
