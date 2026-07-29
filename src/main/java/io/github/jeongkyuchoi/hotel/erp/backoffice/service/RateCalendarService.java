package io.github.jeongkyuchoi.hotel.erp.backoffice.service;

import io.github.jeongkyuchoi.hotel.erp.backoffice.dto.RateCalendarGenerateForm;
import io.github.jeongkyuchoi.hotel.erp.backoffice.dto.RateCalendarGenerateResult;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RateCalendar;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RateCalendarRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RatePlan;
import io.github.jeongkyuchoi.hotel.erp.common.domain.basedata.RatePlanRepository;
import io.github.jeongkyuchoi.hotel.erp.common.exception.NotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 일자별 요금(요금 캘린더) 생성·조회.
 *
 * <p><b>왜 여기는 락이 없는가 (D-022)</b> — 구조는 재고 생성({@link InventoryService})과
 * 같은 "날짜 범위에 행 펼치기"지만, 잠글 이유가 다르다. 재고 행은 예약 트랜잭션이
 * 동시에 {@code sold_qty}/{@code held_qty} 를 올리는 경합 지점이라 첫 조회부터
 * {@code FOR UPDATE} 여야 한다(D-018). 요금표는 예약이 건드리지 않는다 — 예약 금액은
 * 확정 순간 {@code reservation_night} 스냅샷으로 복사되므로, 요금표를 나중에 바꿔도
 * 과거 예약이 흔들리지 않는다. 요금표를 수정하는 것은 관리자 한 명이고, 그 동시성은
 * 유니크 제약 {@code uk_rate_calendar} 만으로 충분하다. 그래서 일반 조회로 병합한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RateCalendarService {

	private static final Long TENANT_ID = 1L;

	private final RateCalendarRepository rateCalendarRepository;
	private final RatePlanRepository ratePlanRepository;

	/** 화면 표시용 조회. */
	public List<RateCalendar> findRange(Long ratePlanId, LocalDate from, LocalDate to) {
		return rateCalendarRepository.findRangeForList(TENANT_ID, ratePlanId, from, to);
	}

	/**
	 * 기간에 대해 요금 행을 만든다. 이미 있으면 금액만 갱신한다.
	 *
	 * <p>주중/주말 금액이 다르면 요일에 따라 금액이 갈린다({@link RateCalendarGenerateForm#amountFor}).
	 */
	@Transactional
	public RateCalendarGenerateResult generate(RateCalendarGenerateForm form) {
		RatePlan ratePlan = ratePlanRepository
				.findByIdAndTenantId(form.getRatePlanId(), TENANT_ID)
				.orElseThrow(() -> new NotFoundException(
						"요금정책을 찾을 수 없습니다. id=" + form.getRatePlanId()));

		LocalDate from = form.getFromDate();
		LocalDate to = form.getToDate();

		// 락 없는 일반 조회로 기존 행을 읽어 병합한다(D-022). 재고와 대비되는 지점이다.
		Map<LocalDate, RateCalendar> existing = rateCalendarRepository
				.findByTenantIdAndRatePlanIdAndStayDateBetweenOrderByStayDate(
						TENANT_ID, ratePlan.getId(), from, to).stream()
				.collect(Collectors.toMap(RateCalendar::getStayDate, Function.identity()));

		int created = 0;
		int updated = 0;
		int unchanged = 0;
		List<RateCalendar> toInsert = new ArrayList<>();

		for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
			BigDecimal amount = form.amountFor(date);
			RateCalendar row = existing.get(date);

			if (row == null) {
				toInsert.add(RateCalendar.builder()
						.tenantId(TENANT_ID)
						.ratePlan(ratePlan)
						.stayDate(date)
						.amount(amount)
						.closed(false)
						.build());
				created++;
				continue;
			}

			// 값 비교는 compareTo 로. equals 는 scale 이 다르면(100 vs 100.00) 다르다고 본다.
			if (row.getAmount().compareTo(amount) == 0) {
				unchanged++;
				continue;
			}

			row.changeAmount(amount); // 변경 감지로 UPDATE 자동
			updated++;
		}

		rateCalendarRepository.saveAll(toInsert);

		log.info("요금 캘린더 생성: ratePlan={} 기간={}~{} 주중={} 주말={} 생성={} 갱신={} 유지={}",
				ratePlan.getCode(), from, to, form.getWeekdayAmount(), form.getWeekendAmount(),
				created, updated, unchanged);

		return new RateCalendarGenerateResult(created, updated, unchanged);
	}
}
