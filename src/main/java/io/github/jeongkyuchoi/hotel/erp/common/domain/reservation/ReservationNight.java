package io.github.jeongkyuchoi.hotel.erp.common.domain.reservation;

import io.github.jeongkyuchoi.hotel.erp.common.domain.support.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 예약 일자별 요금 스냅샷.
 *
 * <p><b>왜 요금을 복사해 두는가</b> — {@code rate_calendar} 를 참조만 하면 요금정책이
 * 바뀔 때 과거 예약 금액까지 함께 변한다. 정규화 관점에서는 중복이지만,
 * 시점 데이터를 보존해야 하는 회계 요구가 정규화보다 우선한다.
 *
 * <p>이 테이블이 있어야 가능한 것: 성수기·주말 요금 차등, 야간마감의 객실료 게시,
 * 부분 취소, 연박 변경.
 *
 * <p>감사 컬럼이 시각뿐이라 {@link BaseTimeEntity} 를 상속한다. 이 행을 누가 만들었는지는
 * 소속 예약({@link Reservation})의 감사 정보로 충분하다.
 */
@Entity
@Table(name = "reservation_night")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReservationNight extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "reservation_id", nullable = false)
	private Reservation reservation;

	@Column(name = "stay_date", nullable = false)
	private LocalDate stayDate;

	/**
	 * 스냅샷 시점의 객실타입/요금정책 id.
	 *
	 * <p>{@code @ManyToOne} 이 아니라 Long 이다. 이 값들은 탐색을 위한 연관이 아니라
	 * "그때 무엇으로 계산했는가"를 남긴 <b>기록</b>이고, 스키마에도 FK 제약이 없다.
	 * 연관으로 만들면 기준정보가 바뀌었을 때 기록이 따라 변하는 것처럼 읽힌다.
	 */
	@Column(name = "room_type_id", nullable = false)
	private Long roomTypeId;

	@Column(name = "rate_plan_id", nullable = false)
	private Long ratePlanId;

	/** 계산 시점 요금. 이후 요금표가 바뀌어도 이 값은 변하지 않는다. */
	@Column(name = "rate_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal rateAmount;

	/** 야간마감이 이 숙박분을 폴리오에 게시했는지. 마감 멱등성 판단의 기준이다. */
	@Column(name = "posted", nullable = false)
	private boolean posted;

	@Builder
	private ReservationNight(LocalDate stayDate, Long roomTypeId, Long ratePlanId,
			BigDecimal rateAmount, boolean posted) {
		this.stayDate = stayDate;
		this.roomTypeId = roomTypeId;
		this.ratePlanId = ratePlanId;
		this.rateAmount = rateAmount;
		this.posted = posted;
	}

	/** {@link Reservation#addNight(ReservationNight)} 가 호출한다. 직접 부르지 않는다. */
	void assignTo(Reservation reservation) {
		this.reservation = reservation;
	}

	/**
	 * 야간마감이 이 숙박분을 폴리오에 게시했다고 표시한다(D-026).
	 *
	 * <p>{@code posted} 를 false → true 로 바꾼다. <b>이미 게시된 숙박분을 다시 게시하지
	 * 않는다.</b> 이미 {@code posted = true} 면 예외를 던져, 마감이 게시 대상 조회
	 * ({@code posted = false} 필터)를 빠뜨린 채 같은 행을 두 번 건드리는 사고를 드러낸다.
	 * 이 가드가 숙박분 단위 멱등성(V2 주석 ②)의 도메인 표현이다.
	 *
	 * @throws IllegalStateException 이미 게시된 숙박분을 다시 게시하려 하면.
	 */
	public void post() {
		if (posted) {
			throw new IllegalStateException(
					"이미 게시된 숙박분입니다. stay_date=" + stayDate + " reservation_id="
							+ (reservation != null ? reservation.getId() : null));
		}
		this.posted = true;
	}
}
