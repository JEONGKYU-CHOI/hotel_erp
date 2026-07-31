# DEPLOY — 무료 시연 배포 (docker-compose + Cloudflare Tunnel)

포폴 시연용 배포. **내 PC에서 앱 전체를 켜고, Cloudflare Tunnel 로 외부 URL 하나를 뚫는다.**
비용 0, 카드 불필요. 프론트·API·백오피스가 한 앱(같은 도메인)이라 CORS·설정 분기가 없다.

```
[면접관] → https://xxxx.trycloudflare.com  (Cloudflare)
                     │  (터널)
                     ▼
        내 PC: docker compose (Spring 앱 + MySQL)  ← localhost:8080
```

시연할 때만 켠다. PC·앱·터널 중 하나라도 꺼지면 링크는 죽는다.

---

## 0. 준비 (최초 1회)

- Docker Desktop 실행 중일 것
- `.env` 만들기:
  ```bash
  cp .env.example .env
  ```
  `.env` 를 열어 `DB_PASSWORD` · `JWT_SECRET`(32자 이상) · `ADMIN_INIT_PASSWORD` 를 실제 값으로.
  결제 시연하려면 `TOSS_*` 도 채운다(테스트 키로 흐름 시연 가능).

---

## 1. 앱 켜기

```bash
docker compose up --build -d
```

- 최초 빌드는 프론트(npm)·백엔드(gradle) 때문에 수 분 걸린다. 이후는 캐시로 빠르다.
- MySQL 이 뜨고 → 앱이 Flyway 로 스키마 생성 → `demo` 시더가 데모 데이터(오늘~+60일 재고) →
  `prod` 시더가 관리자 계정(ADMIN_INIT_*)을 만든다.
- 상태·로그:
  ```bash
  docker compose ps
  docker compose logs -f app
  ```
- 확인: 브라우저에서 http://localhost:8080 (부킹엔진) · http://localhost:8080/admin (백오피스).
  백오피스 로그인은 `.env` 의 `ADMIN_INIT_USERNAME`/`ADMIN_INIT_PASSWORD`.

## 2. 터널 뚫기 (외부 공개)

계정 없이 임시 URL 을 발급하는 quick tunnel:

```bash
"C:\Program Files (x86)\cloudflared\cloudflared.exe" tunnel --url http://localhost:8080
```

- 출력에 `https://<랜덤>.trycloudflare.com` 이 뜬다 — 이게 시연 링크다.
- 이 창을 켜 두는 동안만 링크가 산다. 끄면 죽는다.
- (선택) 고정 URL·본인 도메인을 쓰려면 Cloudflare 로그인 후 named tunnel 을 구성한다.

## 3. 끄기

```bash
docker compose down          # 컨테이너 정지(데이터 볼륨은 유지)
docker compose down -v       # 데이터까지 초기화(다음 기동에 데모 재시드)
```
터널은 cloudflared 창을 Ctrl+C.

---

## 시연 체크리스트

- [ ] `docker compose up -d` → `docker compose logs -f app` 에서 "Started ... in N seconds"
- [ ] http://localhost:8080 홈 뜸 / 객실 검색됨
- [ ] `/admin` 로그인됨(env 계정)
- [ ] cloudflared URL 로 외부에서 접속됨
- [ ] (결제) TOSS 테스트 키로 검색→HOLD→결제창→성공 흐름

## 트러블슈팅

| 증상 | 대응 |
|---|---|
| 앱이 MySQL 못 붙음 | `docker compose logs mysql` — healthy 될 때까지 앱은 대기한다. `.env` 의 `DB_PASSWORD` 일치 확인 |
| `/admin` 로그인 불가 | `ADMIN_INIT_PASSWORD` 를 비워 뒀을 때. 채우고 `docker compose up -d` 재기동 |
| 데모 데이터가 안 보임 | 볼륨에 옛 데이터가 있으면 시더가 건너뛴다. `docker compose down -v` 후 재기동 |
| 포트 8080 충돌 | 호스트에서 8080 쓰는 다른 앱 종료(로컬 bootRun 등) |
| 결제 승인 실패 | 더미/테스트 키로는 실 승인 불가. 콘솔 발급 테스트 키 사용 |
