import { useEffect, useRef, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { api } from '../api/client.js'
import { useI18n } from '../i18n/I18nContext.jsx'

// 토스 결제창이 성공 시 리다이렉트하는 착지점. 쿼리의 paymentKey/orderId/amount 로
// 백엔드 승인(/api/payments/confirm)을 호출해 예약을 확정한다. 금액 위변조·멱등은 서버가 막는다.
export default function PaymentSuccessPage() {
  const [params] = useSearchParams()
  const [status, setStatus] = useState('confirming') // confirming | done | error
  const [result, setResult] = useState(null)
  const [error, setError] = useState(null)
  const { t } = useI18n()

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
      setError(t('pay.badInfo'))
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
    return <div className="card"><h1>{t('pay.confirming.title')}</h1><p className="muted">{t('pay.confirming.sub')}</p></div>
  }

  if (status === 'error') {
    return (
      <div className="card">
        <h1>{t('pay.error.title')}</h1>
        <p className="error">⚠ {error}</p>
        <p className="muted">{t('pay.error.note')}</p>
        <p><Link to="/">{t('pay.toHome')}</Link></p>
      </div>
    )
  }

  return (
    <div className="card">
      <h1>{t('pay.success.title')}</h1>
      <dl className="hold-summary">
        <div><dt>{t('book.field.resNo')}</dt><dd>{result.orderId}</dd></div>
        <div><dt>{t('book.field.amount')}</dt><dd>{t('fmt.currency', { amount: Number(result.amount).toLocaleString() })}</dd></div>
        <div><dt>{t('book.field.status')}</dt><dd>{result.status}</dd></div>
      </dl>
      <p>
        <Link to={`/lookup?no=${encodeURIComponent(result.orderId)}`}>{t('pay.viewDetail')}</Link>
        {'  ·  '}
        <Link to="/">{t('pay.home')}</Link>
      </p>
    </div>
  )
}
