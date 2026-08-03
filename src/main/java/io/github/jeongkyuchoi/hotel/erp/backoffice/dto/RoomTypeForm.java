package io.github.jeongkyuchoi.hotel.erp.backoffice.dto;

import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RoomType;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

/**
 * 객실타입 등록/수정 폼.
 *
 * <p><b>엔티티를 폼에 직접 바인딩하지 않는 이유</b> — 엔티티를 그대로 받으면
 * 요청 파라미터 이름만 맞추면 어떤 필드든 덮어쓸 수 있다. 화면에 없는 {@code tenantId}
 * 나 {@code id} 까지 조작 가능해진다. 받을 값의 목록을 별도 클래스로 못 박아 둔다.
 *
 * <p>여기서는 setter 를 연다. 스프링 MVC 가 요청 파라미터를 채워 넣어야 하는
 * 전송 객체이기 때문이다. 엔티티에 setter 를 열지 않는 것과는 목적이 다르다.
 */
@Getter
@Setter
@NoArgsConstructor
public class RoomTypeForm {

	/**
	 * 타입 코드. 등록 후에는 바꾸지 않는다(수정 화면에서는 읽기 전용).
	 * 대문자·숫자·언더스코어만 허용해 표기가 갈리지 않게 한다.
	 */
	@NotBlank(message = "코드를 입력하세요.")
	@Size(max = 20, message = "코드는 20자 이내여야 합니다.")
	@Pattern(regexp = "^[A-Z0-9_]+$", message = "코드는 대문자·숫자·밑줄만 사용할 수 있습니다.")
	private String code;

	@NotBlank(message = "표시명을 입력하세요.")
	@Size(max = 100, message = "표시명은 100자 이내여야 합니다.")
	private String name;

	/** 영문 표시명(선택). 비우면 부킹엔진 영어 모드에서 한글 표시명으로 폴백한다(D-051). */
	@Size(max = 100, message = "영문 표시명은 100자 이내여야 합니다.")
	private String nameEn;

	private String description;

	// 이미지는 파일 업로드로 받는다(D-053). 아래 imageUrlN 은 사용자 입력이 아니라 저장된
	// 공개 경로(/uploads/…)를 담는 값이다 — 수정 화면에서 hidden 으로 실려 와, 새 파일을 안
	// 올리면 그대로 유지된다. imageNFile 이 있으면 컨트롤러가 저장 후 imageUrlN 을 덮고,
	// removeImageN 이면 비운다. 최대 3장.
	@Size(max = 500)
	private String imageUrl;
	@Size(max = 500)
	private String imageUrl2;
	@Size(max = 500)
	private String imageUrl3;

	/** 새로 업로드하는 이미지 파일(선택). 비었으면 기존 이미지를 유지한다. */
	private MultipartFile image1File;
	private MultipartFile image2File;
	private MultipartFile image3File;

	/** 수정 화면에서 기존 이미지를 지울지 여부(체크 시 해당 슬롯을 비운다). */
	private boolean removeImage1;
	private boolean removeImage2;
	private boolean removeImage3;

	@Min(value = 1, message = "기준 인원은 1명 이상이어야 합니다.")
	@Max(value = 99, message = "기준 인원이 너무 큽니다.")
	private int standardOccupancy = 2;

	@Min(value = 1, message = "최대 인원은 1명 이상이어야 합니다.")
	@Max(value = 99, message = "최대 인원이 너무 큽니다.")
	private int maxOccupancy = 2;

	@Size(max = 30)
	private String bedType;

	@Min(value = 0, message = "노출 순서는 0 이상이어야 합니다.")
	private int displayOrder = 0;

	private boolean active = true;

	/**
	 * 항목 간 관계 검증.
	 *
	 * <p>DB 에도 같은 내용의 CHECK 제약이 있다({@code chk_room_type_occupancy}).
	 * 중복처럼 보이지만 역할이 다르다 — 여기서는 사용자에게 <b>어느 항목이 왜 틀렸는지</b>
	 * 알려주는 것이 목적이고, DB 제약은 코드에 버그가 있어도 잘못된 데이터가
	 * 저장되지 않게 하는 최후 방어선이다. 둘 중 하나만 있으면 안 된다.
	 */
	@AssertTrue(message = "최대 인원은 기준 인원보다 작을 수 없습니다.")
	public boolean isOccupancyConsistent() {
		return maxOccupancy >= standardOccupancy;
	}

	/** 수정 화면을 열 때 기존 값으로 폼을 채운다. */
	public static RoomTypeForm from(RoomType roomType) {
		RoomTypeForm form = new RoomTypeForm();
		form.code = roomType.getCode();
		form.name = roomType.getName();
		form.nameEn = roomType.getNameEn();
		form.description = roomType.getDescription();
		form.imageUrl = roomType.getImageUrl();
		form.imageUrl2 = roomType.getImageUrl2();
		form.imageUrl3 = roomType.getImageUrl3();
		form.standardOccupancy = roomType.getStandardOccupancy();
		form.maxOccupancy = roomType.getMaxOccupancy();
		form.bedType = roomType.getBedType();
		form.displayOrder = roomType.getDisplayOrder();
		form.active = roomType.isActive();
		return form;
	}
}
