import { Link } from 'react-router-dom'
import heroImg from '../assets/hero-2.jpg'
import loungeImg from '../assets/facility-lounge.jpg'
import { useI18n } from '../i18n/I18nContext.jsx'

const CONTENT = {
  ko: {
    eyebrow: 'ABOUT · 브랜드 스토리', title: '머무는 순간이 여행이 되도록',
    heroSub: '더 스테이는 화려함보다 편안함을, 많음보다 알맞음을 택한 도심 호텔입니다.',
    lede: ['여행의 하이라이트는 늘 밖에 있다고들 하지만, 우리는 하루를 닫고 여는 ', '그 방 안의 시간',
      '이 여행의 질을 정한다고 생각했습니다. 그래서 더 스테이는 볼거리를 늘리는 대신, 잘 자고 잘 쉬는 일에 집중했습니다.'],
    philoEyebrow: 'OUR PHILOSOPHY', philoTitle: '덜어낼수록 편안해집니다',
    philoBody: [
      '로비의 과한 장식도, 복잡한 체크인 절차도 없앴습니다. 대신 침구의 감촉, 물의 온도, 아침의 냄새처럼 몸이 먼저 알아채는 것들에 예산을 썼습니다.',
      '도심 강남 한복판이라는 위치는 그대로 두되, 문을 닫는 순간 도시의 소음이 멈추도록 설계했습니다. 바쁜 일정 사이, 온전히 나를 위한 하룻밤을 위한 공간입니다.',
    ],
    valuesTitle: '더 스테이가 지키는 세 가지',
    values: [
      { title: '고요를 설계합니다', body: '객실은 도심 한복판에 있지만 소음은 문 앞에서 멈춥니다. 이중 차음 창과 낮은 층고 조명으로, 자는 시간을 방해하지 않는 것을 첫 번째 원칙으로 둡니다.' },
      { title: '아침을 중요하게 여깁니다', body: '하루의 시작이 머문 기억을 정합니다. 그린하우스의 조식은 계절 재료로 매일 다르게 차리고, 커피는 원두를 그날 내립니다.' },
      { title: '군더더기를 덜어냅니다', body: '필요 없는 절차와 장식을 줄였습니다. 예약은 날짜만 고르면 되고, 체크인은 기다리지 않습니다. 비움이 곧 편안함이라고 믿습니다.' },
    ],
    facts: [
      { n: '3', label: '객실 타입', sub: '스탠다드 · 디럭스 · 스위트' },
      { n: '24h', label: '프론트데스크', sub: '연중무휴 응대' },
      { n: '5분', label: '강남역까지', sub: '2호선 도보' },
      { n: '2018', label: '문을 연 해', sub: '도심 속 휴식의 시작' },
    ],
    ctaTitle: '직접 머물러 확인해 보세요', ctaSub: '글로 옮긴 편안함은 결국 하룻밤으로 증명됩니다.',
    ctaRooms: '객실 둘러보기 →', ctaLocation: '오시는 길',
  },
  en: {
    eyebrow: 'ABOUT · Our story', title: 'So that a stay becomes a journey',
    heroSub: 'The Stay is a city hotel that chose comfort over flash, and just enough over more.',
    lede: ['They say the highlight of a trip is always outside — but we believe the ', 'time inside the room',
      ', where a day ends and begins, sets the quality of the journey. So instead of adding sights, we focused on sleeping and resting well.'],
    philoEyebrow: 'OUR PHILOSOPHY', philoTitle: 'The less there is, the more at ease you feel',
    philoBody: [
      'We removed the over-decorated lobby and the fussy check-in. Instead we spent on what the body notices first — the feel of the linens, the temperature of the water, the smell of morning.',
      'We kept the address in the heart of Gangnam, but designed the room so the city goes quiet the moment the door closes. A night entirely your own, between busy days.',
    ],
    valuesTitle: 'Three things The Stay keeps',
    values: [
      { title: 'We design for quiet', body: 'The room sits downtown, yet the noise stops at the door. Double-glazed windows and low, soft lighting put undisturbed sleep first.' },
      { title: 'We take mornings seriously', body: 'How a day begins shapes how a stay is remembered. Breakfast at the Greenhouse changes daily with the season, and the coffee is brewed that morning.' },
      { title: 'We strip away the excess', body: 'We cut the steps and ornaments you do not need. Booking is just picking dates; check-in has no wait. We believe emptiness is comfort.' },
    ],
    facts: [
      { n: '3', label: 'Room types', sub: 'Standard · Deluxe · Suite' },
      { n: '24h', label: 'Front desk', sub: 'Open every day' },
      { n: '5 min', label: 'To Gangnam Stn.', sub: 'On foot, Line 2' },
      { n: '2018', label: 'The year we opened', sub: 'Calm in the city begins' },
    ],
    ctaTitle: 'Come see for yourself', ctaSub: 'Comfort put into words is, in the end, proven by a single night.',
    ctaRooms: 'Browse rooms →', ctaLocation: 'Directions',
  },
}

export default function AboutPage() {
  const { lang } = useI18n()
  const c = CONTENT[lang] ?? CONTENT.ko
  return (
    <>
      <section className="page-hero" style={{ backgroundImage: `linear-gradient(180deg, rgba(16,18,24,.34), rgba(16,18,24,.62)), url(${heroImg})` }}>
        <div className="page-hero-inner">
          <div className="page-eyebrow">{c.eyebrow}</div>
          <h1 className="page-title">{c.title}</h1>
          <p className="page-sub">{c.heroSub}</p>
        </div>
      </section>

      <div className="section about-intro">
        <p className="about-lede">{c.lede[0]}<strong>{c.lede[1]}</strong>{c.lede[2]}</p>
      </div>

      <div className="section about-split">
        <div className="about-photo" style={{ backgroundImage: `url(${loungeImg})` }} aria-hidden="true" />
        <div className="about-copy">
          <div className="block-eyebrow">{c.philoEyebrow}</div>
          <h2 className="block-title left">{c.philoTitle}</h2>
          {c.philoBody.map((p, i) => <p key={i}>{p}</p>)}
        </div>
      </div>

      <div className="section">
        <h2 className="block-title">{c.valuesTitle}</h2>
        <div className="values-grid">
          {c.values.map((v) => (
            <article key={v.title} className="value-card">
              <h3 className="value-title">{v.title}</h3>
              <p className="value-body">{v.body}</p>
            </article>
          ))}
        </div>
      </div>

      <div className="section about-facts">
        <dl className="facts-grid">
          {c.facts.map((f) => (
            <div key={f.label} className="fact">
              <dt className="fact-n">{f.n}</dt>
              <dd className="fact-label">{f.label}<span className="fact-sub">{f.sub}</span></dd>
            </div>
          ))}
        </dl>
      </div>

      <div className="section about-cta">
        <h2 className="block-title">{c.ctaTitle}</h2>
        <p className="about-cta-sub">{c.ctaSub}</p>
        <div className="about-cta-links">
          <Link to="/rooms" className="cta-dark">{c.ctaRooms}</Link>
          <Link to="/location" className="cta-ghost">{c.ctaLocation}</Link>
        </div>
      </div>
    </>
  )
}
