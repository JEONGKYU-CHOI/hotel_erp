# HANDOFF — 호텔 PMS + 부킹엔진 (Day 1 종료)

- 작성일: 2026-07-30
- 브랜치: `feat/init-schema` (main 아님 — `troubleshooting.md` §1 훅 항목 참조)
- **함께 로드할 파일 (여기 내용을 중복 수록하지 않았음. 반드시 읽을 것)**
  - `docs/decisions.md` — 설계 결정 D-001~D-021 전체
  - `docs/troubleshooting.md` — 이 환경에서 반복해서 물리는 함정 (Day 0 핸드오프의 §2.2 표가 여기로 이관됨)
  - `src/main/resources/db/migration/V1__init_schema.sql` — 스키마 + 설계 근거 주석
  - `git log` 커밋 메시지 — 검증 수치와 판단 근거가 본문에 있음
  - 원본 기획서: `C:\Users\a0109\Downloads\PROJECT-BRIEF (1).md`

---

## 1. 현재 상태 요약

Day 1 완료. **백오피스에서 기준정보를 등록하고 일자별 재고를 만들 수 있는 상태**다.
로그인 → 객실타입/호실/요금정책 CRUD → 재고 생성까지 브라우저로 동작한다.

**예약은 아직 0줄이다.** 다음이 이 프로젝트의 본론인 예약 + 동시성 제어다.
그 준비물(락 동작 실측, 재고 행 생성, D-018 규칙)은 모두 갖춰져 있다.

Day 1 커밋 9건:
```
d571630  feat: 일자별 재고 생성 + 현황 화면
97bb5d8  feat: 호실 · 요금정책 CRUD
50a7745  feat: 백오피스 레이아웃 + 객실타입 CRUD
a8439ab  feat: 보안 설정 (필터 체인 2개)
69adfb2  feat: V1 테이블 8개 JPA 엔티티 매핑
20a811e  docs: D-018 추가 (락 스파이크 결과)
7a08a53  spike: JPA 비관적 락 실측 검증
13926a1  build: React 산출물을 boot jar에 포함
e500293  docs: D-008~D-017 기록, D-001/D-007 supersede
```

---

## 2. 핵심 맥락 (참조 파일에 없는 것만)

### 2.1 사용자 작업 방식 — 반드시 지킬 것

- **답변은 항상 한국어.**
- **작업 착수 전 예상 소요 시간과 단계 수를 먼저 말한다.** ("약 N분, M단계")
- **긴 작업을 한 응답에 몰지 않는다.** 단계마다 끊어서 보고한다.
- **단계마다 "계속할까요?"라고 묻지 않는다.** 결과만 보고하고 계속 진행하고,
  사용자가 멈추라고 하면 멈춘다. 단 되돌리기 어렵거나 외부에 영향을 주는 작업
  (push, 배포, 삭제, 외부 전송)은 예외로 먼저 확인받는다.
- **등장하는 기술 용어는 기본 설명을 곁들인다.** (JPA/Spring 실무 경험 없음)
- 사용자 배경: 백엔드 2년차, Java/Spring/JSP/MyBatis/MySQL 실무.
  **JPA는 개인 공부만 했고 실무 경험 없음.** React는 토이프로젝트 수준.
  목표는 이직용 포트폴리오, 포지션은 **진짜 풀스택(반반)**.

### 2.2 Day 1 작업 방식에서 효과가 확인된 것

- **검증을 실측으로 했다.** 화면을 만들고 "됐습니다"로 끝내지 않고 curl 로
  정상 경로 + 검증 실패 경로 + 권한 차단을 전부 찔러 결과를 커밋 메시지에 남겼다.
  이 과정에서 **조용히 실패하던 버그를 3건 발견**했다(`@AssertTrue` 미표시,
  `/error` 미허용, 불필요한 쿼리). 만들고 넘어갔으면 전부 놓쳤을 것들이다.
- **요청당 SQL 건수를 세는 습관.** N+1 을 주장이 아니라 숫자로 확인했다.
  로그 라인 수 diff 로 센다 — 방법은 `troubleshooting.md` 에 없지만 단순하다:
  요청 전후로 `grep -c "org.hibernate.SQL"` 차이를 본다.

