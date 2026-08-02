import { useEffect, useState } from 'react'
import { api } from '../api/client.js'
import { useBooking } from '../components/BookingContext.jsx'
import { useI18n } from '../i18n/I18nContext.jsx'
import roomStdt from '../assets/room-stdt.jpg'
import roomDlxd from '../assets/room-dlxd.jpg'
import roomExsu from '../assets/room-exsu.jpg'
import diningRest from '../assets/dining-rest.jpg'
import diningCafe from '../assets/dining-cafe.jpg'
import facilityPool from '../assets/facility-pool.jpg'
import facilitySpa from '../assets/facility-spa.jpg'
import facilityLounge from '../assets/facility-lounge.jpg'
import facilityGym from '../assets/facility-gym.jpg'
import locationCity from '../assets/location-city.jpg'

// 이미지·1박 시작가는 언어 무관.
const IMAGES = { STDT: roomStdt, DLXD: roomDlxd, EXSU: roomExsu }
const PRICE_FROM = { STDT: 150000, DLXD: 220000, EXSU: 420000 }
const ORDER = ['STDT', 'DLXD', 'EXSU']

// 객실별 상세 갤러리. 전용 촬영본이 없어 기존 데모 자원을 객실 성격에 맞게 큐레이션한다.
// l 은 언어무관 캡션 키(CONTENT.gcap 에서 언어별 라벨로 치환), 첫 장이 대표 이미지.
const GALLERY = {
  STDT: [
    { src: roomStdt, l: 'room' }, { src: locationCity, l: 'view' },
    { src: facilityGym, l: 'fitness' }, { src: diningCafe, l: 'cafe' },
  ],
  DLXD: [
    { src: roomDlxd, l: 'room' }, { src: locationCity, l: 'view' },
    { src: facilitySpa, l: 'spa' }, { src: facilityLounge, l: 'lounge' },
  ],
  EXSU: [
    { src: roomExsu, l: 'room' }, { src: facilityLounge, l: 'lounge' },
    { src: facilityPool, l: 'pool' }, { src: locationCity, l: 'view' }, { src: diningRest, l: 'dining' },
  ],
}

