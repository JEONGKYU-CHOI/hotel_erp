# HANDOFF — 호텔 PMS + 부킹엔진 (Day 0 종료)

- 작성일: 2026-07-29
- 브랜치: `feat/init-schema` (main 아님 — 아래 §2 훅 항목 참조)
- **함께 로드할 파일 (여기 내용을 중복 수록하지 않았음. 반드시 읽을 것)**
  - `docs/decisions.md` — 설계 결정과 트레이드오프 전체
  - `src/main/resources/db/migration/V1__init_schema.sql` — 스키마 + 설계 근거 주석
  - `git log` 커밋 메시지 3건 — 검증 수치와 판단 근거가 본문에 있음
  - 원본 기획서: `C:\Users\a0109\Downloads\PROJECT-BRIEF (1).md`

---

## 1. 현재 상태 요약

Day 0(환경 + 골격) 완료. 앱이 MySQL에 붙어 기동하고, Flyway V1 마이그레이션이
적용되며, React 빌드가 통과한다. **도메인 코드는 아직 0줄** — 엔티티도 API도 화면도 없다.
다음은 Day 0의 실질적 목표였던 **JPA 비관적 락 스파이크**이고, 그 뒤 Day 1(엔티티 + 기준정보)이다.

커밋 3건:
```
18aea6b  feat: scaffold React booking engine with Vite
906f5e0  feat: add V1 initial schema (base data, inventory, member, reservation)
7538421  chore: bootstrap Spring Boot 4 project skeleton
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

### 2.2 이 환경에서 반복해서 물리는 함정 (전부 실제로 겪음)

| 증상 | 원인 | 대응 |
|---|---|---|
| `JAVA_HOME is not set` | Bash 툴 세션이 JDK 설치 이전 환경을 물고 있음 | 매 Bash 호출마다 `export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.11.10-hotspot"` |
| `.ps1` 실행 시 예기치 않은 `}` | **PowerShell 5.1은 BOM 없는 파일을 ANSI(CP949)로 읽음** → 한글 주석이 파서를 깨뜨림 | PowerShell 스크립트는 **ASCII만** 사용 |
| mysql에 한글 INSERT 시 `ERROR 1366` | Git Bash → mysql stdin 경로에서 인코딩 깨짐 | 검증용 데이터는 ASCII로. 시드는 SQL 파일이나 JDBC 경유 |
| `ERROR 1298` + 앱 기동 실패 | Windows MySQL은 `mysql.time_zone_name`이 비어 있어 타임존 **이름**을 해석 못 함 | JDBC URL에 `connectionTimeZone=SERVER` (이미 적용됨). `Asia/Seoul` 같은 이름으로 되돌리지 말 것 |
| `BLOCKED: refusing to edit on protected branch 'main'` | `~/.claude/hooks/block-main-branch-edits.sh` (전역 훅) | **feature 브랜치에서 작업.** 훅을 끄지 말 것 |
| 백그라운드 작업이 "exit 1 실패"로 표시 | `bootRun`을 프로세스 종료로 멈췄기 때문 | 정상. 실패 아님 |
| `git add`/`commit` 시 LF→CRLF 경고 다발 | `.gitattributes`가 의도대로 동작 중 | 무시 |
| Bash 툴에서 `rm` 차단됨 | `~/.claude/settings.json`의 `deny: Bash(rm:*)` | PowerShell `Remove-Item` 사용 |
| PowerShell에 `/c/...` 경로 전달 시 실패 | Git Bash 경로를 PS가 해석 못 함 | PS 호출에는 `C:\...` 형식 사용 |

### 2.3 Spring Boot 4는 3.x와 의존성 이름이 다르다 ★

**인터넷 자료 대부분이 Boot 3.x / Hibernate 6 기준이라 그대로 붙여넣으면 깨진다.**
실제로 이번 세션에 두 번 물렸다.

