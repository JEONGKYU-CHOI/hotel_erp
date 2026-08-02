import { useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { api } from '../api/client.js'
import { startPayment } from '../payments.js'
import HoldCountdown from '../components/HoldCountdown.jsx'

// 예약번호 → 한국어 상태 라벨.
const STATUS_LABEL = {
  HOLD: '임시 예약 (결제 대기)',
  CONFIRMED: '예약 확정',
  CHECKED_IN: '투숙 중',
  CHECKED_OUT: '퇴실 완료',
  CANCELLED: '취소됨',
  NO_SHOW: '노쇼',
  EXPIRED: '만료됨 (미결제)',
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

  // 결제 대기(HOLD) 예약을 결제한다. 성공하면 토스가 /payment/success 로 넘긴다.
  async function pay() {
    setPayError(null)
    try {
      await startPayment({
        reservationNo: detail.reservationNo,
        amount: detail.totalAmount,
        orderName: `${detail.roomTypeName} ${detail.nights}박`,
      })
    } catch (e) {
      setPayError(e.message || '결제를 시작할 수 없습니다.')
    }
  }

  // 예약 취소. HOLD 는 바로 취소, 결제된 예약은 요금정책에 따라 환불까지 처리된다(백엔드).
  async function cancel() {
    if (!window.confirm('예약을 취소하시겠습니까?\n결제된 예약은 요금정책에 따라 환불됩니다.')) return
    setCancelMsg(null)
    setCancelling(true)
    try {
      const res = await api.cancel(detail.reservationNo, { phone: phone.trim() })
      const won = (v) => Number(v).toLocaleString()
      setCancelMsg(res.refunded
        ? `취소되었습니다. 위약금 ${won(res.penalty)}원 · ${won(res.refund)}원이 환불 처리되었습니다.`
        : res.basis === 'NON_REFUNDABLE'
          ? '취소되었습니다. 환불 불가 요금제라 환불 금액은 없습니다.'
          : '취소되었습니다.')
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
        ? '일치하는 예약이 없습니다. 예약번호와 연락처를 확인하세요.'
        : err.message)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="card">
      <h1>예약 조회</h1>
      <p className="muted">예약번호와 예약 시 입력한 연락처로 조회합니다.</p>

      <form className="book-form" onSubmit={search}>
        <label>
          예약번호
          <input value={reservationNo} onChange={(e) => setReservationNo(e.target.value)}
                 placeholder="R2607..." required />
        </label>
        <label>
          연락처
          <input value={phone} onChange={(e) => setPhone(e.target.value)}
                 placeholder="010-1234-5678" required />
        </label>
        <button type="submit" className="cta" disabled={loading}>
          {loading ? '조회 중…' : '조회'}
        </button>
      </form>

      {error && <p className="error">⚠ {error}</p>}

      {detail && (
        <div className="result">
          <div className={`status-badge s-${detail.status}`}>
            {STATUS_LABEL[detail.status] || detail.status}
          </div>
          <dl className="hold-summary">
            <div><dt>예약번호</dt><dd>{detail.reservationNo}</dd></div>
            <div><dt>예약자</dt><dd>{detail.guestName}</dd></div>
            <div><dt>객실</dt><dd>{detail.roomTypeName} · {detail.ratePlanName}</dd></div>
            <div><dt>기간</dt><dd>{detail.checkInDate} ~ {detail.checkOutDate} ({detail.nights}박)</dd></div>
            <div><dt>인원</dt><dd>성인 {detail.adults}{detail.children > 0 ? ` · 아동 ${detail.children}` : ''}</dd></div>
            <div><dt>결제 금액</dt><dd>{Number(detail.totalAmount).toLocaleString()}원</dd></div>
            {detail.assignedRoomNo && (
              <div><dt>배정 호실</dt><dd>{detail.assignedRoomNo}</dd></div>
            )}
            {detail.status === 'CANCELLED' && detail.cancelReason && (
              <div><dt>취소 사유</dt><dd>{detail.cancelReason}</dd></div>
            )}
            {detail.status === 'HOLD' && detail.holdExpiresAt && (
              <div>
                <dt>결제 마감까지</dt>
                <dd><HoldCountdown expiresAt={detail.holdExpiresAt} /></dd>
              </div>
            )}
          </dl>
          {detail.status === 'HOLD' && (
            <>
              <p className="muted">아직 결제 전입니다. 남은 시간 안에 결제하면 예약이 확정됩니다.</p>
              <button type="button" className="cta" onClick={pay}>결제하기</button>
              {payError && <p className="error">⚠ {payError}</p>}
            </>
          )}
          {(detail.status === 'HOLD' || detail.status === 'CONFIRMED') && (
            <button type="button" className="linkbtn danger cancel-link"
                    onClick={cancel} disabled={cancelling}>
              {cancelling ? '취소 중…' : '예약 취소'}
            </button>
          )}
          {cancelMsg && (
            <p className={cancelMsg.startsWith('⚠') ? 'error' : 'muted'}>{cancelMsg}</p>
          )}
        </div>
      )}

      <p><Link to="/">← 검색으로</Link></p>
    </div>
  )
}
