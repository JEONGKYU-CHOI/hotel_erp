import { useEffect, useState } from 'react'
import hero1 from '../assets/hero.jpg'
import hero2 from '../assets/hero-2.jpg'
import hero3 from '../assets/hero-3.jpg'
import roomStdt from '../assets/room-stdt.jpg'
import roomDlxd from '../assets/room-dlxd.jpg'
import roomExsu from '../assets/room-exsu.jpg'
import diningRest from '../assets/dining-rest.jpg'
import diningBar from '../assets/dining-bar.jpg'
import diningCafe from '../assets/dining-cafe.jpg'
import facilityPool from '../assets/facility-pool.jpg'
import facilitySpa from '../assets/facility-spa.jpg'
import facilityLounge from '../assets/facility-lounge.jpg'
import facilityGym from '../assets/facility-gym.jpg'
import locationCity from '../assets/location-city.jpg'
import { useI18n } from '../i18n/I18nContext.jsx'

// 사진 — cat 은 언어 무관 키, caption 은 언어별. wide: 그리드에서 2칸 차지.
const PHOTOS = [
  { src: hero1, cat: 'exterior', wide: true, caption: { ko: '더 스테이 전경', en: 'The Stay exterior' } },
  { src: roomExsu, cat: 'rooms', caption: { ko: '이그제큐티브 스위트', en: 'Executive Suite' } },
  { src: facilityPool, cat: 'facilities', caption: { ko: '루프탑 인피니티 풀', en: 'Rooftop infinity pool' } },
  { src: diningRest, cat: 'dining', caption: { ko: '더 테이블 · 파인 다이닝', en: 'The Table · fine dining' } },
  { src: roomDlxd, cat: 'rooms', caption: { ko: '디럭스 더블', en: 'Deluxe Double' } },
  { src: facilitySpa, cat: 'facilities', caption: { ko: '스파 & 사우나', en: 'Spa & sauna' } },
  { src: hero2, cat: 'exterior', caption: { ko: '로비 라운지', en: 'Lobby lounge' } },
  { src: diningBar, cat: 'dining', caption: { ko: '바 소셜 · 라운지', en: 'Bar Social · lounge' } },
  { src: roomStdt, cat: 'rooms', caption: { ko: '스탠다드 트윈', en: 'Standard Twin' } },
  { src: facilityLounge, cat: 'facilities', wide: true, caption: { ko: '이그제큐티브 라운지', en: 'Executive lounge' } },
  { src: diningCafe, cat: 'dining', caption: { ko: '그린하우스 · 올데이 카페', en: 'Greenhouse · all-day café' } },
  { src: facilityGym, cat: 'facilities', caption: { ko: '24시 피트니스', en: '24h fitness' } },
  { src: locationCity, cat: 'exterior', caption: { ko: '강남의 밤', en: 'Gangnam at night' } },
  { src: hero3, cat: 'exterior', caption: { ko: '테라스', en: 'Terrace' } },
]

const CAT_KEYS = ['all', 'exterior', 'rooms', 'dining', 'facilities']

const CONTENT = {
  ko: {
    eyebrow: 'GALLERY · 갤러리', title: '머무는 순간의 풍경', sub: '객실부터 다이닝, 루프탑까지 — 더 스테이의 공간을 미리 둘러보세요.',
    closeLabel: '닫기',
    cats: { all: '전체', exterior: '전경', rooms: '객실', dining: '다이닝', facilities: '편의시설' },
  },
  en: {
    eyebrow: 'GALLERY', title: 'Scenes of the stay', sub: 'From rooms to dining to the rooftop — a look around The Stay before you arrive.',
    closeLabel: 'Close',
    cats: { all: 'All', exterior: 'Exterior', rooms: 'Rooms', dining: 'Dining', facilities: 'Facilities' },
  },
}

export default function GalleryPage() {
  const { lang } = useI18n()
  const c = CONTENT[lang] ?? CONTENT.ko
  const [cat, setCat] = useState('all')
  const [lightbox, setLightbox] = useState(null) // { src, caption }

  const photos = cat === 'all' ? PHOTOS : PHOTOS.filter((p) => p.cat === cat)

  // 라이트박스가 열려 있는 동안 배경 스크롤을 막고 ESC 로 닫는다.
  useEffect(() => {
    if (!lightbox) return
    const onKey = (e) => { if (e.key === 'Escape') setLightbox(null) }
    document.body.style.overflow = 'hidden'
    window.addEventListener('keydown', onKey)
    return () => {
      document.body.style.overflow = ''
      window.removeEventListener('keydown', onKey)
    }
  }, [lightbox])

  const cap = (p) => p.caption[lang] ?? p.caption.ko

  return (
    <>
      <section
        className="page-hero"
        style={{ backgroundImage: `linear-gradient(180deg, rgba(16,18,24,.36), rgba(16,18,24,.64)), url(${hero1})` }}
      >
        <div className="page-hero-inner">
          <div className="page-eyebrow">{c.eyebrow}</div>
          <h1 className="page-title">{c.title}</h1>
          <p className="page-sub">{c.sub}</p>
        </div>
      </section>

      <div className="section gallery-section">
        <div className="gallery-filters">
          {CAT_KEYS.map((k) => (
            <button
              key={k}
              type="button"
              className={`gallery-filter${cat === k ? ' active' : ''}`}
              aria-pressed={cat === k}
              onClick={() => setCat(k)}
            >
              {c.cats[k]}
            </button>
          ))}
        </div>

        <div className="gallery-grid">
          {photos.map((p) => (
            <button
              key={p.src}
              type="button"
              className={`gallery-tile${p.wide ? ' wide' : ''}`}
              style={{ backgroundImage: `url(${p.src})` }}
              onClick={() => setLightbox({ src: p.src, caption: cap(p) })}
              aria-label={cap(p)}
            >
              <span className="gallery-cap">{cap(p)}</span>
            </button>
          ))}
        </div>
      </div>

      {lightbox && (
        <div className="lightbox-overlay" onClick={() => setLightbox(null)}>
          <button type="button" className="lightbox-close" aria-label={c.closeLabel} onClick={() => setLightbox(null)}>✕</button>
          <figure className="lightbox-figure" onClick={(e) => e.stopPropagation()}>
            <img className="lightbox-img" src={lightbox.src} alt={lightbox.caption} />
            <figcaption className="lightbox-cap">{lightbox.caption}</figcaption>
          </figure>
        </div>
      )}
    </>
  )
}
