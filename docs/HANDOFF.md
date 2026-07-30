# HANDOFF — 호텔 PMS + 부킹엔진 (Day 3 + 결제)

- 작성일: 2026-07-31 (Day 3 이어서 결제 모듈 추가)
- 브랜치: **`dev`** (개발선). `main` 은 안정 기준선·원격 기본 브랜치 — D-033.
- **함께 로드할 파일 (중복 수록 안 함 — 반드시 읽을 것)**
  - `docs/decisions.md` — 설계 결정 D-001~D-033 전체
  - `docs/troubleshooting.md` — 이 환경의 반복 함정
  - `src/main/resources/db/migration/V1__init_schema.sql`, `V2__night_close.sql` — 스키마 + 근거 주석
  - `git log` — 검증 수치와 판단 근거가 커밋 본문에 있음

---

## 1. 현재 상태 요약

예약 도메인이 **HOLD → CONFIRMED → CHECKED_IN → CHECKED_OUT**(+ CANCELLED/EXPIRED) 라이프사이클
전 구간과, 그 위의 **백오피스 화면 · 부킹엔진 REST · 회원 JWT 인증**까지 섰다. 핵심 3과제
(오버부킹 0 · HOLD 만료 복원 · 마감 멱등)는 Day 2에 완성됐고, Day 3은 그 위에 취소·조회·
체크인아웃·화면·REST·인증을 얹었다. 전 기능이 Testcontainers(실제 MySQL) 통합테스트로 검증됨.

결제 모듈 완료(2026-07-31, D-034): 토스 승인이 확정 트리거. 2중 금액 위변조 검증(요청·응답
vs 서버 저장액) · 3겹 멱등(선조회+UNIQUE+확정서비스) · 웹훅 사후 정합 · 확정 REST 노출.
`POST /api/payments/confirm`·`/webhook`. 전체 스위트 71건 통과(실제 MySQL, 토스만 mock).
토스 키는 환경변수/`application-local.yml`(local 프로파일, gitignore)로만 — 커밋 안 됨.

Day 3 커밋 8건 (dev):
```
9c97a0d  feat: add member JWT authentication (signup, login, /api/me)   (D-032)
b5714d4  feat: add check-in/check-out with room assignment              (D-031)
4d85d8a  feat: add booking engine REST (availability, hold, lookup)      (D-030)
ec15b39  feat: add cancel action to backoffice reservation detail
8c930a9  feat: add backoffice reservation list and detail screens        (D-029)
66ed234  feat: add guest reservation lookup by reservation-no + phone    (D-028)
a9faafd  feat: add reservation cancel with state-based inventory release (D-027)
a50dff2  feat: add night-close batch with proven posting idempotency     (D-026)
```

---

## 2. 핵심 맥락 (참조 파일에 없는 것만)

### 2.1 작업 방식 — Day 1~2와 동일
답변 한국어 · 착수 전 예상 시간/단계 고지 · 단계마다 끊어 보고 · "계속할까요?" 묻지 않고
진행(되돌리기 어렵거나 외부 영향만 먼저 확인) · JPA/Spring 용어 설명 곁들임.

### 2.2 환경 변화 (Day 2 대비)
- **리포 삭제·재생성됨.** 원격 `github.com/JEONGKYU-CHOI/hotel_erp` 를 지우고 동명으로 다시
  만들었다(D-033). **`main`(기본) + `dev`(개발)** 두 브랜치, 둘 다 클린 히스토리
  (Claude author 0 · Co-authored-by 트레일러 0). 로컬도 정리 완료. 앞으로 커밋은 `dev` 에.
- **JWT 도입(D-032).** `app.jwt.secret` 은 개발 기본값이 박혀 있으나 **운영은 환경변수
  `JWT_SECRET` 주입 필수**. `app.jwt.access-token-validity: PT1H`. 의존성 jjwt 0.12.6 추가.
