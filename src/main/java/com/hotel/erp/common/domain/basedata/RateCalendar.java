package com.hotel.erp.common.domain.basedata;

import com.hotel.erp.common.domain.support.BaseEntity;
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
 * 일자별 요금.
 *
 * <p>성수기·주말 요금 차등을 위해 요금을 날짜 단위로 펼쳐 둔다.
 * 행이 없으면 {@link RatePlan#getBaseAmount()} 로 폴백한다.
 *
 * <p>여기 있는 금액은 <b>현재 판매가</b>이지 예약된 금액이 아니다.
 * 예약이 확정되는 순간의 금액은 {@code reservation_night} 에 스냅샷으로 복사된다.
 * 그래야 요금표를 바꿔도 과거 예약 금액이 따라 변하지 않는다.
 */
@Entity
@Table(name = "rate_calendar")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RateCalendar extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "tenant_id", nullable = false)
	private Long tenantId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "rate_plan_id", nullable = false)
	private RatePlan ratePlan;

	@Column(name = "stay_date", nullable = false)
	private LocalDate stayDate;

	@Column(name = "amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal amount;

	/** 해당 일자 판매 중지. 재고와 무관하게 요금 쪽에서 막는 장치다. */
	@Column(name = "closed", nullable = false)
	private boolean closed;

	@Builder
	private RateCalendar(Long tenantId, RatePlan ratePlan, LocalDate stayDate,
			BigDecimal amount, boolean closed) {
		this.tenantId = tenantId;
		this.ratePlan = ratePlan;
		this.stayDate = stayDate;
		this.amount = amount;
		this.closed = closed;
	}
}
