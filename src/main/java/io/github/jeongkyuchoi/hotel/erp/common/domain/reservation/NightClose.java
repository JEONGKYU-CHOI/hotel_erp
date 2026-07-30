package io.github.jeongkyuchoi.hotel.erp.common.domain.reservation;

import io.github.jeongkyuchoi.hotel.erp.common.domain.support.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 야간마감 이력. 영업일당 정확히 한 행이다(D-026).
 *
 * <p><b>이 행의 존재 자체가 "그 영업일은 마감됐다"는 사실이다.</b> {@code business_date} 에
 * 유니크 제약이 걸려 있어, 같은 영업일을 두 번 마감하려 하면 두 번째 INSERT 가 DB 에서
 * 거부된다 — 마감 멱등성의 1차 방어선이다(스키마 {@code V2__night_close.sql} 주석 참조).
 *
 * <p>{@code postedNightCount}/{@code postedAmount} 는 그 마감이 <b>실제로 게시한</b> 숙박분의
 * 집계다. 재마감은 일어나지 않으므로 이 값은 한 번 기록되면 바뀌지 않는다. 감사 컬럼이
 * 시각뿐이라 {@link BaseTimeEntity} 를 상속한다.
 */
@Entity
@Table(name = "night_close")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NightClose extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "tenant_id", nullable = false)
	private Long tenantId;

	/** 마감 대상 영업일. 그날 밤 숙박분을 게시한다. 유니크 키의 한 축이다. */
	@Column(name = "business_date", nullable = false)
	private LocalDate businessDate;

	/** 이 마감이 실제로 게시한 숙박분 수. posted=false → true 로 바꾼 행 수다. */
	@Column(name = "posted_night_count", nullable = false)
	private int postedNightCount;

	/** 게시한 숙박분 요금 합계. */
	@Column(name = "posted_amount", nullable = false, precision = 14, scale = 2)
	private BigDecimal postedAmount;

	/** 마감 실행 주체. 스케줄러 실행은 {@code SYSTEM}, 수동 실행은 직원 식별자. */
	@Column(name = "closed_by", length = 50)
	private String closedBy;

	/** 마감이 완료된 시각. */
	@Column(name = "closed_at", nullable = false)
	private LocalDateTime closedAt;

	@Builder
	private NightClose(Long tenantId, LocalDate businessDate, int postedNightCount,
			BigDecimal postedAmount, String closedBy, LocalDateTime closedAt) {
		this.tenantId = tenantId;
		this.businessDate = businessDate;
		this.postedNightCount = postedNightCount;
		this.postedAmount = postedAmount;
		this.closedBy = closedBy;
		this.closedAt = closedAt;
	}
}
