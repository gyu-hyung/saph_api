# API Service — CLAUDE.md

Kotlin + Spring Boot API 서버. 인증·결제·크레딧·영상 업로드·Job 오케스트레이션을 담당한다.

---

## 실행

```bash
# 전제조건: 루트에서 make infra-up (postgres + redis 필요)
./gradlew bootRun --args='--spring.profiles.active=local'
# 포트: 8080
```

---

## 패키지 구조

```
com.bako.api
├── auth/       # 회원가입, 로그인, JWT 발급·갱신
├── member/     # 내 정보 조회, 회원 탈퇴
├── credit/     # 크레딧 잔액 조회·차감·충전
├── payment/    # 토스페이먼츠 PG 연동, 결제 승인
├── video/      # 영상 업로드, 번역 요청, SRT 다운로드, 스트리밍
├── job/        # Job 목록 조회, SSE 진행률 스트림
└── common/     # ApiResponse, ApiException, GlobalExceptionHandler
```

---

## 핵심 설계 결정 (WHY)

### 업로드·번역 2-Step API 분리
`POST /api/video/upload` → `POST /api/video/translate` 로 분리되어 있다.
업로드 후 영상 길이를 분석해 `requiredCreditMin`을 반환하고, 크레딧 부족 시 결제 팝업을 자연스럽게 삽입하기 위함이다. 하나의 API로 합치지 않는다.

### 크레딧 차감: 비관적 락 필수
`credits` 테이블 차감 시 반드시 `SELECT ... FOR UPDATE`를 사용한다.
동시 번역 요청으로 인한 이중 차감 방지. R2DBC에서 `@Transactional`과 함께 사용.

### Job 상태 전이 순서
```
CREATED → (MQ 발행 성공) → QUEUED → (Worker 수신) → PROCESSING → COMPLETED/FAILED
```
MQ 발행 **실패** 시: 크레딧 복구 + `FAILED` 처리. 이 복구 로직을 빠뜨리지 않는다.

### SSE 구현
Worker의 Redis Pub/Sub(`channel:job-progress`) 이벤트를 구독해 클라이언트에 SSE로 전달한다.
WebSocket이 아닌 SSE를 쓰는 이유: 단방향 서버→클라이언트 스트림으로 충분하고 인프라가 단순하다.

### 영상 스트리밍: HTTP Range 지원 필수
`GET /api/video/stream/{jobId}`는 HTML5 `<video>` 시킹을 위해 `206 Partial Content`를 지원해야 한다.
Range 헤더 없으면 `200 OK` + 전체 파일. 범위 초과 시 `416`.

---

## 금지 패턴

- **Blocking call 금지** — WebFlux Coroutines 기반이므로 `Thread.sleep`, JDBC 직접 사용 금지. R2DBC만 사용.
- **크레딧 차감을 FOR UPDATE 없이 하지 않는다** — 레이스 컨디션 발생.
- **파일 경로를 UUID 없이 원본 파일명으로 저장하지 않는다** — 충돌 및 보안 문제.

---

## API 응답 형식

```kotlin
// 성공: ApiResponse.success(data)   → { "code": "SUCCESS", "data": {...} }
// 에러: throw ApiException(ErrorCode.INSUFFICIENT_CREDITS)
//       → { "code": "INSUFFICIENT_CREDITS", "message": "..." }
```

에러 코드는 `common/ApiException.kt`에 ENUM으로 정의한다.

---

## DB 마이그레이션 (Liquibase)

마이그레이션 파일: `src/main/resources/db/changelog/`
생성 순서 준수: ENUM → `members` → `refresh_tokens` → `credits` → `payments` → `jobs` → `credit_logs`
(`credit_logs`가 `jobs`, `payments`를 FK로 참조하므로 반드시 마지막)

---

## 파일 공유 (Worker와의 인터페이스)

`storage/` 디렉터리를 Docker 공유 볼륨으로 API·Worker가 함께 사용한다.
- API: `storage/videos/` 에 업로드 파일 저장 → `jobs.video_path`에 경로 기록
- Worker: `storage/results/` 에 SRT 저장 → 완료 콜백으로 API에 경로 전달
- API: `jobs.original_srt`, `jobs.translated_srt`에 경로 업데이트 후 다운로드 제공
