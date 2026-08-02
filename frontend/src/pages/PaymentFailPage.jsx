import { Link, useSearchParams } from 'react-router-dom'
import { useI18n } from '../i18n/I18nContext.jsx'

// 토스 결제창이 실패/취소 시 리다이렉트하는 착지점. 예약은 HOLD 로 남아 있다가 만료 스케줄러가
// 회수한다 — 여기서 별도 조치는 하지 않고 사유만 안내한다.
export default function PaymentFailPage() {
  const [params] = useSearchParams()
  const code = params.get('code')
  const message = params.get('message')
  const { t } = useI18n()

  return (
    <div className="card">
      <h1>{t('pay.fail.title')}</h1>
      <p className="error">⚠ {message || t('pay.fail.default')}</p>
      {code && <p className="muted">{t('pay.fail.code', { code })}</p>}
      <p className="muted">{t('pay.fail.note')}</p>
      <p><Link to="/">{t('pay.searchAgain')}</Link></p>
    </div>
  )
}
