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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 요금정책 (조식포함, 환불불가 …).
 *
 * <p>금액은 전부 {@link BigDecimal} 이다. {@code double} 은 부동소수 오차 때문에
 * 더하다 보면 정산이 어긋난다. 0.1 + 0.2 가 0.3 이 아닌 그 문제다.
 */
@Entity
@Table(name = "rate_plan")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RatePlan extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "tenant_id", nullable = false)
	private Long tenantId;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "room_type_id", nullable = false)
	private RoomType roomType;

	@Column(name = "code", nullable = false, length = 20)
	private String code;

	@Column(name = "name", nullable = false, length = 100)
	private String name;

	/** 기본 요금. 해당 일자에 {@link RateCalendar} 행이 없을 때의 폴백이다. */
	@Column(name = "base_amount", nullable = false, precision = 12, scale = 2)
	private BigDecimal baseAmount;

	@Column(name = "breakfast_included", nullable = false)
	private boolean breakfastIncluded;

	@Column(name = "refundable", nullable = false)
	private boolean refundable;

	/** 체크인 며칠 전까지 무료 취소인가 */
	@Column(name = "cancel_deadline_days", nullable = false)
	private short cancelDeadlineDays;

	/** 기한이 지난 뒤 취소 시 위약금율 (%). DB CHECK 로 0~100 이 강제된다. */
	@Column(name = "penalty_rate", nullable = false, precision = 5, scale = 2)
	private BigDecimal penaltyRate;

	@Column(name = "active", nullable = false)
	private boolean active;

	@Builder
	private RatePlan(Long tenantId, RoomType roomType, String code, String name,
			BigDecimal baseAmount, boolean breakfastIncluded, boolean refundable,
			short cancelDeadlineDays, BigDecimal penaltyRate, boolean active) {
		this.tenantId = tenantId;
		this.roomType = roomType;
		this.code = code;
		this.name = name;
		this.baseAmount = baseAmount;
		this.breakfastIncluded = breakfastIncluded;
		this.refundable = refundable;
		this.cancelDeadlineDays = cancelDeadlineDays;
		this.penaltyRate = penaltyRate;
		this.active = active;
	}
}
