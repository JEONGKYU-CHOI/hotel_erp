-- =============================================================================
-- V8 : 환불 이벤트 원장 + 취소 행위자 (payment_cancel ledger)  ★ 감사 이력 · D-042 ★
-- =============================================================================
--   D-039 는 payment.canceled_amount 에 취소 "누적액"만 남겼다 — 잔액 계산엔 충분하나
--   "누가·언제·얼마·왜" 환불했는지 개별 이벤트가 없다. 부분환불을 여러 번 하면 총액에
--   뭉개져 이력이 사라진다. 그래서 환불 1건 = 1행의 append-only 원장을 신설한다.
--
--     · payment_cancel: 환불 실행마다 한 행. cancel_amount·reason·created_by(행위자)·
--       created_at(시각)을 보존한다. payment.canceled_amount 는 이 원장의 합과 일치한다
--       (누적 총액은 그대로 유지 — 폴리오·멱등이 그 값을 쓴다).
--     · created_by 는 JPA 감사(AuditorAware)가 로그인 직원명으로 채운다. 배치·비인증
--       경로는 SYSTEM 이다(JpaAuditingConfig).
--
--   취소(예약) 쪽은 예약에 cancelled_by 를 더해 "누가 취소했는지"를 못박는다. cancelled_at·
--   cancel_reason 과 짝이며, updated_by(마지막 수정자)와 달리 취소 시점에 굳어 덮이지 않는다.
-- =============================================================================

CREATE TABLE payment_cancel (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    tenant_id       BIGINT        NOT NULL DEFAULT 1,
    payment_id      BIGINT        NOT NULL              COMMENT '환불 대상 결제',
    reservation_id  BIGINT        NOT NULL              COMMENT '조회 편의용 비정규화(예약별 이력)',

    cancel_amount   DECIMAL(12,2) NOT NULL              COMMENT '이 이벤트에서 환불한 금액',
    reason          VARCHAR(200)  NULL                  COMMENT '환불 사유',

    created_at      DATETIME      NOT NULL,
    updated_at      DATETIME      NOT NULL,
    created_by      VARCHAR(50)   NULL                  COMMENT '환불 행위자(로그인 직원명 또는 SYSTEM)',
    updated_by      VARCHAR(50)   NULL,

    PRIMARY KEY (id),

    KEY idx_payment_cancel_payment (payment_id),
    KEY idx_payment_cancel_reservation (reservation_id),

    CONSTRAINT fk_payment_cancel__payment FOREIGN KEY (payment_id) REFERENCES payment (id),

    CONSTRAINT chk_payment_cancel_amount CHECK (cancel_amount > 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '환불(부분취소) 이벤트 원장. 환불 1건 = 1행, append-only. 누가·언제·얼마·왜 (D-042).';

-- 취소 행위자 — 예약에 못박는다. cancelled_at·cancel_reason 과 짝.
ALTER TABLE reservation
    ADD COLUMN cancelled_by VARCHAR(50) NULL
        COMMENT '취소 행위자(로그인 직원명 또는 SYSTEM) (D-042)'
        AFTER cancel_reason;
