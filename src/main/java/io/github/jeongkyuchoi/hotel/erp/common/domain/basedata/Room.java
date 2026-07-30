package io.github.jeongkyuchoi.hotel.erp.common.domain.basedata;

import io.github.jeongkyuchoi.hotel.erp.common.domain.support.BaseEntity;
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

	/**
	 * 기준정보 수정.
	 *
	 * <p>{@code occupancyStatus} 가 여기 없는 것은 실수가 아니다. 점유 상태는
	 * <b>예약과 체크인/체크아웃이 바꾸는 값</b>이지 사람이 화면에서 고르는 값이 아니다.
	 * 기준정보 화면에서 임의로 VACANT 로 되돌릴 수 있으면, 투숙 중인 방에
	 * 다른 손님을 배정하는 사고가 난다.
	 *
	 * <p>반대로 청결 상태는 여기서 바꿀 수 있어야 한다. 고장으로 인한
	 * {@link CleanStatus#OUT_OF_ORDER} 처리가 기준정보 담당의 일이기 때문이다.
	 */
	public void update(RoomType roomType, String roomNo, Short floor, CleanStatus cleanStatus) {
		this.roomType = roomType;
		this.roomNo = roomNo;
		this.floor = floor;
		this.cleanStatus = cleanStatus;
	}

	/** 사용 중지는 삭제가 아니라 비활성화다. 과거 예약이 이 호실을 참조한다. */
	public void changeActive(boolean active) {
		this.active = active;
	}

	/** 배정 가능한 호실인가. 체크인 시 호실을 고르는 기준이다. */
	public boolean isAssignable() {
		return active
				&& occupancyStatus == OccupancyStatus.VACANT
				&& (cleanStatus == CleanStatus.CLEAN || cleanStatus == CleanStatus.INSPECTED);
	}

	/**
	 * 체크인 배정 — 점유 상태를 OCCUPIED 로 올린다(D-031).
	 *
	 * <p>이미 투숙 중인 호실을 다시 배정하면 두 손님이 한 방을 쓰는 사고다. 호출자(체크인
	 * 서비스)가 이 호실 행을 {@code FOR UPDATE} 로 잠근 채 불러 동시 배정을 직렬화한다.
	 * 청결 상태는 건드리지 않는다 — 이미 배정 조건({@link #isAssignable})에서 걸렀다.
	 *
	 * @throws IllegalStateException 공실이 아닌 호실을 배정하려 하면.
	 */
	public void occupy() {
		if (occupancyStatus != OccupancyStatus.VACANT) {
			throw new IllegalStateException(
					"공실이 아닌 호실은 배정할 수 없습니다. room=" + roomNo + " status=" + occupancyStatus);
		}
		this.occupancyStatus = OccupancyStatus.OCCUPIED;
	}

	/**
	 * 체크아웃 — 점유를 풀고 청소 대상으로 표시한다(D-031).
	 *
	 * <p>퇴실한 방은 곧바로 다음 손님에게 배정하지 않는다. {@link CleanStatus#DIRTY} 로 두어
	 * 하우스키핑이 청소한 뒤에야 {@link #isAssignable} 를 다시 만족한다. 하우스키핑 모듈은
	 * 1차 범위 밖이지만(스키마 주석) 이 한 줄로 흐름은 성립한다.
	 */
	public void checkOutVacate() {
		this.occupancyStatus = OccupancyStatus.VACANT;
		this.cleanStatus = CleanStatus.DIRTY;
	}
}
