import { useEffect } from 'react'
import BookingForm from './BookingForm.jsx'

// 예약 패널 — 데스크톱은 중앙 모달, 모바일은 하단 바텀시트(CSS로 분기).
export default function BookingModal({ open, initialRoomTypeId, initialRatePlanId, onClose }) {
  // 열려 있는 동안 배경 스크롤을 막고, ESC 로 닫는다.
  useEffect(() => {
    if (!open) return
    const onKey = (e) => { if (e.key === 'Escape') onClose() }
    document.body.style.overflow = 'hidden'
    window.addEventListener('keydown', onKey)
    return () => {
      document.body.style.overflow = ''
      window.removeEventListener('keydown', onKey)
    }
  }, [open, onClose])

  if (!open) return null

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="booking-modal" role="dialog" aria-modal="true" aria-label="예약하기" onClick={(e) => e.stopPropagation()}>
        <div className="modal-head">
          <h3 className="modal-title">예약하기</h3>
          <button type="button" className="modal-close" onClick={onClose} aria-label="닫기">✕</button>
        </div>
        <p className="modal-hint">객실과 날짜를 고르고 잔여 객실을 확인하세요.</p>
        <BookingForm variant="modal" initialRoomTypeId={initialRoomTypeId} initialRatePlanId={initialRatePlanId} onDone={onClose} />
      </div>
    </div>
  )
}
