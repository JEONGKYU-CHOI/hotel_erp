import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api/client.js'
import { useBooking } from '../components/BookingContext.jsx'
import BookingForm from '../components/BookingForm.jsx'
import hero1 from '../assets/hero.jpg'
import hero2 from '../assets/hero-2.jpg'
import hero3 from '../assets/hero-3.jpg'
import roomStdt from '../assets/room-stdt.jpg'
import roomDlxd from '../assets/room-dlxd.jpg'
import roomExsu from '../assets/room-exsu.jpg'
import diningRest from '../assets/dining-rest.jpg'
import diningBar from '../assets/dining-bar.jpg'
import diningCafe from '../assets/dining-cafe.jpg'
import locationCity from '../assets/location-city.jpg'

// 히어로 슬라이드(자동 전환).
const HERO_IMAGES = [hero1, hero2, hero3]

// 객실타입 코드 → 소개 이미지·문구.
const ROOM_META = {
  STDT: { img: roomStdt, desc: '도심 전망의 아늑한 기본 객실. 트윈 베드로 편안한 하룻밤.' },
  DLXD: { img: roomDlxd, desc: '넓은 창과 킹 사이즈 더블 베드를 갖춘 디럭스 객실.' },
  EXSU: { img: roomExsu, desc: '거실이 분리된 최상층 스위트. 파노라마 시티뷰.' },
}
const FALLBACK_META = { img: roomStdt, desc: '편안한 휴식을 위한 객실.' }

// 홈 랜딩 티저용 요약 데이터. 상세는 각 페이지(/facilities, /dining, /location)에 있다.
const HOME_FACILITIES = [
  { icon: '🏊', name: '루프탑 인피니티 풀' },
  { icon: '🧖', name: '스파 & 사우나' },
  { icon: '💪', name: '24시 피트니스' },
  { icon: '🍸', name: '이그제큐티브 라운지' },
]
const DINING_TEASER = [
  { img: diningRest, name: '더 테이블', kind: '파인 다이닝' },
  { img: diningBar, name: '바 소셜', kind: '바 & 라운지' },
  { img: diningCafe, name: '그린하우스', kind: '올데이 카페' },
]

export default function SearchPage() {
  const [roomTypes, setRoomTypes] = useState([])
  const [heroIdx, setHeroIdx] = useState(0)
  const { openBooking } = useBooking()

  useEffect(() => {
    api.roomTypes().then(setRoomTypes).catch(() => {})
  }, [])

  // 5초마다 다음 히어로 이미지로 자동 전환.
  useEffect(() => {
    const id = setInterval(() => setHeroIdx((i) => (i + 1) % HERO_IMAGES.length), 5000)
    return () => clearInterval(id)
  }, [])

  return (
    <>
      <section className="hero">
        <div className="hero-slides">
          {HERO_IMAGES.map((src, i) => (
            <div
              key={i}
              className={`hero-slide${i === heroIdx ? ' active' : ''}`}
              style={{ backgroundImage: `url(${src})` }}
              aria-hidden="true"
            />
          ))}
          <div className="hero-scrim" aria-hidden="true" />
        </div>
        <div className="hero-inner">
          <div className="hero-eyebrow">THE STAY · 도심 속 휴식</div>
          <h1 className="hero-title">머무는 순간, 여행이 됩니다</h1>
          <p className="hero-sub">
            날짜만 고르면 됩니다. 남은 객실을 실시간으로 확인하고 바로 예약하세요.
          </p>
        </div>
        <div className="hero-dots">
          {HERO_IMAGES.map((_, i) => (
            <button
              key={i}
              type="button"
              className={`hero-dot${i === heroIdx ? ' active' : ''}`}
              onClick={() => setHeroIdx(i)}
              aria-label={`히어로 슬라이드 ${i + 1}`}
            />
          ))}
        </div>
      </section>

      {/* 홈 예약 바 — 예약 모달과 같은 BookingForm 을 공유한다. */}
      <BookingForm variant="bar" />

      {roomTypes.length > 0 && (
        <section className="showcase">
          <div className="showcase-head">
            <div className="showcase-eyebrow">ROOMS · 객실 안내</div>
            <h2 className="showcase-title">세 가지 결의 휴식</h2>
            <p className="showcase-sub">도심의 하룻밤부터 최상층 스위트까지, 머무는 목적에 맞춰 고르세요.</p>
          </div>
          <div className="room-cards">
            {roomTypes.map((t) => {
              const meta = ROOM_META[t.code] || FALLBACK_META
              return (
                <article key={t.id} className="room-card">
                  <div className="room-photo" style={{ backgroundImage: `url(${meta.img})` }} />
                  <div className="room-body">
                    <h3 className="room-name">{t.name}</h3>
                    <p className="room-desc">{meta.desc}</p>
                    <div className="room-foot">
                      <span className="room-occ">기준 {t.standardOccupancy}인 · 최대 {t.maxOccupancy}인</span>
                      <button type="button" className="room-cta" onClick={() => openBooking(t.id)}>
                        예약하기 →
                      </button>
                    </div>
                  </div>
                </article>
              )
            })}
          </div>
        </section>
      )}

      {/* 편의시설 티저 */}
      <section className="home-band">
        <div className="band-head">
          <div>
            <div className="showcase-eyebrow">FACILITIES · 편의시설</div>
            <h2 className="block-title left">머무는 순간을 완성하는 것들</h2>
          </div>
          <Link to="/facilities" className="band-link">전체 보기 →</Link>
        </div>
        <div className="facility-strip">
          {HOME_FACILITIES.map((f) => (
            <div key={f.name} className="facility-item">
              <div className="facility-icon">{f.icon}</div>
              <div className="facility-name">{f.name}</div>
            </div>
          ))}
        </div>
      </section>

      {/* 다이닝 티저 */}
      <section className="home-band alt">
        <div className="band-head">
          <div>
            <div className="showcase-eyebrow">DINING · 다이닝</div>
            <h2 className="block-title left">하루를 여는 커피, 하루를 닫는 한 잔</h2>
          </div>
          <Link to="/dining" className="band-link">전체 보기 →</Link>
        </div>
        <div className="dining-teaser">
          {DINING_TEASER.map((o) => (
            <Link key={o.name} to="/dining" className="teaser-card">
              <div className="teaser-photo" style={{ backgroundImage: `url(${o.img})` }} />
              <div className="teaser-body">
                <div className="teaser-kind">{o.kind}</div>
                <div className="teaser-name">{o.name}</div>
              </div>
            </Link>
          ))}
        </div>
      </section>

      {/* 위치 티저 */}
      <section className="home-band">
        <Link to="/location" className="location-teaser" style={{ backgroundImage: `linear-gradient(90deg, rgba(16,18,24,.72), rgba(16,18,24,.35)), url(${locationCity})` }}>
          <div className="loc-teaser-inner">
            <div className="showcase-eyebrow" style={{ color: '#f0e4cf' }}>LOCATION · 오시는 길</div>
            <h2 className="loc-teaser-title">서울의 중심, 강남</h2>
            <p className="loc-teaser-sub">2호선 강남역 도보 5분 · 인천공항 리무진 70분</p>
            <span className="band-link light">오시는 길 보기 →</span>
          </div>
        </Link>
      </section>
    </>
  )
}
