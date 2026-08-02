import { useEffect, useRef } from 'react'
import BookingForm from './BookingForm.jsx'
import { useI18n } from '../i18n/I18nContext.jsx'

// 예약 패널 — 데스크톱은 중앙 모달, 모바일은 하단 바텀시트(CSS로 분기).
export default function BookingModal({ open, initialRoomTypeId, initialRatePlanId, onClose }) {
  const closeRef = useRef(null)
  const { t } = useI18n()

  // 열려 있는 동안 배경 스크롤을 막고, ESC 로 닫는다. 열 때 포커스를 모달 안으로
  // 옮기고, 닫을 때 직전에 포커스돼 있던 요소(예약하기 버튼 등)로 되돌린다 —
  // 키보드·스크린리더 사용자가 모달을 벗어나지 않게 한다.
  useEffect(() => {
    if (!open) return
    const restoreTo = document.activeElement
    const onKey = (e) => { if (e.key === 'Escape') onClose() }
    document.body.style.overflow = 'hidden'
    window.addEventListener('keydown', onKey)
    closeRef.current?.focus()
    return () => {
      document.body.style.overflow = ''
      window.removeEventListener('keydown', onKey)
      if (restoreTo instanceof HTMLElement) restoreTo.focus()
    }
  }, [open, onClose])

  if (!open) return null

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="booking-modal" role="dialog" aria-modal="true" aria-labelledby="booking-modal-title" onClick={(e) => e.stopPropagation()}>
        <div className="modal-head">
          <h3 className="modal-title" id="booking-modal-title">{t('modal.title')}</h3>
          <button type="button" className="modal-close" onClick={onClose} aria-label={t('modal.close')} ref={closeRef}>✕</button>
        </div>
        <p className="modal-hint">{t('modal.hint')}</p>
        <BookingForm variant="modal" initialRoomTypeId={initialRoomTypeId} initialRatePlanId={initialRatePlanId} onDone={onClose} />
      </div>
    </div>
  )
}
