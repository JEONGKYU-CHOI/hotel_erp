import { useEffect, useRef, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { api } from '../api/client.js'
import { startPayment } from '../payments.js'
import { useAuth } from '../auth/AuthContext.jsx'
import HoldCountdown from '../components/HoldCountdown.jsx'

// 검색 화면에서 넘어온 조건(state)으로 HOLD 를 만든다.
// 흐름: 요금정책 로드 → 예약자 정보 입력 → HOLD 생성 → 예약번호·만료·총액 표시 → (다음) 결제.
export default function BookingPage() {
  const { state } = useLocation()
  const navigate = useNavigate()
  const { member } = useAuth()

  // 검색을 거치지 않고 직접 들어오면 검색으로 돌려보낸다.
  useEffect(() => {
    if (!state?.roomTypeId) navigate('/', { replace: true })
  }, [state, navigate])

  const [ratePlans, setRatePlans] = useState([])
  const [ratePlanId, setRatePlanId] = useState('')
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
    api.ratePlans(state.roomTypeId)
      .then((plans) => {
        setRatePlans(plans)
        // 프로모션/패키지에서 넘어오면 그 요금제를 선택, 아니면 첫 요금제.
        const preset = state?.ratePlanId && plans.some((p) => String(p.id) === String(state.ratePlanId))
          ? String(state.ratePlanId)
          : plans[0] ? String(plans[0].id) : ''
        setRatePlanId(preset)
      })
      .catch((e) => setError(e.message))
  }, [state])

  async function submit(e) {
    e.preventDefault()
    setError(null)
    // 정원 초과는 미리 막는다 — 백엔드도 400 으로 방어하지만 즉시 안내가 낫다.
    if (Number(adults) + Number(children) > maxOccupancy) {
      setError(`최대 수용 인원(${maxOccupancy}인)을 초과했습니다. 성인·아동 수를 줄여 주세요.`)
      return
    }
    setSubmitting(true)
    try {
      const res = await api.hold({
        guestName,
        guestPhone,
        guestEmail: guestEmail || null,
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
        orderName: `${state.roomTypeName} ${hold.nightCount}박`,
      })
    } catch (err) {
      // 사용자가 결제창을 닫으면 에러가 온다 — 조용히 메시지만 표시한다.
      setError(err.message || '결제를 시작할 수 없습니다.')
    }
  }

  if (!state?.roomTypeId) return null

  // HOLD 성공 — 예약번호·만료·총액을 보여주고 결제로 잇는다.
  if (hold) {
    return (
      <div className="card">
        <h1>임시 예약 완료</h1>
        <p className="muted">남은 시간 안에 결제하지 않으면 자동 취소됩니다.</p>
        <dl className="hold-summary">
          <div><dt>예약번호</dt><dd>{hold.reservationNo}</dd></div>
          <div><dt>상태</dt><dd>{hold.status}</dd></div>
          <div><dt>기간</dt><dd>{hold.checkInDate} ~ {hold.checkOutDate} ({hold.nightCount}박)</dd></div>
          <div><dt>결제 금액</dt><dd>{Number(hold.totalAmount).toLocaleString()}원</dd></div>
          <div>
            <dt>결제 마감까지</dt>
            <dd><HoldCountdown expiresAt={hold.holdExpiresAt} onExpire={() => setExpired(true)} /></dd>
          </div>
        </dl>
        {error && <p className="error">⚠ {error}</p>}
        {expired ? (
          <>
            <p className="error">⚠ 결제 시간이 만료되어 임시 예약이 취소되었습니다.</p>
            <button type="button" className="cta" onClick={() => navigate('/')}>다시 검색하기</button>
          </>
        ) : (
          <button type="button" className="cta" onClick={pay}>결제하기</button>
        )}
        <p><Link to="/">← 다른 날짜로 다시 검색</Link></p>
      </div>
    )
  }

  return (
    <div className="card">
      <h1>예약자 정보</h1>
      <p className="muted">
        {state.roomTypeName} · {state.checkIn} ~ {state.checkOut}
      </p>

      <form className="book-form" onSubmit={submit}>
        <label>
          요금 정책
          <select value={ratePlanId} onChange={(e) => setRatePlanId(e.target.value)}>
            {ratePlans.length === 0 && <option value="">불러오는 중…</option>}
            {ratePlans.map((p) => (
              <option key={p.id} value={p.id}>
                {p.name} · {Number(p.baseAmount).toLocaleString()}원/박
                {p.breakfastIncluded ? ' · 조식포함' : ''}
                {p.refundable ? ' · 환불가능' : ' · 환불불가'}
              </option>
            ))}
          </select>
        </label>

        <label>
          예약자 이름
          <input value={guestName} onChange={(e) => setGuestName(e.target.value)} required />
        </label>

        <label>
          연락처
          <input value={guestPhone} onChange={(e) => setGuestPhone(e.target.value)}
                 placeholder="010-1234-5678" required />
        </label>

        <label>
          이메일 (선택)
          <input type="email" value={guestEmail} onChange={(e) => setGuestEmail(e.target.value)} />
        </label>

        <div className="pax">
          <label>
            성인
            <input type="number" min="1" max={maxOccupancy}
                   value={adults}
                   onChange={(e) => setAdults(e.target.value)} />
          </label>
          <label>
            아동
            <input type="number" min="0" max={Math.max(0, maxOccupancy - Number(adults))}
                   value={children}
                   onChange={(e) => setChildren(e.target.value)} />
          </label>
        </div>

        {error && <p className="error">⚠ {error}</p>}

        <button type="submit" className="cta" disabled={submitting || !ratePlanId}>
          {submitting ? '처리 중…' : '임시 예약하기'}
        </button>
      </form>
      <p><Link to="/">← 검색으로</Link></p>
    </div>
  )
}
