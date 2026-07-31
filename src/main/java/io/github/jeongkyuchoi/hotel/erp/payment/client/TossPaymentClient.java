package io.github.jeongkyuchoi.hotel.erp.payment.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.github.jeongkyuchoi.hotel.erp.common.exception.PaymentException;
import io.github.jeongkyuchoi.hotel.erp.payment.TossProperties;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * 토스페이먼츠 승인 API 호출 (D-034).
 *
 * <p><b>Basic 인증.</b> 토스는 시크릿 키를 아이디로, 비밀번호는 빈 문자열로 하는 Basic 인증을
 * 요구한다 — {@code base64(secretKey + ":")}. 시크릿 키는 서버에만 있고 클라이언트로 나가지
 * 않는다(그래서 금액 위변조를 막을 수 있다).
 *
 * <p><b>오류는 예외로 변환한다.</b> 토스가 4xx/5xx 로 {@code {code, message}} 를 주면
 * {@link PaymentException} 으로 감싸 던진다 — 카드 거절·이미 처리된 결제·금액 불일치 등이
 * 모두 여기로 온다. 서비스는 성공 응답만 받는다.
 */
@Slf4j
@Component
public class TossPaymentClient {

	private static final String CONFIRM_PATH = "/v1/payments/confirm";
	private static final String CANCEL_PATH = "/v1/payments/{paymentKey}/cancel";
	private static final String GET_PATH = "/v1/payments/{paymentKey}";

	private final RestClient restClient;

	public TossPaymentClient(TossProperties properties) {
		String basic = Base64.getEncoder()
				.encodeToString((properties.secretKey() + ":").getBytes(StandardCharsets.UTF_8));
		// 주입받은 RestClient.Builder 빈에 기대지 않고 정적 팩터리로 만든다 — 자동구성이
		// 없는 컨텍스트(일부 테스트 슬라이스)에서도 동일하게 동작하도록. 이 클라이언트가
		// 쓰는 건 baseUrl·기본 헤더·JSON 직렬화뿐이라 정적 빌더로 충분하다.
		this.restClient = RestClient.builder()
				.baseUrl(properties.baseUrl())
				.defaultHeader(HttpHeaders.AUTHORIZATION, "Basic " + basic)
				.build();
	}

	/**
	 * 결제를 승인한다. 프론트가 결제창에서 받은 {@code paymentKey}/{@code orderId}/{@code amount}
	 * 를 그대로 토스에 넘겨 최종 승인을 요청한다. 성공하면 승인 응답을, 실패하면
	 * {@link PaymentException} 을 던진다.
	 */
	public TossConfirmResponse confirm(String paymentKey, String orderId, BigDecimal amount) {
		return restClient.post()
				.uri(CONFIRM_PATH)
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("paymentKey", paymentKey, "orderId", orderId, "amount", amount))
				.retrieve()
				.onStatus(status -> status.isError(), (request, response) -> {
					TossError error = readError(response.getBody());
					log.warn("토스 승인 실패 — status={} code={} message={}",
							response.getStatusCode(), error.code(), error.message());
					throw new PaymentException(error.code(), error.message());
				})
				.body(TossConfirmResponse.class);
	}

	/**
	 * 결제를 (부분) 취소한다(D-039). 토스 {@code POST /v1/payments/{paymentKey}/cancel} 로
	 * {@code cancelAmount} 만큼 환불한다. {@code cancelAmount} 를 생략하면 전액 취소이지만,
	 * 우리는 위약금만큼 남기는 부분취소가 기본이라 항상 금액을 실어 보낸다.
	 *
	 * <p>성공 응답 본문은 쓰지 않는다(원장 반영은 서버가 계산한 취소액으로 한다). 실패는
	 * {@link PaymentException} 으로 던져 서비스가 롤백하게 한다 — 토스 취소가 실패하면 원장도
	 * 바꾸지 않는다.
	 */
	public void cancel(String paymentKey, BigDecimal cancelAmount, String reason) {
		restClient.post()
				.uri(CANCEL_PATH, paymentKey)
				.contentType(MediaType.APPLICATION_JSON)
				.body(Map.of("cancelReason", reason, "cancelAmount", cancelAmount))
				.retrieve()
				.onStatus(status -> status.isError(), (request, response) -> {
					TossError error = readError(response.getBody());
					log.warn("토스 취소 실패 — status={} code={} message={}",
							response.getStatusCode(), error.code(), error.message());
					throw new PaymentException(error.code(), error.message());
				})
				.toBodilessEntity();
	}

	/**
	 * 결제 한 건을 조회한다(웹훅 재조회 검증, D-041). 토스 {@code GET /v1/payments/{paymentKey}}
	 * 로 <b>권위 있는</b> 결제 객체를 받아 온다 — 상태·주문번호·금액의 진짜 출처는 토스다.
	 *
	 * <p><b>웹훅 위조 방어의 핵심.</b> 웹훅 본문은 공개 경로로 아무나 보낼 수 있어 믿지 않고,
	 * 본문의 {@code paymentKey} 로 여기서 토스에 다시 물어본다. 시크릿 키는 서버에만 있으므로
	 * 위조자는 이 응답을 흉내 낼 수 없다. 실재하지 않는 키·조회 실패는 {@link PaymentException}
	 * 으로 던져, 호출부가 확정을 보류하게 한다. 승인 응답과 필드 모양이 같아 같은 DTO 를 쓴다.
	 */
	public TossConfirmResponse getPayment(String paymentKey) {
		return restClient.get()
				.uri(GET_PATH, paymentKey)
				.retrieve()
				.onStatus(status -> status.isError(), (request, response) -> {
					TossError error = readError(response.getBody());
					log.warn("토스 결제 조회 실패 — status={} code={} message={}",
							response.getStatusCode(), error.code(), error.message());
					throw new PaymentException(error.code(), error.message());
				})
				.body(TossConfirmResponse.class);
	}

	/** 오류 바디를 읽되, 형식이 어긋나도 죽지 않고 일반 코드로 감싼다. */
	private TossError readError(java.io.InputStream body) {
		try {
			TossError parsed = OBJECT_MAPPER.readValue(body, TossError.class);
			if (parsed != null && parsed.code() != null) {
				return parsed;
			}
		} catch (Exception ignored) {
			// 형식 불명 오류 바디 — 아래 기본값으로 떨어진다.
		}
		return new TossError("PAYMENT_APPROVAL_FAILED", "결제 승인에 실패했습니다.");
	}

	private static final com.fasterxml.jackson.databind.ObjectMapper OBJECT_MAPPER =
			new com.fasterxml.jackson.databind.ObjectMapper();

	/** 토스 오류 응답 {@code {code, message}}. */
	@JsonIgnoreProperties(ignoreUnknown = true)
	private record TossError(String code, String message) {
	}
}
