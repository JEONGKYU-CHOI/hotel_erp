import { useEffect, useState } from 'react'
import { api } from '../api/client.js'

// 정산 상태 표시. 백엔드 FolioResponse.Settlement 와 1:1 (D-038).
const SETTLE = {
  PAID: { label: '완납', cls: 's-CHECKED_OUT' },
  OUTSTANDING: { label: '미수', cls: 's-HOLD' },
  REFUND_DUE: { label: '환불 대상', cls: 's-CANCELLED' },
}

const won = (v) => Number(v).toLocaleString() + '원'

// 예약 한 건의 청구서(폴리오). 펼칠 때 GET /api/me/reservations/{no}/folio 로 조립본을 받아 온다.
export default function FolioPanel({ reservationNo }) {
  const [folio, setFolio] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    api.folio(reservationNo)
      .then(setFolio)
      .catch((e) => setError(e.message))
  }, [reservationNo])

  if (error) return <p className="error">⚠ {error}</p>
  if (!folio) return <p className="muted folio-loading">청구서 불러오는 중…</p>

  const s = SETTLE[folio.settlement] || { label: folio.settlement, cls: '' }

  return (
    <div className="folio">
      <div className="folio-head">
        <span className="folio-title">청구서</span>
        <span className={`status-badge ${s.cls}`}>{s.label}</span>
      </div>
      <dl className="folio-lines">
        {folio.charges.map((c, i) => (
          <div key={`c${i}`}>
            <dt>{c.label}{c.date ? <span className="muted"> · {c.date}</span> : null}</dt>
            <dd>{won(c.amount)}</dd>
          </div>
        ))}
        {folio.charges.length === 0 && (
          <div><dt className="muted">청구 항목 없음</dt><dd>0원</dd></div>
        )}
        <div className="folio-sub"><dt>청구 합계</dt><dd>{won(folio.chargeTotal)}</dd></div>
        {folio.credits.map((p, i) => (
          <div key={`p${i}`}>
            <dt>결제<span className="muted"> · {p.method || '카드'}</span></dt>
            <dd className="credit">− {won(p.amount)}</dd>
          </div>
        ))}
        <div className="folio-sub"><dt>결제 합계</dt><dd className="credit">− {won(folio.creditTotal)}</dd></div>
        <div className="folio-balance"><dt>잔액</dt><dd>{won(folio.balance)}</dd></div>
      </dl>
    </div>
  )
}
