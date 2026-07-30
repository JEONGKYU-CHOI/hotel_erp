import { useEffect, useState } from 'react'
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

  const [result, setResult] = useState(null)
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(false)

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
    <div className="card">
      <h1>객실 검색</h1>

      <form className="search-form" onSubmit={search}>
        <label>
          객실 타입
          <select value={roomTypeId} onChange={(e) => setRoomTypeId(e.target.value)}>
            {roomTypes.length === 0 && <option value="">불러오는 중…</option>}
            {roomTypes.map((t) => (
              <option key={t.id} value={t.id}>
                {t.name} (기준 {t.standardOccupancy}인 · 최대 {t.maxOccupancy}인)
              </option>
            ))}
          </select>
        </label>

        <label>
          체크인
          <input type="date" value={checkIn} onChange={(e) => setCheckIn(e.target.value)} />
        </label>

        <label>
          체크아웃
          <input type="date" value={checkOut} onChange={(e) => setCheckOut(e.target.value)} />
        </label>

        <button type="submit" disabled={loading || !roomTypeId}>
          {loading ? '조회 중…' : '가용 조회'}
        </button>
      </form>

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
          {/* 다음 단계: bookableQty > 0 이면 여기에 "예약하기(HOLD)" 버튼을 붙인다. */}
        </div>
      )}
    </div>
  )
}
