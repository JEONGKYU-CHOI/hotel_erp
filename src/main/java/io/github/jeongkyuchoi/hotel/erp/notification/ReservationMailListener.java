package io.github.jeongkyuchoi.hotel.erp.notification;

import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.Reservation;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.ReservationRepository;
import io.github.jeongkyuchoi.hotel.erp.notification.event.ReservationCancelledEvent;
import io.github.jeongkyuchoi.hotel.erp.notification.event.ReservationConfirmedEvent;
import io.github.jeongkyuchoi.hotel.erp.notification.event.ReservationHeldEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 예약 이벤트 → 이메일. <b>커밋 후에만</b>({@link TransactionalEventListener} 의 기본 phase =
 * AFTER_COMMIT) 처리해, 롤백된 예약엔 메일이 나가지 않게 한다. 메일 전송 실패가 예약
 * 트랜잭션을 되돌리는 일도 없다(이미 커밋됨).
 *
 * <p>커밋 후라 원래 트랜잭션/영속성 컨텍스트는 닫혀 있다. 그래서 각 리스너는
 * {@code REQUIRES_NEW} 읽기 트랜잭션을 새로 열어 예약을 다시 로드하고(지연연관 접근),
 * 메일을 조립한다. 메일 조립·전송 중 예외는 여기서 삼킨다 — 알림 실패가 본 흐름의 성공을
 * 뒤엎지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservationMailListener {

	private final ReservationRepository reservationRepository;
	private final ReservationMailService mailService;

	@TransactionalEventListener
	@Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
	public void onHeld(ReservationHeldEvent e) {
		load(e.reservationId()).ifPresent(safe(mailService::sendHeld, e.reservationId()));
	}

	@TransactionalEventListener
	@Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
	public void onConfirmed(ReservationConfirmedEvent e) {
		load(e.reservationId()).ifPresent(safe(mailService::sendConfirmed, e.reservationId()));
	}

	@TransactionalEventListener
	@Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
	public void onCancelled(ReservationCancelledEvent e) {
		load(e.reservationId()).ifPresent(safe(
				r -> mailService.sendCancelled(r, e.penalty(), e.refund()), e.reservationId()));
	}

	private java.util.Optional<Reservation> load(Long id) {
		return reservationRepository.findById(id);
	}

	/** 메일 전송 예외를 삼켜 알림 실패가 조용히 지나가게 한다(본 흐름은 이미 커밋됨). */
	private java.util.function.Consumer<Reservation> safe(
			java.util.function.Consumer<Reservation> action, Long id) {
		return r -> {
			try {
				action.accept(r);
			} catch (RuntimeException ex) {
				log.warn("예약 알림 메일 실패 — reservationId={} : {}", id, ex.toString());
			}
		};
	}
}
