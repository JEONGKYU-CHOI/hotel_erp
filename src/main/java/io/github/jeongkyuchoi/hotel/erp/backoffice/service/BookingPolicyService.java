package io.github.jeongkyuchoi.hotel.erp.backoffice.service;

import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.BookingPolicy;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.BookingPolicyRepository;
import java.time.LocalTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 예약 정책 관리(PMS). 부킹엔진 쪽 읽기는 {@code ReservationService} 가
 * 리포지토리를 직접 읽는다 — 이 서비스는 화면(조회·수정)을 담당한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingPolicyService {

	/** 멀티테넌시는 백로그이므로 전부 1 고정이다(D-007). */
	private static final Long TENANT_ID = 1L;

	/** 정책 행이 없을 때의 기본 마감 시각. Flyway 가 시드하므로 정상 경로에선 쓰이지 않는다. */
	private static final LocalTime DEFAULT_CUTOFF = LocalTime.of(20, 0);

	private final BookingPolicyRepository bookingPolicyRepository;

	/**
	 * 테넌트의 정책을 가져온다. 없으면(방어적으로) 기본값으로 만들어 저장한다 —
	 * 화면이 항상 편집할 대상을 갖도록.
	 */
	@Transactional
	public BookingPolicy getOrCreate() {
		return bookingPolicyRepository.findByTenantId(TENANT_ID)
				.orElseGet(() -> bookingPolicyRepository.save(BookingPolicy.builder()
						.tenantId(TENANT_ID)
						.sameDayCutoffTime(DEFAULT_CUTOFF)
						.build()));
	}

	/** 당일 예약 마감 시각을 바꾼다. */
	@Transactional
	public void updateSameDayCutoff(LocalTime cutoff) {
		getOrCreate().changeSameDayCutoff(cutoff);
	}
}
