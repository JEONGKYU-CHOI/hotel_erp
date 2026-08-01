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

## 5. 다음 후보 (사용자와 논의된 것)

포폴 콘텐츠 더 추가안(우선순위):
1. ✅ **프로모션/패키지** — 구현 완료(§4-1).
2. ✅ **이용안내/FAQ** — 구현 완료(§4-1).
3. 갤러리 ✅(§4-1) · About/브랜드 스토리 · 후기(미완).
4. 다국어(한/영) 토글 · SEO/OG/파비콘 · 로딩 스켈레톤.

미완/보류: 도커 8080 재빌드 반영, `demo`→`main` 머지 여부(사용자 결정 대기), 실제 주소/전화/토스 실키.
