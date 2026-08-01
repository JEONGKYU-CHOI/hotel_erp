import { createContext, useCallback, useContext, useState } from 'react'
import BookingModal from './BookingModal.jsx'

// 예약 모달을 앱 어디서든 여는 통로. openBooking(roomTypeId?) 로 특정 객실을 선택한 채 열 수 있다.
const BookingContext = createContext(null)

export function useBooking() {
  const ctx = useContext(BookingContext)
  if (!ctx) throw new Error('useBooking must be used within BookingProvider')
  return ctx
}

export function BookingProvider({ children }) {
  const [open, setOpen] = useState(false)
  const [roomTypeId, setRoomTypeId] = useState('')

  const openBooking = useCallback((id = '') => {
    setRoomTypeId(id ? String(id) : '')
    setOpen(true)
  }, [])
  const closeBooking = useCallback(() => setOpen(false), [])

  return (
    <BookingContext.Provider value={{ openBooking, closeBooking }}>
      {children}
      <BookingModal open={open} initialRoomTypeId={roomTypeId} onClose={closeBooking} />
    </BookingContext.Provider>
  )
}
