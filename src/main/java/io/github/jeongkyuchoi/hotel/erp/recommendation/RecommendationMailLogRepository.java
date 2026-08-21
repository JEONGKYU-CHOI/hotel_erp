package io.github.jeongkyuchoi.hotel.erp.recommendation;

import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;

/** 추천 메일 발송 이력 조회·기록(추천 메일 4단계). */
public interface RecommendationMailLogRepository extends JpaRepository<RecommendationMailLog, Long> {

	/**
	 * 이 회원에게 이 달 추천 메일을 이미 보냈는가. 발송 전 멱등 확인용 — true 면 건너뛴다.
	 * {@code sendMonth} 는 그 달 1일로 정규화해 넘긴다.
	 */
	boolean existsByTenantIdAndMemberIdAndSendMonth(Long tenantId, Long memberId, LocalDate sendMonth);
}
