package io.github.jeongkyuchoi.hotel.erp.booking.dto;

/**
 * 고객 예약 취소 요청 바디. 사유는 선택이다(비우면 서비스가 "고객 요청"으로 굳힌다).
 */
public record CancelRequest(String reason) {
}
