package com.hotel.erp.common.domain.basedata;

import com.hotel.erp.common.domain.support.BaseEntity;
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
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 실제 호실 (101호, 102호 …). <b>배정 단위</b>다.
 *
 * <p>예약 시점에는 확정하지 않는다. 재고는 타입 단위로만 차감하고,
 * 체크인 때 비어 있고 청소가 끝난 호실을 배정한다.
 */
@Entity
@Table(name = "room")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Room extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "tenant_id", nullable = false)
	private Long tenantId;

	/**
	 * 소속 객실타입.
	 *
	 * <p>{@code fetch = LAZY} 를 명시한다. {@code @ManyToOne} 의 기본값은 EAGER 라서
	 * 빼먹으면 호실 목록 100건 조회에 타입 조회가 따라붙는다.
	 */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "room_type_id", nullable = false)
	private RoomType roomType;

	@Column(name = "room_no", nullable = false, length = 10)
	private String roomNo;

	@Column(name = "floor")
	private Short floor;

	/**
	 * {@code EnumType.STRING} 이다. {@code ORDINAL}(순서 번호)로 저장하면
	 * enum 상수 순서를 바꾸는 순간 기존 데이터의 의미가 통째로 달라진다.
	 * 스키마도 VARCHAR + CHECK 로 잡혀 있다.
	 */
	@Enumerated(EnumType.STRING)
	@Column(name = "occupancy_status", nullable = false, length = 20)
	private OccupancyStatus occupancyStatus;

	@Enumerated(EnumType.STRING)
	@Column(name = "clean_status", nullable = false, length = 20)
	private CleanStatus cleanStatus;

	@Column(name = "active", nullable = false)
	private boolean active;

	@Builder
	private Room(Long tenantId, RoomType roomType, String roomNo, Short floor,
			OccupancyStatus occupancyStatus, CleanStatus cleanStatus, boolean active) {
		this.tenantId = tenantId;
		this.roomType = roomType;
		this.roomNo = roomNo;
		this.floor = floor;
		this.occupancyStatus = occupancyStatus;
		this.cleanStatus = cleanStatus;
		this.active = active;
	}

	/** 배정 가능한 호실인가. 체크인 시 호실을 고르는 기준이다. */
	public boolean isAssignable() {
		return active
				&& occupancyStatus == OccupancyStatus.VACANT
				&& (cleanStatus == CleanStatus.CLEAN || cleanStatus == CleanStatus.INSPECTED);
	}
}
