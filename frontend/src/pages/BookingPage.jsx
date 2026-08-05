import { useEffect, useRef, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { api } from '../api/client.js'
import { startPayment } from '../payments.js'
import { useAuth } from '../auth/AuthContext.jsx'
import HoldCountdown from '../components/HoldCountdown.jsx'
import { useI18n } from '../i18n/I18nContext.jsx'
import { pickName } from '../i18n/messages.js'
import { formatKoreanMobile, isValidEmail, isValidKoreanMobile } from '../validation.js'
import roomStdt from '../assets/room-stdt.jpg'
import roomDlxd from '../assets/room-dlxd.jpg'
import roomExsu from '../assets/room-exsu.jpg'

const ROOM_FALLBACK = { STDT: roomStdt, DLXD: roomDlxd, EXSU: roomExsu }

// 백엔드 상태코드 → i18n 키. 여러 화면이 공유하는 매핑.
const STATUS_KEY = {
  HOLD: 'status.hold',
  CONFIRMED: 'status.confirmed',
  CHECKED_IN: 'status.checkedIn',
  CHECKED_OUT: 'status.checkedOut',
  CANCELLED: 'status.cancelled',
  NO_SHOW: 'status.noShow',
  EXPIRED: 'status.expired',
}

// 검색 화면에서 넘어온 조건(state)으로 HOLD 를 만든다.
// 흐름: 요금정책 로드 → 예약자 정보 입력 → HOLD 생성 → 예약번호·만료·총액 표시 → (다음) 결제.
export default function BookingPage() {
  const { state } = useLocation()
  const navigate = useNavigate()
  const { member } = useAuth()
  const { t, lang } = useI18n()
  const money = (v) => t('fmt.currency', { amount: Number(v).toLocaleString() })
  const statusLabel = (s) => (STATUS_KEY[s] ? t(STATUS_KEY[s]) : s)

  // 검색을 거치지 않고 직접 들어오면 검색으로 돌려보낸다.
  useEffect(() => {
    if (!state?.roomTypeId) navigate('/', { replace: true })
  }, [state, navigate])

  const [ratePlans, setRatePlans] = useState([])
  const [ratePlanId, setRatePlanId] = useState('')
  const [availability, setAvailability] = useState(null)
  const [loadingQuote, setLoadingQuote] = useState(true)
  const [guestName, setGuestName] = useState('')
  const [guestPhone, setGuestPhone] = useState('')
  const [guestEmail, setGuestEmail] = useState('')
  const [adults, setAdults] = useState(state?.adults ? Number(state.adults) : 2)
  const [children, setChildren] = useState(state?.children ? Number(state.children) : 0)
  // 검색 화면에서 넘어온 객실 최대 수용인원. 인원 입력 상한으로 쓴다(백엔드가 최종 방어).
  const maxOccupancy = state?.maxOccupancy ? Number(state.maxOccupancy) : 9

  const [hold, setHold] = useState(null)
  const [expired, setExpired] = useState(false)
  const [error, setError] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  // 멱등키는 화면당 한 번만 만든다 — 재시도(더블클릭)에도 같은 키라 중복 예약이 안 생긴다.
  const idempotencyKey = useRef(crypto.randomUUID())

  // 로그인 회원이면 예약자 정보를 회원 프로필로 미리 채운다 — 그대로 확정하거나 고칠 수 있다.
  useEffect(() => {
    if (!member) return
    setGuestName((v) => v || member.name || '')
    setGuestPhone((v) => v || member.phone || '')
    setGuestEmail((v) => v || member.email || '')
  }, [member])

  useEffect(() => {
    if (!state?.roomTypeId) return
    setLoadingQuote(true)
    Promise.all([
      api.ratePlans(state.roomTypeId),
      api.availability(state.roomTypeId, state.checkIn, state.checkOut),
    ])
      .then(([plans, available]) => {
        setRatePlans(plans)
        setAvailability(available)
        // 프로모션/패키지에서 넘어오면 그 요금제를 선택, 아니면 첫 요금제.
        const preset = state?.ratePlanId && plans.some((p) => String(p.id) === String(state.ratePlanId))
          ? String(state.ratePlanId)
          : plans[0] ? String(plans[0].id) : ''
        setRatePlanId(preset)
      })
      .catch((e) => setError(e.message))
      .finally(() => setLoadingQuote(false))
  }, [state])

  async function submit(e) {
    e.preventDefault()
    setError(null)
    // 정원 초과는 미리 막는다 — 백엔드도 400 으로 방어하지만 즉시 안내가 낫다.
    if (Number(adults) + Number(children) > maxOccupancy) {
      setError(t('book.err.maxOcc', { max: maxOccupancy }))
      return
    }
    if (!member && (!guestName.trim() || !isValidKoreanMobile(guestPhone) || !isValidEmail(guestEmail))) {
      setError(t('book.guest.validation'))
      return
    }
    if (!availability || availability.bookableQty < 1) {
      setError(t('book.result.soldout'))
      return
    }
    setSubmitting(true)
    try {
      const res = await api.hold({
        guestName: member?.name || guestName.trim(),
        guestPhone: member?.phone || guestPhone,
        guestEmail: member?.email || guestEmail.trim(),
        roomTypeId: Number(state.roomTypeId),
        ratePlanId: Number(ratePlanId),
        checkInDate: state.checkIn,
        checkOutDate: state.checkOut,
        adults: Number(adults),
        children: Number(children),
        idempotencyKey: idempotencyKey.current,
      })
      setHold(res)
    } catch (err) {
      setError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  // 토스 결제창을 띄운다. 성공하면 토스가 successUrl 로 리다이렉트하므로 이 함수 뒤 코드는
  // 실행되지 않는다(페이지 전환). 승인·확정은 그 결과 화면(/payment/success)이 맡는다.
  async function pay() {
    setError(null)
    try {
      await startPayment({
        reservationNo: hold.reservationNo,
        amount: hold.totalAmount,
        orderName: `${pickName(lang, state.roomTypeName, state.roomTypeNameEn)} ${t('fmt.nights', { n: hold.nightCount })}`,
        summary: {
          reservationNo: hold.reservationNo,
          roomName: roomDisplayName,
          imageUrl: roomImage,
          ratePlanName: selectedPlan ? pickName(lang, selectedPlan.name, selectedPlan.nameEn) : '',
          checkIn: state.checkIn,
          checkOut: state.checkOut,
          nights: hold.nightCount,
          adults: Number(adults),
          children: Number(children),
          amount: hold.totalAmount,
        },
      })
    } catch (err) {
      // 사용자가 결제창을 닫으면 에러가 온다 — 조용히 메시지만 표시한다.
      setError(err.message || t('book.payErr'))
    }
  }

  if (!state?.roomTypeId) return null

  const selectedPlan = ratePlans.find((p) => String(p.id) === String(ratePlanId))
  const roomDisplayName = pickName(lang, state.roomTypeName, state.roomTypeNameEn)
  const roomImage = state.imageUrls?.[0] || ROOM_FALLBACK[state.roomTypeCode] || roomStdt
  const nights = Math.max(1, Math.round((new Date(state.checkOut) - new Date(state.checkIn)) / 86400000))
  const estimatedTotal = selectedPlan ? Number(selectedPlan.baseAmount) * nights : null

  const bookingSummary = (
    <aside className="booking-summary-card" aria-label={t('book.summary.title')}>
      <img src={roomImage} alt="" className="booking-summary-image" />
      <div className="booking-summary-body">
        <span className="booking-summary-kicker">{t('book.summary.title')}</span>
        <h2>{roomDisplayName}</h2>
        <dl>
          <div><dt>{t('book.field.period')}</dt><dd>{state.checkIn} ~ {state.checkOut}</dd></div>
          <div><dt>{t('book.summary.guests')}</dt><dd>{t('book.summary.guestCount', { adults, children })}</dd></div>
          <div><dt>{t('book.field.ratePlan')}</dt><dd>{selectedPlan ? pickName(lang, selectedPlan.name, selectedPlan.nameEn) : '-'}</dd></div>
          <div><dt>{t('book.field.amount')}</dt><dd>{estimatedTotal == null ? '-' : `${money(estimatedTotal)} ${t('book.summary.estimate')}`}</dd></div>
        </dl>
      </div>
    </aside>
  )

  // HOLD 성공 — 예약번호·만료·총액을 보여주고 결제로 잇는다.
  if (hold) {
    return (
      <div className="booking-page-grid">
        {bookingSummary}
        <div className="card">
        <h1>{t('book.hold.title')}</h1>
        <p className="muted">{t('book.hold.expireNote')}</p>
        <dl className="hold-summary">
          <div><dt>{t('book.field.resNo')}</dt><dd>{hold.reservationNo}</dd></div>
          <div><dt>{t('book.field.status')}</dt><dd>{statusLabel(hold.status)}</dd></div>
          <div><dt>{t('book.field.period')}</dt><dd>{hold.checkInDate} ~ {hold.checkOutDate} ({t('fmt.nights', { n: hold.nightCount })})</dd></div>
          <div><dt>{t('book.field.amount')}</dt><dd>{money(hold.totalAmount)}</dd></div>
          <div>
            <dt>{t('book.field.payDeadline')}</dt>
            <dd><HoldCountdown expiresAt={hold.holdExpiresAt} onExpire={() => setExpired(true)} /></dd>
          </div>
        </dl>
        {error && <p className="error">⚠ {error}</p>}
        {expired ? (
          <>
            <p className="error">⚠ {t('book.hold.expiredMsg')}</p>
            <button type="button" className="cta" onClick={() => navigate('/')}>{t('book.researchDates')}</button>
          </>
        ) : (
          <button type="button" className="cta" onClick={pay}>{t('book.pay')}</button>
        )}
        <p><Link to="/">{t('book.searchAgain')}</Link></p>
        </div>
      </div>
    )
  }

  return (
    <div className="booking-page-grid">
      {bookingSummary}
      <div className="card">
      <h1>{t('book.guest.title')}</h1>

      {loadingQuote && <p className="muted">{t('book.loading')}</p>}
      {!loadingQuote && availability?.bookableQty < 1 && (
        <div className="booking-unavailable">
          <p className="error">⚠ {t('book.result.soldout')}</p>
          <button type="button" className="cta" onClick={() => navigate('/')}>{t('book.researchDates')}</button>
        </div>
      )}

      <form className="book-form" onSubmit={submit}>
        <label>
          {t('book.field.ratePlan')}
          <select value={ratePlanId} onChange={(e) => setRatePlanId(e.target.value)}>
            {ratePlans.length === 0 && <option value="">{t('book.loading')}</option>}
            {ratePlans.map((p) => (
              <option key={p.id} value={p.id}>
                {pickName(lang, p.name, p.nameEn)} · {t('fmt.perNight', { amount: Number(p.baseAmount).toLocaleString() })}
                {p.breakfastIncluded ? t('book.rate.breakfast') : ''}
                {p.refundable ? t('book.rate.refundable') : t('book.rate.nonRefundable')}
              </option>
            ))}
          </select>
        </label>

        {member ? (
          <div className="member-booking-info">
            <strong>{t('book.memberInfo.title')}</strong>
            <span>{member.name} · {member.phone} · {member.email}</span>
          </div>
        ) : (
          <>
            <label>
              {t('book.field.guestName')}
              <input value={guestName} onChange={(e) => setGuestName(e.target.value)} autoComplete="name" required />
            </label>
            <label>
              {t('book.field.phone')}
              <input value={guestPhone} onChange={(e) => setGuestPhone(formatKoreanMobile(e.target.value))}
                     placeholder="010-1234-5678" inputMode="tel" autoComplete="tel" maxLength={13} required />
            </label>
            <label>
              {t('book.field.email')}
              <input type="email" value={guestEmail} onChange={(e) => setGuestEmail(e.target.value)} autoComplete="email" required />
            </label>
          </>
        )}

        <div className="pax">
          <label>
            {t('book.field.adults')}
            <input type="number" min="1" max={maxOccupancy}
                   value={adults}
                   onChange={(e) => setAdults(e.target.value)} />
          </label>
          <label>
            {t('book.field.children')}
            <input type="number" min="0" max={Math.max(0, maxOccupancy - Number(adults))}
                   value={children}
                   onChange={(e) => setChildren(e.target.value)} />
          </label>
        </div>

        {error && <p className="error">⚠ {error}</p>}

        <button type="submit" className="cta" disabled={submitting || loadingQuote || !ratePlanId || availability?.bookableQty < 1}>
          {submitting ? t('book.submitting') : t('book.holdCta')}
        </button>
      </form>
      <p><Link to="/">{t('book.backToSearch')}</Link></p>
      </div>
    </div>
  )
}
