import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api } from '../api/client.js'
import { useAuth } from '../auth/AuthContext.jsx'

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

  useEffect(() => {
    if (authLoading) return
    if (!member) {
      navigate('/login', { replace: true, state: { from: '/my-reservations' } })
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
            </li>
        ))}
      </ul>
    </div>
  )
}
