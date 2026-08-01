-- =============================================================================
-- 예약 정책 — 테넌트별 단일 행 설정. 지금은 '당일 예약 마감 시각' 하나를 담는다.
-- 부킹엔진이 이 값을 읽어(체크인=오늘 & 현재시각>마감 → 거절), PMS 에서 값을 바꾼다.
-- 설정 항목이 늘면 이 테이블에 컬럼을 추가한다.
-- =============================================================================
CREATE TABLE booking_policy (
    id                   BIGINT      NOT NULL AUTO_INCREMENT,
    tenant_id            BIGINT      NOT NULL DEFAULT 1,
    same_day_cutoff_time TIME        NOT NULL DEFAULT '20:00:00' COMMENT '당일 예약 마감 시각',

    created_at           DATETIME    NOT NULL,
    updated_at           DATETIME    NOT NULL,
    created_by           VARCHAR(50) NULL,
    updated_by           VARCHAR(50) NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uk_booking_policy_tenant (tenant_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '예약 정책 (테넌트별 단일 행)';

-- 기본 정책 시드 — 당일 마감 20:00.
INSERT INTO booking_policy (tenant_id, same_day_cutoff_time, created_at, updated_at)
VALUES (1, '20:00:00', NOW(), NOW());
