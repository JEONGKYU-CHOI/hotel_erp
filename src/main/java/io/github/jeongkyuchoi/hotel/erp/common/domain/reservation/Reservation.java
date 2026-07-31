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
	 * 취소 행위자(D-042). 취소 시점에 굳힌다 — {@code updatedBy}(마지막 수정자)와 달리 이후
	 * 수정에 덮이지 않는다. 로그인 직원명 또는 비인증/배치 경로의 SYSTEM. NULL 이면 미취소.
	 */
	@Column(name = "cancelled_by", length = 50)
	private String cancelledBy;

	/**
	 * 취소 위약금 스냅샷(D-037). 취소 시점에 요금정책으로 계산해 굳힌다 — 정책이 나중에 바뀌어도
	 * 이 예약에 물린 금액은 변하지 않는다({@link RatePlan#update} 와 같은 규율). NULL 이면
	 * 아직 취소되지 않았거나 위약금 계산이 적용되지 않은 예약이다.
	 */
	@Column(name = "cancellation_fee", precision = 12, scale = 2)
	private BigDecimal cancellationFee;

	/**
	 * 노쇼 위약금 스냅샷(D-037). 노쇼 판정 시점에 요금정책으로 계산해 굳힌다 —
	 * {@link #cancellationFee} 와 대칭이다. 야간마감의 첫날 게시(폴리오)와는 별개의 값이며,
	 * 둘의 정합은 정산(후속)이 맞춘다. NULL 이면 노쇼가 아니거나 위약금 미적용이다.
	 */
	@Column(name = "no_show_fee", precision = 12, scale = 2)
	private BigDecimal noShowFee;

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
	 * 조회·표시용 상태(D-003). 만료 시각이 지난 HOLD 는 스케줄러가 아직 정리하지 못했어도
	 * {@link ReservationStatus#EXPIRED} 로 보여준다 — 그 외에는 저장된 상태 그대로다.
	 *
	 * <p>회원 목록·비회원 조회·폴리오가 각자 {@code isHoldExpired(now) ? EXPIRED : status}
	 * 삼항을 반복하던 것을 하나로 모은 것이다(D-043). 표시 규칙을 한 곳에서 바꾸면 세 화면이
	 * 함께 움직여, 폴리오가 "만료됨"을 목록과 달리 보고 미수로 잡는 어긋남이 원천 차단된다.
	 */
	public ReservationStatus displayStatus(LocalDateTime now) {
		return isHoldExpired(now) ? ReservationStatus.EXPIRED : status;
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

	/**
	 * 예약 취소. HOLD 또는 CONFIRMED 예약을 CANCELLED 로 전이한다(D-027).
	 *
	 * <p>재고 반환(HOLD 면 {@code held_qty}, CONFIRMED 면 {@code sold_qty})은 호출자(취소
	 * 서비스)가 잠근 재고 행에 대고 따로 한다 — 이 메서드는 예약 상태와 취소 메타(시각·사유)만
	 * 바꾼다. <b>어느 버킷을 되돌릴지는 취소 직전 상태가 결정하므로, 서비스가 이 메서드를
	 * 부르기 전에 상태를 먼저 읽어야 한다.</b>
	 *
	 * <p>{@code hold_expires_at} 은 null 로 지운다 — 취소된 예약은 더 이상 만료 대상이
	 * 아니다(확정과 같은 처리).
	 *
	 * <p><b>취소할 수 없는 상태</b> — CHECKED_IN·CHECKED_OUT 은 이미 투숙이 시작/완료돼
	 * 취소가 아니라 다른 업무(중도퇴실·환불)의 영역이다. EXPIRED·NO_SHOW·CANCELLED 는 이미
	 * 재고를 점유하지 않으므로 취소로 되돌릴 것이 없다. 이미 CANCELLED 인 예약의 재취소(멱등)는
	 * 서비스가 이 메서드를 부르기 전에 걸러 낸다.
	 *
	 * <p>{@code cancellationFee} 는 호출자(취소 서비스)가 {@link CancellationPolicy} 로 계산해
	 * 넘긴 위약금 스냅샷이다 — 미결제(HOLD) 취소는 0 이다. 여기서 다시 계산하지 않는다: 계산은
	 * 순수 함수로 떼어 두고(테스트 용이), 이 메서드는 그 결과를 굳히기만 한다.
	 *
	 * <p>{@code actor} 는 취소를 실행한 주체다(D-042) — 백오피스는 로그인 직원명, 배치·비인증
	 * 경로는 SYSTEM. 취소 시점에 굳혀 이후 수정에 덮이지 않게 한다.
	 *
	 * @throws IllegalStateException HOLD·CONFIRMED 가 아닌 상태에서 부르면.
	 */
	public void cancel(String reason, BigDecimal cancellationFee, String actor) {
		if (status != ReservationStatus.HOLD && status != ReservationStatus.CONFIRMED) {
			throw new IllegalStateException(
					"취소할 수 없는 상태입니다. no=" + reservationNo + " status=" + status);
		}
		this.status = ReservationStatus.CANCELLED;
		this.cancelledAt = LocalDateTime.now();
		this.cancelReason = reason;
		this.cancellationFee = cancellationFee;
		this.cancelledBy = actor;
		this.holdExpiresAt = null;
	}

	/**
	 * CONFIRMED → CHECKED_IN 전이. 이 시점에 물리 호실을 배정한다(D-031).
	 *
	 * <p>예약은 타입 단위로만 팔리고 호실은 비워 두었다가(D-003) 체크인 때 배정한다. 호실의
	 * 점유 상태(OCCUPIED) 전환은 호출자(체크인 서비스)가 잠근 호실 행에 대고 따로 한다 —
	 * 이 메서드는 예약 상태와 배정 호실만 바꾼다.
	 *
	 * @throws IllegalStateException 확정 상태가 아니면. 미결제(HOLD)·취소·이미 투숙 중인
	 *         예약을 체크인하는 사고를 드러낸다.
	 */
	public void checkIn(Room room) {
		if (status != ReservationStatus.CONFIRMED) {
			throw new IllegalStateException(
					"확정된 예약만 체크인할 수 있습니다. no=" + reservationNo + " status=" + status);
		}
		if (room == null) {
			throw new IllegalArgumentException("배정할 호실이 필요합니다. no=" + reservationNo);
		}
		this.status = ReservationStatus.CHECKED_IN;
		this.room = room;
	}

	/**
	 * CHECKED_IN → CHECKED_OUT 전이. 배정 호실은 그대로 둔다(누가 어디에 묵었는지 기록).
	 *
	 * <p>호실의 점유 해제·청소 표시는 호출자가 잠근 호실 행에 대고 한다 — 이 메서드는 예약
	 * 상태만 바꾼다.
	 *
	 * @throws IllegalStateException 투숙 중이 아니면.
	 */
	public void checkOut() {
		if (status != ReservationStatus.CHECKED_IN) {
			throw new IllegalStateException(
					"투숙 중인 예약만 체크아웃할 수 있습니다. no=" + reservationNo + " status=" + status);
		}
		this.status = ReservationStatus.CHECKED_OUT;
	}

	/**
	 * CONFIRMED → NO_SHOW 전이(D-036). 도착일에 투숙하지 않은 확정 예약을 야간마감이 판정한다.
	 *
	 * <p><b>재고는 건드리지 않는다.</b> 도착일이 이미 지났으므로 {@code sold_qty} 를 되돌려도
	 * 그 밤을 다시 팔 수 없다 — 취소(투숙 전, 재고 반환)와 갈리는 지점이다. 첫날 숙박료는
	 * 야간마감이 이 전이 <b>전에</b> 이미 게시했다(노쇼 첫날 과금). 이후 밤은 NO_SHOW 가
	 * 게시 대상 상태(CONFIRMED·CHECKED_IN)에서 빠져 게시되지 않는다.
	 *
	 * <p>재판정(멱등)은 서비스의 조회 필터(CONFIRMED 만)가 걸러 이 메서드까지 오지 않는다.
	 *
	 * <p>{@code noShowFee} 는 호출자(노쇼 서비스)가 {@link CancellationPolicy#quoteNoShow} 로
	 * 계산해 넘긴 위약금 스냅샷이다 — 취소({@link #cancel})와 같은 규율로, 계산은 순수 함수에
	 * 맡기고 이 메서드는 결과를 굳히기만 한다.
	 *
	 * @throws IllegalStateException 확정 상태가 아니면.
	 */
	public void noShow(BigDecimal noShowFee) {
		if (status != ReservationStatus.CONFIRMED) {
			throw new IllegalStateException(
					"확정된 예약만 노쇼 처리할 수 있습니다. no=" + reservationNo + " status=" + status);
		}
		this.status = ReservationStatus.NO_SHOW;
		this.noShowFee = noShowFee;
	}
}
