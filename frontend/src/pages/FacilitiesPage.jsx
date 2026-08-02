import facilityPool from '../assets/facility-pool.jpg'
import facilitySpa from '../assets/facility-spa.jpg'
import facilityLounge from '../assets/facility-lounge.jpg'
import { useI18n } from '../i18n/I18nContext.jsx'

const HIGHLIGHT_IMAGES = [facilityPool, facilitySpa, facilityLounge]

const CONTENT = {
  ko: {
    eyebrow: 'FACILITIES', title: '편의시설', sub: '머무는 동안 필요한 모든 것. 휴식과 업무, 그 사이의 여백까지.',
    facilityTag: 'FACILITY', listTitle: '이용 가능한 시설',
    highlights: [
      { name: '루프탑 인피니티 풀', desc: '25층에서 도심을 내려다보는 시티뷰 인피니티 풀. 연중 운영되며, 선베드와 풀바를 갖췄습니다.' },
      { name: '스파 & 사우나', desc: '아로마 스파 트리트먼트와 건식·습식 사우나. 여행의 피로를 풀어줄 고요한 웰니스 공간.' },
      { name: '이그제큐티브 라운지', desc: '스위트 투숙객을 위한 상시 스낵·음료와 조용한 업무 공간. 도심 전망과 함께합니다.' },
    ],
    facilities: [
      { icon: '🏊', name: '인피니티 풀', desc: '25F 루프탑 · 시티뷰' },
      { icon: '💪', name: '피트니스 센터', desc: '24시간 운영' },
      { icon: '🧖', name: '스파 & 사우나', desc: '건식·습식 사우나' },
      { icon: '🍸', name: '이그제큐티브 라운지', desc: '스위트 투숙객 전용' },
      { icon: '🚗', name: '발렛 파킹', desc: '24시간 발렛·자주식' },
      { icon: '🛎️', name: '24시 룸서비스', desc: '언제든 객실로' },
      { icon: '📶', name: '무료 Wi-Fi', desc: '전 구역 초고속' },
      { icon: '🧳', name: '컨시어지', desc: '투어·교통 안내' },
    ],
  },
  en: {
    eyebrow: 'FACILITIES', title: 'Facilities', sub: 'Everything you need while you stay — rest, work, and the space in between.',
    facilityTag: 'FACILITY', listTitle: 'Available facilities',
    highlights: [
      { name: 'Rooftop Infinity Pool', desc: 'A city-view infinity pool overlooking downtown from the 25th floor. Open year-round, with sun beds and a pool bar.' },
      { name: 'Spa & Sauna', desc: 'Aroma spa treatments and dry & wet saunas. A quiet wellness space to melt away the road.' },
      { name: 'Executive Lounge', desc: 'All-day snacks and drinks for suite guests, plus a quiet place to work — with a view over the city.' },
    ],
    facilities: [
      { icon: '🏊', name: 'Infinity pool', desc: '25F rooftop · city view' },
      { icon: '💪', name: 'Fitness center', desc: 'Open 24 hours' },
      { icon: '🧖', name: 'Spa & sauna', desc: 'Dry & wet sauna' },
      { icon: '🍸', name: 'Executive lounge', desc: 'Suite guests only' },
      { icon: '🚗', name: 'Valet parking', desc: 'Valet & self, 24h' },
      { icon: '🛎️', name: '24h room service', desc: 'To your room, anytime' },
      { icon: '📶', name: 'Free Wi-Fi', desc: 'High-speed everywhere' },
      { icon: '🧳', name: 'Concierge', desc: 'Tours & transport' },
    ],
  },
}

export default function FacilitiesPage() {
  const { lang } = useI18n()
  const c = CONTENT[lang] ?? CONTENT.ko
  return (
    <>
      <section className="page-hero" style={{ backgroundImage: `linear-gradient(180deg, rgba(16,18,24,.34), rgba(16,18,24,.64)), url(${facilityPool})` }}>
        <div className="page-hero-inner">
          <div className="page-eyebrow">{c.eyebrow}</div>
          <h1 className="page-title">{c.title}</h1>
          <p className="page-sub">{c.sub}</p>
        </div>
      </section>

      <div className="detail-list">
        {c.highlights.map((h, i) => (
          <article key={h.name} className={`detail-row ${i % 2 ? 'reverse' : ''}`}>
            <div className="detail-photo" style={{ backgroundImage: `url(${HIGHLIGHT_IMAGES[i]})` }} />
            <div className="detail-body">
              <div className="detail-eyebrow">{c.facilityTag}</div>
              <h2 className="detail-name">{h.name}</h2>
              <p className="detail-desc">{h.desc}</p>
            </div>
          </article>
        ))}
      </div>

      <div className="section">
        <h2 className="block-title">{c.listTitle}</h2>
        <div className="facility-grid">
          {c.facilities.map((f) => (
            <div key={f.name} className="facility-item">
              <div className="facility-icon">{f.icon}</div>
              <div className="facility-name">{f.name}</div>
              <div className="facility-desc">{f.desc}</div>
            </div>
          ))}
        </div>
      </div>
    </>
  )
}
