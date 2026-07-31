# HANDOFF — 호텔 PMS + 부킹엔진 (정산·환불·하우스키핑 + UI 개편)

- 작성일: 2026-08-01 (D-041~044 + 환불/취소 이력 UI + 관리자 대시보드 + 부킹엔진 개편)
- 브랜치: **`dev`** (개발선). `main` 은 안정 기준선·원격 기본 브랜치 — D-033.

## 0. 로컬 실행 (다음 세션 시작 명령어)

전제: **MySQL 8.4 가 3306 에서 돌고 있어야 함**(DB `hotel_erp`, root, 비번은 gitignore 된
`application-local.yml`). 스키마는 Flyway 가 만든다.

- **백엔드**(Bash 에선 JAVA_HOME 수동): 
  ```
  export JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-21.0.11.10-hotspot"
  ./gradlew bootRun --args='--spring.profiles.active=local,dev'
  ```
  → 관리자 **http://localhost:8080/admin** · 직원 로그인 `admin`/`staff` · 비번 `dev1234!`
- **프론트(부킹엔진)**: `cd frontend && npm run dev` → **http://localhost:5173** (Vite 가 `/api` 를 8080 로 프록시)
- **포트 정리**(8080 이미 점유 시): 이전 세션 인스턴스가 살아 있을 수 있다. 
  `powershell.exe -NoProfile -Command "Get-NetTCPConnection -LocalPort 8080 -State Listen | % { Stop-Process -Id \$_.OwningProcess -Force }"`
- **데모 데이터**: 이미 DB 에 시드돼 있다(재시작에도 유지). DB 를 초기화했으면
  `scripts/dev-seed-demo.sql` 로 1회 재시드(파일+`--default-character-set=utf8mb4` 필수 — 한글 함정).
- **테스트**: `./gradlew test` (Testcontainers 실제 MySQL, 112건). JAVA_HOME 필요.
- **함께 로드할 파일 (중복 수록 안 함 — 반드시 읽을 것)**
  - `docs/decisions.md` — 설계 결정 D-001~D-040
    (이번: **D-037** 취소·노쇼 위약금 · **D-038** 폴리오 · **D-039** 환불 · **D-040** 하우스키핑)
  - `docs/troubleshooting.md` — 이 환경의 반복 함정
  - `src/main/resources/db/migration/V1~V7.sql` — 스키마 + 근거 주석
  - `git log` — 검증 수치와 판단 근거가 커밋 본문에 있음

---

## 1. 현재 상태 요약

예약 라이프사이클은 **돈으로 닫혀** 있고(예약→결제→투숙→체크아웃→청소→재배정, 취소/노쇼→
위약금→폴리오→환불), 이번 세션은 그 위의 **감사·동시성 보강**과 **UI 개편**을 했다.
**전 기능 Testcontainers 통합테스트 112건 통과.** 마이그레이션 V8 추가.

이번 세션에 한 것:
- **핸드오프 1~5 완료**: 환불 동시성(결제 행 FOR UPDATE, D-041) · 웹훅 재조회 검증(D-041) ·
  환불/취소 행위자+원장(payment_cancel, cancelled_by, D-042) · HOLD 만료 표시 규칙 통합
  (`Reservation.displayStatus`, D-043) · troubleshooting 보강.
- **하우스키핑 점검 단계**(CLEAN→INSPECTED, D-044) — 배정/재고 연계(A·B)는 후속으로 남김.
- **환불/취소 이력 화면**(백오피스 예약 상세, D-042 후속 UI).
- **관리자 대시보드 개편**: 다크 사이드바 셸 + `/admin` KPI 대시보드(DashboardService).
- **부킹엔진 개편**: 파르나스풍 — 모던 산세리프(Inter+Noto), 니어블랙+크림 미니멀, 히어로 +
  가로 예약 바(객실타입·체크인·체크아웃·인원 + "객실 찾기"). 클래스명 유지.
- **데모 데이터 시드**: 객실타입 3종·요금제·호실·재고(오늘~+60일). `scripts/dev-seed-demo.sql`.

주요 커밋(dev, 최신순): `0eb115e`(부킹엔진 파르나스풍) · `ccff7c1`(관리자 대시보드) ·
`e1e9dba`/`61cc2f9`(D-044 점검) · `e4aadde`(환불/취소 이력 화면) · `62b9708`/`63fab7c`(D-041 웹훅) ·
`a1bc94d`(D-043) · `600f23e`(D-042) · `e9a4225`(환불 동시성 락).

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

핸드오프 1~5 + 후속(이력 화면·점검 단계)·UI 개편은 **모두 완료**. 다음 세션 후보:

1. **부킹엔진 마무리(진행 중이던 것).** ① 히어로 이미지가 아직 placeholder(`frontend/src/assets/hero.png`)
   — 실제 호텔 사진으로 교체하면 파르나스풍이 산다. ② 예약 전체 흐름 라이브 검증(검색→HOLD→
   토스 결제→성공/실패). ③ 나머지 페이지(내 예약·조회·결제결과)를 새 톤에 맞춰 미세 정리.
   ④ **에이전트 브라우저 pane 은 이 환경에서 페인트 검증(스크린샷·getComputedStyle)이 얼어
   불가** — 실제 확인은 사용자 브라우저에서. 디자인 반영은 소스·빌드·DOM 으로만 검증했다.
2. **하우스키핑 A·B 업그레이드(D-044 후속).** A: 점검을 배정 게이트로(`isAssignable=INSPECTED only`).
   B: OUT_OF_ORDER 현황판 처리 + 기간만큼 객실타입 재고 차감(오버셀 방어) — 오버부킹 0 불변식을
   건드리므로 스펙·동시성 테스트 필요.
3. **부분환불 사유 코드 체계**(D-039/D-042 후속) — 지금은 자유텍스트 reason.

---

## 4. 결정과 근거

이번 세션의 설계 결정은 `docs/decisions.md` **D-041~D-044** 에 있다(웹훅 재조회+환불 동시성 /
환불 원장+취소 행위자 / 만료 표시 통합 / 하우스키핑 점검 단계). 여기서 재서술하지 않는다.
직전 세션 D-037~D-040(위약금·폴리오·환불·청소 3단계)도 유효하다.

핸드오프에만 남기는 판단(이번 작업의 순서·처리):
- **사용자 로드맵 순서대로 갔다** — 문서 빚(D-037 기록) → 폴리오 → 환불 → 하우스키핑.
  돈이 정확히 정산되는 축(폴리오·환불)을 먼저, 운영 편의(하우스키핑)를 뒤로. 각 단계는 스펙을
  먼저 좁히고(clarify-spec) 착수·테스트·라이브 검증·커밋했다.
- **라이브 검증에서 실제 버그를 잡았다** — 만료 시각 지난 미청소 HOLD 를 폴리오가 raw HOLD 로
  읽어 "미수"로 오표시. 목록·조회와 같은 규율(EXPIRED 표시)을 폴리오에도 적용해 정합(D-038 §3).
