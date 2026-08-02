import { useEffect, useState } from 'react'
import { api } from '../api/client.js'
import { useBooking } from '../components/BookingContext.jsx'
import { useI18n } from '../i18n/I18nContext.jsx'
import roomStdt from '../assets/room-stdt.jpg'
import roomDlxd from '../assets/room-dlxd.jpg'
import roomExsu from '../assets/room-exsu.jpg'

// 구조(요금제 매핑·이미지·강조)는 언어 무관. 텍스트는 CONTENT[lang] 에서.
const PROMO_META = [
  { planCode: 'STDT-BF', img: roomStdt },
  { planCode: 'DLXD-BF', img: roomDlxd, featured: true },
  { planCode: 'EXSU-NRF', img: roomExsu },
]

const CONTENT = {
  ko: {
    eyebrow: 'PACKAGES · 프로모션 & 패키지', title: '머무는 이유를 더하다',
    sub: '조식·스파·특가까지, 목적에 맞춰 고른 큐레이션 패키지. 실제 요금제와 연결되어 그대로 예약됩니다.',
    perNight: '/박부터', loading: '요금 불러오는 중…', breakfast: '조식포함',
    refundable: '환불가능', nonRefundable: '환불불가', cta: '이 패키지로 예약 →',
    note: '표시 요금은 1박 기준이며 날짜·잔여 상황에 따라 달라질 수 있습니다. 예약 시 실제 요금제와 취소 규정이 적용됩니다.',
    amount: (n) => `${Number(n).toLocaleString()}원`,
    promos: {
      'STDT-BF': { tag: '데일리 스테이', title: '도심 브런치 스테이', roomName: '스탠다드 트윈',
        desc: '스탠다드 트윈에서 보내는 하룻밤과 다음 날 아침. 부담 없는 도심 휴식.', perks: ['조식 2인', '늦은 체크아웃 13시', '웰컴 티'] },
      'DLXD-BF': { tag: '베스트셀러', title: '디럭스 릴랙스 패키지', roomName: '디럭스 더블',
        desc: '킹 베드 디럭스 더블에 조식과 스파 이용을 더한 여유로운 주말.', perks: ['조식 2인', '스파 30분', '주차 무료'] },
      'EXSU-NRF': { tag: '얼리버드 특가', title: '스위트 논리펀더블 특가', roomName: '이그제큐티브 스위트',
        desc: '최상층 스위트를 가장 좋은 값에. 미리 확정하는 대신 환불 불가 조건.', perks: ['조식 2인', '라운지 이용', '파노라마 시티뷰'] },
    },
  },
  en: {
    eyebrow: 'PACKAGES & OFFERS', title: 'One more reason to stay',
    sub: 'Breakfast, spa, special rates — packages curated by purpose. Linked to real rate plans and booked as-is.',
    perNight: '/night', loading: 'Loading rate…', breakfast: 'Breakfast',
    refundable: 'Refundable', nonRefundable: 'Non-refundable', cta: 'Book this package →',
    note: 'Rates shown are per night and may vary by date and availability. The actual rate plan and cancellation policy apply at booking.',
    amount: (n) => `₩${Number(n).toLocaleString()}`,
    promos: {
      'STDT-BF': { tag: 'Daily stay', title: 'City Brunch Stay', roomName: 'Standard Twin',
        desc: 'A night in the Standard Twin and the morning after. An easy escape in the city.', perks: ['Breakfast for 2', 'Late checkout 13:00', 'Welcome tea'] },
      'DLXD-BF': { tag: 'Bestseller', title: 'Deluxe Relax Package', roomName: 'Deluxe Double',
        desc: 'A king-bed Deluxe Double with breakfast and spa — a leisurely weekend.', perks: ['Breakfast for 2', 'Spa 30 min', 'Free parking'] },
      'EXSU-NRF': { tag: 'Early-bird', title: 'Suite Non-refundable Deal', roomName: 'Executive Suite',
        desc: 'The top-floor suite at its best price. Confirm ahead in exchange for a non-refundable rate.', perks: ['Breakfast for 2', 'Lounge access', 'Panoramic city view'] },
    },
  },
}

export default function PromotionsPage() {
  const [plans, setPlans] = useState({}) // planCode -> { plan, roomType }
  const [error, setError] = useState(null)
  const { openBooking } = useBooking()
  const { lang } = useI18n()
  const c = CONTENT[lang] ?? CONTENT.ko

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
          <div className="page-eyebrow">{c.eyebrow}</div>
          <h1 className="page-title">{c.title}</h1>
          <p className="page-sub">{c.sub}</p>
        </div>
      </section>

      {error && <p className="error promo-error">⚠ {error}</p>}

      <section className="promo-grid">
        {PROMO_META.map((meta) => {
          const promo = c.promos[meta.planCode]
          const match = plans[meta.planCode]
          const plan = match?.plan
          const roomType = match?.roomType
          return (
            <article key={meta.planCode} className={`promo-card${meta.featured ? ' featured' : ''}`}>
              <div className="promo-photo" style={{ backgroundImage: `url(${meta.img})` }}>
                <span className="promo-tag">{promo.tag}</span>
              </div>
              <div className="promo-body">
                <h3 className="promo-title">{promo.title}</h3>
                <div className="promo-room">{promo.roomName}</div>
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
                        <span className="promo-amount">{c.amount(plan.baseAmount)}</span>
                        <span className="promo-per">{c.perNight}</span>
                      </>
                    ) : (
                      <span className="promo-per">{c.loading}</span>
                    )}
                  </div>
                  <div className="promo-flags">
                    {plan?.breakfastIncluded && <span className="promo-flag ok">{c.breakfast}</span>}
                    {plan && (
                      <span className={`promo-flag ${plan.refundable ? 'ok' : 'warn'}`}>
                        {plan.refundable ? c.refundable : c.nonRefundable}
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
                  {c.cta}
                </button>
              </div>
            </article>
          )
        })}
      </section>

      <p className="promo-note">{c.note}</p>
    </div>
  )
}
