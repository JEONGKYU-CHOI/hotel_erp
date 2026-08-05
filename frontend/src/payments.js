import { ANONYMOUS, loadTossPayments } from '@tosspayments/tosspayments-sdk'
import { api } from './api/client.js'

// 토스 결제창을 띄운다. 예약(HOLD)이 있는 곳이면 어디서든(예약 직후·내 예약·예약 조회) 쓴다.
//
// 성공하면 토스가 successUrl 로 리다이렉트하므로 이 함수 뒤 코드는 실행되지 않는다(페이지 전환).
// 승인·확정은 그 결과 화면(/payment/success)이 맡는다 — orderId = 우리 예약번호다.
//
// 사용자가 결제창을 닫으면 예외가 던져진다. 호출부에서 잡아 조용히 메시지로 안내한다.
export async function startPayment({ reservationNo, amount, orderName, summary }) {
  if (summary) sessionStorage.setItem('hotel.paymentSummary', JSON.stringify(summary))
  const { clientKey } = await api.paymentConfig()
  const tossPayments = await loadTossPayments(clientKey)
  const payment = tossPayments.payment({ customerKey: ANONYMOUS })
  await payment.requestPayment({
    method: 'CARD',
    amount: { currency: 'KRW', value: Number(amount) },
    orderId: reservationNo, // 우리 예약번호 = 토스 orderId
    orderName,
    successUrl: window.location.origin + '/payment/success',
    failUrl: window.location.origin + '/payment/fail',
  })
}
