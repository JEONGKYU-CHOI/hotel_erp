import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api/client.js'

function isoDate(offsetDays) {
  const d = new Date()
  d.setDate(d.getDate() + offsetDays)
  return d.toISOString().slice(0, 10)
}

// 예약 검색 폼 — 홈 히어로 바(variant="bar")와 예약 모달(variant="modal")이 공유한다.
// 어디서 열든 같은 흐름: 객실·날짜·인원 → 검색 → 이 조건으로 예약(/book).
export default function BookingForm({ initialRoomTypeId = '', initialRatePlanId = '', variant = 'bar', onDone }) {
  const [roomTypes, setRoomTypes] = useState([])
  const [roomTypeId, setRoomTypeId] = useState(initialRoomTypeId ? String(initialRoomTypeId) : '')
  const [checkIn, setCheckIn] = useState(isoDate(1))
  const [checkOut, setCheckOut] = useState(isoDate(2))
  const [adults, setAdults] = useState(2)
  const [children, setChildren] = useState(0)
  const [result, setResult] = useState(null)
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()

  useEffect(() => {
    api.roomTypes()
      .then((types) => {
        setRoomTypes(types)
        setRoomTypeId((prev) => prev || (types[0] ? String(types[0].id) : ''))
      })
      .catch((e) => setError(e.message))
  }, [])

  // 외부에서 특정 객실로 열면(카드·객실 페이지) 그 객실을 선택 상태로 반영한다.
  useEffect(() => {
    if (initialRoomTypeId) setRoomTypeId(String(initialRoomTypeId))
  }, [initialRoomTypeId])

  // 선택 객실의 최대 수용인원. 백엔드가 최종 방어하지만(초과 시 400), 여기서 애초에
  // 초과 조합을 못 고르게 옵션을 제한한다.
  const selectedRoom = roomTypes.find((t) => String(t.id) === String(roomTypeId))
  const maxOcc = selectedRoom?.maxOccupancy || 4

  // 객실을 바꿔 정원이 줄면 현재 성인+아동이 넘칠 수 있다 — 넘치면 정원 안으로 되당긴다.
  useEffect(() => {
    if (Number(adults) > maxOcc) setAdults(maxOcc)
    if (Number(adults) + Number(children) > maxOcc) {
      setChildren(Math.max(0, maxOcc - Number(adults)))
    }
  }, [maxOcc]) // eslint-disable-line react-hooks/exhaustive-deps

  async function search(e) {
    e.preventDefault()
    setError(null)
    setResult(null)
    if (checkOut <= checkIn) {
      setError('체크아웃은 체크인 다음날 이후여야 합니다.')
      return
    }
    setLoading(true)
    try {
      setResult(await api.availability(roomTypeId, checkIn, checkOut))
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  const selectedName = roomTypes.find((t) => String(t.id) === String(roomTypeId))?.name

  return (
    <div className={`bookingform bookingform-${variant}`}>
      <form className="booking-bar" onSubmit={search}>
        <label className="bb-field">
          <span>객실 타입</span>
          <select value={roomTypeId} onChange={(e) => setRoomTypeId(e.target.value)}>
            {roomTypes.length === 0 && <option value="">불러오는 중…</option>}
            {roomTypes.map((t) => (
              <option key={t.id} value={t.id}>{t.name}</option>
            ))}
          </select>
        </label>
        <label className="bb-field">
          <span>체크인</span>
          <input type="date" value={checkIn} onChange={(e) => setCheckIn(e.target.value)} />
        </label>
        <label className="bb-field">
          <span>체크아웃</span>
          <input type="date" value={checkOut} onChange={(e) => setCheckOut(e.target.value)} />
        </label>
        <label className="bb-field">
          <span>성인</span>
          <select value={adults} onChange={(e) => setAdults(Number(e.target.value))}>
            {Array.from({ length: maxOcc }, (_, i) => i + 1).map((n) => (
              <option key={n} value={n}>성인 {n}명</option>
            ))}
          </select>
        </label>
        <label className="bb-field">
          <span>아동</span>
          <select value={children} onChange={(e) => setChildren(Number(e.target.value))}>
            {Array.from({ length: Math.max(1, maxOcc - Number(adults) + 1) }, (_, i) => i).map((n) => (
              <option key={n} value={n}>아동 {n}명</option>
            ))}
          </select>
        </label>
        <button type="submit" className="bb-go" disabled={loading || !roomTypeId}>
          {loading ? '조회 중…' : '객실 찾기'}
        </button>
      </form>

      {(error || result) && (
        <div className="bookingform-result">
          {error && <p className="error">⚠ {error}</p>}
          {result && (
            <div className="result">
              <div className={`summary ${result.bookableQty > 0 ? 'ok' : 'soldout'}`}>
                {result.bookableQty > 0
                  ? `${selectedName || '선택 객실'} · 예약 가능 — ${result.nightCount}박, 남은 객실 ${result.bookableQty}개`
                  : '이 기간은 예약 마감입니다.'}
              </div>
              <table className="nights">
                <thead>
                  <tr><th>숙박일</th><th>가용 객실</th></tr>
                </thead>
                <tbody>
                  {result.nights.map((n) => (
                    <tr key={n.stayDate}>
                      <td>{n.stayDate}</td>
                      <td className={n.availableQty > 0 ? '' : 'zero'}>{n.availableQty}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
              {result.bookableQty > 0 && (
                <button
                  type="button"
                  className="cta"
                  onClick={() => {
                    navigate('/book', {
                      state: { roomTypeId, roomTypeName: selectedName, checkIn, checkOut, adults, children, maxOccupancy: maxOcc, ratePlanId: initialRatePlanId || undefined },
                    })
                    onDone?.()
                  }}
                >
                  이 조건으로 예약하기 →
                </button>
              )}
            </div>
          )}
        </div>
      )}
    </div>
  )
}
