# HANDOFF — 호텔 PMS + 부킹엔진 (정산·환불·하우스키핑까지)

- 작성일: 2026-07-31 (폴리오·환불·하우스키핑 + 회원화면·위약금)
- 브랜치: **`dev`** (개발선). `main` 은 안정 기준선·원격 기본 브랜치 — D-033.
- **함께 로드할 파일 (중복 수록 안 함 — 반드시 읽을 것)**
  - `docs/decisions.md` — 설계 결정 D-001~D-040
    (이번: **D-037** 취소·노쇼 위약금 · **D-038** 폴리오 · **D-039** 환불 · **D-040** 하우스키핑)
  - `docs/troubleshooting.md` — 이 환경의 반복 함정
  - `src/main/resources/db/migration/V1~V7.sql` — 스키마 + 근거 주석
  - `git log` — 검증 수치와 판단 근거가 커밋 본문에 있음

---

## 1. 현재 상태 요약

예약 라이프사이클이 **돈으로 닫혔다**: 예약 → 결제 → 투숙 → 체크아웃 → 청소 → 재배정,
그리고 취소/노쇼 → 위약금 → 청구서(폴리오) → 환불 실행. 이번 세션에 그 위에 회원 화면
(로그인·내 예약·청구서)·정책 위약금·정산·환불·하우스키핑을 얹었다. 핵심 3과제(오버부킹 0 ·
HOLD 만료 복원 · 마감 멱등)와 고객 부킹엔진·회원 흐름은 이전 세션에 완성. **전 기능
Testcontainers(실제 MySQL) 통합테스트 101건 통과.** 마이그레이션 V4~V7 추가.

이번 세션 커밋(dev, 최신순):
```
3e6af88 docs: record D-040 housekeeping cleaning flow decision
7c167ed feat: add housekeeping cleaning flow DIRTY to CLEAN (D-040)
9469106 docs: record D-039 refund execution decision
e6ceaf0 feat: add refund execution via Toss payment cancel (D-039)
6c287b3 docs: record D-038 folio (assembled billing statement) decision
061c893 feat: add folio (billing statement) assembled on read (D-038)
e041448 docs: record D-037 cancellation/no-show penalty policy decision
ab42fa2 feat: add policy-based no-show fee, unified with cancellation (D-037)
3c1224e feat: add cancellation fee policy on reservation cancel (D-037)
cc90153 feat: add member login/signup and my-reservations screens
```

---

## 2. 핵심 맥락 (참조 파일에 없는 것만)

- **폴리오는 조립형**(새 테이블 없음, D-038) — 예약·야간·결제를 읽어 청구서를 그때그때 조립.
  상태별 청구액 규칙이 핵심(진행/완료=숙박료, 취소=cancellation_fee, 노쇼=no_show_fee, 만료=0).
  환불하면 `payment.effectiveAmount`(= amount − canceled_amount)가 줄어 **폴리오 잔액이 자동
  0 으로 수렴**한다(D-039). 취소·노쇼 위약금은 순수 함수 `CancellationPolicy` 로 통일(D-037).
- **백오피스 라이브 검증엔 `dev` 프로파일이 필요하다.** 직원 계정(admin/staff · `dev1234!`)은
  `DevAccountSeeder`(@Profile("dev"))가 시드한다. 로컬 실구동+백오피스 로그인은
  `./gradlew bootRun --args='--spring.profiles.active=local,dev'` 로 띄운다 — local 만으로는
  DB 는 붙지만 직원 로그인이 안 된다. (프론트 부킹엔진은 dev 불필요.)
- **Mockito `eq(BigDecimal)` 는 scale 까지 비교한다** — `140000` ≠ `140000.00`. 결제/금액 검증
  테스트에서 기대값 scale 을 실제(대개 .00)와 맞춰야 통과한다(RefundTest 에서 물렸음).
- **에이전트 Bash 함정**: Windows python 은 MSYS `/tmp` 경로를 못 읽는다(curl `-o /tmp/..` 는
  MSYS tmp 에 쓰지만 python 은 리터럴 `/tmp` 로 읽어 어긋남). 파일 대신 파이프로 넘길 것.
  브라우저 pane 의 `read_page` 가 0x0(비컴포지팅)이면 `get_page_text`·`javascript_tool` 로 우회.

---

## 3. 즉시 다음 단계

1. **환불 동시성 수정 (최우선).** `ReservationCancelService`·`ReservationConfirmService` 등은
   전이 전 `findByIdForUpdate` 로 행을 잠그는데(D-025 규율), **`RefundService`(D-039)만 락 없이**
   폴리오·결제를 읽고 취소한다. 동시 환불 요청 2건이 둘 다 `balance<0` 을 보고 둘 다 토스 취소를
   부르면 이중 환불 여지 — 멱등은 순차 재요청만 막는다. → 결제 행을 `FOR UPDATE` 로 잠그고
   환불하도록 고치고, 동시 이중환불 회귀 테스트 추가(ReservationCancelTest 의 `@RepeatedTest`
   동시성 패턴 참고).
2. **토스 웹훅 서명 검증** — D-034 에서 감수한 보안 갭. 위조 웹훅 오확정 방지.
3. **환불/취소 행위자 기록 + 부분환불 이력** — 지금은 로그만, 원장에 누가·언제·개별 이벤트가 없다.
4. **HOLD 만료 표시 규칙 통합** — `MyReservationSummary`·`ReservationDetail`·`FolioService` 가
   "만료 지난 HOLD = EXPIRED 표시"를 각자 갖고 있다(3중복). 도메인 메서드로 모을 것.
5. **troubleshooting.md 보강**(이번 세션 미반영분): python `/tmp` · 브라우저 0x0 · `local,dev`
   로그인 — §2 의 함정들을 troubleshooting 에 승격.

전체 부족한 부분 리뷰(우선순위)는 이 세션 대화 말미에 정리돼 있다 — 위 1~5 가 그 상위 항목이다.

---

## 4. 결정과 근거

이번 세션의 설계 결정은 `docs/decisions.md` **D-037~D-040** 에 있다(취소·노쇼 위약금 통일 /
폴리오 조립형 / 환불 실행 백오피스 수동 / 하우스키핑 3단계). 여기서 재서술하지 않는다.

핸드오프에만 남기는 판단(이번 작업의 순서·처리):
- **사용자 로드맵 순서대로 갔다** — 문서 빚(D-037 기록) → 폴리오 → 환불 → 하우스키핑.
  돈이 정확히 정산되는 축(폴리오·환불)을 먼저, 운영 편의(하우스키핑)를 뒤로. 각 단계는 스펙을
  먼저 좁히고(clarify-spec) 착수·테스트·라이브 검증·커밋했다.
- **라이브 검증에서 실제 버그를 잡았다** — 만료 시각 지난 미청소 HOLD 를 폴리오가 raw HOLD 로
  읽어 "미수"로 오표시. 목록·조회와 같은 규율(EXPIRED 표시)을 폴리오에도 적용해 정합(D-038 §3).
