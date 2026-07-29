package io.github.jeongkyuchoi.hotel.erp.common.domain.basedata;

import io.github.jeongkyuchoi.hotel.erp.common.domain.support.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 객실타입 (디럭스 더블, 스위트 …). <b>판매 단위</b>다.
 *
 * <p>고객은 "301호"를 예약하는 것이 아니라 "디럭스 더블"을 예약한다.
 * 실제 호실({@link Room}) 배정은 체크인 시점에 한다.
 *
 * <p><b>연관관계 매핑 규칙 (이 프로젝트 공통)</b>
 * <ul>
 *   <li>{@code @ManyToOne} 은 <b>항상 {@code fetch = LAZY}</b> 로 둔다.
 *       기본값이 EAGER 라서 그냥 두면 이 엔티티를 조회할 때마다 연관 테이블 조회가
 *       조용히 따라붙는다. 목록 조회에서 N+1 문제의 출발점이 된다.</li>
 *   <li>예외는 {@code RoomInventory.roomTypeId} 하나다. 락 경로라서 의도하지 않은
 *       쿼리가 섞이면 안 된다(해당 클래스 주석 참조).</li>
 * </ul>
 */
@Entity
@Table(name = "room_type")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomType extends BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "tenant_id", nullable = false)
	private Long tenantId;

	/** 타입 코드 (DLX, STE). 테넌트 안에서 유일하다. */
	@Column(name = "code", nullable = false, length = 20)
	private String code;

	/** 표시명 (디럭스 더블) */
	@Column(name = "name", nullable = false, length = 100)
	private String name;

	/** 부킹엔진에 노출되는 상세 설명 */
	@Column(name = "description", columnDefinition = "TEXT")
	private String description;

	@Column(name = "image_url", length = 500)
	private String imageUrl;

	/**
	 * 기준 인원.
	 *
	 * <p>DB 컬럼이 TINYINT 라서 {@code @JdbcTypeCode} 로 알려준다. 없으면 Hibernate 가
	 * INTEGER 를 기대해 {@code ddl-auto: validate} 가 기동을 막는다.
	 * 자세한 사정은 {@code Reservation.adults} 주석 참조.
	 */
	@JdbcTypeCode(SqlTypes.TINYINT)
	@Column(name = "standard_occupancy", nullable = false)
	private int standardOccupancy;

	/** 최대 인원. DB CHECK 로 {@code >= standardOccupancy} 가 강제된다. */
	@JdbcTypeCode(SqlTypes.TINYINT)
	@Column(name = "max_occupancy", nullable = false)
	private int maxOccupancy;

	/**
	 * DOUBLE, TWIN, KING 등.
	 *
	 * <p>enum 으로 만들지 않았다. 스키마에 CHECK 제약이 없어 값이 열려 있는 컬럼이고,
	 * enum 으로 좁히면 DB 가 허용하는 값을 애플리케이션이 읽다가 터진다.
	 * 제약이 DB 에 없으면 자바에서도 강제하지 않는다.
	 */
	@Column(name = "bed_type", length = 30)
	private String bedType;

	/** 부킹엔진 노출 순서 */
	@Column(name = "display_order", nullable = false)
	private int displayOrder;

	/** 판매 중단은 삭제가 아니라 비활성화로 한다. 과거 예약이 참조하고 있기 때문이다. */
	@Column(name = "active", nullable = false)
	private boolean active;

	@Builder
	private RoomType(Long tenantId, String code, String name, String description,
			String imageUrl, int standardOccupancy, int maxOccupancy,
			String bedType, int displayOrder, boolean active) {
		this.tenantId = tenantId;
		this.code = code;
		this.name = name;
		this.description = description;
		this.imageUrl = imageUrl;
		this.standardOccupancy = standardOccupancy;
		this.maxOccupancy = maxOccupancy;
		this.bedType = bedType;
		this.displayOrder = displayOrder;
		this.active = active;
	}

	/**
	 * 수정 가능한 항목을 한 번에 갱신한다.
	 *
	 * <p>필드마다 setter 를 열지 않는 이유 — setter 가 열려 있으면 어디서든 한 필드씩
	 * 바꿀 수 있고, 그러면 "이름만 바뀌고 최대 인원은 안 바뀐 중간 상태"가 만들어진다.
	 * 변경을 의미 있는 단위로 묶어 두면 그런 상태가 존재할 수 없다.
	 *
	 * <p>{@code code} 는 여기 없다. 코드는 다른 데이터가 참조하는 식별자라
	 * 발급 후 바꾸지 않는다.
	 */
	public void update(String name, String description, String imageUrl,
			int standardOccupancy, int maxOccupancy, String bedType, int displayOrder) {
		this.name = name;
		this.description = description;
		this.imageUrl = imageUrl;
		this.standardOccupancy = standardOccupancy;
		this.maxOccupancy = maxOccupancy;
		this.bedType = bedType;
		this.displayOrder = displayOrder;
	}

	/**
	 * 판매 재개 / 판매 중단.
	 *
	 * <p>삭제 대신 이 방식을 쓴다. 이 객실타입은 호실·요금정책·재고·예약이 FK 로
	 * 참조하고 있어 물리 삭제가 애초에 불가능하고, 가능하더라도 과거 예약이 어떤
	 * 객실타입이었는지 알 수 없게 된다.
	 */
	public void changeActive(boolean active) {
		this.active = active;
	}
}
