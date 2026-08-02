import { useEffect, useState } from 'react'
import { api } from '../api/client.js'
import { useI18n } from '../i18n/I18nContext.jsx'

// 정산 상태 → i18n 키. 백엔드 FolioResponse.Settlement 와 1:1 (D-038).
const SETTLE = {
  PAID: { key: 'folio.settle.paid', cls: 's-CHECKED_OUT' },
  OUTSTANDING: { key: 'folio.settle.outstanding', cls: 's-HOLD' },
  REFUND_DUE: { key: 'folio.settle.refundDue', cls: 's-CANCELLED' },
}

// 예약 한 건의 청구서(폴리오). 펼칠 때 GET /api/me/reservations/{no}/folio 로 조립본을 받아 온다.
export default function FolioPanel({ reservationNo }) {
  const [folio, setFolio] = useState(null)
  const [error, setError] = useState(null)
  const { t } = useI18n()
  const won = (v) => t('fmt.currency', { amount: Number(v).toLocaleString() })

  useEffect(() => {
    api.folio(reservationNo)
      .then(setFolio)
      .catch((e) => setError(e.message))
  }, [reservationNo])

  if (error) return <p className="error">⚠ {error}</p>
  if (!folio) return <p className="muted folio-loading">{t('folio.loading')}</p>

  const s = SETTLE[folio.settlement]
  const settleLabel = s ? t(s.key) : folio.settlement

  return (
    <div className="folio">
      <div className="folio-head">
        <span className="folio-title">{t('folio.title')}</span>
        <span className={`status-badge ${s?.cls || ''}`}>{settleLabel}</span>
      </div>
      <dl className="folio-lines">
        {folio.charges.map((c, i) => (
          <div key={`c${i}`}>
            <dt>{c.label}{c.date ? <span className="muted"> · {c.date}</span> : null}</dt>
            <dd>{won(c.amount)}</dd>
          </div>
        ))}
        {folio.charges.length === 0 && (
          <div><dt className="muted">{t('folio.noCharges')}</dt><dd>{won(0)}</dd></div>
        )}
        <div className="folio-sub"><dt>{t('folio.chargeTotal')}</dt><dd>{won(folio.chargeTotal)}</dd></div>
        {folio.credits.map((p, i) => (
          <div key={`p${i}`}>
            <dt>{t('folio.payment')}<span className="muted"> · {p.method || t('folio.method.card')}</span></dt>
            <dd className="credit">− {won(p.amount)}</dd>
          </div>
        ))}
        <div className="folio-sub"><dt>{t('folio.paymentTotal')}</dt><dd className="credit">− {won(folio.creditTotal)}</dd></div>
        <div className="folio-balance"><dt>{t('folio.balance')}</dt><dd>{won(folio.balance)}</dd></div>
      </dl>
    </div>
  )
}
