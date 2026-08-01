package io.github.jeongkyuchoi.hotel.erp.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 로그로 대체하는 이메일 전송(데모/로컬 기본).
 *
 * <p>실제 SMTP 계정 없이 알림 흐름 전체를 검증·시연할 수 있게, 보낼 메일을 로그로 찍는다.
 * 실 발송이 필요해지면 {@code SmtpEmailSender}(@Profile("prod"))를 추가하고 이 빈을
 * {@code @Profile("!prod")} 또는 {@code @ConditionalOnMissingBean} 으로 물러나게 하면 된다.
 * 지금은 유일한 {@link EmailSender} 구현이라 무조건 활성이다.
 */
@Slf4j
@Component
public class LogEmailSender implements EmailSender {

	@Override
	public void send(EmailMessage message) {
		log.info("\n📧 [MAIL] to={} | subject={}\n{}",
				message.to(), message.subject(), message.body());
	}
}
