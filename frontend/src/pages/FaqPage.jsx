import { Link } from 'react-router-dom'
import locationCity from '../assets/location-city.jpg'

// 이용 안내 요약(체크인·아웃, 정책 요약). 시연용 기준 정보.
const GUIDE = [
  { icon: '🕒', label: '체크인', value: '15:00부터' },
  { icon: '🌅', label: '체크아웃', value: '11:00까지' },
  { icon: '🍽️', label: '조식', value: '07:00 – 10:30 · 그린하우스' },
  { icon: '🚗', label: '주차', value: '발렛·자주식 24시간' },
  { icon: '🐾', label: '반려동물', value: '동반 불가(안내견 예외)' },
  { icon: '🚭', label: '흡연', value: '전 객실 금연' },
]

// 자주 묻는 질문. 취소/환불 항목은 백엔드 요금제 정책과 일치한다:
//   환불가능 요금제 → 체크인 1일 전까지 무료 취소 / 논리펀더블(NRF) → 환불 불가.
const FAQS = [
  {
    q: '체크인·체크아웃 시간은 어떻게 되나요?',
    a: '체크인은 오후 3시부터, 체크아웃은 오전 11시까지입니다. 얼리 체크인·레이트 체크아웃은 객실 상황에 따라 가능하며, 프론트데스크로 문의해 주세요.',
  },
  {
    q: '예약을 취소하면 환불되나요?',
    a: '요금제에 따라 다릅니다. “환불가능” 요금제는 체크인 1일 전까지 무료로 취소·환불됩니다. “논리펀더블 특가”처럼 환불 불가 조건으로 예약한 경우에는 취소해도 환불되지 않습니다. 예약 시 선택한 요금제의 조건이 그대로 적용됩니다.',
  },
  {
    q: '취소는 어디서 하나요?',
    a: '상단 “예약 조회”에서 예약번호와 연락처로 예약을 확인하고 취소할 수 있습니다. 회원으로 예약하셨다면 “내 예약”에서도 관리할 수 있습니다.',
  },
  {
    q: '조식은 포함되어 있나요?',
    a: '요금제에 따라 다릅니다. “조식 포함” 요금제는 2인 조식이 포함되며, 그린하우스에서 오전 7시부터 10시 30분까지 제공됩니다. 프로모션·패키지 페이지에서 조식 포함 상품을 한눈에 볼 수 있습니다.',
  },
  {
    q: '결제는 어떻게 하나요?',
    a: '예약 진행 시 임시 예약(HOLD)이 잡히고, 안내된 시각까지 카드로 결제하면 예약이 확정됩니다. 시간 내 결제하지 않으면 임시 예약은 자동으로 취소됩니다.',
  },
  {
    q: '비회원도 예약할 수 있나요?',
    a: '네, 회원가입 없이 예약할 수 있습니다. 예약 후에는 예약번호와 연락처로 “예약 조회”에서 확인하세요. 회원으로 로그인한 상태에서 예약하면 예약이 자동으로 회원 계정에 연결됩니다.',
  },
  {
    q: '아동·추가 인원 규정은 어떻게 되나요?',
    a: '객실별 기준 인원과 최대 인원이 정해져 있습니다(객실 안내 참고). 예약 시 성인·아동 인원을 입력하면 되며, 최대 인원을 초과하는 예약은 제한됩니다.',
  },
  {
    q: '주차와 Wi-Fi는 이용할 수 있나요?',
    a: '전 구역 초고속 Wi-Fi가 무료로 제공되며, 발렛·자주식 주차를 24시간 이용하실 수 있습니다. 자세한 편의시설은 편의시설 페이지를 참고하세요.',
  },
]

// 이용안내 · FAQ — 체크인/아웃·정책 요약과 자주 묻는 질문. 취소/환불은 백엔드 요금제 정책과 일치.
export default function FaqPage() {
  return (
    <>
      <section
        className="page-hero"
        style={{ backgroundImage: `linear-gradient(180deg, rgba(16,18,24,.4), rgba(16,18,24,.66)), url(${locationCity})` }}
      >
        <div className="page-hero-inner">
          <div className="page-eyebrow">GUIDE · 이용안내</div>
          <h1 className="page-title">이용안내 &amp; FAQ</h1>
          <p className="page-sub">체크인부터 취소·환불 규정까지, 머무시기 전 알아두면 좋은 것들.</p>
        </div>
      </section>

      <div className="section">
        <h2 className="block-title">한눈에 보는 이용안내</h2>
        <div className="guide-grid">
          {GUIDE.map((g) => (
            <div key={g.label} className="guide-item">
              <div className="guide-icon">{g.icon}</div>
              <div className="guide-text">
                <div className="guide-label">{g.label}</div>
                <div className="guide-value">{g.value}</div>
              </div>
            </div>
          ))}
        </div>
      </div>

      <div className="section faq-section">
        <h2 className="block-title">자주 묻는 질문</h2>
        <div className="faq-list">
          {FAQS.map((item) => (
            <details key={item.q} className="faq-item">
              <summary className="faq-q">
                <span>{item.q}</span>
                <span className="faq-mark" aria-hidden="true">+</span>
              </summary>
              <p className="faq-a">{item.a}</p>
            </details>
          ))}
        </div>
        <p className="faq-more">
          더 궁금한 점이 있으면 프론트데스크 <strong>02-0000-0000</strong>(연중무휴 24시간)로 문의하시거나,{' '}
          <Link to="/lookup">예약 조회</Link>에서 내 예약을 확인하세요.
        </p>
      </div>
    </>
  )
}