### 2.3 환경 사실

```
JDK        Temurin 21.0.11  (JAVA_HOME 시스템 등록 완료)
IDE        IntelliJ IDEA Community 2025.2.6.2
MySQL      8.4.9 로컬 설치. 서비스명 MySQL84, Automatic 시작
           설정  C:\ProgramData\MySQL\MySQL Server 8.4\my.ini
           서버 타임존 +09:00, utf8mb4 / utf8mb4_0900_ai_ci
DB         hotel_erp (테이블 9개 + flyway_schema_history)
DBeaver    26.1.3, 연결 'hotel_erp' 저장됨
Docker     설치됐으나 데몬 꺼짐. Day 2 Testcontainers 때 켜면 됨
비밀번호    사용자 환경변수 DB_PASSWORD 에 등록됨 (값은 어디에도 기록하지 않음)
git        user.name=JEONGKYU-CHOI / user.email=a01091075978@gmail.com
GitHub     리포명 hotel-erp 예정. 아직 원격 생성/푸시 안 함
```

**앱 실행** — 개발 중에는 `dev` 프로파일이 필요하다(개발 계정 시더가 그 안에 있다):

```bash
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.11.10-hotspot"
export DB_PASSWORD="$(powershell.exe -NoProfile -Command '[Environment]::GetEnvironmentVariable("DB_PASSWORD","User")' | tr -d '\r\n')"
./gradlew bootRun --console=plain --args='--spring.profiles.active=dev'
```

사용자가 직접 돌릴 때는 새 터미널에서
`.\gradlew.bat bootRun --args='--spring.profiles.active=dev'` (환경변수는 이미 시스템 등록됨).

**개발 계정** (`@Profile("dev")` 시더가 없으면 생성, 있으면 건너뜀)

```
admin / dev1234!   ROLE_ADMIN  — 기준정보 포함 전체
staff / dev1234!   ROLE_STAFF  — /admin/basedata/** 는 403
```

**락 스파이크 실행** (평상시 기동에는 관여하지 않음)

```bash
./gradlew bootRun --args='--spring.profiles.active=spike'
```

### 2.4 DB 에 남아 있는 검증용 데이터 (실데이터 아님)

Day 1 검증 과정에서 만든 것이라 **의미 없는 값이 섞여 있다.** 예약 개발 전에
정리하거나, 그대로 두고 새 타입을 만들어 써도 된다.

```
room_type    SPIKE(id=2, 'Spike Room Type')  ← 락 스파이크가 만듦. 호실·재고가 여기 붙어 있음
             DLX('Deluxe_Double'), STE('Suite'), OK1('Deluxe_Double_EDITED')
room         101(사용중지), 102, 201  ← 전부 SPIKE 타입 소속
rate_plan    BAR('Renamed_Plan'), BFAST, NRF  ← 전부 SPIKE 타입 소속
room_inventory
             2026-07-29  total=10 sold=2  ← 스파이크가 남긴 값. '줄일 수 없음' 검증에 유용
             2026-07-30, 07-31  total=1
             2026-08-01~08-05  total=7 또는 2
```

`2026-07-29` 행의 `sold_qty=2` 는 **재고 축소 거부 로직의 살아 있는 테스트 픽스처**다.
지우기 전에 대체 수단을 확보할 것.

### 2.5 아직 하지 않은 것 (착각 방지)

- **테스트 0개.** 생성기가 만든 컨텍스트 로드 테스트만 있고 실행해본 적 없다.
  D-006 의 통합테스트 3종은 Day 2 과제다.
- 요금 캘린더(`rate_calendar`) 화면 없음. 엔티티만 있다.
- 예약 관련 코드 전부 없음. `Reservation` / `ReservationNight` 엔티티만 매핑돼 있다.
- 부킹엔진(React) 화면 없음. Vite 스캐폴드 그대로다.
- `/api/**` 는 현재 `permitAll` 이다. JWT 필터가 없다(D-008/D-009 미구현).
  **이 상태로 외부에 노출하면 안 된다.**
