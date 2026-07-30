-- =============================================================================
-- V2 : 야간마감 이력 (night_close)  ★ 핵심 기술 과제 ③ : 마감 멱등성 ★
-- =============================================================================
--   야간마감은 하루에 한 번, "그날 밤 숙박분"을 폴리오에 게시하는 배치다(D-004).
--   배치는 재실행될 수 있다 — 크론 중복 기동, 실패 후 수동 재시도, 스케줄러 겹침.
--   같은 영업일을 두 번 마감해도 숙박분이 두 번 게시되어서는 안 된다.
--
--   ── 멱등성을 두 겹으로 막는다 ────────────────────────────────────────────
--   ① 이 테이블의 UNIQUE (tenant_id, business_date)
--      한 영업일당 마감 이력은 정확히 한 행이다. 두 번째 마감이 이 행을 또 만들려 하면
--      DB 가 error 1062 로 거부한다. 애플리케이션이 선조회를 빠뜨려도 DB 가 최후에 막는다.
--   ② reservation_night.posted (V1 에 이미 있음)
--      게시된 숙박분은 posted = TRUE 로 표시된다. 마감은 posted = FALSE 인 행만 게시하므로,
--      설령 마감 이력 없이 게시 로직만 두 번 돌아도 같은 숙박분이 두 번 게시되지 않는다.
--
--   두 방어선은 다른 층을 막는다 — ①은 "영업일 단위 재마감", ②는 "숙박분 단위 재게시".
--   Spring Batch 의 재실행 방지를 프레임워크에 위임하지 않고 직접 설계한 지점이다(D-004).
--
--   ── 왜 폴리오 원장 테이블이 없는가 ──────────────────────────────────────
--   1차 범위에서 "게시"는 reservation_night.posted 전환 + 이 테이블의 집계 기록까지다.
--   고객 청구서(폴리오 원장)는 결제·정산 모듈과 함께 후속 범위에서 다룬다. 마감 멱등성의
--   증명에는 posted 플래그와 마감 이력만으로 충분하다.
-- =============================================================================
CREATE TABLE night_close (
    id                  BIGINT        NOT NULL AUTO_INCREMENT,
    tenant_id           BIGINT        NOT NULL DEFAULT 1,
    business_date       DATE          NOT NULL              COMMENT '마감 대상 영업일 (그날 밤 숙박분)',
    posted_night_count  INT           NOT NULL DEFAULT 0    COMMENT '이 마감이 실제로 게시한 숙박분 수',
    posted_amount       DECIMAL(14,2) NOT NULL DEFAULT 0    COMMENT '게시한 숙박분 요금 합계',
    closed_by           VARCHAR(50)   NULL                  COMMENT '마감 실행 주체 (배치=SYSTEM, 수동=직원)',
    closed_at           DATETIME      NOT NULL              COMMENT '마감이 완료된 시각',

    created_at          DATETIME      NOT NULL,
    updated_at          DATETIME      NOT NULL,

    PRIMARY KEY (id),

    -- ★ 멱등성 1차 방어선. 한 영업일은 한 번만 마감된다. 위 주석 참조.
    UNIQUE KEY uk_night_close (tenant_id, business_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '야간마감 이력. 영업일당 한 행. 마감 멱등성의 근거.';
