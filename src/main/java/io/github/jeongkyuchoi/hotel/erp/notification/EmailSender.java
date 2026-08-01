package io.github.jeongkyuchoi.hotel.erp.notification;

/**
 * 이메일 전송 수단. 도메인/알림 서비스는 이 인터페이스에만 의존한다 — 실제 전송이
 * 로그든 SMTP든 상관하지 않는다.
 *
 * <p>구현 교체가 이 시스템의 요점이다. 데모/로컬은 {@link LogEmailSender}(로그로 대체),
 * 실제 발송이 필요해지면 {@code SmtpEmailSender} 하나를 {@code @Profile("prod")} 로 더해
 * 켜면 된다 — 호출부는 바뀌지 않는다.
 */
public interface EmailSender {

	void send(EmailMessage message);
}
