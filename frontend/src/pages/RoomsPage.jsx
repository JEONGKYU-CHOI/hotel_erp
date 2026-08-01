import { useEffect, useState } from 'react'
import { api } from '../api/client.js'
import { useBooking } from '../components/BookingContext.jsx'
import roomStdt from '../assets/room-stdt.jpg'
import roomDlxd from '../assets/room-dlxd.jpg'
import roomExsu from '../assets/room-exsu.jpg'

// 객실타입 코드별 상세 소개. 이름·정원은 API(실데이터)에서 채우고, 소개/스펙은 여기서 얹는다.
const DETAILS = {
  STDT: {
    img: roomStdt, tagline: '도심 전망의 아늑한 트윈', size: '26㎡', bed: '트윈 베드', view: '시티뷰',
    priceFrom: 150000,
    desc: '첫 여행에도 부담 없는 기본 객실. 도심 전망과 트윈 베드로 편안한 하룻밤을 보장합니다.',
    amenities: ['무료 Wi-Fi', '스마트 TV', '미니바', '레인 샤워', '업무용 데스크'],
  },
  DLXD: {
    img: roomDlxd, tagline: '킹 베드의 디럭스 더블', size: '34㎡', bed: '킹 사이즈 더블', view: '넓은 창 · 시티뷰',
    priceFrom: 220000,
    desc: '넓은 창으로 도심을 담는 디럭스 객실. 킹 사이즈 침대와 여유로운 좌석 공간을 갖췄습니다.',
    amenities: ['무료 Wi-Fi', '65" 스마트 TV', '네스프레소', '욕조 + 레인 샤워', '라운지 체어'],
  },
  EXSU: {
    img: roomExsu, tagline: '거실이 분리된 최상층 스위트', size: '58㎡', bed: '킹 + 소파베드', view: '파노라마 시티뷰',
    priceFrom: 420000,
    desc: '최상층에 자리한 스위트. 분리된 거실과 파노라마 시티뷰로 특별한 순간을 완성합니다.',
    amenities: ['거실 분리형', '파노라마 창', '프리미엄 미니바', '대형 욕조', '웰컴 어메니티', '레이트 체크아웃'],
  },
}
const ORDER = ['STDT', 'DLXD', 'EXSU']

export default function RoomsPage() {
  const [types, setTypes] = useState([])
  const { openBooking } = useBooking()

  useEffect(() => {
    api.roomTypes().then(setTypes).catch(() => {})
  }, [])

  const byCode = Object.fromEntries(types.map((t) => [t.code, t]))

  return (
    <>
      <section className="page-hero" style={{ backgroundImage: `linear-gradient(180deg, rgba(16,18,24,.32), rgba(16,18,24,.62)), url(${roomExsu})` }}>
        <div className="page-hero-inner">
          <div className="page-eyebrow">ACCOMMODATION</div>
          <h1 className="page-title">객실 안내</h1>
          <p className="page-sub">세 가지 결의 공간. 머무는 목적에 맞춰 고르세요.</p>
        </div>
      </section>

      <div className="detail-list">
        {ORDER.map((code, i) => {
          const d = DETAILS[code]
          const t = byCode[code]
          return (
            <article key={code} className={`detail-row ${i % 2 ? 'reverse' : ''}`}>
              <div className="detail-photo" style={{ backgroundImage: `url(${d.img})` }} />
              <div className="detail-body">
                <div className="detail-eyebrow">{d.tagline}</div>
                <h2 className="detail-name">{t?.name || code}</h2>
                <p className="detail-desc">{d.desc}</p>
                <ul className="detail-specs">
                  <li><span>면적</span>{d.size}</li>
                  <li><span>베드</span>{d.bed}</li>
                  <li><span>전망</span>{d.view}</li>
                  <li><span>정원</span>기준 {t?.standardOccupancy ?? '-'}인 · 최대 {t?.maxOccupancy ?? '-'}인</li>
                </ul>
                <div className="amenity-row">
                  {d.amenities.map((a) => <span key={a} className="amenity">{a}</span>)}
                </div>
                <div className="detail-foot">
                  <div className="detail-price"><span>1박</span> {d.priceFrom.toLocaleString()}원~</div>
                  <button type="button" className="cta-dark" onClick={() => openBooking(t?.id)}>예약하기 →</button>
                </div>
              </div>
            </article>
          )
        })}
      </div>
    </>
  )
}
