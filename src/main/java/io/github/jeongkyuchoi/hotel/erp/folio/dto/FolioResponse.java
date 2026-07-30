package io.github.jeongkyuchoi.hotel.erp.folio.dto;

import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.ReservationStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 예약 한 건의 청구서(폴리오) — 읽기 시점에 조립한 값이다(D-038, 조립형).
 *
 * <p>흩어진 청구(숙박료 또는 위약금)와 결제를 한 장으로 모아 잔액을 낸다. 저장하지 않는다 —
 * 원본은 {@code reservation_night}·{@code reservation.*_fee}·{@code payment} 에 있고, 이 DTO 는
 * 그것을 그때그때 합산한 뷰다. 그래서 이중 저장 정합 문제가 없다.
 *
 * <p>불변식: {@code chargeTotal - creditTotal == balance}. 청구액은 예약 상태가 정한다 —
 * 진행/완료는 숙박료 합, 취소·노쇼는 위약금 스냅샷(D-037), 만료는 0.
 *
 * @param charges 청구 라인(숙박료 일자별 또는 위약금)
 * @param credits 결제 라인(승인 결제)
 */
public record FolioResponse(
		String reservationNo,
		ReservationStatus status,
		List<Charge> charges,
		List<Credit> credits,
		BigDecimal chargeTotal,
		BigDecimal creditTotal,
		BigDecimal balance,
		Settlement settlement) {

	/** 청구 한 줄. {@code date} 는 숙박료면 숙박일, 위약금이면 null. */
	public record Charge(LocalDate date, String label, BigDecimal amount) {
	}

	/** 결제 한 줄(원장에서는 대변/credit). */
	public record Credit(LocalDateTime paidAt, String method, BigDecimal amount) {
	}

	/** 정산 상태 — 잔액 부호가 결정한다. */
	public enum Settlement {
		/** 잔액 0 — 완납. */
		PAID,
		/** 잔액 &gt; 0 — 미수(더 받을 것). */
		OUTSTANDING,
		/** 잔액 &lt; 0 — 환불 대상(돌려줄 것). */
		REFUND_DUE
	}
}
