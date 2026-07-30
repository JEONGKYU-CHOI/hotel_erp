import { useEffect, useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { api } from '../api/client.js'

// 토스 결제창이 성공 시 리다이렉트하는 착지점. 쿼리의 paymentKey/orderId/amount 로
// 백엔드 승인(/api/payments/confirm)을 호출해 예약을 확정한다. 금액 위변조·멱등은 서버가 막는다.
export default function PaymentSuccessPage() {
  const [params] = useSearchParams()
  const [status, setStatus] = useState('confirming') // confirming | done | error
  const [result, setResult] = useState(null)
  const [error, setError] = useState(null)

  // StrictMode 는 이펙트를 두 번 부른다 — 승인 호출이 두 번 나가지 않게 막는다
  // (서버도 멱등이지만 불필요한 두 번째 요청을 아낀다).
  const fired = useRef(false)

  useEffect(() => {
    if (fired.current) return
    fired.current = true

    const paymentKey = params.get('paymentKey')
    const orderId = params.get('orderId')
    const amount = params.get('amount')
    if (!paymentKey || !orderId || !amount) {
      setStatus('error')
      setError('결제 정보가 올바르지 않습니다.')
      return
    }

    api.confirmPayment({ paymentKey, orderId, amount: Number(amount) })
      .then((res) => {
        setResult(res)
        setStatus('done')
      })
      .catch((err) => {
        setError(err.message)
        setStatus('error')
      })
  }, [params])

  if (status === 'confirming') {
    return <div className="card"><h1>결제 확인 중…</h1><p className="muted">잠시만 기다려 주세요.</p></div>
  }

  if (status === 'error') {
    return (
      <div className="card">
        <h1>결제 확정 실패</h1>
        <p className="error">⚠ {error}</p>
        <p className="muted">결제가 승인됐다면 자동 정합되거나 고객센터에서 확인됩니다.</p>
        <p><Link to="/">← 처음으로</Link></p>
      </div>
    )
  }

  return (
    <div className="card">
      <h1>✅ 예약이 확정되었습니다</h1>
      <dl className="hold-summary">
        <div><dt>예약번호</dt><dd>{result.orderId}</dd></div>
        <div><dt>결제 금액</dt><dd>{Number(result.amount).toLocaleString()}원</dd></div>
        <div><dt>상태</dt><dd>{result.status}</dd></div>
      </dl>
      <p>
        <Link to={`/lookup?no=${encodeURIComponent(result.orderId)}`}>예약 상세 보기</Link>
        {'  ·  '}
        <Link to="/">처음으로</Link>
      </p>
    </div>
  )
}