- **V2 마이그레이션 추가**(`night_close`, D-026). 스키마 변경 시 엔티티와 함께 고칠 것(D-005).
- **테스트 존재**: 예약(동시성·만료·확정·마감·취소·조회·체크인아웃) + 부킹 REST + 회원 인증 +
  백오피스 화면. 전부 Testcontainers MySQL 8.4, Docker 필요.

### 2.3 도메인·패키지 지도 (코드에 있지만 길잡이)
- feature 패키지: `reservation`(서비스·dto), `booking`(부킹 REST·dto·web), `auth`(회원 인증),
  `backoffice`(백오피스 화면·서비스·dto), `common`(도메인·설정·보안·예외).
- 예약 서비스: `ReservationService.hold` / `ReservationConfirmService` / `ReservationExpiryService`
  + `HoldExpiryScheduler` / `ReservationCancelService`(D-027) / `ReservationQueryService`(D-028) /
  `StayService`(체크인아웃, D-031) / `NightCloseService`+`NightCloseScheduler`(D-026) /
  `AvailabilityService`(무락 표시경로, D-030).
- **락 규율(반드시 지킬 것)**: 재고 첫 조회는 `FOR UPDATE`(D-018) · 상태 전이는 예약 행 락
  우선 · 락 순서는 항상 **예약 → 재고/호실**(D-025, D-031).
- 인증: `common.security`(JwtTokenProvider·JwtAuthenticationFilter·RestAuthenticationEntryPoint).
  `/api` 는 JWT 선택 인증(비회원 예약 경로 유지) · 백오피스는 세션(D-008/D-014).

### 2.4 검증용 DB 데이터
로컬 MySQL SPIKE 데이터는 Day 1 핸드오프(git 이력) 그대로 유효. 테스트는 Testcontainers
격리라 이 데이터와 무관.

---

## 3. 즉시 다음 단계

추천 순서: 체크인아웃(완료) → 회원인증(완료) → 결제(완료, D-034) → **React** → 회원예약연결 → NO_SHOW.

1. **React 부킹엔진 화면 (다음 순위).** `/api`(availability·hold·lookup·auth·me·**payments/confirm**)를
   소비. 날짜선택→가용→HOLD→**결제창(토스 클라이언트 키·`/api/payments/confirm` 연동)**→조회 흐름.
   결제 백엔드가 섰으니 여기서 처음으로 **진짜 end-to-end 결제**가 돈다(지금까진 mock 검증만).
   JS/Vite(D-013), 별도 스택. 토스 결제위젯 SDK + 클라이언트 키 필요(둘 다 확보됨).
2. **회원 예약 연결.** 로그인 회원의 HOLD 에 `memberId` 연결 + `GET /api/me/reservations`.
   현재 hold 는 비회원만(memberId=null). 인증 컨텍스트의 회원 id 를 커맨드에 싣는다.
3. **NO_SHOW 판정.** 당일 미투숙 CONFIRMED → NO_SHOW 를 야간마감(D-026)에 붙인다.
   라이프사이클 전이는 이미 갖춰짐(D-031).

---

## 4. 결정과 근거

이번 세션의 설계 결정은 `docs/decisions.md` **D-026~D-034** 에 있다(야간마감 멱등 / 취소 /
조회 / 백오피스 화면 / 부킹 REST / 체크인아웃 / JWT 인증 / 브랜치 전략 / **결제 D-034**).
여기서 재서술하지 않는다 — 같은 결정이 두 곳에 살면 반드시 어긋난다.

핸드오프에만 남기는 판단(이번 작업의 순서·처리):
- **추천 순서대로 갔다** — 외부 의존 없는 체크인아웃·인증을 먼저 끝내고, 키가 필요한 결제와
  계약 안정 후가 유리한 React 를 뒤로 미뤘다. 근거는 각 세션 응답과 D-030~D-032.
- **깃헙 Claude 잔상은 코드 문제가 아니었다** — `git filter-branch` 백업 ref(로컬) + 깃헙
  기여자 캐시(원격). 리포 재생성으로 해소(D-033, troubleshooting §1).
