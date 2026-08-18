package com.hackathon.skinroutine.dto;

import com.hackathon.skinroutine.domain.User;
import java.util.UUID;

/** #1 · #2 사용자 API 요청/응답 (계약: docs/05) */
public final class UserDtos {

    /** #1 POST /api/users 요청 — {"nickname":"..."} (본문·닉네임 모두 생략 가능) */
    public record CreateRequest(String nickname) {}

    /** #1 응답 — {"userId":"..."} — 프론트는 이 값을 localStorage에 보관 */
    public record CreateResponse(UUID userId) {
        public static CreateResponse from(User user) {
            return new CreateResponse(user.getId());
        }
    }

    /** #2 GET /api/users/me/home 응답 */
    public record HomeResponse(String nickname, boolean todayRecorded, int streakDays) {}

    private UserDtos() {}
}
