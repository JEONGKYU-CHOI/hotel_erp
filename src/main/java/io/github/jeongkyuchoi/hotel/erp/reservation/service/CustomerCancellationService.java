package io.github.jeongkyuchoi.hotel.erp.reservation.service;

import io.github.jeongkyuchoi.hotel.erp.booking.dto.CancellationResult;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.CancellationCharge;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.Reservation;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.ReservationRepository;
import io.github.jeongkyuchoi.hotel.erp.common.exception.NotFoundException;
import io.github.jeongkyuchoi.hotel.erp.payment.service.RefundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 고객 대면 예약 취소 오케스트레이션.
 *
 * <p>백오피스는 취소({@link ReservationCancelService})와 환불({@link RefundService})을 두 번의
 * 요청으로 나눠 처리한다. 고객 화면에서는 버튼 하나로 끝나야 하므로, 여기서 <b>소유 확인 →
 * 취소 → (결제된 예약이면) 환불</b>을 한 흐름으로 묶는다.
 *
 * <ul>
 *   <li><b>소유 확인</b> — 회원은 인증된 id 로, 비회원은 예약번호+전화로만 자기 예약에 접근한다
 *       (조회 경로 D-028 과 같은 규율). 남의 예약은 {@link NotFoundException}.</li>
 *   <li><b>환불 여부는 취소 판정이 정한다</b> — {@link CancellationCharge.Basis} 가 UNPAID(HOLD)·
 *       SETTLED(이미 취소) 이거나 환불액이 0(NON_REFUNDABLE)이면 결제취소를 부르지 않는다.
 *       그 외(FREE·DEADLINE_PASSED)만 토스 결제취소를 실행한다. 취소가 실제로 무엇을 판정했는지에
 *       근거하므로, 소유 확인 시점의 상태를 신뢰하지 않아도 된다.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerCancellationService {

	private static final Long TENANT_ID = 1L;

	private final ReservationRepository reservationRepository;
	private final ReservationCancelService reservationCancelService;
	private final RefundService refundService;

	/** 비회원 취소 — 예약번호 + 전화로 소유를 확인한다. */
	public CancellationResult cancelForGuest(String reservationNo, String phone, String reason) {
		if (phone == null || phone.isBlank()) {
			throw new IllegalArgumentException("연락처를 입력하세요.");
		}
		Long id = resolveId(reservationRepository
				.findByTenantIdAndReservationNoAndGuestPhone(TENANT_ID, reservationNo, phone.trim()));
		return doCancel(id, reservationNo, reason);
	}

	/** 로그인 회원 취소 — 본인 예약만. */
	public CancellationResult cancelForMember(String reservationNo, Long memberId, String reason) {
		Long id = resolveId(reservationRepository
				.findByTenantIdAndReservationNoAndMember_Id(TENANT_ID, reservationNo, memberId));
		return doCancel(id, reservationNo, reason);
	}

	private Long resolveId(java.util.Optional<Reservation> found) {
		return found.map(Reservation::getId)
				.orElseThrow(() -> new NotFoundException("예약을 찾을 수 없습니다."));
	}

	/**
	 * 취소 + (필요 시) 환불. 취소·환불 서비스는 각자의 트랜잭션에서 돈다(백오피스 두 요청과 같은
	 * 순서). 취소가 먼저 커밋되고, 환불은 그 결과로 생긴 환불 대상 잔액을 본다.
	 */
	private CancellationResult doCancel(Long id, String reservationNo, String reason) {
		String r = (reason == null || reason.isBlank()) ? "고객 요청" : reason.trim();

		CancellationCharge charge = reservationCancelService.cancel(id, r);

		boolean refunded = false;
		CancellationCharge.Basis basis = charge.basis();
		boolean payableCancel = basis != CancellationCharge.Basis.UNPAID
				&& basis != CancellationCharge.Basis.SETTLED;
		if (payableCancel && charge.refund().signum() > 0) {
			refundService.refund(id, r); // 토스 결제취소 실행(멱등)
			refunded = true;
		}
		log.info("고객 취소 완료 — no={} 위약금={} 환불={} 실행={} 근거={}",
				reservationNo, charge.penalty(), charge.refund(), refunded, basis);
		return CancellationResult.of(reservationNo, charge, refunded);
	}
}
