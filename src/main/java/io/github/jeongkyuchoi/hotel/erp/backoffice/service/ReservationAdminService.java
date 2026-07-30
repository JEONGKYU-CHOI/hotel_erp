package io.github.jeongkyuchoi.hotel.erp.backoffice.service;

import io.github.jeongkyuchoi.hotel.erp.backoffice.dto.ReservationListRow;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.Reservation;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.ReservationRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.ReservationStatus;
import io.github.jeongkyuchoi.hotel.erp.common.exception.NotFoundException;
import io.github.jeongkyuchoi.hotel.erp.reservation.dto.ReservationDetail;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 백오피스 예약 조회 — 목록·상세(D-029).
 *
 * <p>고객 조회(비회원, {@code ReservationQueryService})와 나눈 이유 — 인증·조회 키가 다르다.
 * 고객은 예약번호+전화로 자기 예약 하나를 보고, 직원은 인증된 세션으로 전체를 목록·필터로
 * 본다. 관심사가 다르면 서비스도 나눈다.
 *
 * <p>둘 다 조립을 <b>읽기 전용 트랜잭션 안에서</b> 끝낸다 — OSIV 를 껐으므로(D 설정)
 * 지연로딩 연관은 트랜잭션 경계 밖에서 초기화할 수 없다.
 */
@Service
@RequiredArgsConstructor
public class ReservationAdminService {

	private static final Long TENANT_ID = 1L;

	private final ReservationRepository reservationRepository;

	/**
	 * 예약 목록. 상태·체크인 기간으로 선택 필터한다(전부 null 이면 전체).
	 */
	@Transactional(readOnly = true)
	public List<ReservationListRow> list(ReservationStatus status, LocalDate from, LocalDate to) {
		LocalDateTime now = LocalDateTime.now();
		return reservationRepository.findForAdminList(TENANT_ID, status, from, to).stream()
				.map(r -> ReservationListRow.from(r, now))
				.toList();
	}

	/**
	 * 예약 상세. 야간 스냅샷·연관을 이 트랜잭션 안에서 조립한다.
	 *
	 * @throws NotFoundException 없거나 다른 테넌트의 예약이면.
	 */
	@Transactional(readOnly = true)
	public ReservationDetail get(Long id) {
		Reservation r = reservationRepository.findById(id)
				.filter(x -> TENANT_ID.equals(x.getTenantId()))
				.orElseThrow(() -> new NotFoundException("예약을 찾을 수 없습니다. id=" + id));
		return ReservationDetail.from(r, LocalDateTime.now());
	}
}
