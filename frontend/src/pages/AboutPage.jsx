import { Link } from 'react-router-dom'
import heroImg from '../assets/hero-2.jpg'
import loungeImg from '../assets/facility-lounge.jpg'

// 브랜드가 지키는 약속. 추상적인 구호 대신 손님이 실제로 겪는 것으로 적는다.
const VALUES = [
  {
    title: '고요를 설계합니다',
    body: '객실은 도심 한복판에 있지만 소음은 문 앞에서 멈춥니다. 이중 차음 창과 낮은 층고 조명으로, 자는 시간을 방해하지 않는 것을 첫 번째 원칙으로 둡니다.',
  },
  {
    title: '아침을 중요하게 여깁니다',
    body: '하루의 시작이 머문 기억을 정합니다. 그린하우스의 조식은 계절 재료로 매일 다르게 차리고, 커피는 원두를 그날 내립니다.',
  },
  {
    title: '군더더기를 덜어냅니다',
    body: '필요 없는 절차와 장식을 줄였습니다. 예약은 날짜만 고르면 되고, 체크인은 기다리지 않습니다. 비움이 곧 편안함이라고 믿습니다.',
  },
]

// 숫자로 보는 소개 — 과장 없이, 시연 기준의 값.
const FACTS = [
  { n: '3', label: '객실 타입', sub: '스탠다드 · 디럭스 · 스위트' },
  { n: '24h', label: '프론트데스크', sub: '연중무휴 응대' },
  { n: '5분', label: '강남역까지', sub: '2호선 도보' },
  { n: '2018', label: '문을 연 해', sub: '도심 속 휴식의 시작' },
]

// About · 브랜드 스토리. 시연용 콘텐츠지만 톤은 실제 호텔 소개처럼.
export default function AboutPage() {
  return (
    <>
      <section
        className="page-hero"
        style={{ backgroundImage: `linear-gradient(180deg, rgba(16,18,24,.34), rgba(16,18,24,.62)), url(${heroImg})` }}
      >
        <div className="page-hero-inner">
          <div className="page-eyebrow">ABOUT · 브랜드 스토리</div>
          <h1 className="page-title">머무는 순간이 여행이 되도록</h1>
          <p className="page-sub">더 스테이는 화려함보다 편안함을, 많음보다 알맞음을 택한 도심 호텔입니다.</p>
        </div>
      </section>

      <div className="section about-intro">
        <p className="about-lede">
          여행의 하이라이트는 늘 밖에 있다고들 하지만, 우리는 하루를 닫고 여는 <strong>그 방 안의 시간</strong>이
          여행의 질을 정한다고 생각했습니다. 그래서 더 스테이는 볼거리를 늘리는 대신, 잘 자고 잘 쉬는
          일에 집중했습니다.
        </p>
      </div>

      <div className="section about-split">
        <div className="about-photo" style={{ backgroundImage: `url(${loungeImg})` }} aria-hidden="true" />
        <div className="about-copy">
          <div className="block-eyebrow">OUR PHILOSOPHY</div>
          <h2 className="block-title left">덜어낼수록 편안해집니다</h2>
          <p>
            로비의 과한 장식도, 복잡한 체크인 절차도 없앴습니다. 대신 침구의 감촉, 물의 온도, 아침의 냄새처럼
            몸이 먼저 알아채는 것들에 예산을 썼습니다.
          </p>
          <p>
            도심 강남 한복판이라는 위치는 그대로 두되, 문을 닫는 순간 도시의 소음이 멈추도록 설계했습니다.
            바쁜 일정 사이, 온전히 나를 위한 하룻밤을 위한 공간입니다.
          </p>
        </div>
      </div>

      <div className="section">
        <h2 className="block-title">더 스테이가 지키는 세 가지</h2>
        <div className="values-grid">
          {VALUES.map((v) => (
            <article key={v.title} className="value-card">
              <h3 className="value-title">{v.title}</h3>
              <p className="value-body">{v.body}</p>
            </article>
          ))}
        </div>
      </div>

      <div className="section about-facts">
        <dl className="facts-grid">
          {FACTS.map((f) => (
            <div key={f.label} className="fact">
              <dt className="fact-n">{f.n}</dt>
              <dd className="fact-label">{f.label}<span className="fact-sub">{f.sub}</span></dd>
            </div>
          ))}
        </dl>
      </div>

      <div className="section about-cta">
        <h2 className="block-title">직접 머물러 확인해 보세요</h2>
        <p className="about-cta-sub">글로 옮긴 편안함은 결국 하룻밤으로 증명됩니다.</p>
        <div className="about-cta-links">
          <Link to="/rooms" className="cta-dark">객실 둘러보기 →</Link>
          <Link to="/location" className="cta-ghost">오시는 길</Link>
        </div>
      </div>
    </>
  )
}
