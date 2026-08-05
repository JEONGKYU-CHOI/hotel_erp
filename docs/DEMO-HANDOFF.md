# DEMO HANDOFF — 무료 시연 배포 + 프론트 홈페이지 콘텐츠

- 작성일: 2026-08-01
- 브랜치: **`demo`** (원격 push 완료). 이 세션 작업은 전부 여기 있음.
- 성격: `main`/`dev`(백엔드·PMS 본선)와 별개로, **포폴 시연용 배포 + 부킹엔진 홈페이지 콘텐츠**를 쌓는 라인.

---

## 1. 이 세션에 한 것 (커밋 2건)

- `0f319a4` **무료 시연 배포 스택** — `Dockerfile`(멀티스테이지: 프론트 포함 bootJar→JRE), `docker-compose.yml`(앱+MySQL8.4 자기완결형), `application-prod.yml`(운영 프로파일), `DemoDataSeeder`(@Profile demo, 데모데이터 오늘~+60일), `ProdAdminSeeder`(@Profile prod, env 비번 관리자), `.env.example`, `docs/DEPLOY.md`.
- `44899cd` **프론트 홈페이지 콘텐츠 + 통합 예약** — 아래 2~3.

## 2. 배포 방식 (확정)

**Vercel/Cloudflare Pages엔 백엔드(상주 JVM+MySQL) 불가** → 결론:
- 프론트+백엔드+DB를 **한 앱(단일 오리진)**으로 `docker compose up` → `localhost:8080`
- 외부 공개는 **Cloudflare Tunnel**(quick tunnel, 계정 불필요)로 `localhost:8080`을 뚫음
- **완전 무료, 카드 불필요.** (OCI 시도했으나 카드 검증 실패 — 유니온페이/체크카드 거부. 그래서 로컬+터널로 전환.)
- 실행·시연 절차는 **`docs/DEPLOY.md`** 에 있음.

## 3. 프론트 변경 (부킹엔진 `/`)

- **히어로 3장 자동 슬라이드**(크로스페이드 5초+닷), 실사진 교체(Unsplash 무료 상업용). 구 `hero.png`는 미사용.
- **신규 페이지**: `RoomsPage`(객실 상세) · `DiningPage`(다이닝 3곳) · `FacilitiesPage`(편의시설) · `LocationPage`(위치/오시는 길). 네비바: 브랜드 옆 주 메뉴(객실·다이닝·편의시설·위치) / 우측 유틸(예약 조회·로그인·예약하기 CTA). 확장 푸터.
- **홈 티저 섹션**: 객실 쇼케이스 + 편의시설·다이닝·위치 티저.
- **통합 예약(핵심)**: 어디서든 "예약하기" → **예약 모달(데스크톱)/바텀시트(모바일)**. 객실 카드·객실 페이지에서 열면 **그 객실이 프리셀렉트**됨. 상시 CTA(데스크톱 상단) + **모바일 하단 고정 예약바**. 홈 예약바와 모달이 **`BookingForm` 하나를 공유**.
  - 파일: `components/BookingContext.jsx`(Provider+useBooking, 모달 렌더), `components/BookingModal.jsx`, `components/BookingForm.jsx`(공용 검색폼). 진입점은 `openBooking(roomTypeId?)`.
- 데스크톱/모바일(375px) 동작을 브라우저 pane에서 실측 검증함(모달 열림·프리셀렉트·바텀시트·하단바 computed style).

## 4. 현재 상태 / 주의

- **로컬 검증은 Vite dev(5173)** 로 했다(핫리로드). `.claude/launch.json`의 `frontend` 설정 사용. 백엔드는 docker(8080)가 API 제공, Vite가 `/api`를 8080으로 프록시.
- ⚠️ **docker(8080) 이미지엔 프론트 최신 변경이 아직 안 구워짐.** 8080/터널에 반영하려면 **`docker compose up --build`로 재빌드** 필요.
- 브라우저 pane은 이 환경에서 **스크린샷(페인트) 검증이 얼어** 불가 → 텍스트/구조/`javascript_tool` computed-style 로 검증했다. 실사진 육안 확인은 사용자 브라우저에서.
- `.env`(비밀값)는 gitignore. `.env.example` 복사해서 채운다.

