package io.github.jeongkyuchoi.hotel.erp.common.domain.basedata;

import io.github.jeongkyuchoi.hotel.erp.common.domain.support.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 예약 정책 — 테넌트별 단일 행 설정.
 *
 * <p>지금은 '당일 예약 마감 시각' 하나를 담는다. 부킹엔진의 HOLD 경로가 이 값을 읽어
 * 당일 예약을 시각으로 끊고({@code ReservationService}), PMS 기준정보 화면에서 값을 바꾼다.
 * 설정 항목이 늘면 이 엔티티에 필드를 더한다.
 */
@Entity
@Table(name = "booking_policy")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BookingPolicy extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "tenant_id", nullable = false)
	private Long tenantId;

	/** 당일 예약 마감 시각. 체크인이 오늘이면 이 시각까지만 예약을 받는다. */
	@Column(name = "same_day_cutoff_time", nullable = false)
	private LocalTime sameDayCutoffTime;

	@Builder
	private BookingPolicy(Long tenantId, LocalTime sameDayCutoffTime) {
		this.tenantId = tenantId;
		this.sameDayCutoffTime = sameDayCutoffTime;
	}

	/** 마감 시각을 바꾼다. (setter 를 열지 않고 의미 있는 변경 메서드로 둔다 — RoomType 규율.) */
	public void changeSameDayCutoff(LocalTime cutoff) {
		this.sameDayCutoffTime = cutoff;
	}
}
