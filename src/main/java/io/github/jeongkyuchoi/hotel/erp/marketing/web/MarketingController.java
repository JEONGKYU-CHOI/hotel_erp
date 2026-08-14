package io.github.jeongkyuchoi.hotel.erp.marketing.web;

import io.github.jeongkyuchoi.hotel.erp.auth.service.MemberAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 마케팅 수신거부(옵트아웃). 추천 메일 하단 링크에서 호출된다.
 *
 * <p>이메일 링크는 브라우저에서 그대로 열리므로 JSON 이 아니라 사람이 읽을 HTML 을 돌려준다.
 * 인증 주체가 없고 토큰만으로 처리하므로 stateless API 체인(permitAll)에 둔다.
 *
 * <p>GET 이 상태를 바꾸는 것은 일반적으로 지양하지만, 메일 클라이언트가 링크를 클릭으로만
 * 여는 수신거부에서는 관례적으로 허용된다. 처리는 토큰 기준 멱등이다.
 */
@RestController
@RequiredArgsConstructor
public class MarketingController {

	private final MemberAuthService memberAuthService;

	@GetMapping(value = "/api/marketing/unsubscribe", produces = MediaType.TEXT_HTML_VALUE)
	public String unsubscribe(@RequestParam("token") String token) {
		memberAuthService.unsubscribe(token);
		// 토큰 유무를 응답으로 구분하지 않는다 — 항상 같은 완료 화면.
		return """
				<!doctype html>
				<html lang="ko"><head><meta charset="utf-8">
				<meta name="viewport" content="width=device-width, initial-scale=1">
				<title>수신거부 완료</title></head>
				<body style="font-family:system-ui,sans-serif;max-width:520px;margin:80px auto;padding:0 20px;text-align:center;color:#222">
				<h1 style="font-size:20px">수신거부가 완료되었습니다</h1>
				<p style="color:#666;line-height:1.6">앞으로 추천 메일을 보내드리지 않습니다.<br>
				예약 확인 등 거래 관련 안내 메일은 계속 발송될 수 있습니다.</p>
				</body></html>
				""";
	}
}
