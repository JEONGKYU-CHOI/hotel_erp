import { Link, useSearchParams } from 'react-router-dom'

// 토스 결제창이 실패/취소 시 리다이렉트하는 착지점. 예약은 HOLD 로 남아 있다가 만료 스케줄러가
// 회수한다 — 여기서 별도 조치는 하지 않고 사유만 안내한다.
export default function PaymentFailPage() {
  const [params] = useSearchParams()
  const code = params.get('code')
  const message = params.get('message')

  return (
    <div className="card">
      <h1>결제가 완료되지 않았습니다</h1>
      <p className="error">⚠ {message || '결제가 취소되었거나 실패했습니다.'}</p>
      {code && <p className="muted">오류 코드: {code}</p>}
      <p className="muted">임시 예약은 잠시 유지되며, 결제하지 않으면 자동 취소됩니다.</p>
      <p><Link to="/">← 다시 검색하기</Link></p>
    </div>
  )
}
