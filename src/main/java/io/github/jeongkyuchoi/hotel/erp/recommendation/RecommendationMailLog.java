package io.github.jeongkyuchoi.hotel.erp.recommendation;

import io.github.jeongkyuchoi.hotel.erp.common.domain.support.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 추천 메일 발송 이력 한 행(추천 메일 4단계). 회원 × 발송월당 한 행이며, 이 존재 자체가
 * "그 달 그 회원에게 이미 보냈다"는 멱등 근거다(V15, 야간마감 {@code NightClose} 와 같은 규율).
 *
 * <p>발송에 성공한 메일만 기록한다 — 실패는 행을 남기지 않아 다음 실행이 재시도한다.
 * {@code email}·{@code subject} 는 발송 시점 스냅샷이라 이후 회원 정보가 바뀌어도 이력은
 * 보낸 그대로 남는다.
 */
@Entity
@Table(name = "recommendation_mail_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecommendationMailLog extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "tenant_id", nullable = false)
	private Long tenantId;

	@Column(name = "member_id", nullable = false)
	private Long memberId;

	/** 발송 대상 달. 그 달 1일로 정규화해 저장한다(달력 월 단위 멱등). */
	@Column(name = "send_month", nullable = false)
	private LocalDate sendMonth;

	@Column(name = "email", nullable = false, length = 255)
	private String email;

	@Column(name = "subject", nullable = false, length = 255)
	private String subject;

	@Column(name = "item_count", nullable = false)
	private int itemCount;

	@Column(name = "sent_at", nullable = false)
	private LocalDateTime sentAt;

	@Builder
	private RecommendationMailLog(Long tenantId, Long memberId, LocalDate sendMonth, String email,
			String subject, int itemCount, LocalDateTime sentAt) {
		this.tenantId = tenantId;
		this.memberId = memberId;
		this.sendMonth = sendMonth;
		this.email = email;
		this.subject = subject;
		this.itemCount = itemCount;
		this.sentAt = sentAt;
	}
}
