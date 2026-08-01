import { useEffect, useState } from 'react'
import { api } from '../api/client.js'
import { useBooking } from '../components/BookingContext.jsx'
import roomStdt from '../assets/room-stdt.jpg'
import roomDlxd from '../assets/room-dlxd.jpg'
import roomExsu from '../assets/room-exsu.jpg'

// 큐레이션 패키지 정의 — 실제 요금제(rate_plan) code 에 매핑한다.
// 가격·조식·환불 여부는 하드코딩하지 않고 API 의 rate-plan 에서 가져와 붙인다(단일 진실원).
const PROMOS = [
  {
    planCode: 'STDT-BF',
    tag: '데일리 스테이',
    title: '도심 브런치 스테이',
    img: roomStdt,
    desc: '스탠다드 트윈에서 보내는 하룻밤과 다음 날 아침. 부담 없는 도심 휴식.',
    perks: ['조식 2인', '늦은 체크아웃 13시', '웰컴 티'],
  },
  {
    planCode: 'DLXD-BF',
    tag: '베스트셀러',
    title: '디럭스 릴랙스 패키지',
    img: roomDlxd,
    desc: '킹 베드 디럭스 더블에 조식과 스파 이용을 더한 여유로운 주말.',
    perks: ['조식 2인', '스파 30분', '주차 무료'],
    featured: true,
  },
  {
    planCode: 'EXSU-NRF',
    tag: '얼리버드 특가',
    title: '스위트 논리펀더블 특가',
    img: roomExsu,
    desc: '최상층 스위트를 가장 좋은 값에. 미리 확정하는 대신 환불 불가 조건.',
    perks: ['조식 2인', '라운지 이용', '파노라마 시티뷰'],
  },
]

// 프로모션/패키지 — 실제 요금제와 연결된 큐레이션 상품. "이 패키지로 예약"이 통합 예약 모달을
// 그 객실+요금제가 프리셀렉트된 채로 연다.
export default function PromotionsPage() {
  const [plans, setPlans] = useState({}) // planCode -> { plan, roomType }
  const [error, setError] = useState(null)
  const { openBooking } = useBooking()

  useEffect(() => {
    let alive = true
    api.roomTypes()
      .then(async (types) => {
        const entries = await Promise.all(
          types.map(async (t) => {
            const rp = await api.ratePlans(t.id)
            return rp.map((p) => [p.code, { plan: p, roomType: t }])
          })
        )
        if (alive) setPlans(Object.fromEntries(entries.flat()))
      })
      .catch((e) => alive && setError(e.message))
    return () => { alive = false }
  }, [])

  return (
    <div className="promos-page">
      <section
        className="page-hero"
        style={{ backgroundImage: `linear-gradient(180deg, rgba(16,18,24,.34), rgba(16,18,24,.64)), url(${roomDlxd})` }}
      >
        <div className="page-hero-inner">
          <div className="page-eyebrow">PACKAGES · 프로모션 &amp; 패키지</div>
          <h1 className="page-title">머무는 이유를 더하다</h1>
          <p className="page-sub">
            조식·스파·특가까지, 목적에 맞춰 고른 큐레이션 패키지. 실제 요금제와 연결되어 그대로 예약됩니다.
          </p>
        </div>
      </section>

      {error && <p className="error promo-error">⚠ {error}</p>}

      <section className="promo-grid">
        {PROMOS.map((promo) => {
          const match = plans[promo.planCode]
          const plan = match?.plan
          const roomType = match?.roomType
          return (
            <article key={promo.planCode} className={`promo-card${promo.featured ? ' featured' : ''}`}>
              <div className="promo-photo" style={{ backgroundImage: `url(${promo.img})` }}>
                <span className="promo-tag">{promo.tag}</span>
              </div>
              <div className="promo-body">
                <h3 className="promo-title">{promo.title}</h3>
                {roomType && <div className="promo-room">{roomType.name}</div>}
                <p className="promo-desc">{promo.desc}</p>
                <ul className="promo-perks">
                  {promo.perks.map((p) => (
                    <li key={p}>{p}</li>
                  ))}
                </ul>
                <div className="promo-foot">
                  <div className="promo-price">
                    {plan ? (
                      <>
                        <span className="promo-amount">{Number(plan.baseAmount).toLocaleString()}원</span>
                        <span className="promo-per">/박부터</span>
                      </>
                    ) : (
                      <span className="promo-per">요금 불러오는 중…</span>
                    )}
                  </div>
                  <div className="promo-flags">
                    {plan?.breakfastIncluded && <span className="promo-flag ok">조식포함</span>}
                    {plan && (
                      <span className={`promo-flag ${plan.refundable ? 'ok' : 'warn'}`}>
                        {plan.refundable ? '환불가능' : '환불불가'}
                      </span>
                    )}
                  </div>
                </div>
                <button
                  type="button"
                  className="promo-cta"
                  disabled={!plan}
                  onClick={() => openBooking(roomType.id, plan.id)}
                >
                  이 패키지로 예약 →
                </button>
              </div>
            </article>
          )
        })}
      </section>

      <p className="promo-note">
        표시 요금은 1박 기준이며 날짜·잔여 상황에 따라 달라질 수 있습니다. 예약 시 실제 요금제와 취소 규정이 적용됩니다.
      </p>
    </div>
  )
}
