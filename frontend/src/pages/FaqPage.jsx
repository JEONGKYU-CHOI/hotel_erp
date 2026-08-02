import { Link } from 'react-router-dom'
import locationCity from '../assets/location-city.jpg'
import { useI18n } from '../i18n/I18nContext.jsx'

const CONTENT = {
  ko: {
    eyebrow: 'GUIDE · 이용안내', title: '이용안내 & FAQ', sub: '체크인부터 취소·환불 규정까지, 머무시기 전 알아두면 좋은 것들.',
    guideTitle: '한눈에 보는 이용안내', faqTitle: '자주 묻는 질문',
    guide: [
      { icon: '🕒', label: '체크인', value: '15:00부터' },
      { icon: '🌅', label: '체크아웃', value: '11:00까지' },
      { icon: '🍽️', label: '조식', value: '07:00 – 10:30 · 그린하우스' },
      { icon: '🚗', label: '주차', value: '발렛·자주식 24시간' },
      { icon: '🐾', label: '반려동물', value: '동반 불가(안내견 예외)' },
      { icon: '🚭', label: '흡연', value: '전 객실 금연' },
    ],
    faqs: [
      { q: '체크인·체크아웃 시간은 어떻게 되나요?', a: '체크인은 오후 3시부터, 체크아웃은 오전 11시까지입니다. 얼리 체크인·레이트 체크아웃은 객실 상황에 따라 가능하며, 프론트데스크로 문의해 주세요.' },
      { q: '예약을 취소하면 환불되나요?', a: '요금제에 따라 다릅니다. “환불가능” 요금제는 체크인 1일 전까지 무료로 취소·환불됩니다. “논리펀더블 특가”처럼 환불 불가 조건으로 예약한 경우에는 취소해도 환불되지 않습니다. 예약 시 선택한 요금제의 조건이 그대로 적용됩니다.' },
      { q: '취소는 어디서 하나요?', a: '상단 “예약 조회”에서 예약번호와 연락처로 예약을 확인하고 취소할 수 있습니다. 회원으로 예약하셨다면 “내 예약”에서도 관리할 수 있습니다.' },
      { q: '조식은 포함되어 있나요?', a: '요금제에 따라 다릅니다. “조식 포함” 요금제는 2인 조식이 포함되며, 그린하우스에서 오전 7시부터 10시 30분까지 제공됩니다. 프로모션·패키지 페이지에서 조식 포함 상품을 한눈에 볼 수 있습니다.' },
      { q: '결제는 어떻게 하나요?', a: '예약 진행 시 임시 예약(HOLD)이 잡히고, 안내된 시각까지 카드로 결제하면 예약이 확정됩니다. 시간 내 결제하지 않으면 임시 예약은 자동으로 취소됩니다.' },
      { q: '비회원도 예약할 수 있나요?', a: '네, 회원가입 없이 예약할 수 있습니다. 예약 후에는 예약번호와 연락처로 “예약 조회”에서 확인하세요. 회원으로 로그인한 상태에서 예약하면 예약이 자동으로 회원 계정에 연결됩니다.' },
      { q: '아동·추가 인원 규정은 어떻게 되나요?', a: '객실별 기준 인원과 최대 인원이 정해져 있습니다(객실 안내 참고). 예약 시 성인·아동 인원을 입력하면 되며, 최대 인원을 초과하는 예약은 제한됩니다.' },
      { q: '주차와 Wi-Fi는 이용할 수 있나요?', a: '전 구역 초고속 Wi-Fi가 무료로 제공되며, 발렛·자주식 주차를 24시간 이용하실 수 있습니다. 자세한 편의시설은 편의시설 페이지를 참고하세요.' },
    ],
    more: ['더 궁금한 점이 있으면 프론트데스크 ', '02-0000-0000', '(연중무휴 24시간)로 문의하시거나, ', '예약 조회', '에서 내 예약을 확인하세요.'],
  },
  en: {
    eyebrow: 'GUIDE', title: 'Guide & FAQ', sub: 'From check-in to cancellation — the things worth knowing before you stay.',
    guideTitle: 'The guide at a glance', faqTitle: 'Frequently asked questions',
    guide: [
      { icon: '🕒', label: 'Check-in', value: 'From 15:00' },
      { icon: '🌅', label: 'Check-out', value: 'By 11:00' },
      { icon: '🍽️', label: 'Breakfast', value: '07:00 – 10:30 · Greenhouse' },
      { icon: '🚗', label: 'Parking', value: 'Valet & self, 24h' },
      { icon: '🐾', label: 'Pets', value: 'Not allowed (guide dogs excepted)' },
      { icon: '🚭', label: 'Smoking', value: 'Non-smoking throughout' },
    ],
    faqs: [
      { q: 'What are the check-in and check-out times?', a: 'Check-in is from 3:00 PM and check-out is by 11:00 AM. Early check-in and late check-out may be possible depending on availability — please ask the front desk.' },
      { q: 'Do I get a refund if I cancel?', a: 'It depends on the rate plan. A “refundable” rate can be cancelled free of charge up to one day before check-in. If you booked a non-refundable special, cancelling does not refund. The conditions of the rate you chose at booking apply.' },
      { q: 'Where do I cancel?', a: 'Use “Find booking” at the top to look up your reservation by number and phone, then cancel there. If you booked as a member, you can also manage it under “My stays”.' },
      { q: 'Is breakfast included?', a: 'It depends on the rate plan. A “breakfast included” rate covers breakfast for two, served at the Greenhouse from 7:00 to 10:30 AM. You can see breakfast-included offers on the Packages page.' },
      { q: 'How does payment work?', a: 'When you book, the room is held (HOLD); pay by card before the shown time and the reservation is confirmed. If you do not pay in time, the hold is released automatically.' },
      { q: 'Can non-members book?', a: 'Yes, you can book without signing up. Afterward, check your booking under “Find booking” with your number and phone. If you book while signed in, it links to your account automatically.' },
      { q: 'What about children and extra guests?', a: 'Each room has a standard and maximum occupancy (see the Rooms page). Enter adults and children when booking; reservations over the maximum are restricted.' },
      { q: 'Are parking and Wi-Fi available?', a: 'High-speed Wi-Fi is free throughout, and valet & self parking is available 24 hours. See the Facilities page for details.' },
    ],
    more: ['For anything else, call the front desk at ', '02-0000-0000', ' (open 24/7), or check your booking under ', 'Find booking', '.'],
  },
}

