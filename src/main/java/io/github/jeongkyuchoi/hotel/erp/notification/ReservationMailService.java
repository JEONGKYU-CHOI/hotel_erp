package io.github.jeongkyuchoi.hotel.erp.notification;

import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.Reservation;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 예약 관련 이메일 조립·발송. 이벤트별(홀딩·확정·취소)로 제목·본문을 만들어
 * {@link EmailSender} 로 넘긴다. 실제 전송 수단은 프로파일이 정한다(데모=로그).
 *
 * <p>수신자 이메일이 없으면(비회원이 이메일을 비운 경우) 조용히 건너뛴다 — 이메일은 예약의
 * 필수값이 아니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationMailService {

	private static final String BRAND = "더 스테이";
	private static final DateTimeFormatter D = DateTimeFormatter.ofPattern("yyyy.MM.dd");

	private final EmailSender emailSender;

	/** 임시 예약(HOLD) 안내 — 결제 유도. */
	public void sendHeld(Reservation r) {
		if (skip(r)) {
			return;
		}
		String body = greeting(r)
				+ "임시 예약이 접수되었습니다. 아래 시각까지 결제하시면 예약이 확정됩니다.\n\n"
				+ summary(r)
				+ (r.getHoldExpiresAt() != null
						? "\n결제 기한 : " + r.getHoldExpiresAt().format(
								DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm"))
						: "")
				+ "\n\n예약 조회·결제는 홈페이지 '예약 조회'에서 하실 수 있습니다.";
		emailSender.send(new EmailMessage(r.getGuestEmail(),
				"[" + BRAND + "] 임시 예약이 접수되었습니다 (" + r.getReservationNo() + ")", body));
	}

	/** 예약 확정(결제 완료) 안내. */
	public void sendConfirmed(Reservation r) {
		if (skip(r)) {
			return;
		}
		String body = greeting(r)
				+ "예약이 확정되었습니다. 이용에 참고해 주세요.\n\n"
				+ summary(r)
				+ "\n결제 금액 : " + won(r.getTotalAmount()) + "원"
				+ "\n\n체크인은 15:00부터입니다. 즐거운 여정 되세요.";
		emailSender.send(new EmailMessage(r.getGuestEmail(),
				"[" + BRAND + "] 예약이 확정되었습니다 (" + r.getReservationNo() + ")", body));
	}

	/** 예약 취소·환불 안내. 위약금·환불액은 취소 판정값을 그대로 받는다(HOLD 는 환불 0). */
	public void sendCancelled(Reservation r, BigDecimal penalty, BigDecimal refund) {
		if (skip(r)) {
			return;
		}
		String refundLine = refund != null && refund.signum() > 0
				? "\n위약금 : " + won(penalty) + "원\n환불 금액 : " + won(refund) + "원 (결제하신 수단으로 환불 처리됩니다)"
				: "\n환불 금액은 없습니다.";
		String body = greeting(r)
				+ "예약이 취소되었습니다.\n\n"
				+ summary(r)
				+ refundLine
				+ "\n\n이용해 주셔서 감사합니다.";
		emailSender.send(new EmailMessage(r.getGuestEmail(),
				"[" + BRAND + "] 예약이 취소되었습니다 (" + r.getReservationNo() + ")", body));
	}

	// ---- 조립 헬퍼 --------------------------------------------------------

	private boolean skip(Reservation r) {
		if (r.getGuestEmail() == null || r.getGuestEmail().isBlank()) {
			log.debug("수신 이메일 없음 — 메일 생략. no={}", r.getReservationNo());
			return true;
		}
		return false;
	}

	private String greeting(Reservation r) {
		return r.getGuestName() + "님, 안녕하세요. " + BRAND + "입니다.\n\n";
	}

	private String summary(Reservation r) {
		long nights = ChronoUnit.DAYS.between(r.getCheckInDate(), r.getCheckOutDate());
		return "예약번호 : " + r.getReservationNo()
				+ "\n객실 : " + r.getRoomType().getName() + " · " + r.getRatePlan().getName()
				+ "\n일정 : " + r.getCheckInDate().format(D) + " ~ " + r.getCheckOutDate().format(D)
				+ " (" + nights + "박)";
	}

	private String won(BigDecimal v) {
		return String.format("%,d", v.longValue());
	}
}
