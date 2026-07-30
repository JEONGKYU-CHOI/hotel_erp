package io.github.jeongkyuchoi.hotel.erp.common.domain.payment;

/**
 * 결제 상태.
 *
 * <p>1차 범위는 승인 성공분만 다룬다 — 승인 실패는 행으로 남기지 않고 예외로 드러낸다
 * (스키마 {@code V3__payment.sql} 주석 참조). {@code CANCELED} 는 후속 환불 범위의 자리다.
 */
public enum PaymentStatus {
	/** 토스 승인 성공. 예약이 이 결제로 확정됐다. */
	APPROVED,
	/** 승인 후 취소(환불). 후속 범위. */
	CANCELED
}
