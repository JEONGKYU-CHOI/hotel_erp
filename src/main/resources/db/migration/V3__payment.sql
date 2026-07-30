-- =============================================================================
-- V3 : 결제 (payment)  ★ 확정 트리거 · 결제 멱등성 ★
-- =============================================================================
--   결제 성공이 HOLD → CONFIRMED 확정의 유일한 트리거다(D-010, D-023, D-030 §2).
--   토스페이먼츠 표준 흐름에서 서버가 맡는 부분:
--     프론트 결제창 → 사용자 결제 → successUrl 리다이렉트(paymentKey, orderId, amount)
--     → 서버가 승인 API(POST /v1/payments/confirm) 호출 → 성공 시 이 행을 남기고 예약 확정.
--
--   ── 멱등성을 두 겹으로 막는다 (야간마감 V2 와 같은 규율) ──────────────────
--   ① 이 테이블의 UNIQUE (payment_key)
--      payment_key 는 토스가 결제 건마다 유일하게 발급한다. 결제 웹훅·승인 콜백은
--      재시도된다 — 같은 payment_key 로 이 행을 두 번 만들려 하면 DB 가 1062 로 거부한다.
--      애플리케이션의 선조회가 경합에 밀려도 DB 가 최후에 이중 확정을 막는다.
--   ② reservation.status (V1 에 이미 있음) + ReservationConfirmService 의 멱등
--      확정 서비스는 이미 CONFIRMED 면 재고를 다시 옮기지 않는다(D-030). 결제 행이
--      두 번 시도돼도 재고 이동은 정확히 한 번이다. 두 방어선은 다른 층을 막는다 —
--      ①은 "결제 건 단위 재기록", ②는 "예약 단위 재확정".
--
--   ── 금액 위변조 검증의 근거값 ────────────────────────────────────────────
--   amount 는 승인 시점의 결제 금액 스냅샷이다. 서버는 이 값을 프론트가 보낸 금액이 아니라
--   예약의 total_amount(V1) 와 대조하고, 토스 승인 응답의 금액과도 대조한다. 셋이 모두
--   같아야 확정한다 — "결제창 금액 조작"과 "응답 위조"를 각각 막는다.
--
--   ── order_id 를 예약번호로 둔다 ──────────────────────────────────────────
--   토스에 넘기는 orderId 는 우리 예약번호(reservation_no)를 그대로 쓴다. 예약 1건당
--   성공 결제 1건이라 UNIQUE (tenant_id, order_id) 로 못 박는다. 환불/부분결제는 후속 범위.
--
--   ── 실패 결제는 행으로 남기지 않는다 ─────────────────────────────────────
--   1차 범위에서 payment 행은 승인 성공분만 남긴다. 승인 실패는 예외로 드러내고 예약을
--   확정하지 않는다(HOLD 유지 → 만료 스케줄러가 회수). 실패 이력 적재는 후속 범위.
-- =============================================================================
CREATE TABLE payment (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    tenant_id       BIGINT        NOT NULL DEFAULT 1,
    reservation_id  BIGINT        NOT NULL              COMMENT '확정 대상 예약',

    order_id        VARCHAR(64)   NOT NULL              COMMENT '토스 orderId. 우리 예약번호(reservation_no)',
    payment_key     VARCHAR(200)  NOT NULL              COMMENT '토스가 결제 건마다 발급하는 유일 키. 멱등 기준',
    amount          DECIMAL(12,2) NOT NULL              COMMENT '승인 금액 스냅샷. 예약 total_amount 와 대조',
    status          VARCHAR(20)   NOT NULL              COMMENT 'APPROVED | CANCELED',
    method          VARCHAR(30)   NULL                  COMMENT '결제수단(카드 등). 토스 응답 값',
    approved_at     DATETIME      NULL                  COMMENT '토스 승인 시각',

    created_at      DATETIME      NOT NULL,
    updated_at      DATETIME      NOT NULL,

    PRIMARY KEY (id),

    -- ★ 멱등성 1차 방어선. 결제 건당 한 행. 위 주석 참조.
    UNIQUE KEY uk_payment_key (payment_key),
    -- 예약 1건당 성공 결제 1건.
    UNIQUE KEY uk_payment_order (tenant_id, order_id),

    KEY idx_payment_reservation (reservation_id),

    CONSTRAINT fk_payment__reservation FOREIGN KEY (reservation_id) REFERENCES reservation (id),

    CONSTRAINT chk_payment_status CHECK (status IN ('APPROVED', 'CANCELED')),
    CONSTRAINT chk_payment_amount CHECK (amount >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '결제. 예약당 성공 결제 한 행. 확정 트리거이자 결제 멱등성의 근거.';
