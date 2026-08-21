package io.github.jeongkyuchoi.hotel.erp.backoffice.web;

import io.github.jeongkyuchoi.hotel.erp.recommendation.DispatchResult;
import io.github.jeongkyuchoi.hotel.erp.recommendation.RecommendationMailDispatcher;
import io.github.jeongkyuchoi.hotel.erp.recommendation.RecommendationMailLogRepository;
import java.time.YearMonth;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * 추천 메일 수동 발송(백오피스). 월간 스케줄러({@code RecommendationMailScheduler})와 같은
 * 배치를 관리자가 즉시 실행할 수 있게 한다 — 시연에서 한 달을 기다리지 않고 바로 결과를 보기 위함.
 *
 * <p>발송은 회원 × 발송월당 멱등이라, 수동 실행을 여러 번 눌러도(또는 스케줄러와 겹쳐도)
 * 같은 달 같은 회원에게 이중 발송되지 않는다 — 두 번째부터는 "건너뜀"으로 집계된다.
 */
@Controller
@RequiredArgsConstructor
public class MarketingAdminController {

	/** 단일 테넌트 데모(백오피스 서비스들과 같은 상수). */
	private static final Long DEMO_TENANT = 1L;

	private final RecommendationMailDispatcher dispatcher;
	private final RecommendationMailLogRepository logRepository;

	@GetMapping("/admin/marketing")
	public String page(Model model) {
		model.addAttribute("currentMonth", YearMonth.now().toString());
		model.addAttribute("sentTotal", logRepository.count());
		return "admin/marketing";
	}

	/** 이번 달 추천 메일을 즉시 발송한다. 결과 요약을 플래시로 띄우고 화면으로 되돌아간다. */
	@PostMapping("/admin/marketing/run")
	public String run(RedirectAttributes redirect) {
		DispatchResult result = dispatcher.dispatch(DEMO_TENANT, YearMonth.now());
		redirect.addFlashAttribute("flashSuccess", String.format(
				"추천 메일 발송 완료 — 대상 %d명 중 발송 %d · 건너뜀 %d · 실패 %d",
				result.candidates(), result.sent(), result.skipped(), result.failed()));
		return "redirect:/admin/marketing";
	}
}
