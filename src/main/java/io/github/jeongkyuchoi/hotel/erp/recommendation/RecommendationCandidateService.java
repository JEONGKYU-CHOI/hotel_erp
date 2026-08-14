package io.github.jeongkyuchoi.hotel.erp.recommendation;

import io.github.jeongkyuchoi.hotel.erp.analytics.SalesAggregationService;
import io.github.jeongkyuchoi.hotel.erp.analytics.SalesMetrics;
import io.github.jeongkyuchoi.hotel.erp.analytics.Segment;
import io.github.jeongkyuchoi.hotel.erp.common.domain.member.Member;
import io.github.jeongkyuchoi.hotel.erp.common.domain.member.MemberRepository;
import io.github.jeongkyuchoi.hotel.erp.common.domain.member.MemberStatus;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 회원별 추천 후보 선정(추천 메일 2단계). 1단계 세그먼트 인기 랭킹을 개별 회원에게 매핑한다.
 *
 * <p>후보 구성(설계 합의)
 * <ul>
 *   <li><b>본인 이력 우선</b> — 회원이 과거 체크아웃한 객실타입·요금제를 최근 투숙 순으로 먼저
 *       채운다(재방문 유도가 전환에 유리).</li>
 *   <li><b>세그먼트 인기로 채움</b> — 남는 자리를, 회원이 속한 세그먼트에서 인기였던 후보로
 *       체크아웃 랭킹 순으로 채운다. 이미 이력으로 든 조합은 건너뛴다.</li>
 *   <li><b>최대 3개</b> — 부족하면 있는 만큼만. 이력·인기 둘 다 비면 그 회원은 발송 대상에서
 *       빠진다.</li>
 *   <li><b>인기 데이터 기간</b> — 발송 월 직전 3개 완료월. 세그먼트를 잘게 쪼개면 한 달 표본이
 *       얇아 랭킹이 흔들리므로 여러 달을 누적한다.</li>
 * </ul>
 */
@Service
public class RecommendationCandidateService {

	/** 회원 한 명당 추천 후보 최대 개수. */
	static final int MAX_ITEMS = 3;

	/** 인기 데이터를 모으는 완료월 수(발송 월 직전 N개월). */
	static final int WINDOW_MONTHS = 3;

	private final SalesAggregationService salesAggregationService;
	private final MemberStayHistoryRepository stayHistoryRepository;
	private final MemberRepository memberRepository;

	public RecommendationCandidateService(SalesAggregationService salesAggregationService,
			MemberStayHistoryRepository stayHistoryRepository, MemberRepository memberRepository) {
		this.salesAggregationService = salesAggregationService;
		this.stayHistoryRepository = stayHistoryRepository;
		this.memberRepository = memberRepository;
	}

	/**
	 * 발송 월 기준으로 수신 동의 회원 각각의 추천 후보를 만든다.
	 *
	 * <p>인기 랭킹의 나이대 기준연도와 회원 세그먼트 판정의 기준연도를 <b>발송 월의 연도</b>로
	 * 통일한다 — 그래야 회원이 집계된 세그먼트와 같은 칸에 든다.
	 *
	 * @param tenantId  테넌트
	 * @param sendMonth 메일을 보낼 달력 월(이 달 직전 3개월을 인기 근거로 본다)
	 * @return 후보가 1개 이상인 회원의 추천 묶음. 대상이 없으면 빈 리스트.
	 */
	@Transactional(readOnly = true)
	public List<MemberRecommendation> selectFor(Long tenantId, YearMonth sendMonth) {
		int referenceYear = sendMonth.getYear();
		LocalDate windowStart = sendMonth.minusMonths(WINDOW_MONTHS).atDay(1);
		LocalDate windowEnd = sendMonth.atDay(1); // 발송 월 1일 직전까지(반열린 구간)

		Map<Segment, List<SalesMetrics>> popularBySegment = popularityBySegment(
				tenantId, windowStart, windowEnd, referenceYear);

		List<Member> targets = memberRepository
				.findByTenantIdAndStatusAndMarketingConsentTrueAndGenderIsNotNullAndBirthDateIsNotNull(
						tenantId, MemberStatus.ACTIVE);

		List<MemberRecommendation> result = new ArrayList<>();
		for (Member member : targets) {
			Segment segment = Segment.of(member.getGender(), member.getBirthDate(), referenceYear);
			List<RecommendationItem> items = buildItems(tenantId, member, segment, popularBySegment);
			if (items.isEmpty()) {
				continue; // 이력·인기 둘 다 없으면 보낼 것이 없다.
			}
			result.add(new MemberRecommendation(member.getId(), member.getEmail(), member.getName(),
					member.getUnsubscribeToken(), segment, items));
		}
		return result;
	}

	/**
	 * 세그먼트별 인기 랭킹을 조회한다. 집계 결과가 이미 체크아웃 내림차순이므로, 세그먼트로
	 * 그룹핑해도 각 리스트는 랭킹 순서를 유지한다.
	 */
	private Map<Segment, List<SalesMetrics>> popularityBySegment(Long tenantId,
			LocalDate windowStart, LocalDate windowEnd, int referenceYear) {
		List<SalesMetrics> ranked = salesAggregationService
				.aggregateRange(tenantId, windowStart, windowEnd, referenceYear);
		Map<Segment, List<SalesMetrics>> bySegment = new LinkedHashMap<>();
		for (SalesMetrics metrics : ranked) {
			bySegment.computeIfAbsent(metrics.segment(), s -> new ArrayList<>()).add(metrics);
		}
		return bySegment;
	}

	/**
	 * 본인 이력(최근 순) 먼저, 세그먼트 인기(랭킹 순)로 채워 최대 {@value #MAX_ITEMS} 개. 같은
	 * 객실타입·요금제 조합은 한 번만 담는다.
	 */
	private List<RecommendationItem> buildItems(Long tenantId, Member member, Segment segment,
			Map<Segment, List<SalesMetrics>> popularBySegment) {
		Map<RecommendationItem.Key, RecommendationItem> picked = new LinkedHashMap<>();

		for (MemberStayRow stay : stayHistoryRepository.findStayedRoomTypes(tenantId, member.getId())) {
			if (picked.size() >= MAX_ITEMS) {
				break;
			}
			RecommendationItem item = new RecommendationItem(stay.roomTypeId(), stay.roomTypeName(),
					stay.ratePlanId(), stay.ratePlanName(), RecommendationSource.HISTORY);
			picked.putIfAbsent(item.key(), item);
		}

		for (SalesMetrics metrics : popularBySegment.getOrDefault(segment, List.of())) {
			if (picked.size() >= MAX_ITEMS) {
				break;
			}
			RecommendationItem item = new RecommendationItem(metrics.roomTypeId(), metrics.roomTypeName(),
					metrics.ratePlanId(), metrics.ratePlanName(), RecommendationSource.POPULAR);
			picked.putIfAbsent(item.key(), item);
		}

		return List.copyOf(picked.values());
	}
}
