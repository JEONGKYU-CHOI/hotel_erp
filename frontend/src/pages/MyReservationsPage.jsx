import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api } from '../api/client.js'
import { startPayment } from '../payments.js'
import { useAuth } from '../auth/AuthContext.jsx'
import FolioPanel from './FolioPanel.jsx'
import HoldCountdown from '../components/HoldCountdown.jsx'

// 상태별 표시 라벨. 색은 기존 .status-badge.s-<STATUS> 컨벤션을 그대로 재사용한다(booking.css).
const LABEL = {
  HOLD: '결제 대기',
  CONFIRMED: '예약 확정',
  CHECKED_IN: '체크인',
  CHECKED_OUT: '체크아웃',
  CANCELLED: '취소됨',
  EXPIRED: '만료됨',
  NO_SHOW: '노쇼',
}

// 로그인 회원의 예약 목록(GET /api/me/reservations). 로그인 안 했으면 로그인으로 보낸다.
export default function MyReservationsPage() {
  const { member, loading: authLoading } = useAuth()
  const navigate = useNavigate()

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
        orderName: `${r.roomTypeName} ${r.nights}박`,
      })
    } catch (e) {
      setPayError(e.message || '결제를 시작할 수 없습니다.')
    }
  }

  // 예약 취소. 로그인 회원 경로(토큰으로 소유 확인). 결제된 예약은 요금정책에 따라 환불까지 처리.
  async function cancel(r) {
    if (!window.confirm('예약을 취소하시겠습니까?\n결제된 예약은 요금정책에 따라 환불됩니다.')) return
    setCancelMsg(null)
    setCancellingNo(r.reservationNo)
    try {
      const res = await api.cancel(r.reservationNo)
      const won = (v) => Number(v).toLocaleString()
      setCancelMsg(res.refunded
        ? `${r.reservationNo} 취소 · 위약금 ${won(res.penalty)}원, ${won(res.refund)}원 환불 처리되었습니다.`
        : res.basis === 'NON_REFUNDABLE'
          ? `${r.reservationNo} 취소 · 환불 불가 요금제라 환불 금액은 없습니다.`
          : `${r.reservationNo} 취소되었습니다.`)
      setList(await api.myReservations()) // 목록 갱신
    } catch (e) {
      setCancelMsg('⚠ ' + e.message)
    } finally {
      setCancellingNo(null)
    }
  }

  useEffect(() => {
    if (authLoading) return
    if (!member) {
      navigate('/member-login', { replace: true, state: { from: '/my-reservations' } })
      return
    }
    api.myReservations()
      .then(setList)
      .catch((e) => setError(e.message))
  }, [member, authLoading, navigate])

  if (authLoading || (!list && !error)) return <p className="muted">불러오는 중…</p>

  return (
    <div className="card">
      <h1>내 예약</h1>
      {error && <p className="error">⚠ {error}</p>}
      {list && list.length === 0 && (
        <p className="muted">아직 예약이 없습니다. <Link to="/">객실을 검색해 보세요.</Link></p>
      )}
      <ul className="res-list">
        {list?.map((r) => (
            <li key={r.reservationNo} className="res-item">
              <div className="res-head">
                <span className="res-no">{r.reservationNo}</span>
                <span className={`status-badge s-${r.status}`}>{LABEL[r.status] || r.status}</span>
              </div>
              <div className="res-body">
                <strong>{r.roomTypeName}</strong>
                <span className="muted"> · {r.ratePlanName}</span>
              </div>
              <div className="res-meta muted">
                {r.checkInDate} ~ {r.checkOutDate} ({r.nights}박) ·
                {' '}{Number(r.totalAmount).toLocaleString()}원
              </div>
              {r.status === 'HOLD' && r.holdExpiresAt && (
                <div className="res-meta">
                  결제 마감까지 <HoldCountdown expiresAt={r.holdExpiresAt} />
                </div>
              )}
              <div className="res-actions">
                <button type="button" className="linkbtn folio-toggle"
                        onClick={() => setOpenNo(openNo === r.reservationNo ? null : r.reservationNo)}>
                  {openNo === r.reservationNo ? '청구서 닫기' : '청구서 보기'}
                </button>
                {r.status === 'HOLD' && (
                  <button type="button" className="cta cta-sm" onClick={() => pay(r)}>
                    결제하기
                  </button>
                )}
                {(r.status === 'HOLD' || r.status === 'CONFIRMED') && (
                  <button type="button" className="linkbtn danger" onClick={() => cancel(r)}
                          disabled={cancellingNo === r.reservationNo}>
                    {cancellingNo === r.reservationNo ? '취소 중…' : '예약 취소'}
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
