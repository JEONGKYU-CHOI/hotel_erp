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
  - ⚠️ **예약 플로우 폼(BookingForm·BookingPage·LookupPage·MyReservations·Login·Signup·
    결제 착지)은 아직 한국어** — 기능 퍼널이라 별도 후속. 같은 두 패턴으로 확장하면 된다.
- **검증**: dev(5173) 실측(렌더·필터·언어전환·모달 포커스) + vite 프로덕션 빌드 무에러 +
  백엔드 전체 스위트 그린. 8080 은 docker 재빌드 반영.

## 5. 다음 후보 (사용자와 논의된 것)

- 프로모션/FAQ/갤러리 ✅(§4-1). About/브랜드 스토리 · 후기 · 다국어(한/영) 미완 → §6.

## 6. 남은 백로그 (다음 세션)

- **[D 운영]** ✅ 테스트 스위트 회귀 확인(세션3) · ⭐ `demo`→`main` 머지 결정 · ⭐ 고정 URL(named tunnel, ⚠️OG 절대URL 선행조건 — **본인 Cloudflare 계정·도메인 필요**) · 실주소/전화/토스 실키
- **[C 완성도]** ✅ SEO/OG/파비콘 · ✅ 결제 만료 카운트다운 · ✅ 로딩 스켈레톤·빈/에러 상태 (세션3) · ✅ 접근성(세션5)
- **[B 콘텐츠]** ✅ 후기(리뷰) · ✅ About/브랜드 스토리 · ✅ 다국어(한/영, 셸+홈까지·본문 페이지 확장 남음) (세션5) · 객실별 상세 갤러리
- **[A 기능]** ✅ PMS 대시보드 지표(매출·OCC·ADR, 세션4)

남은 것(대부분 사용자 액션·결정): **고정 URL+OG 절대화(Cloudflare 계정) · demo→main 머지 결정 ·
실주소/전화/토스 실키 · i18n 본문 페이지 확장 · 객실별 상세 갤러리**.
