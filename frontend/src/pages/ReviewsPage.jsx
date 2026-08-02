import { useState } from 'react'
import diningCafe from '../assets/dining-cafe.jpg'

// 큐레이션 후기. 시연용이라 실계정 리뷰는 아니지만, 실제 투숙객이 남길 법한 톤으로.
// rating 은 1~5. category 로 필터한다.
const REVIEWS = [
  { name: '김서연', when: '2026.07 · 디럭스 더블', rating: 5, category: '커플',
    text: '창밖이 강남 한복판인데 방 안은 거짓말처럼 조용했어요. 오랜만에 알람 없이 푹 잤습니다. 조식의 커피가 특히 좋았어요.' },
  { name: '이준호', when: '2026.06 · 스탠다드 트윈', rating: 4, category: '출장',
    text: '체크인이 정말 빨랐습니다. 늦게 도착했는데 기다림 없이 바로 방으로. 업무용 데스크가 넓어서 노트북 작업하기 편했어요.' },
  { name: 'M. Tanaka', when: '2026.06 · 이그제큐티브 스위트', rating: 5, category: '가족',
    text: '거실이 분리돼 있어 아이가 자는 동안 따로 쉴 수 있었습니다. 파노라마 시티뷰는 밤이 더 예뻤어요. 다시 오고 싶은 곳.' },
  { name: '박지우', when: '2026.05 · 디럭스 더블', rating: 5, category: '커플',
    text: '기대 없이 갔다가 아침에 반했습니다. 그린하우스 조식이 계절 재료로 매일 다르다는 게 인상적. 군더더기 없는 미니멀한 무드가 좋았어요.' },
  { name: '정민아', when: '2026.05 · 스탠다드 트윈', rating: 4, category: '혼자',
    text: '혼자만의 시간이 필요해서 골랐는데 정확한 선택. 욕조에서 반신욕하고 바로 잔 하룻밤. 예약도 날짜만 고르면 돼서 간편했습니다.' },
  { name: 'S. Wilson', when: '2026.04 · 이그제큐티브 스위트', rating: 5, category: '출장',
    text: 'Quiet, clean, and effortless. The late checkout made my morning meeting stress-free. The lounge coffee is genuinely good.' },
]

const CATEGORIES = ['전체', '커플', '가족', '출장', '혼자']

function Stars({ rating }) {
  return (
    <span className="stars" role="img" aria-label={`별점 5점 만점에 ${rating}점`}>
      {'★★★★★'.slice(0, rating)}<span className="stars-off">{'★★★★★'.slice(rating)}</span>
    </span>
  )
}

// 후기 · 리뷰. 카테고리 필터 + 평균 평점 요약.
export default function ReviewsPage() {
  const [cat, setCat] = useState('전체')
  const list = cat === '전체' ? REVIEWS : REVIEWS.filter((r) => r.category === cat)
  const avg = (REVIEWS.reduce((s, r) => s + r.rating, 0) / REVIEWS.length).toFixed(1)

  return (
    <>
      <section
        className="page-hero"
        style={{ backgroundImage: `linear-gradient(180deg, rgba(16,18,24,.4), rgba(16,18,24,.64)), url(${diningCafe})` }}
      >
        <div className="page-hero-inner">
          <div className="page-eyebrow">REVIEWS · 투숙 후기</div>
          <h1 className="page-title">머문 분들의 이야기</h1>
          <p className="page-sub">가장 정직한 소개는 다녀간 손님의 말이라고 믿습니다.</p>
        </div>
      </section>

      <div className="section review-summary">
        <div className="review-score">
          <div className="review-avg">{avg}</div>
          <Stars rating={Math.round(avg)} />
          <div className="review-count">투숙 후기 {REVIEWS.length}건 · 시연용 큐레이션</div>
        </div>
      </div>

      <div className="section">
        <div className="review-filters" role="group" aria-label="후기 유형 필터">
          {CATEGORIES.map((c) => (
            <button
              key={c}
              type="button"
              className={`review-chip${cat === c ? ' active' : ''}`}
              aria-pressed={cat === c}
              onClick={() => setCat(c)}
            >
              {c}
            </button>
          ))}
        </div>

        <div className="review-grid">
          {list.map((r) => (
            <article key={r.name + r.when} className="review-card">
              <Stars rating={r.rating} />
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