## 4-1. 이 세션 추가분 (프로모션 · FAQ · 갤러리)

핸드오프 §5의 후보 1~3을 구현했다. 셋 다 Vite dev(5173)에서 실측 검증.

- **① 프로모션/패키지** (`/packages`, `PromotionsPage.jsx`) — 큐레이션 패키지를 **실제 rate_plan code**(`STDT-BF`/`DLXD-BF`/`EXSU-NRF`)에 매핑. 가격·조식·환불 플래그는 하드코딩 없이 API rate-plan 에서 가져온다. "이 패키지로 예약" → 통합 예약 모달이 **객실+요금제 프리셀렉트**된 채 열림.
  - **통합 흐름 확장(핵심)**: `openBooking(roomTypeId, ratePlanId)` 로 요금제까지 프리셀렉트. `BookingContext → BookingModal → BookingForm(navigate state.ratePlanId) → BookingPage(요금제 선택 반영)` 4곳 관통. end-to-end 검증: 디럭스 패키지 → `/book` 에서 `DLXD-BF`(id 4) 자동 선택 확인.
  - 홈(`SearchPage`)에 프로모션 티저 밴드 추가(→ `/packages`).
- **② 이용안내/FAQ** (`/faq`, `FaqPage.jsx`) — 이용안내 6항목(체크인 15:00/체크아웃 11:00 등) + FAQ 아코디언 8개(`<details>`, JS 무의존). **취소/환불 문구를 백엔드 요금제 정책과 일치**: 환불가능=체크인 1일 전 무료취소(`cancelDeadlineDays:1`), NRF=환불 불가.
- **③ 갤러리** (`/gallery`, `GalleryPage.jsx`) — 사진 14장, 카테고리 필터(전체/전경/객실/다이닝/편의시설) + 라이트박스(ESC·오버레이 클릭 닫기).
- 네비: 주 메뉴에 **프로모션·갤러리**, 유틸에 **이용안내** 추가. 푸터 링크도 갱신. CSS는 `booking.css` 하단에 프로모션·FAQ·갤러리·홈티저 섹션 추가.

## 4-2. 이 세션 추가분 (기능 확장 · 세션 2)

전부 배포(8080 재빌드) 반영 + `origin/demo` push 완료. 결정은 decisions.md 참조.

- **예약 안전장치**: 최대인원 초과 차단(성인+아동 > maxOccupancy) · 당일예약 허용+마감시각(**D-045**, PMS 설정). `@Future`→`@FutureOrPresent`.
- **라우팅**: SPA 딥링크 폴백에 콘텐츠 라우트 등록(D-035 적용 — 직접접속/새로고침 302 해소) · 회원로그인 `/login`→`/member-login` 분리(PMS `/login` 충돌 해소).
- **결제/취소**: HOLD 나중 결제(예약조회·내예약에서 결제 버튼, 토스 호출 `payments.js`로 공용화) · **고객 취소/환불**(**D-046**, HOLD 즉시취소·확정 실환불) · **이메일 알림 3종**(**D-047**, 로그 발송).
- **UI**: 히어로 타이틀 한 글자 줄바꿈 정리(`word-break:keep-all`+`text-wrap:balance`, hero-inner 840px).
- **시연 검증**: 실 토스 테스트결제로 확정→확정메일→취소+실환불(220,000)→취소메일까지 end-to-end 확인.
- 커밋: `66e865d`~`d9e9407` (전부 push됨).

## 4-3. 이 세션 추가분 (품질 · 완성도 · 세션 3)

전부 `origin/demo` push 완료(`79a0ff9`~`276daa6`). 결정은 decisions.md D-048 참조.