export default function FaqPage() {
  const { lang } = useI18n()
  const c = CONTENT[lang] ?? CONTENT.ko
  return (
    <>
      <section
        className="page-hero"
        style={{ backgroundImage: `linear-gradient(180deg, rgba(16,18,24,.4), rgba(16,18,24,.66)), url(${locationCity})` }}
      >
        <div className="page-hero-inner">
          <div className="page-eyebrow">{c.eyebrow}</div>
          <h1 className="page-title">{c.title}</h1>
          <p className="page-sub">{c.sub}</p>
        </div>
      </section>

      <div className="section">
        <h2 className="block-title">{c.guideTitle}</h2>
        <div className="guide-grid">
          {c.guide.map((g) => (
            <div key={g.label} className="guide-item">
              <div className="guide-icon">{g.icon}</div>
              <div className="guide-text">
                <div className="guide-label">{g.label}</div>
                <div className="guide-value">{g.value}</div>
              </div>
            </div>
          ))}
        </div>
      </div>

      <div className="section faq-section">
        <h2 className="block-title">{c.faqTitle}</h2>
        <div className="faq-list">
          {c.faqs.map((item) => (
            <details key={item.q} className="faq-item">
              <summary className="faq-q">
                <span>{item.q}</span>
                <span className="faq-mark" aria-hidden="true">+</span>
              </summary>
              <p className="faq-a">{item.a}</p>
            </details>
          ))}
        </div>
        <p className="faq-more">
          {c.more[0]}<strong>{c.more[1]}</strong>{c.more[2]}<Link to="/lookup">{c.more[3]}</Link>{c.more[4]}
        </p>
      </div>
    </>
  )
}
