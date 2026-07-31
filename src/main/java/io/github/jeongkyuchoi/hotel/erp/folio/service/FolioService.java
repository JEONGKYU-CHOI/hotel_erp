package io.github.jeongkyuchoi.hotel.erp.folio.service;

import io.github.jeongkyuchoi.hotel.erp.common.domain.payment.Payment;
import io.github.jeongkyuchoi.hotel.erp.common.domain.payment.PaymentRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.Reservation;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.ReservationRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.ReservationStatus;
import io.github.jeongkyuchoi.hotel.erp.common.exception.NotFoundException;
import io.github.jeongkyuchoi.hotel.erp.folio.dto.FolioResponse;
import io.github.jeongkyuchoi.hotel.erp.folio.dto.FolioResponse.Charge;
import io.github.jeongkyuchoi.hotel.erp.folio.dto.FolioResponse.Credit;
import io.github.jeongkyuchoi.hotel.erp.folio.dto.FolioResponse.Settlement;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 폴리오(청구서) 조립 — 예약·야간·결제를 읽어 청구/결제/잔액을 낸다(D-038, 조립형).
 *
 * <p><b>저장하지 않는다.</b> 새 원장 테이블 없이 기존 소스를 읽어 {@link FolioResponse} 로
 * 조립한다. 금액이 두 곳에 살지 않으므로(단일 진실원천) 정합 문제가 없다. 부작용이 없어
 * 읽기 전용 트랜잭션에서 지연로딩 연관에 접근해 조립을 끝낸다.
 *
 * <p><b>청구액은 상태가 정한다</b> — 진행/완료(HOLD·CONFIRMED·CHECKED_IN·CHECKED_OUT)는
 * 숙박료 합, 취소는 {@code cancellation_fee}, 노쇼는 {@code no_show_fee}(D-037), 만료는 0.
 * 취소·노쇼가 total 이 아니라 위약금인 것이 이 조립의 핵심이다 — "정책상 받을 금액"을 청구로
 * 본다. 결제는 승인(APPROVED) 결제의 합이다.
 */
@Service
@RequiredArgsConstructor
public class FolioService {

	private static final Long TENANT_ID = 1L;

	private final ReservationRepository reservationRepository;
	private final PaymentRepository paymentRepository;

	/** 백오피스용 — 예약 id 로 청구서를 조립한다(소유 검증 없음, 직원 권한은 시큐리티가 판단). */
	@Transactional(readOnly = true)
	public FolioResponse forAdmin(Long reservationId) {
		Reservation reservation = reservationRepository.findById(reservationId)
				.orElseThrow(() -> new NotFoundException("예약을 찾을 수 없습니다. id=" + reservationId));
		return assemble(reservation);
	}

	/**
	 * 고객용 — 로그인 회원이 자기 예약의 청구서만 본다(D-032 규율).
	 *
	 * <p>미존재와 소유 불일치를 구분하지 않고 같은 {@link NotFoundException} 을 던진다 —
	 * 예약번호 유효성을 확인해 주는 오라클이 되지 않게 한다(D-028 과 같은 규율).
	 */
	@Transactional(readOnly = true)
	public FolioResponse forMember(Long memberId, String reservationNo) {
		Reservation reservation = reservationRepository
				.findByTenantIdAndReservationNo(TENANT_ID, reservationNo)
				.filter(r -> r.getMember() != null && r.getMember().getId().equals(memberId))
				.orElseThrow(() -> new NotFoundException("예약을 찾을 수 없습니다."));
		return assemble(reservation);
	}

	private FolioResponse assemble(Reservation reservation) {
		// 표시 상태 — 만료 시각이 지난 HOLD 는 스케줄러가 아직 청소 전이어도 EXPIRED 로 본다.
		// 회원 목록·비회원 조회와 같은 규율을 도메인 메서드로 모았다(D-043) — 청구서가 "만료됨"을
		// 목록과 달리 보고 미수로 잡는 어긋남을 원천 차단한다.
		ReservationStatus displayStatus = reservation.displayStatus(LocalDateTime.now());

		List<Charge> charges = chargesFor(reservation, displayStatus);
		List<Credit> credits = creditsFor(reservation);

		BigDecimal chargeTotal = sum(charges, Charge::amount);
		BigDecimal creditTotal = sum(credits, Credit::amount);
		BigDecimal balance = chargeTotal.subtract(creditTotal);

		return new FolioResponse(
				reservation.getReservationNo(), displayStatus,
				charges, credits, chargeTotal, creditTotal, balance, settlementOf(balance));
	}

	/** 상태별 청구 라인. 진행/완료는 숙박료 일자별, 취소·노쇼는 위약금 한 줄, 만료는 없음. */
	private List<Charge> chargesFor(Reservation reservation, ReservationStatus status) {
		return switch (status) {
			case HOLD, CONFIRMED, CHECKED_IN, CHECKED_OUT -> reservation.getNights().stream()
					.map(n -> new Charge(n.getStayDate(), "숙박료", n.getRateAmount()))
					.toList();
			case CANCELLED -> penaltyLine("취소 위약금", reservation.getCancellationFee());
			case NO_SHOW -> penaltyLine("노쇼 위약금", reservation.getNoShowFee());
			case EXPIRED -> List.of();
		};
	}

	/** 위약금 한 줄. 금액이 없거나(과거 데이터) 0 이면 라인을 만들지 않는다. */
	private List<Charge> penaltyLine(String label, BigDecimal fee) {
		if (fee == null || fee.signum() == 0) {
			return List.of();
		}
		return List.of(new Charge(null, label, fee));
	}

	/**
	 * 결제 대변(credit) 라인. 환불(부분취소)을 반영해 <b>유효 결제액</b>(amount − canceled)을
	 * 올린다(D-039). 전액 환불된 결제(유효액 0)는 원장에서 빠진다 — 환불하면 대변이 줄어
	 * 잔액이 0 으로 수렴한다.
	 */
	private List<Credit> creditsFor(Reservation reservation) {
		List<Credit> credits = new ArrayList<>();
		for (Payment p : paymentRepository.findByReservationIdOrderByApprovedAt(reservation.getId())) {
			BigDecimal effective = p.effectiveAmount();
			if (effective.signum() > 0) {
				credits.add(new Credit(p.getApprovedAt(), p.getMethod(), effective));
			}
		}
		return credits;
	}

	private Settlement settlementOf(BigDecimal balance) {
		int sign = balance.signum();
		if (sign == 0) {
			return Settlement.PAID;
		}
		return sign > 0 ? Settlement.OUTSTANDING : Settlement.REFUND_DUE;
	}

	private <T> BigDecimal sum(List<T> lines, java.util.function.Function<T, BigDecimal> amount) {
		return lines.stream().map(amount).reduce(BigDecimal.ZERO, BigDecimal::add);
	}
}
