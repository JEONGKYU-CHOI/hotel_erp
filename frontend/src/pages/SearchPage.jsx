import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api/client.js'

// 오늘 기준 기본 날짜(내일 체크인, 1박)를 YYYY-MM-DD 로.
function isoDate(offsetDays) {
  const d = new Date()
  d.setDate(d.getDate() + offsetDays)
  return d.toISOString().slice(0, 10)
}

export default function SearchPage() {
  const [roomTypes, setRoomTypes] = useState([])
  const [roomTypeId, setRoomTypeId] = useState('')
  const [checkIn, setCheckIn] = useState(isoDate(1))
  const [checkOut, setCheckOut] = useState(isoDate(2))
  const [adults, setAdults] = useState(2)

  const [result, setResult] = useState(null)
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()

  // 첫 진입에 판매 중인 객실타입을 불러온다.
  useEffect(() => {
    api.roomTypes()
      .then((types) => {
        setRoomTypes(types)
        if (types.length > 0) setRoomTypeId(String(types[0].id))
      })
      .catch((e) => setError(e.message))
  }, [])

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

  return (
    <>
      <section className="hero">
        <div className="hero-inner">
          <div className="hero-eyebrow">THE STAY · 도심 속 휴식</div>
          <h1 className="hero-title">머무는 순간, 여행이 됩니다</h1>
          <p className="hero-sub">
            날짜만 고르면 됩니다. 남은 객실을 실시간으로 확인하고 바로 예약하세요.
          </p>
        </div>
      </section>

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
          <span>인원</span>
          <select value={adults} onChange={(e) => setAdults(e.target.value)}>
            {[1, 2, 3, 4].map((n) => (
              <option key={n} value={n}>성인 {n}명</option>
            ))}
          </select>
        </label>

        <button type="submit" className="bb-go" disabled={loading || !roomTypeId}>
          {loading ? '조회 중…' : '객실 찾기'}
        </button>
      </form>

      <div className="section">
      {error && <p className="error">⚠ {error}</p>}

      {result && (
        <div className="result">
          <div className={`summary ${result.bookableQty > 0 ? 'ok' : 'soldout'}`}>
            {result.bookableQty > 0
              ? `예약 가능 — ${result.nightCount}박, 남은 객실 ${result.bookableQty}개`
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
              onClick={() =>
                navigate('/book', {
                  state: {
                    roomTypeId,
                    roomTypeName: roomTypes.find((t) => String(t.id) === String(roomTypeId))?.name,
                    checkIn,
                    checkOut,
                    adults,
                  },
                })
              }
            >
              이 조건으로 예약하기 →
            </button>
          )}
        </div>
      )}
      </div>
    </>
  )
}
