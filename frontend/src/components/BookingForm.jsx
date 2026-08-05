import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api/client.js'
import { useI18n } from '../i18n/I18nContext.jsx'
import { roomName } from '../i18n/messages.js'

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
  const [error, setError] = useState(null)
  const navigate = useNavigate()
  const { t, lang } = useI18n()

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

  function search(e) {
    e.preventDefault()
    setError(null)
    if (checkOut <= checkIn) {
      setError(t('book.err.checkoutAfter'))
      return
    }
    navigate('/book', {
      state: {
        roomTypeId,
        roomTypeName: selectedRt?.name,
        roomTypeNameEn: selectedRt?.nameEn,
        roomTypeCode: selectedRt?.code,
        imageUrls: selectedRt?.imageUrls || [],
        checkIn,
        checkOut,
        adults,
        children,
        maxOccupancy: maxOcc,
        ratePlanId: initialRatePlanId || undefined,
      },
    })
    onDone?.()
  }

  const selectedRt = roomTypes.find((rt) => String(rt.id) === String(roomTypeId))
  return (
    <div className={`bookingform bookingform-${variant}`}>
      <form className="booking-bar" onSubmit={search}>
        <label className="bb-field">
          <span>{t('book.field.roomType')}</span>
          <select value={roomTypeId} onChange={(e) => setRoomTypeId(e.target.value)}>
            {roomTypes.length === 0 && <option value="">{t('book.loading')}</option>}
            {roomTypes.map((rt) => (
              <option key={rt.id} value={rt.id}>{roomName(lang, rt)}</option>
            ))}
          </select>
        </label>
        <label className="bb-field">
          <span>{t('book.field.checkIn')}</span>
          {/* 오늘부터 선택 가능(당일 예약). 지난 날짜는 막는다. */}
          <input type="date" min={isoDate(0)} value={checkIn} onChange={(e) => setCheckIn(e.target.value)} />
        </label>
        <label className="bb-field">
          <span>{t('book.field.checkOut')}</span>
          {/* 최소 1박 — 체크인 다음날부터. */}
          <input type="date" min={isoDate(0)} value={checkOut} onChange={(e) => setCheckOut(e.target.value)} />
        </label>
        <label className="bb-field">
          <span>{t('book.field.adults')}</span>
          <select value={adults} onChange={(e) => setAdults(Number(e.target.value))}>
            {Array.from({ length: maxOcc }, (_, i) => i + 1).map((n) => (
              <option key={n} value={n}>{t('book.adultsN', { n })}</option>
            ))}
          </select>
        </label>
        <label className="bb-field">
          <span>{t('book.field.children')}</span>
          <select value={children} onChange={(e) => setChildren(Number(e.target.value))}>
            {Array.from({ length: Math.max(1, maxOcc - Number(adults) + 1) }, (_, i) => i).map((n) => (
              <option key={n} value={n}>{t('book.childrenN', { n })}</option>
            ))}
          </select>
        </label>
        <button type="submit" className="bb-go" disabled={!roomTypeId}>
          {t('cta.reserve')}
        </button>
      </form>

      {error && (
        <div className="bookingform-result">
          <p className="error">⚠ {error}</p>
        </div>
      )}
    </div>
  )
}
