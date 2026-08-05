import locationCity from '../assets/location-city.jpg'
import { useEffect, useRef, useState } from 'react'
import { useI18n } from '../i18n/I18nContext.jsx'

const HOTEL = { lat: 37.49794, lng: 127.02761 }

const CONTENT = {
  ko: {
    eyebrow: 'LOCATION', title: '위치 · 오시는 길', sub: '서울의 중심, 강남에서 만나는 도심 속 휴식.',
    addressLabel: '주소', addressValue: '서울특별시 강남구 테헤란로 000 (역삼동)\n더 스테이 호텔',
    contactLabel: '문의', contactValue: '프론트데스크 02-0000-0000 · 연중무휴 24시간',
    cinoutLabel: '체크인 / 체크아웃', cinoutValue: '체크인 15:00 · 체크아웃 11:00',
    pin: '📍 강남', directionsTitle: '오시는 길', nearbyTitle: '주변 명소',
    mapTitle: '더 스테이 호텔', copy: '주소 복사', copied: '복사됨', openMap: '카카오맵에서 보기', route: '길찾기', mapFallback: '지도를 불러오지 못했습니다. 아래 주소와 외부 지도 링크를 이용해 주세요.',
    transport: [
      { icon: '🚇', label: '지하철', desc: '2호선 강남역 3번 출구에서 도보 5분' },
      { icon: '✈️', label: '공항', desc: '인천국제공항 리무진 약 70분 · 김포공항 약 40분' },
      { icon: '🚌', label: '버스', desc: '강남역 정류장 간선·광역버스 다수 정차' },
      { icon: '🚗', label: '자가용', desc: '24시간 발렛 파킹 · 투숙객 주차 무료' },
    ],
    nearby: ['코엑스 몰', '봉은사', '가로수길', '선릉·정릉', '스타필드', '한강공원'],
  },
  en: {
    eyebrow: 'LOCATION', title: 'Location & directions', sub: 'Calm in the city, in the heart of Gangnam, Seoul.',
    addressLabel: 'Address', addressValue: 'Teheran-ro 000, Gangnam-gu, Seoul (Yeoksam-dong)\nThe Stay Hotel',
    contactLabel: 'Contact', contactValue: 'Front desk 02-0000-0000 · Open 24/7',
    cinoutLabel: 'Check-in / Check-out', cinoutValue: 'Check-in 15:00 · Check-out 11:00',
    pin: '📍 Gangnam', directionsTitle: 'Getting here', nearbyTitle: 'Nearby',
    mapTitle: 'The Stay Hotel', copy: 'Copy address', copied: 'Copied', openMap: 'Open Kakao Map', route: 'Directions', mapFallback: 'The map could not be loaded. Use the address and external map links below.',
    transport: [
      { icon: '🚇', label: 'Subway', desc: '5 min walk from Exit 3, Gangnam Station (Line 2)' },
      { icon: '✈️', label: 'Airport', desc: 'Incheon ~70 min by limousine · Gimpo ~40 min' },
      { icon: '🚌', label: 'Bus', desc: 'Many trunk & express lines stop at Gangnam Station' },
      { icon: '🚗', label: 'Car', desc: '24h valet parking · free for guests' },
    ],
    nearby: ['COEX Mall', 'Bongeunsa Temple', 'Garosu-gil', 'Seolleung Royal Tombs', 'Starfield', 'Han River Park'],
  },
}

function KakaoMap({ c }) {
  const mapRef = useRef(null)
  const [loaded, setLoaded] = useState(false)
  const [copied, setCopied] = useState(false)
  const key = import.meta.env.VITE_KAKAO_MAP_KEY

  useEffect(() => {
    if (!key) return
    const init = () => window.kakao.maps.load(() => {
      const position = new window.kakao.maps.LatLng(HOTEL.lat, HOTEL.lng)
      const map = new window.kakao.maps.Map(mapRef.current, { center: position, level: 3 })
      const marker = new window.kakao.maps.Marker({ position })
      marker.setMap(map)
      new window.kakao.maps.InfoWindow({ content: `<div class="kakao-info">${c.mapTitle}</div>` }).open(map, marker)
      setLoaded(true)
    })
    if (window.kakao?.maps) { init(); return }
    const script = document.createElement('script')
    script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${key}&autoload=false`
    script.async = true
    script.onload = init
    document.head.appendChild(script)
    return () => script.remove()
  }, [c.mapTitle, key])

  const address = c.addressValue.split('\n')[0]
  const mapUrl = `https://map.kakao.com/link/map/${encodeURIComponent(c.mapTitle)},${HOTEL.lat},${HOTEL.lng}`
  const routeUrl = `https://map.kakao.com/link/to/${encodeURIComponent(c.mapTitle)},${HOTEL.lat},${HOTEL.lng}`
  return (
    <section className="location-map-wrap" aria-label={c.mapTitle}>
      <div ref={mapRef} className={`location-map${loaded ? ' loaded' : ''}`}>
        {!loaded && <div className="map-fallback" style={{ backgroundImage: `linear-gradient(rgba(16,18,24,.55), rgba(16,18,24,.55)), url(${locationCity})` }}><span>{c.mapFallback}</span></div>}
      </div>
      <div className="map-actions">
        <button type="button" onClick={async () => { await navigator.clipboard.writeText(address); setCopied(true) }}>{copied ? c.copied : c.copy}</button>
        <a href={mapUrl} target="_blank" rel="noreferrer">{c.openMap}</a>
        <a href={routeUrl} target="_blank" rel="noreferrer">{c.route}</a>
      </div>
    </section>
  )
}

export default function LocationPage() {
  const { lang } = useI18n()
  const c = CONTENT[lang] ?? CONTENT.ko
  return (
    <>
      <section className="page-hero" style={{ backgroundImage: `linear-gradient(180deg, rgba(16,18,24,.40), rgba(16,18,24,.66)), url(${locationCity})` }}>
        <div className="page-hero-inner">
          <div className="page-eyebrow">{c.eyebrow}</div>
          <h1 className="page-title">{c.title}</h1>
          <p className="page-sub">{c.sub}</p>
        </div>
      </section>

      <div className="section">
        <div className="location-grid">
          <div className="location-info">
            <div className="loc-block">
              <div className="loc-label">{c.addressLabel}</div>
              <div className="loc-value" style={{ whiteSpace: 'pre-line' }}>{c.addressValue}</div>
            </div>
            <div className="loc-block">
              <div className="loc-label">{c.contactLabel}</div>
              <div className="loc-value">{c.contactValue}</div>
            </div>
            <div className="loc-block">
              <div className="loc-label">{c.cinoutLabel}</div>
              <div className="loc-value">{c.cinoutValue}</div>
            </div>
          </div>
          <KakaoMap c={c} />
        </div>

        <h3 className="block-title left">{c.directionsTitle}</h3>
        <div className="transport-grid">
          {c.transport.map((tr) => (
            <div key={tr.label} className="transport-item">
              <div className="transport-icon">{tr.icon}</div>
              <div>
                <div className="transport-label">{tr.label}</div>
                <div className="transport-desc">{tr.desc}</div>
              </div>
            </div>
          ))}
        </div>

        <h3 className="block-title left">{c.nearbyTitle}</h3>
        <div className="amenity-row">
          {c.nearby.map((n) => <span key={n} className="amenity">{n}</span>)}
        </div>
      </div>
    </>
  )
}