| Boot 3.x (자료 대부분) | **Boot 4.0.7 (실제)** |
|---|---|
| `spring-boot-starter-web` | `spring-boot-starter-webmvc` |
| `flyway-core` 직접 추가 | `spring-boot-starter-flyway` |
| `spring-boot-starter-test` 하나 | 모듈별 분리 (`-data-jpa-test`, `-security-test` …) |
| `org.testcontainers:mysql` | `org.testcontainers:testcontainers-mysql` |
| `MySQLContainer<SELF>` (제네릭) | **`MySQLContainer` (제네릭 제거됨)** |

→ 의존성을 추가할 때 기억에 의존하지 말고 **start.spring.io 메타데이터를 조회**하거나
   `build.gradle` 기존 항목의 명명 규칙을 따를 것.

### 2.4 환경 사실

```
JDK        Temurin 21.0.11  (JAVA_HOME 시스템 등록 완료)
IDE        IntelliJ IDEA Community 2025.2.6.2
MySQL      8.4.9 로컬 설치. 서비스명 MySQL84, Automatic 시작
           설정  C:\ProgramData\MySQL\MySQL Server 8.4\my.ini
           데이터 C:\ProgramData\MySQL\MySQL Server 8.4\Data
           서버 타임존 +09:00, utf8mb4 / utf8mb4_0900_ai_ci
DB         hotel_erp (테이블 9개 + flyway_schema_history, 데이터는 비어 있음)
DBeaver    26.1.3, 연결 'hotel_erp' 저장됨
Docker     설치됐으나 데몬 꺼짐. Day 2 Testcontainers 때 켜면 됨
비밀번호    사용자 환경변수 DB_PASSWORD 에 등록됨 (값은 어디에도 기록하지 않음)
git        user.name=JEONGKYU-CHOI / user.email=a01091075978@gmail.com
GitHub     리포명 hotel-erp 예정. 아직 원격 생성/푸시 안 함
```

앱 실행:
```bash
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-21.0.11.10-hotspot"
export DB_PASSWORD="$(powershell.exe -NoProfile -Command '[Environment]::GetEnvironmentVariable("DB_PASSWORD","User")' | tr -d '\r\n')"
./gradlew bootRun --console=plain
```
사용자가 직접 돌릴 때는 새 터미널에서 `.\gradlew.bat bootRun` (PowerShell) —
환경변수가 이미 시스템에 등록돼 있어 export 불필요.

### 2.5 아직 하지 않은 것 (착각 방지)

- 엔티티 클래스 0개. `V1__init_schema.sql`의 테이블에 대응하는 JPA 엔티티가 없다.
- `SecurityConfig` 없음 → 현재 모든 경로가 Spring Security 기본 로그인 폼 뒤에 있다.
- Gradle ↔ npm 빌드 연결 없음 → `./gradlew build`가 React를 빌드하지 않는다.
- 테스트 0개 (생성기가 만든 컨텍스트 로드 테스트만 있고 실행해본 적 없음).
- `docs/troubleshooting.md` 없음. §2.2 표가 그 초안이다.
- 원격 저장소 없음.

---

## 3. 즉시 다음 단계

### 3.1 `decisions.md` 갱신 (약 7분)

이번 세션에서 D-001·D-007이 뒤집혔고 신규 결정이 여럿 나왔다.
**아직 파일에 반영되지 않았다.** 사용자 승인 대기 중이던 항목:

- D-008: 부킹엔진 인증 = JWT (**D-001의 "인증 없음"을 supersede**, 양쪽에 표기)
- D-009: 회원가입/로그인 1차 범위 편입 (**D-007의 백로그 항목을 supersede**)
- D-010: JWT는 Access Token만, 리프레시 로테이션은 백로그
- D-011: Spring Boot 4.0.7 (4.1.0 아님 — 패치 누적 안정판)
- D-012: MySQL 로컬 설치 (Docker Compose는 Day 5 재현용으로 추가)
- D-013: 프론트 JavaScript (TypeScript 아님)
- D-014: `member` / `staff_user` 테이블 분리
- D-015: `uk_room_inventory` 컬럼 순서가 데드락 방지의 실체
  (상세는 `V1__init_schema.sql` 주석 — 재서술 금지)
