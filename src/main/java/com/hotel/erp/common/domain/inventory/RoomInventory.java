package com.hotel.erp.common.domain.inventory;

import com.hotel.erp.common.domain.support.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 일자별 객실 재고. 이 프로젝트의 락 대상 엔티티다.
 *
 * <p>가용 재고 = {@code totalQty - soldQty - heldQty}.
 * 스키마와 인덱스 설계 근거는 {@code V1__init_schema.sql} 의 {@code room_inventory} 블록
 * 주석에 있다(D-015). 여기에 재서술하지 않는다.
 *
 * <p><b>{@code roomTypeId} 를 연관관계(@ManyToOne)가 아니라 Long 으로 둔 이유</b><br>
 * 락 쿼리는 {@code room_inventory} 행만 잠그면 된다. {@code @ManyToOne} 을 두면
 * 재고를 읽을 때마다 {@code room_type} 조회가 따라붙거나 프록시가 생기고,
 * 락의 대상과 범위를 눈으로 따라가기 어려워진다. 연관 객체가 실제로 필요해지는
 * 시점에 추가한다.
 *
 * <p>Day 0 스파이크 단계의 최소 구현이다. Day 1 에서 도메인 패키지 구조를 정리하며
 * 위치와 메서드가 바뀔 수 있다.
 */
@Entity
@Table(name = "room_inventory")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 가 리플렉션으로 쓰는 기본 생성자
public class RoomInventory extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY) // MySQL AUTO_INCREMENT
	private Long id;

	@Column(name = "tenant_id", nullable = false)
	private Long tenantId;

	@Column(name = "room_type_id", nullable = false)
	private Long roomTypeId;

	@Column(name = "stay_date", nullable = false)
	private LocalDate stayDate;

	@Column(name = "total_qty", nullable = false)
	private int totalQty;

	@Column(name = "sold_qty", nullable = false)
	private int soldQty;

	@Column(name = "held_qty", nullable = false)
	private int heldQty;

	@Builder
	private RoomInventory(Long tenantId, Long roomTypeId, LocalDate stayDate,
			int totalQty, int soldQty, int heldQty) {
		this.tenantId = tenantId;
		this.roomTypeId = roomTypeId;
		this.stayDate = stayDate;
		this.totalQty = totalQty;
		this.soldQty = soldQty;
		this.heldQty = heldQty;
	}

	/** 가용 재고. 파생값이므로 컬럼으로 두지 않는다. */
	public int availableQty() {
		return totalQty - soldQty - heldQty;
	}

	/** 이미 나간 물량. 판매 총량을 이 값 아래로 내릴 수 없다. */
	public int committedQty() {
		return soldQty + heldQty;
	}

	/**
	 * 판매 총량을 이 값으로 바꿀 수 있는가.
	 *
	 * <p>DB 에 {@code CHECK (sold_qty + held_qty <= total_qty)} 가 걸려 있어서
	 * 어기면 error 3819 로 거부된다. 하지만 그때는 트랜잭션 전체가 깨지고 사용자에게는
	 * 알아볼 수 없는 예외가 나간다. 미리 물어보고 건너뛸 수 있게 한다.
	 */
	public boolean canChangeTotalQtyTo(int newTotalQty) {
		return newTotalQty >= committedQty();
	}

	/**
	 * 판매 총량 변경 (재고 생성/재생성).
	 *
	 * <p>{@code soldQty} / {@code heldQty} 는 건드리지 않는다. 그 둘은 예약이 바꾸는
	 * 값이고, 재고 생성 배치가 손대면 이미 확정된 예약이 사라진다.
	 */
	public void changeTotalQty(int newTotalQty) {
		if (!canChangeTotalQtyTo(newTotalQty)) {
			throw new IllegalArgumentException(
					"이미 판매·점유된 수량(" + committedQty() + ")보다 적게 설정할 수 없습니다.");
		}
		this.totalQty = newTotalQty;
	}
}
