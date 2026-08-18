# skinroutine-backend

피부 웰니스 루틴 서비스 백엔드 (멋사대학 14기 중앙해커톤 · 중앙해커톤B팀)

**사진 + 오늘 상태 3개 → AI 분석 → 오늘의 루틴 딱 1개 → 실천 체크 → 변화 리포트**

- 스택: Java 17 · Spring Boot 3.5 · Gradle · JPA · Supabase(Postgres + Storage) · OpenAI(gpt-4o-mini) · Render 배포
- 서버는 **1개**, 로그인 없음 — 서버가 발급한 ID를 `X-User-Id` 헤더로 보낸다
- 상세 설계 문서: 팀 공유 `05_API_설계.md` / `06_API_설계_요약.md`

---

## 1. 처음 실행하기 (B·C의 첫 과제)

준비물: **JDK 17 이상** (없으면 [Adoptium Temurin 17](https://adoptium.net) 설치. IntelliJ가 있으면 File > Project Structure에서 다운로드 가능)

> ⚠️ **클론 경로에 한글이 있으면 안 됩니다.**
> 예: `C:\Users\홍길동\...`, `바탕 화면\멋사\...` → 빌드는 되는데 **테스트가 전부 ClassNotFoundException으로 깨집니다** (Gradle 테스트 워커의 한글 클래스패스 버그, 실제로 겪음).
> `C:\dev\skinroutine-backend` 같은 **영문 경로**에 클론하세요.

```bash
git clone <레포 주소>
cd skinroutine-backend
gradlew bootRun --args="--spring.profiles.active=local"
```

브라우저에서 http://localhost:8080/api/health 열어서 `{"ok":true}` 나오면 성공! 🎉

**local 프로필은 아무 키·계정 없이 동작합니다:**

| 구성 | local 프로필에서는 |
|---|---|
| DB | H2 인메모리 (서버 끄면 초기화). 웹 콘솔 http://localhost:8080/h2-console — JDBC URL `jdbc:h2:mem:skinroutine`, 사용자 `sa`, 비밀번호 없음 |
| 사진 | `./local-uploads` 폴더 저장 + `http://localhost:8080/local-photos/**` 로 서빙 |
| AI | `OPENAI_API_KEY` 없으면 자동으로 **룰 기반 폴백 루틴** 응답 (`isFallback: true`) — 에러 아니고 정상 동작 |

테스트 실행: `gradlew test` (18개 — 전부 통과 상태로 유지할 것)

---

## 2. API 12개 계약 (구현 현황 · 담당)

| # | 메서드·경로 | 하는 일 | 상태 | 담당 |
|---|---|---|---|---|
| 1 | `POST /api/users` | 익명 ID 발급 `{nickname?}` → `{userId}` | ✅ 완성 (견본) | C 참고용 |
| 2 | `GET /api/users/me/home` | 홈: `{nickname, todayRecorded, streakDays}` | ✅ 완성 (견본) | C 참고용 |
| 3 | `POST /api/records` | ⭐ 사진+상태 → 저장→분석→루틴 한 번에 (5~10초) | ✅ 완성 | A |
| 4 | `GET /api/records/today` | 오늘 결과 재조회 (없으면 404) | ✅ 완성 | A |
| 5 | `GET /api/records?limit=30` | 기록 이력 `{"records":[...]}` | ✅ 완성 | A |
| 6 | `GET /api/routines/today` | 오늘 루틴 상세 | 🔲 **B가 구현** | B |
| 7 | `POST /api/routines/today/complete` | 완료 체크 → `{status, completedAt, streakDays}` | 🔲 **B가 구현** | B |
| 8 | `POST /api/routines/today/defer` | "나중에 할게요" | 🔲 **B가 구현** | B |
| 9 | `POST /api/routines/today/alternative` | 다른 루틴 재추천 (로직은 A가 만들어 둠) | 🔲 **B가 구현** | B |
| 10 | `GET /api/reports/summary?days=14` | 변화 리포트 (집계는 A가 만들어 둠) | 🔲 **C가 구현** | C |
| 11 | `POST /api/demo/session` | 심사용 데모 계정 (시드는 A가 만들어 둠) | 🔲 **C가 구현** | C |
| 12 | `GET /api/health` | 헬스체크 `{"ok":true}` | ✅ 완성 (견본) | C 참고용 |

**에러 계약** (전부 자동 처리됨 — `GlobalExceptionHandler`):

```json
{"error": {"code": "ALREADY_RECORDED", "message": "오늘은 이미 기록했어요. ..."}}
```

| HTTP | code | 언제 |
|---|---|---|
| 401 | `MISSING_USER_ID` / `INVALID_USER_ID` | X-User-Id 없음 / 형식 오류·미등록 |
| 404 | `NO_RECORD_TODAY` | 오늘 기록 없는데 #4·#9 호출 |
| 409 | `ALREADY_RECORDED` | 같은 날 #3 두 번째 호출 (하루 1기록) |
| 400 | `VALIDATION_ERROR` / `MISSING_PART` / `INVALID_IMAGE` 등 | 값 범위·파일 누락·깨진 이미지 |

**#3 실제 응답 예시** (local에서 실측):

```json
{
  "record": {"id": 1, "recordDate": "2026-08-17", "photoUrl": "http://.../2026-08-17-00e40c5b.jpg",
             "sleepHours": 5.5, "hadDrinkOrSnack": true, "stressLevel": 2},
  "analysis": {"score": 56, "levels": {"redness": 4, "moisture": 2, "oil": 3},
               "labels": ["수면 부족", "음주·야식 영향"],
               "insight": "수면이 부족한 날은 피부 회복력이 떨어지기 쉬워요.", "isFallback": true},
  "routine": {"id": 1, "title": "취침 30분 앞당기기", "reason": "수면 부족이 피부 컨디션을 끌어내리고 있어요",
              "method": "오늘은 평소보다 30분 일찍 눕고, 자기 전 휴대폰은 침대 밖에 두세요",
              "expectedMinutes": 1, "status": "suggested", "generation": 1}
}
```

---

## 3. 폴더 구조 (누가 어디를 만지나)

패키지 루트 `com.hackathon.skinroutine` — 흐름: controller → service → repository → domain

| 패키지 | 상태 | 내용 |
|---|---|---|
| `config/` | ✅ A 완성 | 설정 바인딩 · RestClient(OpenAI/Supabase) · CORS |
| `common/` | ✅ A 완성 | KST 날짜(`KoreaTime`) · 예외(`ApiException`) · 에러 변환기 |
| `domain/` | ✅ A 완성 | 엔티티 4개: User · DailyRecord · Analysis · Routine |
| `repository/` | ✅ A 완성 | 쿼리 메서드까지 준비됨 (B·C는 호출만) |
| `dto/` | ✅ A 완성 | 응답 계약 그대로 — B·C는 `from(...)` 팩토리만 쓰면 됨 |
| `service/` | ✅ A 완성 | 파이프라인 · AI · 폴백 · 스트릭 · 리포트 집계 전부 |
| `controller/` | 절반 | Health·User·Record ✅ / **Routine = B** / **Report·Demo = C** |
| `seed/` | 틀 완성 | `DemoSeedService` — **시나리오 값 다듬기 = C** |

컨트롤러 읽는 순서(쉬움→어려움): `HealthController` → `UserController` → `RecordController`

---

## 4. B 가이드 — RoutineController (#6·7·8·9)

새 파일 `controller/RoutineController.java` 하나만 만들면 됩니다. **어려운 로직은 전부 이미 만들어져 있어서, 아래 재료를 호출만 하면 됩니다.**

| 재료 (이미 완성) | 용도 |
|---|---|
| `userService.requireUser(userId)` | 사용자 확인 (401 자동) |
| `recordRepository.findByUserIdAndRecordDate(userId, KoreaTime.today())` | 오늘 기록 찾기 |
| `routineRepository.findTopByRecordIdOrderByGenerationDesc(recordId)` | **오늘의 루틴** (항상 최신 세대) |
| `routine.markCompleted()` / `routine.markDeferred()` → `routineRepository.save(routine)` | #7 · #8 상태 변경 |
| `streakService.currentStreak(userId)` | #7 응답의 streakDays |
| `routineGenerationService.generateAlternative(user)` | **#9 전부** (AI 재추천+폴백+저장) |
| `RoutineDtos.RoutineResponse.from(routine)` / `RoutineDtos.ActionResponse.from(routine, streak)` | 응답 만들기 |

만들 메서드 4개 (경로·반환 타입 계약):

```java
@GetMapping("/api/routines/today")                  // #6 → RoutineDtos.RoutineResponse
@PostMapping("/api/routines/today/complete")        // #7 → RoutineDtos.ActionResponse
@PostMapping("/api/routines/today/defer")           // #8 → RoutineDtos.ActionResponse
@PostMapping("/api/routines/today/alternative")     // #9 → RoutineDtos.RoutineResponse
```

- 오늘 기록·루틴이 없으면: `throw ApiException.notFound("NO_ROUTINE_TODAY", "오늘 루틴이 아직 없어요. 먼저 기록을 남겨 주세요.");`
- 힌트: #6을 먼저 완성하고, #7·#8은 #6에서 찾은 루틴에 `markCompleted()`/`markDeferred()`만 추가. #9는 `generateAlternative` 호출 한 줄이 핵심.

## 5. C 가이드 — Report(#10) · Demo(#11) + 시드 값

**둘 다 "서비스 호출 → DTO 반환" 한 줄짜리입니다.** `UserController`를 열어 놓고 똑같이 따라 만드세요.

- `controller/ReportController.java` — `GET /api/reports/summary?days=14`(기본값 14)
  → `reportService.summary(userId, days)` 반환 (집계 로직은 이미 완성)
- `controller/DemoController.java` — `POST /api/demo/session`
  → `DemoDtos.SessionResponse.from(demoSeedService.createDemoSession())` 반환
- 시드 시나리오 다듬기: `seed/DemoSeedService.java` 상단의 `TITLES / REASONS / METHODS / MINUTES` 배열 (index를 맞춰 수정)

---

## 6. 배포 (Render) — 팀장

1. Render Web Service 생성 → 이 레포 연결 (루트의 `Dockerfile` 자동 인식)
2. Environment에 `.env.example` 목록의 키 등록 (**키를 절대 코드·커밋에 넣지 않기**)
3. ⚠️ Supabase DB 접속은 **Session pooler** 주소 사용 — Direct connection은 IPv6 전용이라 Render에서 연결 불가
4. Supabase Storage에서 `photos` 버킷을 **Public**으로 생성
5. UptimeRobot 무료 플랜 → `GET /api/health` 10분 간격 핑 (무료 티어 15분 슬립 방지 — "행사 종료까지 정상 작동" 요건)
6. 제출 전: `CORS_ALLOWED_ORIGINS`를 프론트 배포 도메인으로 좁히기

## 7. 협업 규칙 (STEP07)

- 브랜치: `feat/이름-기능` (예: `feat/yoon-routine-api`) → PR → **리뷰 1명 승인 후 main 머지** (main 직접 푸시 금지)
- 커밋 메시지: `feat:` `fix:` `docs:` `refactor:` 형식
- 키·비밀번호가 실수로 커밋되면 **즉시 팀장에게** (히스토리에서 지워야 함 — 파일 삭제 커밋으로는 안 지워짐)
