package io.github.jeongkyuchoi.hotel.erp.notification;

/**
 * 렌더된 이메일 한 통. 전송 수단({@link EmailSender})과 무관한 순수 값이다.
 *
 * @param to      수신자 이메일
 * @param subject 제목
 * @param body    본문(지금은 평문. HTML 로 확장하면 여기에 contentType 을 더한다)
 */
public record EmailMessage(String to, String subject, String body) {
}
