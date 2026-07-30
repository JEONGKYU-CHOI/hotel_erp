package io.github.jeongkyuchoi.hotel.erp.reservation.service;

import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.Reservation;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.ReservationRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * NO_SHOW 판정 — 도착일에 투숙하지 않은 확정 예약을 NO_SHOW 로 전이한다(D-036).
 *
 * <p><b>야간마감(D-026) 뒤에 돈다.</b> 마감이 도착 첫날 숙박료를 이미 게시(노쇼 첫날 과금)한
 * 다음, 여전히 CONFIRMED 인 그날 도착분을 NO_SHOW 로 바꾼다. 순서를 뒤집으면 첫날 과금이
 * 누락된다(NO_SHOW 는 게시 대상 상태가 아니므로).
 *
 * <p><b>멱등.</b> 조회가 CONFIRMED 만 잡으므로, 재실행하면 이미 NO_SHOW 로 바뀐 건은 다시
 * 잡히지 않는다. 도메인 {@link Reservation#noShow()} 가드가 한 번 더 못 박는다.
 *
 * <p><b>재고는 건드리지 않는다.</b> 도착일이 지난 재고를 되돌려도 그 밤을 다시 팔 수 없다
 * (취소와 갈리는 지점, {@link Reservation#noShow()} 주석 참조).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NoShowService {

	private static final Long TENANT_ID = 1L;

	private final ReservationRepository reservationRepository;

	/**
	 * 주어진 도착일의 미투숙 확정 예약을 모두 NO_SHOW 로 전이하고, 처리 건수를 돌려준다.
	 *
	 * @param arrivalDate 판정 대상 도착일(정시 배치는 직전 영업일)
	 */
	@Transactional
	public int markNoShows(LocalDate arrivalDate) {
		List<Reservation> candidates =
				reservationRepository.findNoShowCandidates(TENANT_ID, arrivalDate);
		for (Reservation r : candidates) {
			r.noShow();
		}
		if (!candidates.isEmpty()) {
			log.info("NO_SHOW 판정: arrival={} {}건", arrivalDate, candidates.size());
		}
		return candidates.size();
	}
}