- **테스트 회귀 확인 + 수정**: 최대인원 초과 차단 로직에 걸려 깨진 기존 픽스처 2건
  (`ReservationQueryTest`·`ReservationAdminScreenTest`, maxOccupancy 2→3). 전체 115 tests 그린.
- **당일마감 Clock 주입**(**D-048**): 시각 의존 로직을 `Clock` 빈 주입으로 결정론화,
  `SameDayCutoffTest` 경계 3케이스 신설. 커밋 `79a0ff9`.
- **UI 버그**: 로그인/회원가입 카드에서 `.muted` 음수 마진이 회원가입 문구를 버튼과
  겹치게 하던 것 해소(`.auth-card` 스코프 재정의). 커밋 `56afd86`.
- **SEO/OG/파비콘**(§6 [C] ✅): description·canonical·Open Graph·Twitter Card 메타,
  1200×630 OG 이미지(`public/og-image.jpg`), Claude 템플릿 로고 → 브랜드 모노그램 favicon,
  theme-color `#17140f`. 커밋 `a34ba10`.
- **HOLD 결제 만료 카운트다운**(§6 [C] ✅): `HoldCountdown` 컴포넌트(mm:ss, 2분 이하 경고색,
  만료 시 onExpire), Booking·Lookup·MyReservations 3곳 적용. `MyReservationSummary` DTO 에
  `holdExpiresAt` 필드 추가(백엔드). 커밋 `018e23b`.
- **로딩 스켈레톤·빈/에러 상태**(§6 [C] ✅): 재사용 `Skeleton` 컴포넌트(`SkeletonLine`·
  `SkeletonResList`, reduced-motion 존중), 내 예약 3상태 분리(로딩→스켈레톤/에러→재시도/
  빈→CTA). 커밋 `276daa6`.

- **데모 회원 계정**(`DemoDataSeeder`): `demo@thestay.example` / `demo1234!` — 로그인·내 예약
  시연용. bcrypt 인코딩 위해 SQL 시드가 아닌 Java 로 심고 이메일 멱등. `@Profile("demo")`.

⚠️ **미완/주의**:
- **OG 절대 URL**: 데모가 동적 Cloudflare 터널이라 `og:url`·`og:image` 를 상대경로로 뒀다.
  카카오톡은 절대경로만 읽으니 **고정 URL(named tunnel) 확보 후 절대 URL 로 교체**해야 카톡
  썸네일이 뜬다(index.html 에 주석). favicon "S" 육안 확인은 스크린샷 얼어 미확인.
- **MyReservations 목록·HOLD 카운트다운**: 데모 회원 시드 후 실측 완료 — 로그인→내 예약에
  HOLD 1건·"결제 대기" 배지·카운트다운(role=timer) 렌더 확인. `/me/reservations` 응답에
  `holdExpiresAt` 포함도 API 확인. (실시간 틱은 숨김 pane setInterval 스로틀로 정지 — 실 앱 정상)
- `Skeleton` 컴포넌트는 재사용 가능 — 다른 목록/검색 페이지 로딩에도 쓸 수 있다.

## 4-4. 이 세션 추가분 (PMS 대시보드 KPI · 세션 4)

`origin/demo` push 완료. 결정은 decisions.md **D-049** 참조.

- **대시보드 실적 지표**(§6 [A] ✅): 백오피스 `/admin` 대시보드에 **오늘 매출·가동률(OCC)·ADR**
  3타일 추가. 집계는 게시대상 상태(CONFIRMED·CHECKED_IN, D-026)만 실적으로 인정 — HOLD·취소·
  노쇼 제외. 세 지표가 같은 판매객실 수를 공유(OCC 분자 = ADR 분모).
  - 매출=ReservationNight 요금 합 · OCC=판매/총객실(RoomInventory) · ADR=매출/판매객실.
  - `RoomRevenueStat` 생성자 프로젝션(Object[]+coalesce 는 방언별 타입 문제로 폐기).
  - 커밋 `551aa09`. 파일: `DashboardService`·`DashboardView`·`ReservationNightRepository`·
    `RoomInventoryRepository`·`RoomRevenueStat`·`admin/index.html`·`DashboardKpiTest`.
