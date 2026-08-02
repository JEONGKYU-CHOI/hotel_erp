import { Link, Route, Routes, useNavigate } from 'react-router-dom'
import SearchPage from './pages/SearchPage.jsx'
import RoomsPage from './pages/RoomsPage.jsx'
import PromotionsPage from './pages/PromotionsPage.jsx'
import DiningPage from './pages/DiningPage.jsx'
import FacilitiesPage from './pages/FacilitiesPage.jsx'
import LocationPage from './pages/LocationPage.jsx'
import FaqPage from './pages/FaqPage.jsx'
import GalleryPage from './pages/GalleryPage.jsx'
import AboutPage from './pages/AboutPage.jsx'
import ReviewsPage from './pages/ReviewsPage.jsx'
import BookingPage from './pages/BookingPage.jsx'
import PaymentSuccessPage from './pages/PaymentSuccessPage.jsx'
import PaymentFailPage from './pages/PaymentFailPage.jsx'
import LookupPage from './pages/LookupPage.jsx'
import LoginPage from './pages/LoginPage.jsx'
import SignupPage from './pages/SignupPage.jsx'
import MyReservationsPage from './pages/MyReservationsPage.jsx'
import { useAuth } from './auth/AuthContext.jsx'
import { BookingProvider, useBooking } from './components/BookingContext.jsx'
import './booking.css'

// 상단바 우측 — 로그인 상태에 따라 "내 예약 + 이름/로그아웃" 또는 "로그인" 을 보인다.
function AuthNav() {
  const { member, logout } = useAuth()
  const navigate = useNavigate()

  if (member) {
    return (
      <>
        <Link to="/my-reservations">내 예약</Link>
        <span className="who">{member.name}님</span>
        <button type="button" className="linkbtn" onClick={() => { logout(); navigate('/') }}>
          로그아웃
        </button>
      </>
    )
  }
  return <Link to="/member-login">로그인</Link>
}

// 상시 "예약하기" — 데스크톱 상단바 CTA.
function ReserveCTA() {
  const { openBooking } = useBooking()
  return (
    <button type="button" className="reserve-cta" onClick={() => openBooking()}>
      예약하기
    </button>
  )
}

// 모바일 전용 하단 고정 예약 바(엄지로 닿는 위치).
function MobileReserveBar() {
  const { openBooking } = useBooking()
  return (
    <div className="mobile-reserve">
      <button type="button" className="mobile-reserve-btn" onClick={() => openBooking()}>
        예약하기
      </button>
    </div>
  )
}

// 푸터 "예약하기" — 모달을 연다.
function FooterReserve() {
  const { openBooking } = useBooking()
  return (
    <button type="button" className="foot-linkbtn" onClick={() => openBooking()}>
      예약하기
    </button>
  )
}

// 부킹엔진 셸 — 상단 바 + 라우트. 예약은 어디서든 BookingProvider 의 모달로 통일한다.
function App() {
  return (
    <BookingProvider>
      <div className="booking-shell">
        <a href="#main" className="skip-link">본문 바로가기</a>
        <header className="topbar">
          <Link to="/" className="brand">더 스테이</Link>
          <nav className="topnav topnav-main" aria-label="주 메뉴">
            <Link to="/rooms">객실</Link>
            <Link to="/packages">프로모션</Link>
            <Link to="/dining">다이닝</Link>
            <Link to="/facilities">편의시설</Link>
            <Link to="/location">위치</Link>
            <Link to="/gallery">갤러리</Link>
            <Link to="/about">소개</Link>
            <Link to="/reviews">후기</Link>
          </nav>
          <nav className="topnav topnav-util" aria-label="예약 및 계정">
            <Link to="/faq">이용안내</Link>
            <Link to="/lookup">예약 조회</Link>
            <AuthNav />
            <ReserveCTA />
          </nav>
        </header>
        <main className="content" id="main">
          <Routes>
            <Route path="/" element={<SearchPage />} />
            <Route path="/rooms" element={<RoomsPage />} />
            <Route path="/packages" element={<PromotionsPage />} />
            <Route path="/dining" element={<DiningPage />} />
            <Route path="/facilities" element={<FacilitiesPage />} />
            <Route path="/location" element={<LocationPage />} />
            <Route path="/faq" element={<FaqPage />} />
            <Route path="/gallery" element={<GalleryPage />} />
            <Route path="/about" element={<AboutPage />} />
            <Route path="/reviews" element={<ReviewsPage />} />
            <Route path="/book" element={<BookingPage />} />
            <Route path="/payment/success" element={<PaymentSuccessPage />} />
            <Route path="/payment/fail" element={<PaymentFailPage />} />
            <Route path="/lookup" element={<LookupPage />} />
            <Route path="/member-login" element={<LoginPage />} />
            <Route path="/signup" element={<SignupPage />} />
            <Route path="/my-reservations" element={<MyReservationsPage />} />
          </Routes>
        </main>
        <footer className="site-footer" aria-label="사이트 정보">
          <div className="foot-grid">
            <div className="foot-col foot-about">
              <div className="foot-brand">더 스테이</div>
              <p>고요한 하룻밤, 정성스러운 아침.<br />서울 강남의 도심 속 휴식.</p>
            </div>
            <div className="foot-col">
              <h4>둘러보기</h4>
              <Link to="/rooms">객실</Link>
              <Link to="/packages">프로모션</Link>
              <Link to="/dining">다이닝</Link>
              <Link to="/facilities">편의시설</Link>
              <Link to="/location">위치</Link>
              <Link to="/gallery">갤러리</Link>
              <Link to="/about">소개</Link>
              <Link to="/reviews">후기</Link>
            </div>
            <div className="foot-col">
              <h4>예약</h4>
              <FooterReserve />
              <Link to="/lookup">예약 조회</Link>
              <Link to="/faq">이용안내 · FAQ</Link>
            </div>
            <div className="foot-col">
              <h4>문의</h4>
              <p>프론트데스크 02-0000-0000<br />연중무휴 24시간<br />서울 강남구 테헤란로 000</p>
            </div>
          </div>
          <div className="foot-base">© 2026 THE STAY · 데모 포트폴리오 사이트</div>
        </footer>
        <MobileReserveBar />
      </div>
    </BookingProvider>
  )
}

export default App
