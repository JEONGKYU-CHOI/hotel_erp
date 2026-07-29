-- =============================================================================
-- V1 : 초기 스키마 (기준정보 · 재고 · 회원 · 예약)
-- =============================================================================
-- 규칙
--   * 이 파일은 한 번 실행되면 절대 수정하지 않는다. Flyway 가 체크섬을 저장하며,
--     내용이 바뀌면 다음 기동에서 checksum mismatch 로 실패한다. 변경은 V2, V3 로.
--   * 상태값은 ENUM 이 아니라 VARCHAR + CHECK 로 둔다. ENUM 은 값 추가마다
--     ALTER TABLE 이 필요하고 MySQL 전용이다. VARCHAR 는 JPA 의
--     @Enumerated(EnumType.STRING) 과 그대로 맞물린다.
--   * 금액은 DECIMAL(12,2). DOUBLE 은 부동소수 오차로 정산이 어긋난다.
--   * tenant_id 는 전부 1 로 고정한다. 멀티테넌시는 백로그(D-007)지만,
--     컬럼을 나중에 추가하면 모든 인덱스와 유니크 제약을 다시 짜야 하므로 미리 둔다.
--   * CHECK 제약은 MySQL 8.0.16+ 에서 실제로 강제된다(8.4 사용).
--     애플리케이션 버그가 있어도 DB 가 마지막에 막는 다층 방어를 노린다.
-- =============================================================================


