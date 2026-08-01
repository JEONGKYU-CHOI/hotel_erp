package io.github.jeongkyuchoi.hotel.erp.reservation.service;

import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RateCalendar;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RateCalendarRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RatePlan;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RatePlanRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RoomType;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RoomTypeRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.inventory.RoomInventory;
import io.github.jeongkyuchoi.hotel.erp.common.domain.inventory.RoomInventoryRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.member.Member;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.Reservation;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.ReservationNight;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.ReservationRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.ReservationStatus;
import io.github.jeongkyuchoi.hotel.erp.common.exception.NotEnoughInventoryException;
import io.github.jeongkyuchoi.hotel.erp.common.exception.NotFoundException;
import io.github.jeongkyuchoi.hotel.erp.reservation.dto.ReservationHoldCommand;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 예약 임시점유(HOLD) 생성. 이 프로젝트의 핵심 동시성 경로다.
 *
 * <p><b>이 서비스가 지키는 단 하나의 규칙 — 재고를 읽는 첫 쿼리가 락 쿼리다(D-018).</b><br>
 * "먼저 가용 재고를 조회해 보여주고, 확정 단계에서 락을 잡는다"를 <b>한 트랜잭션 안에서</b>
 * 하지 않는다. 그렇게 하면 락을 잡고도 REPEATABLE READ 스냅샷의 옛 값을 읽어, 락을
 * 걸었는데도 오버부킹이 난다. Day 0 스파이크에서 실측한 사실이다. 그래서 이 트랜잭션은
 * {@link RoomInventoryRepository#lockForUpdateNative}로 재고를 처음 만나고, 화면 표시용
 * 조회({@code findRange} 계열)는 절대 부르지 않는다.
 *
 * <p>흐름: 멱등 확인 → 기준정보 로드 → <b>재고 락</b> → 야간별 hold + 요금 스냅샷 →
 * HOLD 예약 저장. 오버부킹은 세 겹으로 막힌다 — 도메인 {@link RoomInventory#hold},
 * DB {@code CHECK}, 그리고 이 전체를 감싸는 행 락.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReservationService {

	private static final Long TENANT_ID = 1L;

	/** HOLD 유효시간. 이 안에 결제하지 않으면 스케줄러가 EXPIRED 로 돌리고 재고를 반환한다. */
	private static final Duration HOLD_TTL = Duration.ofMinutes(10);

	private final ReservationRepository reservationRepository;
	private final RoomInventoryRepository roomInventoryRepository;
	private final RoomTypeRepository roomTypeRepository;
	private final RatePlanRepository ratePlanRepository;
	private final RateCalendarRepository rateCalendarRepository;
	private final ReservationNoGenerator reservationNoGenerator;
	private final EntityManager entityManager;

	/**
	 * HOLD 예약을 만든다. 재고의 {@code heldQty} 를 야간마다 1씩 올린다.
	 *
	 * @throws NotEnoughInventoryException 어느 하루라도 재고가 부족하면. 트랜잭션 전체가
	 *         롤백되어 이미 올린 다른 날짜의 {@code heldQty} 도 함께 되돌아간다 —
	 *         연박은 전부 되거나 전부 안 되거나여야 한다.
	 */
	@Transactional
	public Reservation hold(ReservationHoldCommand cmd) {
		// 1) 멱등성 — 같은 키의 예약이 이미 있으면 그것을 그대로 돌려준다.
		//    더블클릭·네트워크 재시도로 재고를 두 번 잡는 것을 막는다.
		var existing = reservationRepository
				.findByTenantIdAndIdempotencyKey(TENANT_ID, cmd.idempotencyKey());
		if (existing.isPresent()) {
			log.info("멱등 재요청 — 기존 예약 반환: key={} no={}",
					cmd.idempotencyKey(), existing.get().getReservationNo());
			return existing.get();
		}

		// 2) 날짜 검증. 최소 1박이어야 한다.
		int nights = cmd.nights();
		if (nights < 1) {
			throw new IllegalArgumentException(
					"체크아웃은 체크인 다음날 이후여야 합니다. in=" + cmd.checkInDate()
							+ " out=" + cmd.checkOutDate());
		}
		LocalDate firstNight = cmd.checkInDate();
		LocalDate lastNight = cmd.checkOutDate().minusDays(1); // 체크아웃 당일은 숙박 아님

		// 3) 기준정보 로드 + 정합성 검증
		RoomType roomType = roomTypeRepository
				.findByIdAndTenantId(cmd.roomTypeId(), TENANT_ID)
				.orElseThrow(() -> new NotFoundException(
						"객실타입을 찾을 수 없습니다. id=" + cmd.roomTypeId()));
		RatePlan ratePlan = ratePlanRepository
				.findByIdAndTenantId(cmd.ratePlanId(), TENANT_ID)
				.orElseThrow(() -> new NotFoundException(
						"요금정책을 찾을 수 없습니다. id=" + cmd.ratePlanId()));
		if (!ratePlan.getRoomType().getId().equals(roomType.getId())) {
			throw new IllegalArgumentException(
					"요금정책이 이 객실타입에 속하지 않습니다. ratePlan=" + ratePlan.getId()
							+ " roomType=" + roomType.getId());
		}
		if (!roomType.isActive() || !ratePlan.isActive()) {
			throw new NotEnoughInventoryException("판매 중지된 객실타입/요금정책입니다.");
		}

		// 3-1) 정원 검증 — 성인+아동이 객실 최대 수용인원을 넘으면 예약을 막는다.
		//      웹 DTO 의 @Min 은 하한(성인 1↑·아동 0↑)만 본다. 상한은 객실타입마다 다르므로
		//      기준정보를 로드한 여기서 검증하는 것이 맞다(백오피스·API 어느 입구든 동일 방어).
		int guests = cmd.adults() + cmd.children();
		if (guests > roomType.getMaxOccupancy()) {
			throw new IllegalArgumentException(
					"최대 수용 인원을 초과했습니다. 최대 " + roomType.getMaxOccupancy()
							+ "인 · 요청 " + guests + "인(성인 " + cmd.adults()
							+ " 아동 " + cmd.children() + ")");
		}

		// 4) ★ 재고 락 — 이 트랜잭션에서 재고를 처음 만나는 쿼리다(D-018).
		//    stay_date 오름차순으로 잠근다. 순서를 보장하는 것은 ORDER BY 가 아니라
		//    (room_type_id, stay_date) 선두 인덱스다(D-015). 연박 간 데드락을 막는다.
		Map<LocalDate, RoomInventory> inventoryByDate = new HashMap<>();
		for (RoomInventory inv : roomInventoryRepository
				.lockForUpdateNative(roomType.getId(), firstNight, lastNight)) {
			inventoryByDate.put(inv.getStayDate(), inv);
		}

		// 5) 요금표 조회. 재고와 다른 테이블이라 락 규칙(D-018)과 무관하다 —
		//    예약이 요금표를 건드리지 않으므로 일반 조회로 안전하다.
		Map<LocalDate, RateCalendar> rateByDate = new HashMap<>();
		for (RateCalendar rc : rateCalendarRepository
				.findByTenantIdAndRatePlanIdAndStayDateBetweenOrderByStayDate(
						TENANT_ID, ratePlan.getId(), firstNight, lastNight)) {
			rateByDate.put(rc.getStayDate(), rc);
		}

		// 6) 야간별로 hold + 요금 스냅샷을 쌓는다.
		Reservation reservation = Reservation.builder()
				.tenantId(TENANT_ID)
				.reservationNo(reservationNoGenerator.generate())
				.member(resolveMember(cmd.memberId()))
				.guestName(cmd.guestName())
				.guestPhone(cmd.guestPhone())
				.guestEmail(cmd.guestEmail())
				.roomType(roomType)
				.ratePlan(ratePlan)
				.checkInDate(cmd.checkInDate())
				.checkOutDate(cmd.checkOutDate())
				.adults(cmd.adults())
				.children(cmd.children())
				.status(ReservationStatus.HOLD)
				.totalAmount(BigDecimal.ZERO) // 아래에서 합산 후 확정
				.holdExpiresAt(LocalDateTime.now().plus(HOLD_TTL))
				.idempotencyKey(cmd.idempotencyKey())
				.build();

		for (LocalDate date = firstNight; !date.isAfter(lastNight); date = date.plusDays(1)) {
			RoomInventory inv = inventoryByDate.get(date);
			if (inv == null) {
				// 재고 행이 없으면 팔 수 없다. 재고 생성이 먼저다.
				throw new NotEnoughInventoryException(
						date + " 재고가 생성되지 않았습니다. 먼저 재고를 만들어야 예약을 받습니다.");
			}
			BigDecimal amount = resolveRate(date, rateByDate.get(date), ratePlan);

			inv.hold(1); // 도메인 1차 방어 — 가용 부족이면 여기서 예외
			reservation.addNight(ReservationNight.builder()
					.stayDate(date)
					.roomTypeId(roomType.getId())
					.ratePlanId(ratePlan.getId())
					.rateAmount(amount)
					.posted(false)
					.build());
		}
		reservation.recalculateTotalAmount();

		Reservation saved = reservationRepository.save(reservation);
		log.info("HOLD 생성: no={} roomType={} {}~{} {}박 총액={} 만료={}",
				saved.getReservationNo(), roomType.getCode(), cmd.checkInDate(),
				cmd.checkOutDate(), nights, saved.getTotalAmount(), saved.getHoldExpiresAt());
		return saved;
	}

	/**
	 * 해당 야간의 판매가를 정한다.
	 *
	 * <p>{@code rate_calendar} 행이 있으면 그 금액, 없으면 요금정책의 기본 요금으로 폴백한다.
	 * 행이 {@code closed} 면 그 날짜는 판매 중지다.
	 */
	private BigDecimal resolveRate(LocalDate date, RateCalendar rc, RatePlan ratePlan) {
		if (rc == null) {
			return ratePlan.getBaseAmount();
		}
		if (rc.isClosed()) {
			throw new NotEnoughInventoryException(date + " 은(는) 판매 중지된 날짜입니다.");
		}
		return rc.getAmount();
	}

	/**
	 * 회원 연결. 비회원(null)이 기본 경로다.
	 *
	 * <p>{@code getReference} 는 실제 조회 없이 프록시를 준다. 존재 검증은 저장 시 FK 로
	 * 이뤄진다. 회원 로그인(D-009)이 붙기 전이라 현재 호출자는 항상 null 을 넘긴다.
	 */
	private Member resolveMember(Long memberId) {
		return memberId == null ? null : entityManager.getReference(Member.class, memberId);
	}
}