- 원격 저장소 없음. 로컬 커밋만 있다.
- 로그인 화면은 만들었으나 백오피스 대시보드(`/admin`)는 안내문만 있다.

---

## 3. 즉시 다음 단계

### 3.1 요금 캘린더 (약 20분)

기준정보의 마지막 조각. 재고 생성과 **같은 "날짜 범위에 행 펼치기"** 구조라
`InventoryService.generate()` 를 그대로 참고하면 된다. 단 **락이 필요 없다** —
`rate_calendar` 는 예약이 동시에 건드리는 행이 아니다. 이 차이를 근거와 함께
남기면 "왜 저기는 락을 걸고 여기는 안 거는가"에 답할 수 있다.

- `RateCalendarRepository` + `RateCalendarService.generate(ratePlanId, from, to, amount)`
- 요일별 차등 입력(주중/주말 다른 금액)이 있으면 성수기 요금 시연이 된다
- 화면: 월 단위 그리드가 이상적이지만, 목록 + 범위 생성 폼으로 시작해도 된다

### 3.2 예약 + 동시성 제어 ★ 이 프로젝트의 본론

**순서 주의: 서비스 로직이 먼저, 화면이 나중이다.** 화면부터 만들면
동시성 검증을 화면 너머에서 하게 되어 무엇이 깨졌는지 보이지 않는다.

1. **예약 생성 서비스** — HOLD 상태로 만들고 `held_qty` 를 올린다.
   - **재고 조회는 첫 쿼리부터 `lockForUpdateNative`(D-018).** 표시용 조회를
     같은 트랜잭션에서 부르면 그 순간 규칙이 깨진다.
   - `idempotency_key` 유니크 제약으로 더블클릭·재시도를 막는다
   - `reservation_night` 를 숙박일수만큼 만들어 금액 스냅샷을 남긴다
     (`Reservation.addNight()` 가 연관관계 편의 메서드다)
2. **동시성 통합테스트** (D-006) — Docker 를 켜고 Testcontainers 로 실제 MySQL.
   `CountDownLatch` 로 동시 요청을 만들어 **오버부킹 0건**을 증명한다.
   이 테스트가 D-018 의 회귀 테스트를 겸한다.
3. HOLD 만료 스케줄러 → 재고 복원 (D-003)
4. 예약 조회 (예약번호 + 전화번호), 취소

### 3.3 그 외 백로그 (급하지 않음)

- `/admin` 대시보드 채우기 (오늘 도착/출발, 재고 현황)
- 기준정보 목록에 활성/비활성 필터 (D-020 의 대가를 완화)
- 원격 저장소 생성 및 푸시
- `docker-compose.yml` (D-012 — Day 5 재현용)

---

## 4. 결정과 근거

Day 1 의 설계 결정은 전부 `docs/decisions.md` 에 있다(D-019·D-020·D-021 신규).
**여기서 재서술하지 않는다** — 같은 결정이 두 곳에 살면 나중에 반드시 어긋난다.

핸드오프에만 남기는 것(프로젝트 결정이 아니라 이번 작업의 판단):

- **락 스파이크를 도메인 코드보다 먼저 했다.** 그 결과가 D-018 이 되었고,
  Day 1 의 재고 생성이 그 규칙의 첫 적용 사례가 되었다. 순서를 바꿨다면
  재고 생성 코드를 락 없이 짰다가 Day 2 에 다시 뜯었을 것이다.
- **객실타입 CRUD 를 하나의 완결된 수직 슬라이스로 먼저 만들었다.**
  리포지토리부터 화면까지 끝낸 뒤 호실·요금정책에 같은 패턴을 펼쳤다.
  세 개를 계층별로 병렬 진행했다면 패턴이 굳기 전에 세 벌을 고쳐야 했다.
- **재고 생성을 요금 캘린더와 묶지 않았다.** 겉보기에 같은 "날짜 범위 펼치기"지만
  재고는 락과 CHECK 제약이 걸린 구간이라 검증 밀도가 달라야 했다.
- **검증에서 실패한 경로를 커밋 메시지에 남겼다.** "됐다"만 적으면 다음 사람이
  같은 함정을 다시 밟는다. 조용히 실패하던 버그 3건이 그렇게 기록됐다.
