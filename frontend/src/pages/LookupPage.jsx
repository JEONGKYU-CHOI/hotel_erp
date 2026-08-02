import { useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { api } from '../api/client.js'
import { startPayment } from '../payments.js'
import HoldCountdown from '../components/HoldCountdown.jsx'
import { useI18n } from '../i18n/I18nContext.jsx'

// 백엔드 상태코드 → i18n 키.
const STATUS_KEY = {
  HOLD: 'status.hold',
  CONFIRMED: 'status.confirmed',
  CHECKED_IN: 'status.checkedIn',
  CHECKED_OUT: 'status.checkedOut',
  CANCELLED: 'status.cancelled',
  NO_SHOW: 'status.noShow',
  EXPIRED: 'status.expired',
}

export default function LookupPage() {
  // 결제 완료 화면 등에서 ?no=..&phone=.. 로 넘어오면 초기값으로 채운다.
  const [params] = useSearchParams()
  const [reservationNo, setReservationNo] = useState(params.get('no') || '')
  const [phone, setPhone] = useState(params.get('phone') || '')

  const [detail, setDetail] = useState(null)
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(false)
  const [payError, setPayError] = useState(null)
  const [cancelMsg, setCancelMsg] = useState(null)
  const [cancelling, setCancelling] = useState(false)
  const { t } = useI18n()
  const money = (v) => t('fmt.currency', { amount: Number(v).toLocaleString() })
  const statusLabel = (s) => (STATUS_KEY[s] ? t(STATUS_KEY[s]) : s)

  // 결제 대기(HOLD) 예약을 결제한다. 성공하면 토스가 /payment/success 로 넘긴다.
  async function pay() {
    setPayError(null)
    try {
      await startPayment({
        reservationNo: detail.reservationNo,
        amount: detail.totalAmount,
        orderName: `${detail.roomTypeName} ${t('fmt.nights', { n: detail.nights })}`,
      })
    } catch (e) {
      setPayError(e.message || t('book.payErr'))
    }
  }

  // 예약 취소. HOLD 는 바로 취소, 결제된 예약은 요금정책에 따라 환불까지 처리된다(백엔드).
  async function cancel() {
    if (!window.confirm(t('lookup.confirmCancel'))) return
    setCancelMsg(null)
    setCancelling(true)
    try {
      const res = await api.cancel(detail.reservationNo, { phone: phone.trim() })
      setCancelMsg(res.refunded
        ? t('lookup.cancel.refunded', { penalty: money(res.penalty), refund: money(res.refund) })
        : res.basis === 'NON_REFUNDABLE'
          ? t('lookup.cancel.nonRefundable')
          : t('lookup.cancel.done'))
      setDetail(await api.lookup(detail.reservationNo, phone.trim())) // 상태 갱신
    } catch (e) {
      setCancelMsg('⚠ ' + e.message)
    } finally {
      setCancelling(false)
    }
  }

  async function search(e) {
    e.preventDefault()
    setError(null)
    setDetail(null)
    setLoading(true)
    try {
      setDetail(await api.lookup(reservationNo.trim(), phone.trim()))
    } catch (err) {
      // 소유 불일치·없음 모두 404 로 온다(존재 여부를 흘리지 않는 D-028 규율).
      setError(err.status === 404
        ? t('lookup.notFound')
        : err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="card">
      <h1>{t('lookup.title')}</h1>
      <p className="muted">{t('lookup.sub')}</p>

      <form className="book-form" onSubmit={search}>
        <label>
          {t('book.field.resNo')}
          <input value={reservationNo} onChange={(e) => setReservationNo(e.target.value)}
                 placeholder="R2607..." required />
        </label>
        <label>
          {t('book.field.phone')}
          <input value={phone} onChange={(e) => setPhone(e.target.value)}
                 placeholder="010-1234-5678" required />
        </label>
        <button type="submit" className="cta" disabled={loading}>
          {loading ? t('book.searching') : t('lookup.search')}
        </button>
      </form>

      {error && <p className="error">⚠ {error}</p>}

      {detail && (
        <div className="result">
          <div className={`status-badge s-${detail.status}`}>
            {statusLabel(detail.status)}
          </div>
          <dl className="hold-summary">
            <div><dt>{t('book.field.resNo')}</dt><dd>{detail.reservationNo}</dd></div>
            <div><dt>{t('lookup.field.guest')}</dt><dd>{detail.guestName}</dd></div>
            <div><dt>{t('lookup.field.room')}</dt><dd>{detail.roomTypeName} · {detail.ratePlanName}</dd></div>
            <div><dt>{t('book.field.period')}</dt><dd>{detail.checkInDate} ~ {detail.checkOutDate} ({t('fmt.nights', { n: detail.nights })})</dd></div>
            <div><dt>{t('lookup.field.pax')}</dt><dd>{t('lookup.pax.adults', { n: detail.adults })}{detail.children > 0 ? t('lookup.pax.children', { n: detail.children }) : ''}</dd></div>
            <div><dt>{t('book.field.amount')}</dt><dd>{money(detail.totalAmount)}</dd></div>
            {detail.assignedRoomNo && (
              <div><dt>{t('lookup.field.assignedRoom')}</dt><dd>{detail.assignedRoomNo}</dd></div>
            )}
            {detail.status === 'CANCELLED' && detail.cancelReason && (
              <div><dt>{t('lookup.field.cancelReason')}</dt><dd>{detail.cancelReason}</dd></div>
            )}
            {detail.status === 'HOLD' && detail.holdExpiresAt && (
              <div>
                <dt>{t('book.field.payDeadline')}</dt>
                <dd><HoldCountdown expiresAt={detail.holdExpiresAt} /></dd>
              </div>
            )}
          </dl>
          {detail.status === 'HOLD' && (
            <>
              <p className="muted">{t('lookup.holdNote')}</p>
              <button type="button" className="cta" onClick={pay}>{t('book.pay')}</button>
              {payError && <p className="error">⚠ {payError}</p>}
            </>
          )}
          {(detail.status === 'HOLD' || detail.status === 'CONFIRMED') && (
            <button type="button" className="linkbtn danger cancel-link"
                    onClick={cancel} disabled={cancelling}>
              {cancelling ? t('lookup.cancelling') : t('lookup.cancelBtn')}
            </button>
          )}
          {cancelMsg && (
            <p className={cancelMsg.startsWith('⚠') ? 'error' : 'muted'}>{cancelMsg}</p>
          )}
        </div>
      )}

      <p><Link to="/">{t('book.backToSearch')}</Link></p>
    </div>
  )
}
