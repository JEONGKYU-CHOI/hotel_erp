-- =============================================================================
-- V6 : 결제 취소/환불 (payment cancel)  ★ 실제 환불 실행 · D-039 ★
-- =============================================================================
--   결제 한 건에 대해 얼마가 환불(취소)됐는지를 payment 행에 누적한다. 토스 부분취소를
--   지원하므로 "전액 취소"가 아니라 "취소 누적액"으로 기록한다:
--     · canceled_amount = 지금까지 이 결제에서 환불한 금액의 합
--     · 유효 결제액(폴리오가 대변으로 보는 값) = amount - canceled_amount
--     · canceled_amount == amount 가 되면 status 를 CANCELED 로 올린다(완전 취소)
--
--   폴리오(D-038)는 이 값을 빼서 잔액을 다시 낸다 — 환불하면 대변이 줄어 잔액이 0 으로
--   수렴한다. 환불 재실행(멱등)도 canceled_amount 로 막는다: 이미 환불된 만큼은 다시 취소하지
--   않는다.
--
--   NULL 아님·기본 0 — 기존 결제 행(환불 이력 없음)은 canceled_amount 0 으로 자연히 채워진다.
-- =============================================================================

ALTER TABLE payment
    ADD COLUMN canceled_amount DECIMAL(12,2) NOT NULL DEFAULT 0
        COMMENT '환불(취소) 누적액 (D-039). 유효 결제액 = amount - canceled_amount'
        AFTER amount,
    ADD COLUMN canceled_at DATETIME NULL
        COMMENT '마지막 환불 실행 시각 (D-039)'
        AFTER approved_at,
    ADD CONSTRAINT chk_payment_canceled CHECK (canceled_amount >= 0 AND canceled_amount <= amount);