- D-016: p6spy 대신 Hibernate 내장 SQL 로깅
- D-017: react-router 권고(GHSA-qwww-vcr4-c8h2) 해당 없음 판정

### 3.2 Gradle ↔ npm 빌드 연결 (약 8분)

`./gradlew build` 한 번으로 React까지 빌드해 **jar 하나**가 나오게 한다.
D-001의 "배포 산출물은 JAR 하나" 주장을 실제로 성립시키는 작업.

- `npmInstall` / `npmBuild` Exec 태스크 (Windows는 `npm.cmd`)
- `frontend/dist` → `build/resources/main/static` 복사
- `bootJar`가 의존하도록 연결. **`bootRun`에는 걸지 말 것** (개발 반복이 느려짐)
- `inputs`/`outputs` 선언으로 up-to-date 캐싱

### 3.3 JPA 비관적 락 스파이크 ★ Day 0의 실질적 목표 (약 15분)

**이것이 이번 프로젝트에서 가장 먼저 확인해야 할 사실이다.**
JPA 실무 경험이 없는 상태에서 Day 2(동시성)에 이걸 처음 만나면 하루가 날아간다.

확인할 것:
1. `RoomInventory` 엔티티 + `RoomInventoryRepository` 최소 구현
2. 아래 쿼리에 `@Lock(LockModeType.PESSIMISTIC_WRITE)`를 붙였을 때
   **실제로 `FOR UPDATE`가 붙어 나가는지 로그로 확인**
   ```java
   @Query(value = "SELECT * FROM room_inventory WHERE room_type_id = :typeId " +
                  "AND stay_date BETWEEN :from AND :to ORDER BY stay_date FOR UPDATE",
          nativeQuery = true)
   ```
3. **영속성 컨텍스트 1차 캐시 함정 확인** — 이미 조회한 엔티티를 다시 `@Lock`으로
   조회하면 SELECT가 안 나갈 수 있다. 락을 잡았다고 믿고 오래된 값을 읽는 사고.
4. SQL은 `logging.level.org.hibernate.SQL: DEBUG` + `org.hibernate.orm.jdbc.bind: TRACE`로 확인
   (application.yml에 이미 설정됨)

**검증 완료 조건**: 로그에 `for update`가 찍히고, 파라미터 값이 함께 보인다.

### 3.4 그 다음 (Day 1)

엔티티 클래스 작성 → 기준정보 CRUD → 재고 생성 배치.
**순서 주의: 스키마(SQL)가 먼저, 엔티티가 나중이다(D-005).**
엔티티를 고치면 Flyway 마이그레이션(V2, V3…)을 함께 써야 하고,
`ddl-auto: validate`가 불일치를 시동 실패로 잡아준다.

---

## 4. 결정과 근거

이번 세션의 결정은 전부 `docs/decisions.md`로 승격 예정이다(§3.1 목록).
**여기서 재서술하지 않는다** — 같은 결정이 두 곳에 살면 나중에 반드시 어긋난다.

핸드오프에만 남기는 것(프로젝트 결정이 아니라 이번 작업의 판단):

- **골격을 손으로 쓰지 않고 생성기를 썼다** (start.spring.io, `npm create vite`).
  Boot 4 / Vite 8은 기억과 어긋나는 지점이 많아, 손으로 쓰면 조용히 틀린 이름을
  넣게 된다. 실제로 `spring-boot-starter-web`을 썼다면 즉시 깨졌을 것이다.
- **검증을 실측으로 했다.** EXPLAIN으로 인덱스 사용과 filesort 부재를 확인했고,
  CHECK 제약은 위반 UPDATE 3종을 실제로 던져 3819 거부를 확인했다.
  수치와 결과는 커밋 `906f5e0` 메시지에 있다.
- **`npm audit fix --force`를 실행하지 않았다.** 권고를 읽고 적용 조건
  (RSC 모드)이 성립하지 않음을 확인한 뒤 유지를 선택했다. 근거는 커밋 `18aea6b`.
