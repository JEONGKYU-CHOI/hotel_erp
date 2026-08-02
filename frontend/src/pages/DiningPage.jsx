import diningRest from '../assets/dining-rest.jpg'
import diningBar from '../assets/dining-bar.jpg'
import diningCafe from '../assets/dining-cafe.jpg'
import { useI18n } from '../i18n/I18nContext.jsx'

// 이미지는 언어와 무관 — 인덱스로 텍스트와 짝짓는다.
const IMAGES = [diningRest, diningBar, diningCafe]

// 페이지 로컬 콘텐츠(산문·태그가 많아 전역 사전 대신 여기에 한/영을 함께 둔다).
const CONTENT = {
  ko: {
    eyebrow: 'DINING', title: '다이닝', sub: '아침의 커피부터 늦은 밤의 한 잔까지, 세 개의 공간.',
    outlets: [
      { name: '더 테이블', kind: '파인 다이닝 · 25F', hours: '디너 18:00 – 22:00 (라스트오더 21:00)',
        desc: '제철 식재료로 완성하는 코스 다이닝. 도심의 야경을 배경으로 특별한 저녁을 즐기세요.',
        tags: ['코스 요리', '와인 페어링', '시티뷰'] },
      { name: '바 소셜', kind: '바 & 라운지 · 25F', hours: '17:00 – 01:00',
        desc: '시그니처 칵테일과 싱글몰트 셀렉션. 조용한 밤을 위한 어른의 라운지.',
        tags: ['시그니처 칵테일', '싱글몰트', '라이브 뮤직'] },
      { name: '그린하우스', kind: '올데이 카페 · 2F', hours: '조식 06:30 – 10:30 · 카페 10:30 – 20:00',
        desc: '식물로 둘러싸인 온실 카페. 갓 내린 커피와 조식 뷔페로 하루를 엽니다.',
        tags: ['조식 뷔페', '핸드드립 커피', '브런치'] },
    ],
  },
  en: {
    eyebrow: 'DINING', title: 'Dining', sub: 'From morning coffee to a late-night glass — three spaces.',
    outlets: [
      { name: 'The Table', kind: 'Fine dining · 25F', hours: 'Dinner 18:00 – 22:00 (last order 21:00)',
        desc: 'A seasonal tasting course against the city skyline. Dinner made an occasion.',
        tags: ['Tasting course', 'Wine pairing', 'City view'] },
      { name: 'Bar Social', kind: 'Bar & lounge · 25F', hours: '17:00 – 01:00',
        desc: 'Signature cocktails and a single-malt selection. A grown-up lounge for a quiet night.',
        tags: ['Signature cocktails', 'Single malt', 'Live music'] },
      { name: 'Greenhouse', kind: 'All-day café · 2F', hours: 'Breakfast 06:30 – 10:30 · Café 10:30 – 20:00',
        desc: 'A glasshouse café wrapped in greenery. Freshly brewed coffee and a breakfast buffet to open the day.',
        tags: ['Breakfast buffet', 'Hand-drip coffee', 'Brunch'] },
    ],
  },
}

export default function DiningPage() {
  const { lang } = useI18n()
  const c = CONTENT[lang] ?? CONTENT.ko
  return (
    <>
      <section className="page-hero" style={{ backgroundImage: `linear-gradient(180deg, rgba(16,18,24,.34), rgba(16,18,24,.64)), url(${diningRest})` }}>
        <div className="page-hero-inner">
          <div className="page-eyebrow">{c.eyebrow}</div>
          <h1 className="page-title">{c.title}</h1>
          <p className="page-sub">{c.sub}</p>
        </div>
      </section>

      <div className="detail-list">
        {c.outlets.map((o, i) => (
          <article key={o.name} className={`detail-row ${i % 2 ? 'reverse' : ''}`}>
            <div className="detail-photo" style={{ backgroundImage: `url(${IMAGES[i]})` }} />
            <div className="detail-body">
              <div className="detail-eyebrow">{o.kind}</div>
              <h2 className="detail-name">{o.name}</h2>
              <p className="detail-desc">{o.desc}</p>
              <div className="amenity-row">
                {o.tags.map((t) => <span key={t} className="amenity">{t}</span>)}
              </div>
              <div className="detail-foot">
                <div className="dining-hours">🕒 {o.hours}</div>
              </div>
            </div>
          </article>
        ))}
      </div>
    </>
  )
}
