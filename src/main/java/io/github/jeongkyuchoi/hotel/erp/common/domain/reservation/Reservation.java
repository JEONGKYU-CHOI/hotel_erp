package io.github.jeongkyuchoi.hotel.erp.common.domain.reservation;

import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RatePlan;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.Room;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RoomType;
import io.github.jeongkyuchoi.hotel.erp.common.domain.member.Member;
import io.github.jeongkyuchoi.hotel.erp.common.domain.support.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 예약. {@link ReservationStatus#HOLD} 상태가 임시점유를 겸한다(D-003).
 *
 * <p><b>숙박일수 컬럼이 없는 이유</b> — {@code checkOutDate - checkInDate} 로 계산되는
 * 파생값이다. 컬럼으로 두면 두 날짜와 어긋날 여지가 생긴다. {@link #nights()} 로 계산한다.
 *
 * <p><b>호실을 예약 시점에 확정하지 않는 이유</b> — 타입 재고만 차감하고 {@link #room} 은
 * null 로 둔다. 체크인 때 비어 있고 청소가 끝난 호실을 배정한다.
 */
@Entity
@Table(name = "reservation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "tenant_id", nullable = false)
	private Long tenantId;

	/** 고객에게 노출되는 예약번호. 비회원 조회의 한쪽 키다. */
	@Column(name = "reservation_no", nullable = false, length = 20)
	private String reservationNo;

	/** null 이면 비회원 예약이다. 비회원 예약이 기본 경로다. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id")
	private Member member;

	@Column(name = "guest_name", nullable = false, length = 50)
	private String guestName;

	/** 비회원 예약 조회의 나머지 한쪽 키다. */
	@Column(name = "guest_phone", nullable = false, length = 20)
	private String guestPhone;

	@Column(name = "guest_email", length = 255)
	private String guestEmail;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "room_type_id", nullable = false)
	private RoomType roomType;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "rate_plan_id", nullable = false)
	private RatePlan ratePlan;

	@Column(name = "check_in_date", nullable = false)
	private LocalDate checkInDate;

	@Column(name = "check_out_date", nullable = false)
	private LocalDate checkOutDate;

	/**
	 * 투숙 인원.
	 *
	 * <p>{@code @JdbcTypeCode(TINYINT)} 이 붙은 이유 — 자바 타입은 {@code int} 로 두되
	 * JDBC 레벨 타입만 TINYINT 로 알려준다. 이게 없으면 Hibernate 는 INTEGER 를
	 * 기대하고, {@code ddl-auto: validate} 가 실제 컬럼(TINYINT)과 다르다며
	 * 애플리케이션 기동을 실패시킨다.
	 *
	 * <p>자바 타입을 {@code byte} 로 바꾸는 방법도 있지만, 인원수를 더하고 비교하는
	 * 업무 코드에서 {@code byte} 는 계속 int 로 승격돼 캐스팅만 늘어난다.
	 * 저장 크기는 DB 가, 다루기 편한 타입은 자바가 갖는 편이 낫다.
	 */
	@JdbcTypeCode(SqlTypes.TINYINT)
	@Column(name = "adults", nullable = false)
	private int adults;

	@JdbcTypeCode(SqlTypes.TINYINT)
	@Column(name = "children", nullable = false)
	private int children;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private ReservationStatus status;

	/** {@link #nights} 합계의 스냅샷. 조회할 때마다 재계산하지 않기 위한 값이다. */
	@Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal totalAmount;

	/** {@code status = HOLD} 일 때만 값이 있다. */
	@Column(name = "hold_expires_at")
	private LocalDateTime holdExpiresAt;

	/** 체크인 시 배정된다. 그 전에는 null 이다. */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "room_id")
	private Room room;

	/**
	 * 멱등키. 유니크 제약이 걸려 있어 같은 요청이 두 번 들어와도 예약이 두 건 생기지 않는다.
	 * 네트워크 재시도와 사용자의 더블클릭을 함께 막는다.
	 */
	@Column(name = "idempotency_key", nullable = false, length = 64)
	private String idempotencyKey;

	@Column(name = "cancelled_at")
	private LocalDateTime cancelledAt;

	@Column(name = "cancel_reason", length = 200)
	private String cancelReason;

	/**
	 * 일자별 요금 스냅샷. 예약과 생사를 같이 하므로 {@code cascade = ALL} +
	 * {@code orphanRemoval} 로 묶는다. 예약 없이 존재할 이유가 없는 데이터다.
	 *
	 * <p><b>주의</b> — 이 컬렉션을 fetch join 하면서 페이징하면 안 된다. 결과 행이
	 * 숙박일수만큼 뻥튀기되어 페이지 크기가 맞지 않는다. Hibernate 가 조용히
	 * 메모리에서 페이징해 버리는 것을 막으려고 {@code application.yml} 에
	 * {@code fail_on_pagination_over_collection_fetch: true} 를 켜 두었다.
	 */
	@OneToMany(mappedBy = "reservation", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ReservationNight> nights = new ArrayList<>();

	@Builder
	private Reservation(Long tenantId, String reservationNo, Member member, String guestName,
			String guestPhone, String guestEmail, RoomType roomType, RatePlan ratePlan,
			LocalDate checkInDate, LocalDate checkOutDate, int adults, int children,
			ReservationStatus status, BigDecimal totalAmount, LocalDateTime holdExpiresAt,
			String idempotencyKey) {
		this.tenantId = tenantId;
		this.reservationNo = reservationNo;
		this.member = member;
		this.guestName = guestName;
		this.guestPhone = guestPhone;
		this.guestEmail = guestEmail;
		this.roomType = roomType;
		this.ratePlan = ratePlan;
		this.checkInDate = checkInDate;
		this.checkOutDate = checkOutDate;
		this.adults = adults;
		this.children = children;
		this.status = status;
		this.totalAmount = totalAmount;
		this.holdExpiresAt = holdExpiresAt;
		this.idempotencyKey = idempotencyKey;
	}

	/** 외부에서 리스트를 직접 갈아끼우지 못하게 읽기 전용으로 내보낸다. */
	public List<ReservationNight> getNights() {
		return Collections.unmodifiableList(nights);
	}

	/** 연관관계 편의 메서드. 양쪽을 함께 세팅해야 cascade 저장이 제대로 동작한다. */
	public void addNight(ReservationNight night) {
		nights.add(night);
		night.assignTo(this);
	}

	/**
	 * {@code totalAmount} 를 야간 스냅샷 합계로 다시 계산한다.
	 *
	 * <p>합계를 밖에서 받아 세팅하지 않고 야간 목록에서 직접 더한다 —
	 * {@code total_amount} 와 {@code reservation_night} 합이 어긋날 여지를 없앤다.
	 * {@link #addNight}로 야간을 모두 채운 뒤 호출한다.
	 */
	public void recalculateTotalAmount() {
		this.totalAmount = nights.stream()
				.map(ReservationNight::getRateAmount)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	/** 숙박일수. 파생값이므로 컬럼으로 두지 않는다. */
	public int nights() {
		return (int) java.time.temporal.ChronoUnit.DAYS.between(checkInDate, checkOutDate);
	}

	/**
	 * HOLD 가 만료됐는가.
	 *
	 * <p>스케줄러가 아직 정리하지 못한 건도 조회 시점에 만료로 취급하기 위한 것이다.
	 * 그래야 스케줄러 주기로 인한 재고 반환 지연이 사용자에게 보이지 않는다(D-003).
	 */
	public boolean isHoldExpired(LocalDateTime now) {
		return status == ReservationStatus.HOLD
				&& holdExpiresAt != null
				&& holdExpiresAt.isBefore(now);
	}

	/**
	 * HOLD → EXPIRED 전이. 스케줄러가 만료분을 정리할 때 부른다(D-003).
	 *
	 * <p>재고의 {@code held_qty} 반환은 호출자(만료 서비스)가 잠근 재고 행에 대고
	 * 따로 한다 — 이 메서드는 예약 상태만 바꾼다. {@code hold_expires_at} 은 지우지 않고
	 * 남긴다. "언제 만료됐나"가 이탈 예약 분석의 근거가 된다.
	 *
	 * @throws IllegalStateException HOLD 가 아닌 상태에서 부르면. 이미 확정·취소된 예약을
	 *         만료로 덮어쓰는 사고를 드러낸다.
	 */
	public void expire() {
		if (status != ReservationStatus.HOLD) {
			throw new IllegalStateException(
					"HOLD 가 아닌 예약을 만료시킬 수 없습니다. no=" + reservationNo + " status=" + status);
		}
		this.status = ReservationStatus.EXPIRED;
	}

	/**
	 * HOLD → CONFIRMED 전이. 결제 성공 시 부른다.
	 *
	 * <p>재고의 {@code held_qty → sold_qty} 이동은 호출자(확정 서비스)가 잠근 재고 행에
	 * 대고 따로 한다 — 이 메서드는 예약 상태만 바꾼다. {@code hold_expires_at} 은 null 로
	 * 지운다: 확정된 예약은 더 이상 만료 대상이 아니므로, 스케줄러가 훑는 조건
	 * ({@code status=HOLD and hold_expires_at < now})에서 자연히 빠진다.
	 *
	 * <p><b>만료된 뒤에는 확정할 수 없다.</b> 이미 EXPIRED 면 재고가 반환돼 다른 손님에게
	 * 팔렸을 수 있다. 결제가 만료 직후 도착하면 여기서 거부하고, 환불은 상위 계층이 맡는다.
	 *
	 * @throws IllegalStateException HOLD 가 아닌 상태에서 부르면.
	 */
	public void confirm() {
		if (status != ReservationStatus.HOLD) {
			throw new IllegalStateException(
					"HOLD 가 아닌 예약을 확정할 수 없습니다. no=" + reservationNo + " status=" + status);
		}
		this.status = ReservationStatus.CONFIRMED;
		this.holdExpiresAt = null;
	}
}
