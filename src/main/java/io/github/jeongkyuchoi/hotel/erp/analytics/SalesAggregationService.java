package io.github.jeongkyuchoi.hotel.erp.analytics;

import io.github.jeongkyuchoi.hotel.erp.common.domain.member.Gender;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.ReservationStatus;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 월간 판매 지표 집계(추천 메일 1단계). DB 가 접어 준 {@link SalesRow} 를 세그먼트 × 객실타입 ×
 * 요금제 단위 {@link SalesMetrics} 로 최종 집계한다.
 *
 * <p>산출물(세그먼트별 인기 객실·요금제)은 2단계 회원별 추천후보 선정 → 3단계 Gemini 문구
 * 생성 → 4단계 월간 스케줄러 발송으로 이어진다.
 */
@Service
public class SalesAggregationService {

	/**
	 * 판매로 인정해 세는 상태. 실제 판매가 아닌 {@code HOLD·EXPIRED·CANCELLED} 는 빠진다.
	 * {@link #aggregateMonth} 가 그대로 리포지토리 {@code in :statuses} 로 넘긴다.
	 */
	static final Set<ReservationStatus> COUNTED_STATUSES = EnumSet.of(
			ReservationStatus.CHECKED_OUT,
			ReservationStatus.CONFIRMED,
			ReservationStatus.CHECKED_IN,
			ReservationStatus.NO_SHOW);

	private final SalesAggregationRepository repository;

	public SalesAggregationService(SalesAggregationRepository repository) {
		this.repository = repository;
	}

	/**
	 * 한 달치 판매 지표를 세그먼트 × 객실타입 × 요금제 단위로 집계한다.
	 *
	 * <p>기준날짜는 체크아웃일이다. 달력 월(1일~말일)을 {@code [1일, 다음달 1일)} 반열린 구간으로
	 * 조회하므로 월말·윤년 경계가 자동으로 맞는다.
	 *
	 * <p>정렬은 <b>체크아웃 건수 내림차순</b> — 추천 랭킹의 기준이다. 동수는 객실타입 id,
	 * 요금제 id 순으로 결정적으로 정렬해 테스트·발송이 재현 가능하게 한다.
	 *
	 * @param tenantId 테넌트
	 * @param month    집계 대상 달력 월
	 * @return 세그먼트별 지표. 판매가 하나도 없으면 빈 리스트.
	 */
	@Transactional(readOnly = true)
	public List<SalesMetrics> aggregateMonth(Long tenantId, YearMonth month) {
		LocalDate monthStart = month.atDay(1);
		LocalDate nextMonthStart = month.plusMonths(1).atDay(1);

		List<SalesRow> rows = repository.aggregate(tenantId, monthStart, nextMonthStart, COUNTED_STATUSES);

		// 출생연도 단위로 흩어진 행을 나이대 세그먼트로 접으며 상태별 카운트를 누적한다.
		// 삽입 순서를 유지(LinkedHashMap)하되, 최종 정렬은 아래에서 체크아웃 수 기준으로 다시 한다.
		Map<Bucket, long[]> accumulator = new LinkedHashMap<>();
		for (SalesRow row : rows) {
			Segment segment = segmentOf(row.gender(), row.birthYear(), month.getYear());
			Bucket bucket = new Bucket(segment, row.roomTypeId(), row.roomTypeName(),
					row.ratePlanId(), row.ratePlanName());
			long[] counts = accumulator.computeIfAbsent(bucket, k -> new long[3]);
			add(counts, row.status(), row.count());
		}

		return accumulator.entrySet().stream()
				.map(e -> toMetrics(e.getKey(), e.getValue()))
				.sorted(Comparator
						.comparingLong(SalesMetrics::checkoutCount).reversed()
						.thenComparingLong(SalesMetrics::roomTypeId)
						.thenComparingLong(SalesMetrics::ratePlanId))
				.toList();
	}

	/**
	 * 성별·출생연도를 세그먼트로 접는다. 둘 중 하나라도 없으면(비회원·프로필 미상)
	 * {@link Segment#GUEST} 전체집계로 모은다.
	 *
	 * <p>나이는 {@code 기준연도 - 출생연도} 로 계산한다 — 생일 경과 여부까지 따지지 않는
	 * 근사값이다. 나이대 구간(10년 단위)에는 이 근사로 충분하고, 세그먼트가 생일 하루로
	 * 흔들리지 않아 오히려 안정적이다.
	 */
	private Segment segmentOf(Gender gender, Integer birthYear, int referenceYear) {
		if (gender == null || birthYear == null) {
			return Segment.GUEST;
		}
		return new Segment(gender, AgeBand.fromAge(referenceYear - birthYear));
	}

	/** 상태를 세 갈래(체크아웃/예약/노쇼)로 눌러 담는다. */
	private void add(long[] counts, ReservationStatus status, long count) {
		switch (status) {
			case CHECKED_OUT -> counts[0] += count;
			case CONFIRMED, CHECKED_IN -> counts[1] += count;
			case NO_SHOW -> counts[2] += count;
			default -> {
				// COUNTED_STATUSES 로 이미 걸렀으므로 여기 오지 않는다. 방어적으로 무시한다.
			}
		}
	}

	private SalesMetrics toMetrics(Bucket b, long[] counts) {
		return new SalesMetrics(b.segment(), b.roomTypeId(), b.roomTypeName(),
				b.ratePlanId(), b.ratePlanName(), counts[0], counts[1], counts[2]);
	}

	/** 누적 키 — 세그먼트 × 객실타입 × 요금제. 이름은 표시용이라 동치성 판단에도 포함해 무해하다. */
	private record Bucket(Segment segment, long roomTypeId, String roomTypeName,
			long ratePlanId, String ratePlanName) {
	}
}