-- -----------------------------------------------------------------------------
-- 1. room_type : 객실타입 (디럭스 더블, 스위트 …)
--    1차 범위의 콘텐츠 관리는 여기까지다(브리프 3번) — 설명과 대표 이미지 수준.
-- -----------------------------------------------------------------------------
CREATE TABLE room_type (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id           BIGINT       NOT NULL DEFAULT 1,
    code                VARCHAR(20)  NOT NULL              COMMENT '타입 코드 (DLX, STE)',
    name                VARCHAR(100) NOT NULL              COMMENT '표시명 (디럭스 더블)',
    description         TEXT         NULL                  COMMENT '상세 설명 (부킹엔진 노출)',
    image_url           VARCHAR(500) NULL,
    standard_occupancy  TINYINT      NOT NULL DEFAULT 2    COMMENT '기준 인원',
    max_occupancy       TINYINT      NOT NULL DEFAULT 2    COMMENT '최대 인원',
    bed_type            VARCHAR(30)  NULL                  COMMENT 'DOUBLE, TWIN, KING …',
    display_order       INT          NOT NULL DEFAULT 0,
    active              BOOLEAN      NOT NULL DEFAULT TRUE,

    created_at          DATETIME     NOT NULL,
    updated_at          DATETIME     NOT NULL,
    created_by          VARCHAR(50)  NULL,
    updated_by          VARCHAR(50)  NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_room_type_code (tenant_id, code),
    CONSTRAINT chk_room_type_occupancy
        CHECK (standard_occupancy >= 1 AND max_occupancy >= standard_occupancy)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '객실타입 (판매 단위)';


-- -----------------------------------------------------------------------------
-- 2. room : 실제 호실 (101호, 102호 …)
--
--    상태를 점유(occupancy)와 청결(clean) 2축으로 분리한다(브리프 5번 결정).
--    한 컬럼에 몰면 VACANT_CLEAN / VACANT_DIRTY / OCCUPIED_CLEAN … 으로
--    상태가 조합 폭발한다. 대가는 조회 시 조건이 늘어나는 것.
--
--    하우스키핑 모듈 자체는 1차 범위에서 제외했으나 clean_status 컬럼은 남긴다.
--    체크아웃 시 DIRTY 로 바꾸는 한 줄만 구현하면 설계 서사는 성립한다.
-- -----------------------------------------------------------------------------
CREATE TABLE room (
    id                BIGINT      NOT NULL AUTO_INCREMENT,
    tenant_id         BIGINT      NOT NULL DEFAULT 1,
    room_type_id      BIGINT      NOT NULL,
    room_no           VARCHAR(10) NOT NULL              COMMENT '호실 번호',
    floor             SMALLINT    NULL,
    occupancy_status  VARCHAR(20) NOT NULL DEFAULT 'VACANT',
    clean_status      VARCHAR(20) NOT NULL DEFAULT 'CLEAN',
    active            BOOLEAN     NOT NULL DEFAULT TRUE,

    created_at        DATETIME    NOT NULL,
    updated_at        DATETIME    NOT NULL,
    created_by        VARCHAR(50) NULL,
    updated_by        VARCHAR(50) NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_room_no (tenant_id, room_no),
    KEY idx_room_type (room_type_id),
    CONSTRAINT fk_room__room_type FOREIGN KEY (room_type_id) REFERENCES room_type (id),
    CONSTRAINT chk_room_occupancy_status
        CHECK (occupancy_status IN ('VACANT', 'OCCUPIED')),
    CONSTRAINT chk_room_clean_status
        CHECK (clean_status IN ('CLEAN', 'DIRTY', 'INSPECTED', 'OUT_OF_ORDER'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '호실 (배정 단위). 예약 시점에는 확정하지 않고 체크인 때 배정한다.';


-- -----------------------------------------------------------------------------
-- 3. rate_plan : 요금정책
-- -----------------------------------------------------------------------------
CREATE TABLE rate_plan (
    id                      BIGINT        NOT NULL AUTO_INCREMENT,
    tenant_id               BIGINT        NOT NULL DEFAULT 1,
    room_type_id            BIGINT        NOT NULL,
    code                    VARCHAR(20)   NOT NULL,
    name                    VARCHAR(100)  NOT NULL              COMMENT '조식포함 · 환불불가 …',
    base_amount             DECIMAL(12,2) NOT NULL              COMMENT '기본 요금 (일자별 요금이 없을 때 폴백)',
    breakfast_included      BOOLEAN       NOT NULL DEFAULT FALSE,
    refundable              BOOLEAN       NOT NULL DEFAULT TRUE,
    cancel_deadline_days    SMALLINT      NOT NULL DEFAULT 1    COMMENT '체크인 며칠 전까지 무료 취소',
    penalty_rate            DECIMAL(5,2)  NOT NULL DEFAULT 0    COMMENT '기한 후 취소 위약금율 (%)',
    active                  BOOLEAN       NOT NULL DEFAULT TRUE,

    created_at              DATETIME      NOT NULL,
    updated_at              DATETIME      NOT NULL,
    created_by              VARCHAR(50)   NULL,
    updated_by              VARCHAR(50)   NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_rate_plan_code (tenant_id, code),
    KEY idx_rate_plan_room_type (room_type_id),
    CONSTRAINT fk_rate_plan__room_type FOREIGN KEY (room_type_id) REFERENCES room_type (id),
    CONSTRAINT chk_rate_plan_amount CHECK (base_amount >= 0),
    CONSTRAINT chk_rate_plan_penalty CHECK (penalty_rate >= 0 AND penalty_rate <= 100)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '요금정책';


-- -----------------------------------------------------------------------------
-- 4. rate_calendar : 일자별 요금
--    성수기 · 주말 요금 차등을 위해 요금을 날짜 단위로 펼쳐 둔다.
--    행이 없으면 rate_plan.base_amount 로 폴백한다.
-- -----------------------------------------------------------------------------
CREATE TABLE rate_calendar (
    id            BIGINT        NOT NULL AUTO_INCREMENT,
    tenant_id     BIGINT        NOT NULL DEFAULT 1,
    rate_plan_id  BIGINT        NOT NULL,
    stay_date     DATE          NOT NULL,
    amount        DECIMAL(12,2) NOT NULL,
    closed        BOOLEAN       NOT NULL DEFAULT FALSE COMMENT '해당 일자 판매 중지',

    created_at    DATETIME      NOT NULL,
    updated_at    DATETIME      NOT NULL,
    created_by    VARCHAR(50)   NULL,
    updated_by    VARCHAR(50)   NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_rate_calendar (rate_plan_id, stay_date),
    CONSTRAINT fk_rate_calendar__rate_plan FOREIGN KEY (rate_plan_id) REFERENCES rate_plan (id),
    CONSTRAINT chk_rate_calendar_amount CHECK (amount >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '일자별 요금';


-- =============================================================================
-- 5. room_inventory : 일자별 객실 재고  ★ 이 프로젝트의 핵심 테이블 ★
-- =============================================================================
--   물량형(사전 생성) 모델이다. 계산형(예약 COUNT)은 락을 걸 대상 행이 없어
--   동시성 제어가 불가능하다(브리프 5번). 대가는 재고 행 생성 배치가 필요한 것.
--
--   가용 재고 = total_qty - sold_qty - held_qty
--
--   ── 인덱스 컬럼 순서가 데드락 방지의 실체다 ──────────────────────────────
--   연박 예약은 여러 날짜 행을 한 트랜잭션에서 잠근다. 요청마다 락 획득 순서가
--   다르면 데드락이 난다. 그래서 다음처럼 순서를 고정한다.
--
--       SELECT ... WHERE room_type_id = ? AND stay_date BETWEEN ? AND ?
--       ORDER BY stay_date FOR UPDATE
--
--   그런데 ORDER BY 는 "결과를 정렬하라"는 지시일 뿐 "그 순서로 잠그라"는
--   지시가 아니다. InnoDB 는 행을 *읽는 순서대로* 락을 건다. 따라서
--   (room_type_id, stay_date) 를 선두로 갖는 인덱스가 있어야
--   인덱스 레인지 스캔이 stay_date 오름차순으로 일어나고, 그제서야
--   락 획득 순서가 보장된다. 인덱스가 없어 풀스캔이 되면 PK(id) 순으로 읽혀
--   ORDER BY 는 정렬만 하고 데드락 방지는 조용히 무너진다.
--
--   → uk_room_inventory 의 컬럼 순서를 (room_type_id, stay_date, tenant_id) 로 둔다.
--     유니크 제약은 컬럼 순서와 무관하게 성립하므로, 순서를 락 쿼리에 맞춰
--     인덱스 하나가 유니크 보장과 락 순서 보장을 동시에 하게 한다.
--     tenant_id 를 선두에 두면 이 인덱스를 레인지 스캔에 쓸 수 없다.
--
--   검증: EXPLAIN 으로 uk_room_inventory 를 타는지 확인하고,
--         인덱스를 지운 상태에서 데드락이 재현되는지도 테스트로 남긴다.
-- =============================================================================
CREATE TABLE room_inventory (
    id            BIGINT      NOT NULL AUTO_INCREMENT,
    tenant_id     BIGINT      NOT NULL DEFAULT 1,
    room_type_id  BIGINT      NOT NULL,
    stay_date     DATE        NOT NULL,
    total_qty     INT         NOT NULL DEFAULT 0 COMMENT '판매 가능 총 객실 수',
    sold_qty      INT         NOT NULL DEFAULT 0 COMMENT '확정 예약 수',
    held_qty      INT         NOT NULL DEFAULT 0 COMMENT '임시점유(HOLD) 수',

    created_at    DATETIME    NOT NULL,
    updated_at    DATETIME    NOT NULL,
    created_by    VARCHAR(50) NULL,
    updated_by    VARCHAR(50) NULL,

    PRIMARY KEY (id),

    -- 컬럼 순서에 의미가 있다. 위 주석 참조.
    UNIQUE KEY uk_room_inventory (room_type_id, stay_date, tenant_id),

    CONSTRAINT fk_room_inventory__room_type FOREIGN KEY (room_type_id) REFERENCES room_type (id),

    -- DB 를 최후 방어선으로 삼는다. 애플리케이션 로직에 버그가 있어도
    -- 오버부킹은 여기서 error 3819 로 거부된다.
    CONSTRAINT chk_room_inventory_qty CHECK (
        total_qty >= 0
        AND sold_qty >= 0
        AND held_qty >= 0
        AND sold_qty + held_qty <= total_qty
    )
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '일자별 객실 재고. 동시성 제어의 락 대상.';


-- -----------------------------------------------------------------------------
-- 6. member : 고객 회원 (부킹엔진, JWT 인증)
--
--    직원 계정과 테이블을 분리한다. 섞으면 고객 레코드에 ADMIN 권한을 줄 수
--    있는 구조가 되고, 인증 경로도 서로 다르다(D-001: 부킹엔진 JWT /
--    백오피스 세션). 관심사가 다르면 테이블도 나눈다.
--
--    비회원 예약이 기본이므로(실제 호텔 직판 사이트 방식) 회원은 선택 사항이다.
-- -----------------------------------------------------------------------------
CREATE TABLE member (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id      BIGINT       NOT NULL DEFAULT 1,
    email          VARCHAR(255) NOT NULL,
    password_hash  VARCHAR(255) NOT NULL COMMENT 'BCrypt. 평문은 어떤 경우에도 저장하지 않는다.',
    name           VARCHAR(50)  NOT NULL,
    phone          VARCHAR(20)  NOT NULL,
    status         VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',

    created_at     DATETIME     NOT NULL,
    updated_at     DATETIME     NOT NULL,
    created_by     VARCHAR(50)  NULL,
    updated_by     VARCHAR(50)  NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_member_email (tenant_id, email),
    KEY idx_member_phone (phone),
    CONSTRAINT chk_member_status CHECK (status IN ('ACTIVE', 'DORMANT', 'WITHDRAWN'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '고객 회원';


-- -----------------------------------------------------------------------------
-- 7. staff_user : 직원 계정 (백오피스, 세션 인증)
-- -----------------------------------------------------------------------------
CREATE TABLE staff_user (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    tenant_id      BIGINT       NOT NULL DEFAULT 1,
    username       VARCHAR(50)  NOT NULL,
    password_hash  VARCHAR(255) NOT NULL COMMENT 'BCrypt',
    name           VARCHAR(50)  NOT NULL,
    role           VARCHAR(20)  NOT NULL DEFAULT 'STAFF',
    active         BOOLEAN      NOT NULL DEFAULT TRUE,

    created_at     DATETIME     NOT NULL,
    updated_at     DATETIME     NOT NULL,
    created_by     VARCHAR(50)  NULL,
    updated_by     VARCHAR(50)  NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_staff_username (tenant_id, username),
    CONSTRAINT chk_staff_role CHECK (role IN ('ADMIN', 'STAFF'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '백오피스 직원 계정';


-- =============================================================================
-- 8. reservation : 예약
-- =============================================================================
--   ── Hold 전용 테이블을 두지 않는다 ────────────────────────────────────────
--   임시점유를 별도 테이블로 만들지 않고 예약 자체를 status = 'HOLD' 로 생성한다.
--     * 예약번호를 미리 발급해 PG 에 넘길 수 있고, 결제 성공 시 상태만 전이하면
--       된다(행 이동 불필요).
--     * 만료된 HOLD 가 EXPIRED 로 남아 이탈 예약 통계가 부수적으로 확보된다.
--     * 테이블이 하나 줄어든다.
--   스케줄러는 (status='HOLD' AND hold_expires_at < NOW()) 를 찾아 EXPIRED 로
--   바꾸고 room_inventory.held_qty 를 되돌린다. 조회 시점에도 만료분을 제외하면
--   스케줄러 주기로 인한 지연이 사용자에게 보이지 않는다.
--
--   ── 호실은 예약 시점에 확정하지 않는다 ────────────────────────────────────
--   타입 재고만 차감하고 room_id 는 NULL 로 둔다. 체크인 때 배정한다(브리프 5번).
--
--   ── 멱등성 ────────────────────────────────────────────────────────────────
--   idempotency_key 에 유니크 제약을 걸어 같은 요청이 두 번 들어와도 예약이
--   두 건 생기지 않게 한다. 네트워크 재시도와 사용자의 더블클릭 양쪽을 막는다.
--
--   ── nights 컬럼을 두지 않는 이유 ──────────────────────────────────────────
--   숙박일수는 check_out_date - check_in_date 로 계산되는 파생값이다.
--   컬럼으로 두면 두 날짜와 어긋날 수 있는 여지가 생긴다. 필요하면 계산하거나
--   reservation_night 행 수를 센다.
-- =============================================================================
CREATE TABLE reservation (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    tenant_id         BIGINT        NOT NULL DEFAULT 1,
    reservation_no    VARCHAR(20)   NOT NULL COMMENT '고객에게 노출되는 예약번호',

    member_id         BIGINT        NULL     COMMENT 'NULL 이면 비회원 예약',
    guest_name        VARCHAR(50)   NOT NULL,
    guest_phone       VARCHAR(20)   NOT NULL COMMENT '비회원 예약 조회 키',
    guest_email       VARCHAR(255)  NULL,

    room_type_id      BIGINT        NOT NULL,
    rate_plan_id      BIGINT        NOT NULL,
    check_in_date     DATE          NOT NULL,
    check_out_date    DATE          NOT NULL,
    adults            TINYINT       NOT NULL DEFAULT 1,
    children          TINYINT       NOT NULL DEFAULT 0,

    status            VARCHAR(20)   NOT NULL,
    total_amount      DECIMAL(12,2) NOT NULL COMMENT 'reservation_night 합계 스냅샷',

    hold_expires_at   DATETIME      NULL     COMMENT 'status=HOLD 일 때만 값이 있다',
    room_id           BIGINT        NULL     COMMENT '체크인 시 배정',
    idempotency_key   VARCHAR(64)   NOT NULL,

    cancelled_at      DATETIME      NULL,
    cancel_reason     VARCHAR(200)  NULL,

    created_at        DATETIME      NOT NULL,
    updated_at        DATETIME      NOT NULL,
    created_by        VARCHAR(50)   NULL,
    updated_by        VARCHAR(50)   NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_reservation_no (reservation_no),
    UNIQUE KEY uk_reservation_idempotency (tenant_id, idempotency_key),

    -- 스케줄러가 만료 HOLD 를 훑는 경로
    KEY idx_reservation_hold (status, hold_expires_at),
    -- 백오피스 도착목록 (오늘 체크인 예정)
    KEY idx_reservation_arrival (tenant_id, check_in_date, status),
    -- 비회원 예약 조회 (예약번호 + 전화번호)
    KEY idx_reservation_guest_phone (guest_phone),
    KEY idx_reservation_member (member_id),
    KEY idx_reservation_room (room_id),

    CONSTRAINT fk_reservation__room_type FOREIGN KEY (room_type_id) REFERENCES room_type (id),
    CONSTRAINT fk_reservation__rate_plan FOREIGN KEY (rate_plan_id) REFERENCES rate_plan (id),
    CONSTRAINT fk_reservation__member    FOREIGN KEY (member_id)    REFERENCES member (id),
    CONSTRAINT fk_reservation__room      FOREIGN KEY (room_id)      REFERENCES room (id),

    CONSTRAINT chk_reservation_status CHECK (
        status IN ('HOLD', 'CONFIRMED', 'CHECKED_IN', 'CHECKED_OUT',
                   'CANCELLED', 'NO_SHOW', 'EXPIRED')
    ),
    CONSTRAINT chk_reservation_dates  CHECK (check_out_date > check_in_date),
    CONSTRAINT chk_reservation_amount CHECK (total_amount >= 0),
    CONSTRAINT chk_reservation_pax    CHECK (adults >= 1 AND children >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '예약. HOLD 상태가 임시점유를 겸한다.';


-- -----------------------------------------------------------------------------
-- 9. reservation_night : 예약 일자별 요금 스냅샷
--
--    요금은 "계산 시점 스냅샷"으로 저장한다(브리프 5번 결정).
--    rate_calendar 를 참조만 하면 정책이 바뀔 때 과거 예약 금액까지 변한다.
--    정규화 관점에서는 중복으로 보이지만, 시점 데이터를 보존해야 하는
--    회계 요구가 정규화보다 우선한다. 이 판단은 decisions.md 에 기록한다.
--
--    이 테이블이 있어야 가능한 것:
--      * 성수기 · 주말 요금 차등 (일자마다 금액이 다름)
--      * 야간마감의 "오늘 밤 객실료 게시" (해당 일자 행을 폴리오로 전기)
--      * 부분 취소 · 연박 변경
-- -----------------------------------------------------------------------------
CREATE TABLE reservation_night (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    reservation_id  BIGINT        NOT NULL,
    stay_date       DATE          NOT NULL,
    room_type_id    BIGINT        NOT NULL,
    rate_plan_id    BIGINT        NOT NULL,
    rate_amount     DECIMAL(12,2) NOT NULL COMMENT '계산 시점 요금 스냅샷',
    posted          BOOLEAN       NOT NULL DEFAULT FALSE COMMENT '야간마감이 폴리오에 게시했는지',

    created_at      DATETIME      NOT NULL,
    updated_at      DATETIME      NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_reservation_night (reservation_id, stay_date),
    KEY idx_reservation_night_date (stay_date, posted),
    CONSTRAINT fk_reservation_night__reservation
        FOREIGN KEY (reservation_id) REFERENCES reservation (id),
    CONSTRAINT chk_reservation_night_amount CHECK (rate_amount >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '예약 일자별 요금 스냅샷';
