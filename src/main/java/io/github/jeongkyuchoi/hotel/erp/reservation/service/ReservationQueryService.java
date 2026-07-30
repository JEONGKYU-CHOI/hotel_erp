package io.github.jeongkyuchoi.hotel.erp.reservation.service;

import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.ReservationRepository;
import io.github.jeongkyuchoi.hotel.erp.common.exception.NotFoundException;
import io.github.jeongkyuchoi.hotel.erp.reservation.dto.MyReservationSummary;
import io.github.jeongkyuchoi.hotel.erp.reservation.dto.ReservationDetail;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 예약 조회 — 비회원 경로(예약번호 + 전화번호). 인증 없이 도는 읽기 경로다(D-028).
 *
 * <p><b>왜 예약번호만으로는 안 되나</b> — 예약번호는 고객에게 노출되는 값이라 그것만으로
 * 조회를 열면 번호를 아는 누구나 남의 예약을 본다. 전화번호를 함께 요구해 소유 증명을
 * 겸한다. 예약번호는 열거하기 어렵게 무작위 4자리를 포함한다(D-023).
 *
 * <p><b>미존재와 불일치를 구분하지 않는다.</b> 예약번호가 없든 전화번호가 틀렸든 같은
 * {@link NotFoundException} 을 던진다 — "그 예약번호는 존재하지만 전화가 틀렸다"를 흘리면
 * 예약번호 유효성을 확인해 주는 오라클이 된다. 조회 쿼리가 두 조건을 함께 걸어 이 구분이
 * 애초에 드러나지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationQueryService {

	private static final Long TENANT_ID = 1L;

	private final ReservationRepository reservationRepository;

	/**
	 * 예약번호 + 전화번호로 예약 상세를 조회한다.
	 *
	 * <p>읽기 전용 트랜잭션이다 — {@link ReservationDetail#from} 이 지연로딩 연관
	 * (roomType·ratePlan·nights)에 접근하므로 트랜잭션 경계 안에서 조립을 끝낸다.
	 *
	 * @throws NotFoundException 일치하는 예약이 없으면(미존재·전화 불일치 모두).
	 */
	@Transactional(readOnly = true)
	public ReservationDetail findForGuest(String reservationNo, String guestPhone) {
		return reservationRepository
				.findByTenantIdAndReservationNoAndGuestPhone(TENANT_ID, reservationNo, guestPhone)
				.map(r -> ReservationDetail.from(r, LocalDateTime.now()))
				.orElseThrow(() -> new NotFoundException("예약을 찾을 수 없습니다."));
	}

	/**
	 * 로그인 회원의 예약 목록(D-032). 인증된 회원 id 로만 조회하므로 소유 증명이 따로 필요 없다
	 * — 남의 예약이 섞일 수 없다. 읽기 전용 트랜잭션에서 요약으로 조립한다(지연로딩 접근).
	 */
	@Transactional(readOnly = true)
	public List<MyReservationSummary> listForMember(Long memberId) {
		LocalDateTime now = LocalDateTime.now();
		return reservationRepository.findForMember(TENANT_ID, memberId).stream()
				.map(r -> MyReservationSummary.from(r, now))
				.toList();
	}
}
