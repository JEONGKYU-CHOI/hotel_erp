import locationCity from '../assets/location-city.jpg'

const TRANSPORT = [
  { icon: '🚇', label: '지하철', desc: '2호선 강남역 3번 출구에서 도보 5분' },
  { icon: '✈️', label: '공항', desc: '인천국제공항 리무진 약 70분 · 김포공항 약 40분' },
  { icon: '🚌', label: '버스', desc: '강남역 정류장 간선·광역버스 다수 정차' },
  { icon: '🚗', label: '자가용', desc: '24시간 발렛 파킹 · 투숙객 주차 무료' },
]
const NEARBY = ['코엑스 몰', '봉은사', '가로수길', '선릉·정릉', '스타필드', '한강공원']

export default function LocationPage() {
  return (
    <>
      <section className="page-hero" style={{ backgroundImage: `linear-gradient(180deg, rgba(16,18,24,.40), rgba(16,18,24,.66)), url(${locationCity})` }}>
        <div className="page-hero-inner">
          <div className="page-eyebrow">LOCATION</div>
          <h1 className="page-title">위치 · 오시는 길</h1>
          <p className="page-sub">서울의 중심, 강남에서 만나는 도심 속 휴식.</p>
        </div>
      </section>

      <div className="section">
        <div className="location-grid">
          <div className="location-info">
            <div className="loc-block">
              <div className="loc-label">주소</div>
              <div className="loc-value">서울특별시 강남구 테헤란로 000 (역삼동)<br />더 스테이 호텔</div>
            </div>
            <div className="loc-block">
              <div className="loc-label">문의</div>
              <div className="loc-value">프론트데스크 02-0000-0000 · 연중무휴 24시간</div>
            </div>
            <div className="loc-block">
              <div className="loc-label">체크인 / 체크아웃</div>
              <div className="loc-value">체크인 15:00 · 체크아웃 11:00</div>
            </div>
          </div>
          <div className="location-photo" style={{ backgroundImage: `url(${locationCity})` }}>
            <span className="map-pin">📍 강남</span>
          </div>
        </div>

        <h3 className="block-title left">오시는 길</h3>
        <div className="transport-grid">
          {TRANSPORT.map((t) => (
            <div key={t.label} className="transport-item">
              <div className="transport-icon">{t.icon}</div>
              <div>
                <div className="transport-label">{t.label}</div>
                <div className="transport-desc">{t.desc}</div>
              </div>
            </div>
          ))}
        </div>

        <h3 className="block-title left">주변 명소</h3>
        <div className="amenity-row">
          {NEARBY.map((n) => <span key={n} className="amenity">{n}</span>)}
        </div>
      </div>
    </>
  )
}
