package io.github.jeongkyuchoi.hotel.erp.analytics;

import io.github.jeongkyuchoi.hotel.erp.common.domain.member.Gender;
import io.github.jeongkyuchoi.hotel.erp.common.domain.reservation.ReservationStatus;

/**
 * {@link SalesAggregationRepository} 의 group-by 한 줄 — DB 가 접을 수 있는 최소 차원까지만 묶은
 * 중간 산출물이다. 나이대 구간으로 접는 일은 {@link SalesAggregationService} 가 자바에서 한다.
 *
 * <p><b>나이대가 아니라 출생연도({@code birthYear})로 그룹핑하는 이유</b> — 나이대 경계는
 * 기준월(집계 대상 달)에 따라 달라지는 파생값이라 DB 그룹핑 키로 두면 쿼리에 기준월 산술이
 * 섞여 이식성이 나빠진다. 출생연도는 불변 사실이므로 DB 는 이것까지만 묶고, 서비스가
 * {@code 기준연도 - 출생연도} 로 나이대를 계산한다. 회원 한 명 단위라 행이 크게 늘지 않는다.
 *
 * <p>{@code gender}·{@code birthYear} 는 비회원 예약(회원 LEFT JOIN 결과 null)에서 null 이며,
 * 서비스가 이를 {@link Segment#GUEST} 로 접는다.
 *
 * @param roomTypeId   객실타입 id
 * @param roomTypeName 객실타입명(표시용)
 * @param ratePlanId   요금제 id
 * @param ratePlanName 요금제명(표시용)
 * @param gender       회원 성별. 비회원은 null.
 * @param birthYear    회원 출생연도. 비회원 또는 생년월일 미상은 null.
 * @param status       예약 상태(체크아웃/예약/노쇼 분류의 원본)
 * @param count        해당 조합의 예약 건수
 */
public record SalesRow(
		Long roomTypeId, String roomTypeName,
		Long ratePlanId, String ratePlanName,
		Gender gender, Integer birthYear,
		ReservationStatus status, long count) {
}
