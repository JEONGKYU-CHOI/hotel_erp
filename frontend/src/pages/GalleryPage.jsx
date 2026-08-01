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

// 갤러리 사진 — 카테고리로 필터링한다. wide: 그리드에서 2칸을 차지(리듬).
const PHOTOS = [
  { src: hero1, cat: '전경', caption: '더 스테이 전경', wide: true },
  { src: roomExsu, cat: '객실', caption: '이그제큐티브 스위트' },
  { src: facilityPool, cat: '편의시설', caption: '루프탑 인피니티 풀' },
  { src: diningRest, cat: '다이닝', caption: '더 테이블 · 파인 다이닝' },
  { src: roomDlxd, cat: '객실', caption: '디럭스 더블' },
  { src: facilitySpa, cat: '편의시설', caption: '스파 & 사우나' },
  { src: hero2, cat: '전경', caption: '로비 라운지' },
  { src: diningBar, cat: '다이닝', caption: '바 소셜 · 라운지' },
  { src: roomStdt, cat: '객실', caption: '스탠다드 트윈' },
  { src: facilityLounge, cat: '편의시설', caption: '이그제큐티브 라운지', wide: true },
  { src: diningCafe, cat: '다이닝', caption: '그린하우스 · 올데이 카페' },
  { src: facilityGym, cat: '편의시설', caption: '24시 피트니스' },
  { src: locationCity, cat: '전경', caption: '강남의 밤' },
  { src: hero3, cat: '전경', caption: '테라스' },
]

const CATEGORIES = ['전체', '전경', '객실', '다이닝', '편의시설']

// 갤러리 — 카테고리 필터 + 라이트박스. 시연용 실사진(무료 상업용).
export default function GalleryPage() {
  const [cat, setCat] = useState('전체')
  const [lightbox, setLightbox] = useState(null) // { src, caption }

  const photos = cat === '전체' ? PHOTOS : PHOTOS.filter((p) => p.cat === cat)

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

  return (
    <>
      <section
        className="page-hero"
        style={{ backgroundImage: `linear-gradient(180deg, rgba(16,18,24,.36), rgba(16,18,24,.64)), url(${hero1})` }}
      >
        <div className="page-hero-inner">
          <div className="page-eyebrow">GALLERY · 갤러리</div>
          <h1 className="page-title">머무는 순간의 풍경</h1>
          <p className="page-sub">객실부터 다이닝, 루프탑까지 — 더 스테이의 공간을 미리 둘러보세요.</p>
        </div>
      </section>

      <div className="section gallery-section">
        <div className="gallery-filters">
          {CATEGORIES.map((c) => (
            <button
              key={c}
              type="button"
              className={`gallery-filter${cat === c ? ' active' : ''}`}
              onClick={() => setCat(c)}
            >
              {c}
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
              onClick={() => setLightbox(p)}
              aria-label={p.caption}
            >
              <span className="gallery-cap">{p.caption}</span>
            </button>
          ))}
        </div>
      </div>

      {lightbox && (
        <div className="lightbox-overlay" onClick={() => setLightbox(null)}>
          <button type="button" className="lightbox-close" aria-label="닫기" onClick={() => setLightbox(null)}>✕</button>
          <figure className="lightbox-figure" onClick={(e) => e.stopPropagation()}>
            <img className="lightbox-img" src={lightbox.src} alt={lightbox.caption} />
            <figcaption className="lightbox-cap">{lightbox.caption}</figcaption>
          </figure>
        </div>
      )}
    </>
  )
}
