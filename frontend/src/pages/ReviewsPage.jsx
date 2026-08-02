import { useState } from 'react'
import diningCafe from '../assets/dining-cafe.jpg'
import { useI18n } from '../i18n/I18nContext.jsx'

// 후기 데이터. 평점·카테고리 키는 언어 무관, 표시 텍스트만 언어별.
// category 키: couple/family/business/solo.
const CONTENT = {
  ko: {
    eyebrow: 'REVIEWS · 투숙 후기', title: '머문 분들의 이야기', sub: '가장 정직한 소개는 다녀간 손님의 말이라고 믿습니다.',
    countTpl: '투숙 후기 {n}건 · 시연용 큐레이션',
    cats: { all: '전체', couple: '커플', family: '가족', business: '출장', solo: '혼자' },
    filterLabel: '후기 유형 필터', starLabel: (r) => `별점 5점 만점에 ${r}점`,
    reviews: [
      { name: '김서연', when: '2026.07 · 디럭스 더블', rating: 5, category: 'couple', text: '창밖이 강남 한복판인데 방 안은 거짓말처럼 조용했어요. 오랜만에 알람 없이 푹 잤습니다. 조식의 커피가 특히 좋았어요.' },
      { name: '이준호', when: '2026.06 · 스탠다드 트윈', rating: 4, category: 'business', text: '체크인이 정말 빨랐습니다. 늦게 도착했는데 기다림 없이 바로 방으로. 업무용 데스크가 넓어서 노트북 작업하기 편했어요.' },
      { name: 'M. Tanaka', when: '2026.06 · 이그제큐티브 스위트', rating: 5, category: 'family', text: '거실이 분리돼 있어 아이가 자는 동안 따로 쉴 수 있었습니다. 파노라마 시티뷰는 밤이 더 예뻤어요. 다시 오고 싶은 곳.' },
      { name: '박지우', when: '2026.05 · 디럭스 더블', rating: 5, category: 'couple', text: '기대 없이 갔다가 아침에 반했습니다. 그린하우스 조식이 계절 재료로 매일 다르다는 게 인상적. 군더더기 없는 미니멀한 무드가 좋았어요.' },
      { name: '정민아', when: '2026.05 · 스탠다드 트윈', rating: 4, category: 'solo', text: '혼자만의 시간이 필요해서 골랐는데 정확한 선택. 욕조에서 반신욕하고 바로 잔 하룻밤. 예약도 날짜만 고르면 돼서 간편했습니다.' },
      { name: 'S. Wilson', when: '2026.04 · 이그제큐티브 스위트', rating: 5, category: 'business', text: '조용하고, 깔끔하고, 번거로움이 없었습니다. 레이트 체크아웃 덕에 아침 미팅이 여유로웠어요. 라운지 커피가 정말 좋습니다.' },
    ],
  },
  en: {
    eyebrow: 'REVIEWS · Guest stories', title: 'Stories from those who stayed', sub: 'We believe the most honest introduction is what a guest says after leaving.',
    countTpl: '{n} guest reviews · demo curation',
    cats: { all: 'All', couple: 'Couple', family: 'Family', business: 'Business', solo: 'Solo' },
    filterLabel: 'Filter reviews by type', starLabel: (r) => `Rated ${r} out of 5`,
    reviews: [
      { name: 'Seoyeon Kim', when: 'Jul 2026 · Deluxe Double', rating: 5, category: 'couple', text: 'The window looks onto the middle of Gangnam, yet the room was impossibly quiet. I slept deeply, no alarm, for the first time in ages. The breakfast coffee was especially good.' },
      { name: 'Junho Lee', when: 'Jun 2026 · Standard Twin', rating: 4, category: 'business', text: 'Check-in was genuinely fast. I arrived late and went straight to the room, no waiting. The wide work desk made laptop work easy.' },
      { name: 'M. Tanaka', when: 'Jun 2026 · Executive Suite', rating: 5, category: 'family', text: 'The separate living room let me rest while my child slept. The panoramic city view was even prettier at night. A place I want to return to.' },
      { name: 'Jiwoo Park', when: 'May 2026 · Deluxe Double', rating: 5, category: 'couple', text: 'I went with no expectations and fell for it by morning. Loved that the Greenhouse breakfast changes daily with the season. The minimal, uncluttered mood was perfect.' },
      { name: 'Mina Jung', when: 'May 2026 · Standard Twin', rating: 4, category: 'solo', text: 'I needed time alone and this was exactly right. A soak in the tub and straight to sleep. Booking was simple — just pick the dates.' },
      { name: 'S. Wilson', when: 'Apr 2026 · Executive Suite', rating: 5, category: 'business', text: 'Quiet, clean, and effortless. The late checkout made my morning meeting stress-free. The lounge coffee is genuinely good.' },
    ],
  },
}

function Stars({ rating, label }) {
  return (
    <span className="stars" role="img" aria-label={label(rating)}>
      {'★★★★★'.slice(0, rating)}<span className="stars-off">{'★★★★★'.slice(rating)}</span>
    </span>
  )
}

export default function ReviewsPage() {
  const { lang } = useI18n()
  const c = CONTENT[lang] ?? CONTENT.ko
  const [cat, setCat] = useState('all')
  const list = cat === 'all' ? c.reviews : c.reviews.filter((r) => r.category === cat)
  const avg = (c.reviews.reduce((s, r) => s + r.rating, 0) / c.reviews.length).toFixed(1)
  const catKeys = ['all', 'couple', 'family', 'business', 'solo']

  return (
    <>
      <section className="page-hero" style={{ backgroundImage: `linear-gradient(180deg, rgba(16,18,24,.4), rgba(16,18,24,.64)), url(${diningCafe})` }}>
        <div className="page-hero-inner">
          <div className="page-eyebrow">{c.eyebrow}</div>
          <h1 className="page-title">{c.title}</h1>
          <p className="page-sub">{c.sub}</p>
        </div>
      </section>

      <div className="section review-summary">
        <div className="review-score">
          <div className="review-avg">{avg}</div>
          <Stars rating={Math.round(avg)} label={c.starLabel} />
          <div className="review-count">{c.countTpl.replace('{n}', c.reviews.length)}</div>
        </div>
      </div>

      <div className="section">
        <div className="review-filters" role="group" aria-label={c.filterLabel}>
          {catKeys.map((k) => (
            <button
              key={k}
              type="button"
              className={`review-chip${cat === k ? ' active' : ''}`}
              aria-pressed={cat === k}
              onClick={() => setCat(k)}
            >
              {c.cats[k]}
            </button>
          ))}
        </div>

        <div className="review-grid">
          {list.map((r) => (
            <article key={r.name + r.when} className="review-card">
              <Stars rating={r.rating} label={c.starLabel} />
              <p className="review-text">{r.text}</p>
              <div className="review-meta">
                <span className="review-name">{r.name}</span>
                <span className="review-when">{r.when}</span>
              </div>
            </article>
          ))}
        </div>
      </div>
    </>
  )
}