// 객실 코드별 상세 소개(이름·정원 숫자는 API 실데이터, 소개/스펙 텍스트는 여기서 언어별로).
const CONTENT = {
  ko: {
    eyebrow: 'ACCOMMODATION', title: '객실 안내', sub: '세 가지 결의 공간. 머무는 목적에 맞춰 고르세요.',
    specArea: '면적', specBed: '베드', specView: '전망', specOcc: '정원',
    occValue: (s, m) => `기준 ${s}인 · 최대 ${m}인`, priceUnit: '1박', priceSuffix: '원~', cta: '예약하기 →',
    closeLabel: '닫기', galleryLabel: '객실 사진', prevLabel: '이전 사진', nextLabel: '다음 사진',
    gcap: { room: '객실 전경', view: '도심 전망', fitness: '피트니스 센터', cafe: '올데이 카페', spa: '스파 & 사우나', lounge: '이그제큐티브 라운지', pool: '루프탑 풀', dining: '파인 다이닝' },
    rooms: {
      STDT: { name: '스탠다드 트윈', tagline: '도심 전망의 아늑한 트윈', size: '26㎡', bed: '트윈 베드', view: '시티뷰',
        desc: '첫 여행에도 부담 없는 기본 객실. 도심 전망과 트윈 베드로 편안한 하룻밤을 보장합니다.',
        amenities: ['무료 Wi-Fi', '스마트 TV', '미니바', '레인 샤워', '업무용 데스크'] },
      DLXD: { name: '디럭스 더블', tagline: '킹 베드의 디럭스 더블', size: '34㎡', bed: '킹 사이즈 더블', view: '넓은 창 · 시티뷰',
        desc: '넓은 창으로 도심을 담는 디럭스 객실. 킹 사이즈 침대와 여유로운 좌석 공간을 갖췄습니다.',
        amenities: ['무료 Wi-Fi', '65" 스마트 TV', '네스프레소', '욕조 + 레인 샤워', '라운지 체어'] },
      EXSU: { name: '이그제큐티브 스위트', tagline: '거실이 분리된 최상층 스위트', size: '58㎡', bed: '킹 + 소파베드', view: '파노라마 시티뷰',
        desc: '최상층에 자리한 스위트. 분리된 거실과 파노라마 시티뷰로 특별한 순간을 완성합니다.',
        amenities: ['거실 분리형', '파노라마 창', '프리미엄 미니바', '대형 욕조', '웰컴 어메니티', '레이트 체크아웃'] },
    },
  },
  en: {
    eyebrow: 'ACCOMMODATION', title: 'Rooms', sub: 'Three kinds of space — choose by the reason you travel.',
    specArea: 'Size', specBed: 'Bed', specView: 'View', specOcc: 'Occupancy',
    occValue: (s, m) => `Standard ${s} · Max ${m}`, priceUnit: 'Per night', priceSuffix: '~', cta: 'Book →',
    closeLabel: 'Close', galleryLabel: 'Room photos', prevLabel: 'Previous photo', nextLabel: 'Next photo',
    gcap: { room: 'Room', view: 'City view', fitness: 'Fitness center', cafe: 'All-day café', spa: 'Spa & sauna', lounge: 'Executive lounge', pool: 'Rooftop pool', dining: 'Fine dining' },
    rooms: {
      STDT: { name: 'Standard Twin', tagline: 'A cozy twin with a city view', size: '26㎡', bed: 'Twin beds', view: 'City view',
        desc: 'An easy first choice. A calm night guaranteed by a city view and two comfortable beds.',
        amenities: ['Free Wi-Fi', 'Smart TV', 'Minibar', 'Rain shower', 'Work desk'] },
      DLXD: { name: 'Deluxe Double', tagline: 'A deluxe double with a king bed', size: '34㎡', bed: 'King double', view: 'Wide window · city view',
        desc: 'A deluxe room framing the city through a wide window, with a king bed and a roomy seating area.',
        amenities: ['Free Wi-Fi', '65" Smart TV', 'Nespresso', 'Bathtub + rain shower', 'Lounge chair'] },
      EXSU: { name: 'Executive Suite', tagline: 'A top-floor suite with a separate living room', size: '58㎡', bed: 'King + sofa bed', view: 'Panoramic city view',
        desc: 'A top-floor suite. A separate living room and a panoramic city view make the moment special.',
        amenities: ['Separate living room', 'Panoramic windows', 'Premium minibar', 'Large bathtub', 'Welcome amenities', 'Late checkout'] },
    },
  },
}

// 객실 한 건의 상세 갤러리 — 대표 사진을 좌우(‹ ›) 버튼으로 넘긴다. 대표 클릭 시 라이트박스 확대.
// 여러 장일 때만 좌우 버튼·닷·썸네일이 뜬다(1장이면 단일 사진).
function RoomGallery({ images, galleryLabel, prevLabel, nextLabel, onOpen }) {
  const [i, setI] = useState(0)
  const n = images.length
  const idx = i % n // images 가 언어전환 등으로 바뀌어도 범위를 벗어나지 않게
  const active = images[idx]
  const go = (d) => setI((p) => (((p + d) % n) + n) % n) // 순환(끝에서 처음으로)

  return (
    <div className="detail-gallery">
      <div className="detail-main-wrap">
        <button type="button" className="detail-main" style={{ backgroundImage: `url(${active.src})` }}
                onClick={() => onOpen(active)} aria-label={active.caption}>
          <span className="detail-zoom" aria-hidden="true">⤢</span>
        </button>
        {n > 1 && (
          <>
            <button type="button" className="detail-nav prev" aria-label={prevLabel} onClick={() => go(-1)}>‹</button>
            <button type="button" className="detail-nav next" aria-label={nextLabel} onClick={() => go(1)}>›</button>
            <div className="detail-dots" aria-hidden="true">
              {images.map((im, k) => <span key={`${im.src}-${k}`} className={`detail-dot${k === idx ? ' active' : ''}`} />)}
            </div>
          </>
        )}
      </div>
      {n > 1 && (
        <div className="detail-thumbs" role="group" aria-label={galleryLabel}>
          {images.map((im, k) => (
            <button type="button" key={`${im.src}-${k}`}
                    className={`detail-thumb${k === idx ? ' active' : ''}`}
                    style={{ backgroundImage: `url(${im.src})` }}
                    aria-label={im.caption} aria-pressed={k === idx}
                    onClick={() => setI(k)} />
          ))}
        </div>
      )}
    </div>
  )
}

