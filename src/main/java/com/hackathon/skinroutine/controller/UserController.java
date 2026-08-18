package com.hackathon.skinroutine.controller;

import com.hackathon.skinroutine.dto.UserDtos;
import com.hackathon.skinroutine.service.UserService;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * #1 · #2 사용자 API (담당 C · 견본으로 완성해 둠)
 *
 * [B·C 패턴] 사용자 확인이 필요한 API는 @RequestHeader("X-User-Id") UUID userId 를 받아
 * 서비스에 그대로 넘긴다. 401 처리는 서비스의 requireUser + GlobalExceptionHandler가 알아서 한다.
 * 컨트롤러는 "받고 → 서비스 호출하고 → DTO로 돌려주기"만 한다 (로직 금지).
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** #1 익명 사용자 생성 — 본문 {"nickname":"..."} 은 생략 가능 */
    @PostMapping
    public UserDtos.CreateResponse create(@RequestBody(required = false) UserDtos.CreateRequest request) {
        String nickname = request == null ? null : request.nickname();
        return UserDtos.CreateResponse.from(userService.create(nickname));
    }

    /** #2 홈 화면 데이터 — {nickname, todayRecorded, streakDays} */
    @GetMapping("/me/home")
    public UserDtos.HomeResponse home(@RequestHeader("X-User-Id") UUID userId) {
        return userService.home(userId);
    }
}
