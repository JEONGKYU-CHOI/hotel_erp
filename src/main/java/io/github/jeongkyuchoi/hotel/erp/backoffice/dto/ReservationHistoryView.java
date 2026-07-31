package io.github.jeongkyuchoi.hotel.erp.backoffice.dto;

import io.github.jeongkyuchoi.hotel.erp.common.domain.payment.PaymentCancel;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.Reservation;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 백오피스 예약 상세의 환불·취소 이력(D-042 후속). "누가·언제·얼마·왜"를 시각 순으로 한 표에
 * 모아 보여준다 — 취소 한 건(예약에 굳은 위약금 스냅샷)과 환불 이벤트들(payment_cancel 원장)을
 * 합친다.
 *
 * <p><b>백오피스 전용이다.</b> 행위자({@code cancelledBy}·{@code createdBy})는 직원 정보라
 * 비회원 조회({@code ReservationDetail}, D-028)에는 싣지 않는다. 그래서 공용 상세 뷰가 아니라
 * 여기 별도 뷰로 조립한다.
 */
public record ReservationHistoryView(List<Entry> entries) {

	/**
	 * 이력 한 줄.
	 *
	 * @param type   구분 — {@code 취소} 또는 {@code 환불}
	 * @param amount 취소는 위약금 스냅샷, 환불은 돌려준 금액
	 * @param reason 사유(없을 수 있음)
	 * @param actor  처리자(로그인 직원명 또는 배치·비인증의 SYSTEM)
	 * @param at     발생 시각
	 */
	public record Entry(String type, BigDecimal amount, String reason, String actor,
			LocalDateTime at) {
	}

	public boolean isEmpty() {
		return entries.isEmpty();
	}

	/**
	 * 예약(취소 스냅샷)과 환불 원장을 합쳐 시각 순으로 조립한다. 취소된 예약이면 취소 한 줄이
	 * 앞서고, 환불 실행마다 한 줄이 뒤따른다(보통 취소 → 환불 순).
	 */
	public static ReservationHistoryView of(Reservation r, List<PaymentCancel> refunds) {
		List<Entry> entries = new ArrayList<>();
		if (r.getCancelledAt() != null) {
			entries.add(new Entry("취소", r.getCancellationFee(), r.getCancelReason(),
					r.getCancelledBy(), r.getCancelledAt()));
		}
		for (PaymentCancel pc : refunds) {
			entries.add(new Entry("환불", pc.getCancelAmount(), pc.getReason(),
					pc.getCreatedBy(), pc.getCreatedAt()));
		}
		entries.sort(Comparator.comparing(Entry::at));
		return new ReservationHistoryView(entries);
	}
}