export default function RoomsPage() {
  const [types, setTypes] = useState([])
  const { openBooking } = useBooking()
  const { lang } = useI18n()
  const c = CONTENT[lang] ?? CONTENT.ko
  const [lightbox, setLightbox] = useState(null) // { src, caption }

  useEffect(() => {
    api.roomTypes().then(setTypes).catch(() => {})
  }, [])

  // 라이트박스가 열려 있는 동안 배경 스크롤을 막고 ESC 로 닫는다(GalleryPage 와 같은 규율).
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

  const byCode = Object.fromEntries(types.map((t) => [t.code, t]))

  // 객실별 갤러리에 언어별 캡션을 입혀 준다: "스탠다드 트윈 · 도심 전망".
  const galleryFor = (code) =>
    (GALLERY[code] ?? []).map((im) => ({ src: im.src, caption: `${c.rooms[code].name} · ${c.gcap[im.l]}` }))

  return (
    <>
      <section className="page-hero" style={{ backgroundImage: `linear-gradient(180deg, rgba(16,18,24,.32), rgba(16,18,24,.62)), url(${roomExsu})` }}>
        <div className="page-hero-inner">
          <div className="page-eyebrow">{c.eyebrow}</div>
          <h1 className="page-title">{c.title}</h1>
          <p className="page-sub">{c.sub}</p>
        </div>
      </section>

      <div className="detail-list">
        {ORDER.map((code, i) => {
          const d = c.rooms[code]
          const rt = byCode[code]
          // PMS 에 등록한 이미지(최대 3장)를 우선, 없으면 큐레이션 폴백(D-052).
          const dbImages = (rt?.imageUrls ?? []).map((url) => ({ src: url, caption: d.name }))
          const images = dbImages.length > 0 ? dbImages : galleryFor(code)
          return (
            <article key={code} className={`detail-row ${i % 2 ? 'reverse' : ''}`}>
              {images.length > 0
                ? <RoomGallery images={images} galleryLabel={c.galleryLabel}
                               prevLabel={c.prevLabel} nextLabel={c.nextLabel} onOpen={setLightbox} />
                : <div className="detail-photo" style={{ backgroundImage: `url(${IMAGES[code]})` }} />}
              <div className="detail-body">
                <div className="detail-eyebrow">{d.tagline}</div>
                <h2 className="detail-name">{d.name}</h2>
                <p className="detail-desc">{d.desc}</p>
                <ul className="detail-specs">
                  <li><span>{c.specArea}</span>{d.size}</li>
                  <li><span>{c.specBed}</span>{d.bed}</li>
                  <li><span>{c.specView}</span>{d.view}</li>
                  <li><span>{c.specOcc}</span>{c.occValue(rt?.standardOccupancy ?? '-', rt?.maxOccupancy ?? '-')}</li>
                </ul>
                <div className="amenity-row">
                  {d.amenities.map((a) => <span key={a} className="amenity">{a}</span>)}
                </div>
                <div className="detail-foot">
                  <div className="detail-price"><span>{c.priceUnit}</span> {PRICE_FROM[code].toLocaleString()}{c.priceSuffix}</div>
                  <button type="button" className="cta-dark" onClick={() => openBooking(rt?.id)}>{c.cta}</button>
                </div>
              </div>
            </article>
          )
        })}
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
