import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api } from '../api/client.js'
import { startPayment } from '../payments.js'
import { useAuth } from '../auth/AuthContext.jsx'
import FolioPanel from './FolioPanel.jsx'
import HoldCountdown from '../components/HoldCountdown.jsx'
import { SkeletonResList } from '../components/Skeleton.jsx'
import { useI18n } from '../i18n/I18nContext.jsx'

// 백엔드 상태코드 → i18n 키. 색은 .status-badge.s-<STATUS> 컨벤션 그대로(booking.css).
const STATUS_KEY = {
  HOLD: 'status.hold',
  CONFIRMED: 'status.confirmed',
  CHECKED_IN: 'status.checkedIn',
  CHECKED_OUT: 'status.checkedOut',
  CANCELLED: 'status.cancelled',
  EXPIRED: 'status.expired',
  NO_SHOW: 'status.noShow',
}

// 로그인 회원의 예약 목록(GET /api/me/reservations). 로그인 안 했으면 로그인으로 보낸다.
export default function MyReservationsPage() {
  const { member, loading: authLoading } = useAuth()
  const navigate = useNavigate()
  const { t } = useI18n()
  const money = (v) => t('fmt.currency', { amount: Number(v).toLocaleString() })
  const statusLabel = (s) => (STATUS_KEY[s] ? t(STATUS_KEY[s]) : s)

  const [list, setList] = useState(null)
  const [error, setError] = useState(null)
  const [openNo, setOpenNo] = useState(null) // 청구서를 펼친 예약번호
  const [payError, setPayError] = useState(null)
  const [cancelMsg, setCancelMsg] = useState(null)
  const [cancellingNo, setCancellingNo] = useState(null)

  // 결제 대기(HOLD) 예약을 나중에 결제한다. 성공하면 토스가 /payment/success 로 넘긴다.
  async function pay(r) {
    setPayError(null)
    try {
      await startPayment({
        reservationNo: r.reservationNo,
        amount: r.totalAmount,
        orderName: `${r.roomTypeName} ${t('fmt.nights', { n: r.nights })}`,
      })
    } catch (e) {
      setPayError(e.message || t('book.payErr'))
    }
  }

  // 예약 취소. 로그인 회원 경로(토큰으로 소유 확인). 결제된 예약은 요금정책에 따라 환불까지 처리.
  async function cancel(r) {
    if (!window.confirm(t('lookup.confirmCancel'))) return
    setCancelMsg(null)
    setCancellingNo(r.reservationNo)
    try {
      const res = await api.cancel(r.reservationNo)
      setCancelMsg(res.refunded
        ? t('mine.cancel.refunded', { no: r.reservationNo, penalty: money(res.penalty), refund: money(res.refund) })
        : res.basis === 'NON_REFUNDABLE'
          ? t('mine.cancel.nonRefundable', { no: r.reservationNo })
          : t('mine.cancel.done', { no: r.reservationNo }))
      setList(await api.myReservations()) // 목록 갱신
    } catch (e) {
      setCancelMsg('⚠ ' + e.message)
    } finally {
      setCancellingNo(null)
    }
  }

  // 목록 로드 — 에러 재시도에서도 다시 부를 수 있게 함수로 뺀다.
  function load() {
    setError(null)
    setList(null)
    api.myReservations()
      .then(setList)
      .catch((e) => setError(e.message))
  }

  useEffect(() => {
    if (authLoading) return
    if (!member) {
      navigate('/member-login', { replace: true, state: { from: '/my-reservations' } })
      return
    }
    load()
  }, [member, authLoading, navigate])

  // 로딩 — 목록 자리에 스켈레톤을 깔아 화면이 비지 않게 한다.
  if (authLoading || (!list && !error)) {
    return (
      <div className="card">
        <h1>{t('mine.title')}</h1>
        <SkeletonResList count={3} />
      </div>
    )
  }

  // 에러 — 사람이 읽을 메시지 + 다시 시도.
  if (error) {
    return (
      <div className="card">
        <h1>{t('mine.title')}</h1>
        <div className="state-block">
          <p className="error">⚠ {t('mine.loadError', { msg: error })}</p>
          <button type="button" className="cta" onClick={load}>{t('mine.retry')}</button>
        </div>
      </div>
    )
  }

  // 빈 상태 — 안내 + 다음 행동 유도.
  if (list.length === 0) {
    return (
      <div className="card">
        <h1>{t('mine.title')}</h1>
        <div className="state-block">
          <div className="state-emoji" aria-hidden="true">🗓️</div>
          <p className="muted">{t('mine.empty')}</p>
          <Link to="/" className="cta">{t('mine.searchRooms')}</Link>
        </div>
      </div>
    )
  }

  return (
    <div className="card">
      <h1>내 예약</h1>
      <ul className="res-list">
        {list?.map((r) => (
            <li key={r.reservationNo} className="res-item">
              <div className="res-head">
                <span className="res-no">{r.reservationNo}</span>
                <span className={`status-badge s-${r.status}`}>{statusLabel(r.status)}</span>
              </div>
              <div className="res-body">
                <strong>{r.roomTypeName}</strong>
                <span className="muted"> · {r.ratePlanName}</span>
              </div>
              <div className="res-meta muted">
                {r.checkInDate} ~ {r.checkOutDate} ({t('fmt.nights', { n: r.nights })}) ·
                {' '}{money(r.totalAmount)}
              </div>
              {r.status === 'HOLD' && r.holdExpiresAt && (
                <div className="res-meta">
                  {t('book.field.payDeadline')} <HoldCountdown expiresAt={r.holdExpiresAt} />
                </div>
              )}
              <div className="res-actions">
                <button type="button" className="linkbtn folio-toggle"
                        onClick={() => setOpenNo(openNo === r.reservationNo ? null : r.reservationNo)}>
                  {openNo === r.reservationNo ? t('mine.folioClose') : t('mine.folioOpen')}
                </button>
                {r.status === 'HOLD' && (
                  <button type="button" className="cta cta-sm" onClick={() => pay(r)}>
                    {t('book.pay')}
                  </button>
                )}
                {(r.status === 'HOLD' || r.status === 'CONFIRMED') && (
                  <button type="button" className="linkbtn danger" onClick={() => cancel(r)}
                          disabled={cancellingNo === r.reservationNo}>
                    {cancellingNo === r.reservationNo ? t('lookup.cancelling') : t('lookup.cancelBtn')}
                  </button>
                )}
              </div>
              {r.status === 'HOLD' && payError && <p className="error">⚠ {payError}</p>}
              {cancelMsg && cancelMsg.includes(r.reservationNo) && (
                <p className={cancelMsg.startsWith('⚠') ? 'error' : 'muted'}>{cancelMsg}</p>
              )}
              {openNo === r.reservationNo && <FolioPanel reservationNo={r.reservationNo} />}
            </li>
        ))}
      </ul>
    </div>
  )
}
