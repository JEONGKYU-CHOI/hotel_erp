package io.github.jeongkyuchoi.hotel.erp.recommendation;

import java.time.LocalDate;

/**
 * 회원이 과거 체크아웃한 객실타입·요금제 한 줄({@link MemberStayHistoryRepository} 산출물).
 * 같은 조합을 여러 번 묵었어도 한 줄로 접히며, {@code lastCheckOut} 이 가장 최근 투숙일이다.
 *
 * @param roomTypeId   객실타입 id
 * @param roomTypeName 객실타입명
 * @param ratePlanId   요금제 id
 * @param ratePlanName 요금제명
 * @param lastCheckOut 이 조합의 가장 최근 체크아웃일(최신 투숙 우선 정렬 기준)
 */
public record MemberStayRow(
		Long roomTypeId, String roomTypeName,
		Long ratePlanId, String ratePlanName,
		LocalDate lastCheckOut) {
}