- **검증**: DashboardKpiTest 3케이스(확정 집계·HOLD 제외·화면 렌더). 전체 **118 tests 그린**.
  PMS 육안 확인은 관리자 비번(.env 시크릿)이 필요해 생략 — 렌더는 MockMvc 로 검증.

## 4-5. 이 세션 추가분 (콘텐츠 · 접근성 · 다국어 · 세션 5)

`origin/demo` push 완료. 결정은 decisions.md **D-050** 참조.

- **About/브랜드 스토리**(`/about`, §6 [B] ✅) + **후기/리뷰**(`/reviews`, 카테고리 필터·평균
  평점, §6 [B] ✅). 네비·푸터 링크, WebMvcConfig SPA 폴백 등록. 커밋 `d5d4ed8`.
- **접근성**(§6 [C] ✅): 스킵 링크·랜드마크 라벨·전역 `:focus-visible`·BookingModal 포커스
  이동/복원·`aria-labelledby`. 커밋 `6fa0ef8`.
- **다국어(한/영)**(§6 [B] ✅, D-050): 무의존 i18n(`i18n/messages.js`·`I18nContext`) + 상단
  언어 토글. 두 패턴 병행 — **셸/공통은 전역 사전 `t()`**(커밋 `ed76a9d`), **콘텐츠 페이지는
  페이지 로컬 `CONTENT={ko,en}` co-locate + `useI18n().lang`**(산문·태그가 많아 사전 부적합).
  - **콘텐츠 페이지 9종 완료**: 객실·다이닝·편의시설·위치(`3b3b064`) · About·후기·FAQ
    (`494cf79`) · 갤러리·프로모션(`b8b687a`). 필터는 표시문자열이 아닌 키로 동작해 언어전환에도
    유지. 요금은 API 유지·통화 포맷만 언어별.
  - ✅ **예약 플로우 폼 i18n 완료**: BookingForm·BookingPage·LookupPage·MyReservations·Login·
    Signup·결제 착지·BookingModal·FolioPanel까지 한/영 전환. 후속 커밋 `2089d3d` 참고.
  - ✅ 객실명·요금제명은 DB `name_en`을 단일 소스로 사용하고, 비어 있으면 한글로 폴백한다
    (D-051, `d6ab452`).
- **검증**: dev(5173) 실측(렌더·필터·언어전환·모달 포커스) + vite 프로덕션 빌드 무에러 +
  백엔드 전체 스위트 그린. 8080 은 docker 재빌드 반영.

## 5. 다음 후보 (사용자와 논의된 것)

- 콘텐츠·예약 퍼널 한/영 i18n, About, 후기, 갤러리, 객실별 상세 갤러리까지 완료됐다.
- 후기는 당분간 노출하지 않기로 해 내비·푸터 링크만 숨겼고 페이지·라우트는 유지한다.
- 향후 후보는 고정 URL+OG 절대 URL, 업로드 저장소 R2 이전, `demo`→`main` 병합 결정이다.

## 6. 남은 백로그 (다음 세션)

- **[운영 결정]** `demo`→`main` 병합 여부.
- **[외부 자산 필요]** 도메인 구매 후 Cloudflare named tunnel 고정 URL 구성과 OG 절대 URL 교체.
- **[외부 자산 필요]** 실제 주소·전화번호·토스 운영 키 교체.
- **[선택 개선]** 로컬 업로드 볼륨을 Cloudflare R2/S3로 이전(D-053의 저장 서비스 구현 교체).
- **[선택 개선]** 후기 기능 재노출 여부 결정(현재 링크만 숨김).

## 7. Codex 새 세션 인계 — 2026-08-05

### 1. 현재 상태 요약

