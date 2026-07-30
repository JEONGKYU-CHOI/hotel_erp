package io.github.jeongkyuchoi.hotel.erp.reservation.service;

import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.NightClose;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.NightCloseRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.ReservationNight;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.ReservationNightRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.ReservationStatus;
import io.github.jeongkyuchoi.hotel.erp.reservation.dto.NightCloseResult;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 야간마감 — 그날 밤 숙박분을 폴리오에 게시한다. 핵심 기술 과제 ③(마감 멱등성)의 본체다
 * (D-004, D-026).
 *
 * <p><b>무엇을 하나</b> — 영업일 하나를 받아, 그날 묵는 예약({@code CONFIRMED}·
 * {@code CHECKED_IN})의 <b>미게시</b> 숙박분을 {@code posted = true} 로 전환하고, 게시 집계를
 * {@link NightClose} 이력으로 남긴다.
 *
 * <p><b>왜 두 번 돌아도 안전한가 — 멱등성 두 겹</b>
 * <ol>
 *   <li><b>선조회 + 유니크 제약(영업일 단위).</b> 게시 전에 이 영업일이 이미 마감됐는지
 *       조회한다. 있으면 아무 것도 게시하지 않고 기존 이력을 그대로 돌려준다. 두 실행이
 *       동시에 선조회를 통과하는 드문 경합은 {@code uk_night_close} 유니크 제약이 막는다 —
 *       나중 INSERT 가 거부되며 그 트랜잭션의 게시(posted 전환)까지 함께 롤백된다.
 *   <li><b>{@code posted = false} 필터(숙박분 단위).</b> 게시 대상 조회가 미게시분만
 *       가져오므로, 설령 이력 없이 게시 로직만 두 번 돌아도 같은 숙박분이 두 번
 *       게시되지 않는다. {@link ReservationNight#post()} 의 가드가 이를 도메인에서 한 번 더
 *       못 박는다.
 * </ol>
 *
 * <p>Spring Batch 의 재실행 방지를 프레임워크에 위임하는 대신 이 멱등성을 직접 설계했다
 * (D-004). 마감은 하루 1회라 청크·재시작 같은 배치 인프라가 필요 없고, 재실행 방지야말로
 * 이 과제가 증명하려는 것이다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NightCloseService {

	private static final Long TENANT_ID = 1L;

	/** 숙박료를 게시할 예약 상태. 실제로 그날 묵는 예약만 게시 대상이다. */
	private static final List<ReservationStatus> POSTABLE_STATUSES =
			List.of(ReservationStatus.CONFIRMED, ReservationStatus.CHECKED_IN);

	private final NightCloseRepository nightCloseRepository;
	private final ReservationNightRepository reservationNightRepository;

	/**
	 * 영업일 하나를 마감한다. 재실행하면 게시 없이 {@code alreadyClosed} 결과를 돌려준다.
	 *
	 * @param businessDate 마감 대상 영업일
	 * @param closedBy     실행 주체 (스케줄러=SYSTEM, 수동=직원 식별자)
	 */
	@Transactional
	public NightCloseResult close(LocalDate businessDate, String closedBy) {
		// ── 멱등 1차: 선조회 ──────────────────────────────────────────────
		// 이미 마감된 영업일이면 게시하지 않는다. 재실행이 여기서 걸러진다.
		var existing = nightCloseRepository.findByTenantIdAndBusinessDate(TENANT_ID, businessDate);
		if (existing.isPresent()) {
			log.info("야간마감 재요청(멱등) — 이미 마감됨 date={} 기존 게시 {}건",
					businessDate, existing.get().getPostedNightCount());
			return NightCloseResult.alreadyClosed(existing.get());
		}

		// ── 게시: posted=false 인 숙박분만 (멱등 2차: 숙박분 단위) ────────────
		List<ReservationNight> nights = reservationNightRepository.findPostableNights(
				TENANT_ID, businessDate, POSTABLE_STATUSES);

		BigDecimal postedAmount = BigDecimal.ZERO;
		for (ReservationNight night : nights) {
			night.post(); // posted false → true. 이미 true 면 예외(조회 필터상 오지 않음).
			postedAmount = postedAmount.add(night.getRateAmount());
		}

		// ── 이력 기록. uk_night_close 가 영업일 단위 재마감을 최후에 막는다 ────
		NightClose closed = nightCloseRepository.save(NightClose.builder()
				.tenantId(TENANT_ID)
				.businessDate(businessDate)
				.postedNightCount(nights.size())
				.postedAmount(postedAmount)
				.closedBy(closedBy)
				.closedAt(LocalDateTime.now())
				.build());

		log.info("야간마감 완료: date={} 게시 {}건 합계={}",
				businessDate, closed.getPostedNightCount(), closed.getPostedAmount());
		return NightCloseResult.closed(closed);
	}
}
