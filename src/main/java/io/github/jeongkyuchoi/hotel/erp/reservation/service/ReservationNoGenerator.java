package io.github.jeongkyuchoi.hotel.erp.reservation.service;

import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.ReservationRepository;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 고객에게 노출되는 예약번호 생성기.
 *
 * <p>형식: {@code R} + {@code yyMMdd} + 무작위 4자리 (예: {@code R2607301A2B}).
 *
 * <p><b>왜 DB 시퀀스나 AUTO_INCREMENT id 를 그대로 쓰지 않는가</b><br>
 * 예약번호가 연속된 정수면 남의 예약번호를 쉽게 추측할 수 있다. 비회원 조회가
 * {@code 예약번호 + 전화번호} 두 키라 번호 하나로 조회가 뚫리지는 않지만,
 * 추측 가능한 식별자는 그 자체로 열거 공격의 표면이 된다. 날짜 접두사로 사람이 읽기
 * 쉬우면서 뒤에 무작위를 붙여 추측을 어렵게 한다.
 *
 * <p>무작위라 충돌 가능성이 있어 유니크 제약({@code uk_reservation_no})과 재시도로 막는다.
 * 하루 발급량 대비 4자리(약 168만 조합)면 충돌은 드물고, 나더라도 재생성한다.
 */
@Component
@RequiredArgsConstructor
public class ReservationNoGenerator {

	private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyMMdd");
	// 헷갈리는 문자(0/O, 1/I) 를 뺀 집합. 고객이 전화로 불러줄 때 실수를 줄인다.
	private static final char[] ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
	private static final int RANDOM_LEN = 4;
	private static final int MAX_ATTEMPTS = 5;

	private final ReservationRepository reservationRepository;
	private final SecureRandom random = new SecureRandom();

	/** 충돌하지 않는 예약번호를 만든다. */
	public String generate() {
		for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
			String candidate = build();
			if (!reservationRepository.existsByReservationNo(candidate)) {
				return candidate;
			}
		}
		// 5번 연속 충돌은 사실상 일어나지 않는다. 일어났다면 조합 공간이 잘못됐거나
		// 무언가 근본적으로 이상한 것이므로 조용히 넘기지 않고 드러낸다.
		throw new IllegalStateException(
				"예약번호 생성 " + MAX_ATTEMPTS + "회 연속 충돌. 조합 공간을 확인하세요.");
	}

	private String build() {
		StringBuilder sb = new StringBuilder("R").append(LocalDate.now().format(DATE));
		for (int i = 0; i < RANDOM_LEN; i++) {
			sb.append(ALPHABET[random.nextInt(ALPHABET.length)]);
		}
		return sb.toString();
	}
}