- 작업 브랜치는 `demo`, HEAD는 `0be48e4`이며 `origin/demo`와 동기화되어 있다.
- 워킹트리는 깨끗하다. Claude에서 구현한 코드 변경은 전부 커밋·push 완료 상태다.
- 예약 퍼널·콘텐츠 페이지 한/영 i18n, DB 기준정보 `name_en`, 객실 이미지 최대 3장,
  PMS 직접 업로드, 부킹엔진 좌우 캐러셀, 후기 메뉴 숨김까지 완료됐다.
- Docker Desktop과 앱(8080), Cloudflare quick tunnel은 현재 모두 내려가 있다.
- PMS에서 업로드한 객실 이미지 9장(객실 3종 × 3장)은 Git이 아니라 Docker named volume
  `hotel_uploads`에 저장된다. `docker compose down`에는 유지되지만 `down -v`에는 삭제된다.

### 2. 핵심 맥락

- 실행·외부 공개 절차는 `docs/DEPLOY.md`, 영속 설계 결정은 `docs/decisions.md` D-001~D-053을 따른다.
- 최신 핵심 결정은 D-050(i18n), D-051(`name_en`), D-052(객실 이미지 3장·캐러셀),
  D-053(로컬 볼륨 파일 업로드·추후 R2 교체 가능 구조)이다.
- `FileStorageService`가 업로드 저장·삭제를 캡슐화한다. 추후 Cloudflare R2/S3로 옮길 때
  호출부는 유지하고 저장 구현만 교체하는 방향이다.
- 객실명/설명/베드타입의 한글 깨짐 데이터는 복구 완료됐다. 현재 값은
  `스탠다드 트윈/트윈`, `디럭스 더블/더블`, `이그제큐티브 스위트/킹`이다.
- quick tunnel은 계정 없이 쓸 수 있으나 재기동마다 URL이 바뀐다. 고정 URL은 도메인 구매와
  Cloudflare named tunnel 구성이 필요하므로 보류했다.
- 후기 페이지 코드는 유지하고 내비·푸터 링크만 숨겨 둔 상태다(`/reviews` 직접 접근은 가능).

### 3. 즉시 다음 단계

1. Docker Desktop을 실행한다.
2. 저장소에서 `git switch demo`와 `git pull --ff-only origin demo`로 기준점을 맞춘다.
3. 일반 기동은 `docker compose up -d`, 코드 반영 재빌드는 `docker compose up -d --build`를 쓴다.
4. `docker compose ps`와 `http://localhost:8080`, `http://localhost:8080/admin`을 확인한다.
5. 프론트 핫리로드가 필요하면 별도 터미널에서 `cd frontend` 후 `npm run dev`를 실행한다.
6. 외부 시연이 필요할 때만 quick tunnel을 실행하고, 출력된 새 URL을 공유한다.
7. 신규 개발 후보는 고정 URL+OG 절대 URL, 업로드 저장소 R2 이전, `demo`→`main` 병합 결정이다.

### 4. 결정과 근거

- D-050: 라이브러리 없이 사전+컨텍스트로 한/영 UI를 구성한다.
- D-051: 객실·요금제 번역은 프론트 하드코딩 대신 DB `name_en`을 단일 소스로 쓴다.
- D-052: 객실 이미지는 최대 3장 고정 컬럼으로 관리하고 부킹엔진 캐러셀에 노출한다.
- D-053: 현 단계는 비용 없는 Docker 로컬 볼륨을 쓰고, 정식 배포 시 저장 서비스 구현만 R2로 교체한다.

### 새 세션 첫 프롬프트

아래 문장을 새 Codex 세션 첫 메시지로 사용한다.

> `demo` 브랜치 작업을 이어가자. 먼저 `docs/DEMO-HANDOFF.md`의 "Codex 새 세션 인계 — 2026-08-05"와
> `docs/decisions.md` D-050~D-053을 읽고, `git status`와 Docker 상태를 확인한 다음 현재 상태를 요약해줘.
> 기존 사용자 변경과 Docker 볼륨의 업로드 이미지는 보존하고, 내가 다음 작업을 지정할 때까지 코드는 수정하지 마.
