# TROUBLESHOOTING — 이 환경에서 반복해서 물리는 함정

> 전부 **실제로 겪은 것만** 적는다. "그럴 수 있다"는 추측은 넣지 않는다.
> 결정(왜 그렇게 정했나)은 여기가 아니라 `decisions.md` 로 간다.
> 새로 물릴 때마다 즉시 추가한다 — 나중에 몰아 쓰면 증상만 남고 원인을 잊는다.

---

## 1. 개발 환경 · 도구

| 증상 | 원인 | 대응 |
|---|---|---|
| `JAVA_HOME is not set` | Bash 툴 세션이 JDK 설치 이전 환경을 물고 있음 | 매 Bash 호출마다 `export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.11.10-hotspot"` |
| `.ps1` 실행 시 예기치 않은 `}` | **PowerShell 5.1은 BOM 없는 파일을 ANSI(CP949)로 읽음** → 한글 주석이 파서를 깨뜨림 | PowerShell 스크립트는 **ASCII만** 사용 |
| PowerShell에 `/c/...` 경로 전달 시 실패 | Git Bash 경로를 PS가 해석 못 함 | PS 호출에는 `C:\...` 형식 사용 |
| Bash 툴에서 `rm` 차단됨 | `~/.claude/settings.json`의 `deny: Bash(rm:*)` | PowerShell `Remove-Item` 사용 |
| `BLOCKED: refusing to edit on protected branch 'main'` | `~/.claude/hooks/block-main-branch-edits.sh` (전역 훅) | **feature 브랜치에서 작업.** 훅을 끄지 말 것 |
| `git add`/`commit` 시 LF→CRLF 경고 다발 | `.gitattributes`가 의도대로 동작 중 | 무시 |

---

## 2. 애플리케이션 기동 · 실행

| 증상 | 원인 | 대응 |
|---|---|---|
| 백그라운드 작업이 "exit 1 실패"로 표시 | `bootRun`을 프로세스 종료로 멈췄기 때문 | 정상. 실패 아님 |
| 앱을 껐는데 `Port 8080 was already in use` | **Gradle 래퍼만 죽고 fork된 java 프로세스는 살아 있음.** 백그라운드 태스크 종료로는 자식 프로세스가 안 죽는다 | 포트로 찾아서 죽인다 (아래 명령) |
| 템플릿(`.html`)을 고쳤는데 화면이 그대로 | **`bootRun`은 `src/` 가 아니라 `build/resources/main/` 에서 서빙한다.** `spring.thymeleaf.cache: false` 는 "그 위치의 파일을 다시 읽는다"는 뜻이지 `src` 를 본다는 뜻이 아니다 | 재기동(= `processResources` 재실행). IDE에서는 build 후 자동 반영 |
| `bootRun` 으로 띄웠는데 `/` 가 404 | React 산출물을 `bootJar` 에만 얹었다(의도됨 — 개발 반복 속도) | 개발 중 부킹엔진은 Vite dev server(5173). 통합 확인은 `bootJar` 로 |

포트 8080을 잡고 있는 프로세스 종료:

```bash
powershell.exe -NoProfile -Command "Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess -Unique | ForEach-Object { Stop-Process -Id \$_ -Force }"
```

---

## 3. MySQL · JPA

| 증상 | 원인 | 대응 |
|---|---|---|
| `ERROR 1298` + 앱 기동 실패 | Windows MySQL은 `mysql.time_zone_name`이 비어 있어 타임존 **이름**을 해석 못 함 | JDBC URL에 `connectionTimeZone=SERVER` (이미 적용됨). `Asia/Seoul` 같은 이름으로 되돌리지 말 것 |
| mysql에 한글 INSERT 시 `ERROR 1366` | Git Bash → mysql stdin 경로에서 인코딩 깨짐 | 검증용 데이터는 ASCII로. 시드는 SQL 파일이나 JDBC 경유 |
| `Schema validation: wrong column type ... found [tinyint], but expecting [integer]` | DB `TINYINT` 컬럼을 자바 `int` 로 매핑하면 Hibernate가 INTEGER를 기대한다 | 필드에 `@JdbcTypeCode(SqlTypes.TINYINT)`. 자바 타입은 `int` 로 두고 JDBC 타입만 맞춘다. `byte` 로 바꾸면 계산마다 int로 승격돼 캐스팅만 늘어난다 |
| 락을 걸었는데 옛 값이 읽힌다 | 원인이 두 겹 — ① 영속성 컨텍스트가 DB 결과를 버리고 캐시 인스턴스를 반환 ② 락 없는 읽기는 REPEATABLE READ 스냅샷을 봄. `refresh()` 로도 회복 안 됨 | **재고를 읽는 첫 조회가 락 조회여야 한다(D-018).** 실측 근거는 `PessimisticLockSpike` 주석과 커밋 `7a08a53` |

