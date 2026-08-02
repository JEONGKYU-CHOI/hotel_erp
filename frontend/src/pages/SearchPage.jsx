import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api/client.js'
import { useBooking } from '../components/BookingContext.jsx'
import { useI18n } from '../i18n/I18nContext.jsx'
import { roomName } from '../i18n/messages.js'
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

// 객실타입 코드 → 소개 이미지. 문구는 i18n(home.roomDesc.*)에서 코드로 찾는다.
const ROOM_IMG = { STDT: roomStdt, DLXD: roomDlxd, EXSU: roomExsu }
const FALLBACK_IMG = roomStdt

// 홈 랜딩 티저용 요약 데이터. 표시문구는 i18n 키로, 이미지·아이콘은 여기서. 상세는 각 페이지에.
const HOME_FACILITIES = [
  { icon: '🏊', nameKey: 'home.fac.pool' },
  { icon: '🧖', nameKey: 'home.fac.spa' },
  { icon: '💪', nameKey: 'home.fac.fitness' },
  { icon: '🍸', nameKey: 'home.fac.lounge' },
]
const DINING_TEASER = [
  { img: diningRest, nameKey: 'home.dining.table.name', kindKey: 'home.dining.table.kind' },
  { img: diningBar, nameKey: 'home.dining.bar.name', kindKey: 'home.dining.bar.kind' },
  { img: diningCafe, nameKey: 'home.dining.cafe.name', kindKey: 'home.dining.cafe.kind' },
]

export default function SearchPage() {
  const [roomTypes, setRoomTypes] = useState([])
  const [heroIdx, setHeroIdx] = useState(0)
  const { openBooking } = useBooking()
  const { t, lang } = useI18n()

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
          <div className="hero-eyebrow">{t('hero.eyebrow')}</div>
          <h1 className="hero-title">{t('hero.title')}</h1>
          <p className="hero-sub">{t('hero.sub')}</p>
        </div>
        <div className="hero-dots">
          {HERO_IMAGES.map((_, i) => (
            <button
              key={i}
              type="button"
              className={`hero-dot${i === heroIdx ? ' active' : ''}`}
              onClick={() => setHeroIdx(i)}
              aria-label={t('home.heroSlide', { n: i + 1 })}
            />
          ))}
        </div>
      </section>

      {/* 홈 예약 바 — 예약 모달과 같은 BookingForm 을 공유한다. */}
      <BookingForm variant="bar" />

      {roomTypes.length > 0 && (
        <section className="showcase">
          <div className="showcase-head">
            <div className="showcase-eyebrow">{t('home.rooms.eyebrow')}</div>
            <h2 className="showcase-title">{t('home.rooms.title')}</h2>
            <p className="showcase-sub">{t('home.rooms.sub')}</p>
          </div>
          <div className="room-cards">
            {roomTypes.map((rt) => {
              const img = ROOM_IMG[rt.code] || FALLBACK_IMG
              const desc = rt.code && ROOM_IMG[rt.code]
                ? t(`home.roomDesc.${rt.code}`)
                : t('home.roomDesc.fallback')
              return (
                <article key={rt.id} className="room-card">
                  <div className="room-photo" style={{ backgroundImage: `url(${img})` }} />
                  <div className="room-body">
                    <h3 className="room-name">{roomName(lang, rt)}</h3>
                    <p className="room-desc">{desc}</p>
                    <div className="room-foot">
                      <span className="room-occ">{t('home.occ', { std: rt.standardOccupancy, max: rt.maxOccupancy })}</span>
                      <button type="button" className="room-cta" onClick={() => openBooking(rt.id)}>
                        {t('home.roomCta')}
                      </button>
                    </div>
                  </div>
                </article>
              )
            })}
          </div>
        </section>
      )}

      {/* 프로모션 티저 */}
      <section className="home-band">
        <Link to="/packages" className="promo-teaser">
          <div className="promo-teaser-body">
            <div className="showcase-eyebrow" style={{ color: 'var(--accent-dark)' }}>{t('home.packages.eyebrow')}</div>
            <h2 className="promo-teaser-title">{t('home.packages.title')}</h2>
            <p className="promo-teaser-sub">{t('home.packages.sub')}</p>
            <span className="band-link">{t('home.packages.link')}</span>
          </div>
        </Link>
      </section>

      {/* 편의시설 티저 */}
      <section className="home-band">
        <div className="band-head">
          <div>
            <div className="showcase-eyebrow">{t('home.facilities.eyebrow')}</div>
            <h2 className="block-title left">{t('home.facilities.title')}</h2>
          </div>
          <Link to="/facilities" className="band-link">{t('home.viewAll')}</Link>
        </div>
        <div className="facility-strip">
          {HOME_FACILITIES.map((f) => (
            <div key={f.nameKey} className="facility-item">
              <div className="facility-icon">{f.icon}</div>
              <div className="facility-name">{t(f.nameKey)}</div>
            </div>
          ))}
        </div>
      </section>

      {/* 다이닝 티저 */}
      <section className="home-band alt">
        <div className="band-head">
          <div>
            <div className="showcase-eyebrow">{t('home.dining.eyebrow')}</div>
            <h2 className="block-title left">{t('home.dining.title')}</h2>
          </div>
          <Link to="/dining" className="band-link">{t('home.viewAll')}</Link>
        </div>
        <div className="dining-teaser">
          {DINING_TEASER.map((o) => (
            <Link key={o.nameKey} to="/dining" className="teaser-card">
              <div className="teaser-photo" style={{ backgroundImage: `url(${o.img})` }} />
              <div className="teaser-body">
                <div className="teaser-kind">{t(o.kindKey)}</div>
                <div className="teaser-name">{t(o.nameKey)}</div>
              </div>
            </Link>
          ))}
        </div>
      </section>

      {/* 위치 티저 */}
      <section className="home-band">
        <Link to="/location" className="location-teaser" style={{ backgroundImage: `linear-gradient(90deg, rgba(16,18,24,.72), rgba(16,18,24,.35)), url(${locationCity})` }}>
          <div className="loc-teaser-inner">
            <div className="showcase-eyebrow" style={{ color: '#f0e4cf' }}>{t('home.location.eyebrow')}</div>
            <h2 className="loc-teaser-title">{t('home.location.title')}</h2>
            <p className="loc-teaser-sub">{t('home.location.sub')}</p>
            <span className="band-link light">{t('home.location.link')}</span>
          </div>
        </Link>
      </section>
    </>
  )
}
