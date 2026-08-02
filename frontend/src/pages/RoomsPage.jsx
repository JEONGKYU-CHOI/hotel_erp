import { useEffect, useState } from 'react'
import { api } from '../api/client.js'
import { useBooking } from '../components/BookingContext.jsx'
import { useI18n } from '../i18n/I18nContext.jsx'
import roomStdt from '../assets/room-stdt.jpg'
import roomDlxd from '../assets/room-dlxd.jpg'
import roomExsu from '../assets/room-exsu.jpg'

// 이미지·1박 시작가는 언어 무관.
const IMAGES = { STDT: roomStdt, DLXD: roomDlxd, EXSU: roomExsu }
const PRICE_FROM = { STDT: 150000, DLXD: 220000, EXSU: 420000 }
const ORDER = ['STDT', 'DLXD', 'EXSU']

// 객실 코드별 상세 소개(이름·정원 숫자는 API 실데이터, 소개/스펙 텍스트는 여기서 언어별로).
const CONTENT = {
  ko: {
    eyebrow: 'ACCOMMODATION', title: '객실 안내', sub: '세 가지 결의 공간. 머무는 목적에 맞춰 고르세요.',
    specArea: '면적', specBed: '베드', specView: '전망', specOcc: '정원',
    occValue: (s, m) => `기준 ${s}인 · 최대 ${m}인`, priceUnit: '1박', priceSuffix: '원~', cta: '예약하기 →',
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

export default function RoomsPage() {
  const [types, setTypes] = useState([])
  const { openBooking } = useBooking()
  const { lang } = useI18n()
  const c = CONTENT[lang] ?? CONTENT.ko

  useEffect(() => {
    api.roomTypes().then(setTypes).catch(() => {})
  }, [])

  const byCode = Object.fromEntries(types.map((t) => [t.code, t]))

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
          return (
            <article key={code} className={`detail-row ${i % 2 ? 'reverse' : ''}`}>
              <div className="detail-photo" style={{ backgroundImage: `url(${IMAGES[code]})` }} />
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
    </>
  )
}
