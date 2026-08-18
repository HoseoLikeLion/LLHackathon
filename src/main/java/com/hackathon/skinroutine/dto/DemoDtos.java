package com.hackathon.skinroutine.dto;

import com.hackathon.skinroutine.domain.User;
import java.util.UUID;

/** #11 POST /api/demo/session 응답 — C의 DemoController가 사용 */
public final class DemoDtos {

    /** 프론트는 이 userId를 localStorage에 그대로 넣으면 데모 계정으로 전환된다 */
    public record SessionResponse(UUID userId, String nickname) {
        public static SessionResponse from(User user) {
            return new SessionResponse(user.getId(), user.getNickname());
        }
    }

    private DemoDtos() {}
}
