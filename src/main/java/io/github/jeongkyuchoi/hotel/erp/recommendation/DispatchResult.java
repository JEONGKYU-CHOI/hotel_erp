package io.github.jeongkyuchoi.hotel.erp.recommendation;

/**
 * 추천 메일 배치 한 번의 결과 요약(추천 메일 4단계). 스케줄러·수동 트리거가 로그·응답으로 쓴다.
 *
 * @param candidates 발송 후보로 뽑힌 회원 수(수신동의·프로필 있는 회원 중 추천거리가 있던 수)
 * @param sent       이번에 실제로 발송한 수
 * @param skipped    이미 이 달에 보내 건너뛴 수(멱등)
 * @param failed     발송 중 예외로 실패한 수(이력 미기록, 다음 실행에서 재시도)
 */
public record DispatchResult(int candidates, int sent, int skipped, int failed) {
}
