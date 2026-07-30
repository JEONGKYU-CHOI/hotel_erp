# HANDOFF — 호텔 PMS + 부킹엔진 (Day 2 종료)

- 작성일: 2026-07-30
- 브랜치: `feat/init-schema` (원격 푸시됨)
- **함께 로드할 파일 (중복 수록 안 함 — 반드시 읽을 것)**
  - `docs/decisions.md` — 설계 결정 D-001~D-025 전체
  - `docs/troubleshooting.md` — 이 환경의 반복 함정
  - `src/main/resources/db/migration/V1__init_schema.sql` — 스키마 + 근거 주석
  - `git log` — 검증 수치와 판단 근거가 커밋 본문에 있음

---

## 1. 현재 상태 요약

Day 2 완료. 기준정보의 마지막 조각(요금 캘린더)과 **예약 동시성 코어**가 들어갔다.
예약 라이프사이클은 **HOLD 생성 → (만료 시 재고 복원 / 결제 확정)**까지 동작하고
전부 실측 검증됐다. 핵심 3과제 중 **2개 증명 완료**(①오버부킹 0건, ②HOLD 만료 복원).
남은 핵심은 ③야간마감 멱등이다.

Day 2 커밋 5건:
```
54d9ca2  feat: 예약 확정 + 확정/만료 경합 직렬화
f37fc86  feat: HOLD 만료 스케줄러 + 재고 복원
06222a8  feat: 예약 HOLD 서비스 + 오버부킹 0건 증명
bf80ba5  refactor: 베이스 패키지 io.github.jeongkyuchoi.hotel.erp 이관
012be30  feat: 요금 캘린더 생성 (주중/주말 차등)
```

---

## 2. 핵심 맥락 (참조 파일에 없는 것만)

### 2.1 작업 방식 — Day 1과 동일
답변 한국어 · 착수 전 예상 시간/단계 고지 · 단계별로 끊어 보고 ·
단계마다 "계속할까요?" 묻지 않고 진행(되돌리기 어렵거나 외부 영향 주는 것만 먼저 확인) ·
JPA/Spring 용어는 설명 곁들임. 사용자 배경은 Day 1 핸드오프(git 이력) 참조.

### 2.2 환경 변화 (Day 1 대비 — 이전 핸드오프의 "아직 안 한 것" 다수가 뒤집힘)

- **원격 저장소 생성됨**: `github.com/JEONGKYU-CHOI/hotel_erp` (public).
  `feat/init-schema` 푸시됨. `main`은 로컬 클린 스켈레톤(785bcf2)만 있고 원격 미푸시.
- **커밋 히스토리 정리됨**: 전 커밋에서 Claude 공동저자 트레일러 제거(force-push).
  앞으로도 안 붙음 — `.claude/settings.local.json`의 `includeCoAuthoredBy:false`.
  GitHub Contributors의 claude는 캐시 지연으로 곧 사라짐(원격 커밋엔 0건).
- **git push 허용 규칙 추가**: `~/.claude/settings.json` allow 에 `Bash(git push:*)`.
- **베이스 패키지**: `io.github.jeongkyuchoi.hotel.erp` (com.hotel.erp 에서 이관).
  Gradle `group` 도 `io.github.jeongkyuchoi`. `src/main/java` 소스 루트는 그대로.
- **IntelliJ**: Ultimate 2026.2.0.1 (build IU-262.8665.337). (Community 2025.2 에서 변경)
- **테스트 존재**: 예약 통합테스트 12건(Testcontainers MySQL 8.4). 실행에 Docker 필요.

### 2.3 예약 도메인 지도 (코드에 있지만 길잡이)

- feature 패키지: `io...hotel.erp.reservation.{dto, service}`
- 서비스: `ReservationService.hold` / `ReservationConfirmService.confirm` /
  `ReservationExpiryService.expireOne` + `HoldExpiryScheduler.sweep`
- **락 규율 (반드시 지킬 것)**: 재고 첫 조회는 `FOR UPDATE`(D-018) ·
  상태 전이는 예약 행 락 우선 · 락 순서는 항상 **예약 → 재고**(D-025)
- `RoomInventory` 도메인 메서드 준비됨: `hold` / `releaseHold` / `confirmHold`.
  취소용 "sold/held 반환"은 상태에 따라 갈리며 아직 서비스 미구현.

### 2.4 검증용 DB 데이터
Day 1 핸드오프 §2.4(git 이력) 그대로 유효(로컬 MySQL의 SPIKE 데이터). 테스트는
Testcontainers 격리라 이 데이터와 무관하다.

---

## 3. 즉시 다음 단계

1. **야간마감 (D-006 ③, D-004) — 핵심 3과제 마지막.**
   `reservation_night.posted` 를 게시 여부 기준으로, 마감 이력 테이블 + 유니크 제약으로
   **2회 실행 시 중복 게시 0**을 증명한다. CONFIRMED/CHECKED_IN 예약의 그날 숙박분을
   폴리오에 게시. Testcontainers 통합테스트로 멱등 증명(핵심 3과제 완성).
2. **예약 취소 (CANCELLED).** `Reservation.cancel` 재작성(이전 것 폐기함) + 취소 서비스:
   취소 직전 상태에 따라 `held`(HOLD) 또는 `sold`(CONFIRMED)를 반환. 락 우선(예약→재고).
3. **예약 조회.** 예약번호+전화(비회원 경로). 리포지토리 조회 메서드는 이미 있음.
4. **화면/REST.** 백오피스 예약 목록·상세 / 부킹엔진 REST(`/api`, 현재 `permitAll` —
   JWT 전 외부 노출 금지).

---

## 4. 결정과 근거

이번 세션의 설계 결정은 `docs/decisions.md` **D-022~D-025**에 있다(요금 캘린더 무락 /
HOLD 생성 / 만료 스케줄러 / 상태 전이 예약행 락). 여기서 재서술하지 않는다.

핸드오프에만 남기는 판단(프로젝트 결정이 아니라 이번 작업의 순서·처리):

- **패키지 이관을 예약 착수 전에 했다.** 파일이 더 늘기 전 40여 개 일괄 변경이 쌌다.
  (Windows 파일 잠금으로 `git mv` 가 막혀 PowerShell `Move-Item` 으로 우회 — Gradle
  데몬이 소스를 물고 있었다. troubleshooting §1 참조.)
- **확정을 만료 다음에 만들었다.** 만료(반환)와 확정(이동)이 `held_qty` 를 두고 경합하는
  쌍이라, 만료가 있어야 확정/만료 경합 테스트(D-025)가 성립했다.
- **출처 불명 `cancel()` 폐기.** IntelliJ 재설치 후 워킹트리에 미커밋 `Reservation.cancel()`
  이 나타남(커밋에도 없고 나도·사용자도 안 씀). 검증 안 된 코드라 HEAD 기준 폐기 후
  재작성 선택. 재발 시 troubleshooting §1 참조.
