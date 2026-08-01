import { createContext, useCallback, useContext, useState } from 'react'
import BookingModal from './BookingModal.jsx'

// 예약 모달을 앱 어디서든 여는 통로. openBooking(roomTypeId?, ratePlanId?) 로 특정 객실을
// (프로모션이면 요금제까지) 선택한 채 열 수 있다.
const BookingContext = createContext(null)

export function useBooking() {
  const ctx = useContext(BookingContext)
  if (!ctx) throw new Error('useBooking must be used within BookingProvider')
  return ctx
}

export function BookingProvider({ children }) {
  const [open, setOpen] = useState(false)
  const [roomTypeId, setRoomTypeId] = useState('')
  const [ratePlanId, setRatePlanId] = useState('')

  // roomTypeId: 객실 카드·객실 페이지에서 프리셀렉트. ratePlanId: 프로모션/패키지에서 요금제까지 프리셀렉트.
  const openBooking = useCallback((id = '', planId = '') => {
    setRoomTypeId(id ? String(id) : '')
    setRatePlanId(planId ? String(planId) : '')
    setOpen(true)
  }, [])
  const closeBooking = useCallback(() => setOpen(false), [])

  return (
    <BookingContext.Provider value={{ openBooking, closeBooking }}>
      {children}
      <BookingModal open={open} initialRoomTypeId={roomTypeId} initialRatePlanId={ratePlanId} onClose={closeBooking} />
    </BookingContext.Provider>
  )
}