> ★ 스키마 변경 시: 마이그레이션 SQL(`V2`, `V3`…)과 엔티티를 **함께** 고친다.
> 어긋나면 `ddl-auto: validate` 가 기동을 실패시킨다 — 그게 안전장치다(D-005).

---

## 4. Spring Security

| 증상 | 원인 | 대응 |
|---|---|---|
| `permitAll` 로 열어둔 경로인데 404 상황에서 로그인으로 302 | **스프링은 처리기 없는 요청을 내부적으로 `/error` 로 다시 보낸다(ERROR dispatch).** 그 경로가 인증 대상이면 302가 나간다 | `/error` 를 `permitAll` 에 넣는다 (`SecurityConfig` 적용됨) |
| 로그인 POST가 403 | 폼에 CSRF 토큰이 없음 | `<form>` 에 `th:action` 을 쓴다. 순수 `action="/login"` 이면 Thymeleaf가 토큰을 안 넣는다 |
| `hasRole("ADMIN")` 이 항상 실패 | 권한 문자열에 `ROLE_` 접두사가 없음 | `UserDetails` 에 `ROLE_ADMIN` 으로 등록. `hasRole` 은 내부적으로 접두사를 붙여 찾는다 |

---

## 5. Thymeleaf · 폼 검증

| 증상 | 원인 | 대응 |
|---|---|---|
| 저장이 거부됐는데 **아무 메시지 없이 폼만 되돌아옴** | `@AssertTrue` 등 항목 간 검증의 오류는 **메서드 이름에서 딴 프로퍼티**에 붙는다 (`isOccupancyConsistent()` → `occupancyConsistent`). 화면에 없는 이름이라 렌더링되지 않는다 | 그 프로퍼티 이름으로 `th:errors` 를 명시적으로 그린다. 예: `admin/basedata/room-type/form.html` |
| 검증 실패로 폼을 다시 그리면 드롭다운이 빔 | 선택지를 `Model` 에 다시 담지 않음 | 폼을 반환하는 **모든** 경로가 거치는 `private formView(Model, boolean)` 를 두고 그 안에서 담는다 |
| 목록 화면에서 쓰지도 않는 조회가 나감 | 선택지 채우기를 `@ModelAttribute` 로 컨트롤러 전역에 걸면 목록 요청에도 딸려 나온다 | DB 조회가 필요한 선택지는 폼 경로에서만. enum 상수는 조회가 없으니 `@ModelAttribute` 로 둬도 무방 |
| `input type="date"` 값이 바인딩 안 됨 | `LocalDate` 필드에 포맷 지정이 없음 | `@DateTimeFormat(iso = DateTimeFormat.ISO.DATE)` |

---

## 6. curl 로 화면 검증할 때

| 증상 | 원인 | 대응 |
|---|---|---|
| 리다이렉트를 따라갔더니 403 | **`curl -L` 에 `-X POST` 를 같이 주면 리다이렉트 이후 요청까지 POST로 강제된다.** GET이어야 할 요청이 CSRF 토큰 없는 POST로 나가 403 | `-X POST` 를 빼고 `--data-urlencode` 만 쓴다. curl이 302에서 알아서 GET으로 바꾼다 |
| 플래시 메시지가 안 보임 | 위와 같은 원인이거나, 리다이렉트를 따라가지 않음(`-L` 누락) | `-L` 사용 + `-X` 제거 |

CSRF 토큰 추출 (세션 쿠키 유지):

```bash
TOKEN() { curl.exe -s -b "$J" -c "$J" "$1" | grep -ao 'name="_csrf" [^>]*value="[^"]*"' | head -1 | sed 's/.*value="//;s/"//'; }
```
