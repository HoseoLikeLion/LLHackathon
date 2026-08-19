# skinroutine-backend

피부 웰니스 루틴 서비스 백엔드 (멋사대학 14기 중앙해커톤 · 중앙해커톤B팀)

**사진 + 오늘 상태 3개 → AI 분석 → 오늘의 루틴 딱 1개 → 실천 체크 → 변화 리포트**

- 스택: Java 17 · Spring Boot 3.5 · Gradle · JPA · Supabase(Postgres + Storage) · OpenAI(gpt-4o-mini) · Render 배포
- 서버는 **1개**, 로그인 없음 — 서버가 발급한 ID를 `X-User-Id` 헤더로 보낸다
- 상세 설계 문서: 팀 공유 `05_API_설계.md` / `06_API_설계_요약.md`

---

## 1. 처음 실행하기 (팀원 공통)

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

테스트 실행: `gradlew test` (**29개 — 전부 통과 상태로 유지할 것**. 기능을 추가하면 테스트도 같이 추가)

---

## 2. API 12개 계약 (✅ 12개 전부 구현 완료)

| # | 메서드·경로 | 하는 일 | 상태 | 담당 |
|---|---|---|---|---|
| 1 | `POST /api/users` | 익명 ID 발급 `{nickname?}` → `{userId}` | ✅ | 김동건 |
| 2 | `GET /api/users/me/home` | 홈: `{nickname, todayRecorded, streakDays}` | ✅ | 김동건 |
| 3 | `POST /api/records` | ⭐ 사진+상태 → 저장→분석→루틴 한 번에 (5~10초) | ✅ | 김동건 |
| 4 | `GET /api/records/today` | 오늘 결과 재조회 (없으면 404) | ✅ | 김동건 |
| 5 | `GET /api/records?limit=30` | 기록 이력 `{"records":[...]}` | ✅ | 김동건 |
| 6 | `GET /api/routines/today` | 오늘 루틴 상세 (항상 최신 generation) | ✅ | 고륜 |
| 7 | `POST /api/routines/today/complete` | 완료 체크 → `{status, completedAt, streakDays}` | ✅ | 고륜 |
| 8 | `POST /api/routines/today/defer` | "나중에 할게요" | ✅ | 고륜 |
| 9 | `POST /api/routines/today/alternative` | 다른 루틴 재추천 (generation+1) | ✅ | 고륜 |
| 10 | `GET /api/reports/summary?days=14` | 변화 리포트 | ✅ | 고륜 |
| 11 | `POST /api/demo/session` | 심사용 데모 계정 (20일 시드 자동 생성) | ✅ | 고륜 |
| 12 | `GET /api/health` | 헬스체크 `{"ok":true}` (UptimeRobot 핑 대상) | ✅ | 김동건 |

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
| `config/` | ✅ 완성 | 설정 바인딩 · RestClient(OpenAI/Supabase) · CORS |
| `common/` | ✅ 완성 | KST 날짜(`KoreaTime`) · 예외(`ApiException`) · 에러 변환기 |
| `domain/` | ✅ 완성 | 엔티티 4개: User · DailyRecord · Analysis · Routine |
| `repository/` | ✅ 완성 | 쿼리 메서드 완비 |
| `dto/` | ✅ 완성 | 응답 계약 그대로 — `from(...)` 팩토리 사용 |
| `service/` | ✅ 완성 | 파이프라인 · AI · 폴백 · 스트릭 · 리포트 집계 전부 |
| `controller/` | ✅ 완성 | Health·User·Record(김동건) · Routine·Report·Demo(고륜) |
| `seed/` | ✅ 완성 | `DemoSeedService` — 데모 계정 + 20일 시드 (문구는 상단 배열에서 조절) |

컨트롤러 읽는 순서(쉬움→어려움): `HealthController` → `UserController` → `RecordController`

---

## 4. 실제 구현 — RoutineController (#6·7·8·9)

이 섹션은 루틴 관련 API를 실제로 구현한 방식으로 정리한다. 팀원은 컨트롤러를 읽고 “요청을 받고, 서비스에 넘기고, DTO로 반환하는 구조”를 그대로 따라가면 된다.

- 파일: `controller/RoutineController.java`
- 핵심 책임: 사용자 확인 → 오늘 기록 찾기 → 오늘 루틴 찾기 → 상태 반영 → 응답 생성

### 4-1. 오늘 루틴 조회 (#6)
- 경로: `GET /api/routines/today`
- 헤더: `X-User-Id`
- 동작:
  - 사용자 존재 여부 확인 (`userService.requireUser`)
  - 오늘 `DailyRecord` 조회
  - 그 기록에 연결된 가장 최신 `Routine` 조회
  - `RoutineDtos.RoutineResponse.from(routine)`로 응답

예시 응답:
```json
{
  "id": 1,
  "title": "취침 30분 앞당기기",
  "reason": "수면 부족이 피부 컨디션을 끌어내리고 있어요",
  "method": "오늘은 평소보다 30분 일찍 눕고, 휴대폰은 침대 밖에 두세요",
  "expectedMinutes": 1,
  "status": "suggested",
  "generation": 1
}
```

### 4-2. 루틴 완료 처리 (#7)
- 경로: `POST /api/routines/today/complete`
- 동작:
  - 오늘 루틴을 불러오고
  - `routine.markCompleted()`
  - `routineRepository.save(routine)`
  - `streakService.currentStreak(userId)`로 연속 일수 계산
  - `RoutineDtos.ActionResponse.from(routine, streakDays)` 반환

예시 응답:
```json
{
  "status": "completed",
  "completedAt": "2026-08-19T21:00:00Z",
  "streakDays": 12
}
```

### 4-3. 루틴 미루기 (#8)
- 경로: `POST /api/routines/today/defer`
- 동작:
  - 오늘 루틴 조회
  - `routine.markDeferred()`
  - 저장
  - 현재 streakDays 계산 후 응답

예시 응답:
```json
{
  "status": "deferred",
  "completedAt": null,
  "streakDays": 11
}
```

### 4-4. 다른 루틴 재추천 (#9)
- 경로: `POST /api/routines/today/alternative`
- 동작:
  - 현재 사용자 확인
  - `routineGenerationService.generateAlternative(user)` 실행
  - 새 루틴을 다시 생성하고 응답

예시 응답:
```json
{
  "id": 2,
  "title": "물 500ml 먼저 마시기",
  "reason": "속수분부터 채우면 당김이 줄어요",
  "method": "지금 물 한 컵을 마시고 오늘 1L를 목표로 해요",
  "expectedMinutes": 1,
  "status": "suggested",
  "generation": 2
}
```

### 4-5. 구현 포인트
- 오늘 루틴 조회는 `DailyRecordRepository.findByUserIdAndRecordDate(...)` + `RoutineRepository.findTopByRecordIdOrderByGenerationDesc(...)` 조합으로 처리
- 루틴 상태 변경은 `markCompleted()` / `markDeferred()` 메서드만 호출하면 됨
- 프론트는 응답의 `status`를 기준으로 버튼 상태를 바꾸면 됨
- 예외 발생 시 `ApiException.notFound("NO_ROUTINE_TODAY", ...)`가 자동으로 404 처리됨

---

## 5. 실제 구현 — Report(#10) · Demo(#11) + 시드 값

리포트 화면과 데모 시연용 API. 구현은 “서비스 호출 → DTO 반환”이고, 집계·시드 로직은 service·seed 패키지에 있다.

### 5-1. 리포트 API (#10)
- 파일: `controller/ReportController.java`
- 경로: `GET /api/reports/summary?days=14`
- 헤더: `X-User-Id`
- 동작: `reportService.summary(userId, days)` 호출
- 반환 구조:
  - `latestVsPrevious`: 최근 vs 직전 분석 비교
  - `trends`: 날짜별 점수/3축 변화
  - `routineEffects`: 루틴별 효과 요약

예시 응답:
```json
{
  "latestVsPrevious": {
    "redness": "down",
    "moisture": "up",
    "oil": "same"
  },
  "trends": [
    {
      "date": "2026-08-01",
      "score": 62,
      "redness": 4,
      "moisture": 2,
      "oil": 3
    },
    {
      "date": "2026-08-02",
      "score": 68,
      "redness": 3,
      "moisture": 3,
      "oil": 3
    }
  ],
  "routineEffects": [
    {
      "title": "취침 30분 앞당기기",
      "executedCount": 5,
      "note": "실천한 다음 날 붉음 감소가 함께 나타났어요"
    }
  ]
}
```

### 5-2. 데모 세션 API (#11)
- 파일: `controller/DemoController.java`
- 경로: `POST /api/demo/session`
- 동작:
  - `DemoSeedService.createDemoSession()` 호출
  - 데모 유저 생성
  - `DemoDtos.SessionResponse.from(...)`으로 응답 변환

예시 응답:
```json
{
  "userId": "3b0a7c7d-1d74-4d8e-a8de-1d4c3a9c7b2d",
  "nickname": "데모 체험"
}
```

### 5-3. 시드 데이터 값
- 파일: `seed/DemoSeedService.java`
- 시드 방식:
  - 최근 20일치 데이터 자동 생성
  - 첫 주는 수면 부족, 음주/야식, 스트레스가 높아 피부 붉음·건조 신호가 큼
  - 이후 루틴 실천 여부에 따라 점수와 3축(붉음/수분/유분)이 개선되는 흐름을 시뮬레이션
- 중요 포인트:
  - 데모 계정은 실제 사용자와 분리되어 독립적으로 관리됨
  - `DEMO` 계정은 심사 시연용으로 사용됨
  - 리포트 화면에서 “어떤 루틴이 효과가 있었는지”를 보여주기 위한 값들이 들어 있음

### 5-4. 시드 문구 조절 위치
아래 상수 배열을 수정하면 데모 화면 문구를 손쉽게 바꿀 수 있다.

```java
private static final String[] TITLES = { ... };
private static final String[] REASONS = { ... };
private static final String[] METHODS = { ... };
private static final int[] MINUTES = { ... };
```

예시:
- `수분 보충 케어`
- `저자극 진정 케어`
- `취침 30분 앞당기기`
- `물 500ml 먼저 마시기`

### 5-5. 구현 위치
- `service/ReportService.java`
- `seed/DemoSeedService.java`
- `dto/ReportDtos.java`
- `dto/DemoDtos.java`

### 5-6. 검증
아래 명령으로 테스트를 돌리면 된다.
```bash
./gradlew test --tests 'com.hackathon.skinroutine.service.ReportAndSeedTest'
```

결과:
- 데모 시드 생성
- 리포트 집계
- 스트릭 계산
- 모두 정상 동작

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
