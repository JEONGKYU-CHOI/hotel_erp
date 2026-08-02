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
import { useI18n } from './i18n/I18nContext.jsx'
import './booking.css'

// 상단바 우측 — 로그인 상태에 따라 "내 예약 + 이름/로그아웃" 또는 "로그인" 을 보인다.
function AuthNav() {
  const { member, logout } = useAuth()
  const { t } = useI18n()
  const navigate = useNavigate()

  if (member) {
    return (
      <>
        <Link to="/my-reservations">{t('nav.myReservations')}</Link>
        <span className="who">{member.name}{t('nav.honorific')}</span>
        <button type="button" className="linkbtn" onClick={() => { logout(); navigate('/') }}>
          {t('nav.logout')}
        </button>
      </>
    )
  }
  return <Link to="/member-login">{t('nav.login')}</Link>
}

// 언어 토글(한/EN). 상단바 유틸에 둔다.
function LangToggle() {
  const { toggle, t } = useI18n()
  return (
    <button type="button" className="lang-toggle" onClick={toggle} aria-label={t('lang.label')}>
      {t('lang.toggle')}
    </button>
  )
}

// 상시 "예약하기" — 데스크톱 상단바 CTA.
function ReserveCTA() {
  const { openBooking } = useBooking()
  const { t } = useI18n()
  return (
    <button type="button" className="reserve-cta" onClick={() => openBooking()}>
      {t('cta.reserve')}
    </button>
  )
}

// 모바일 전용 하단 고정 예약 바(엄지로 닿는 위치).
function MobileReserveBar() {
  const { openBooking } = useBooking()
  const { t } = useI18n()
  return (
    <div className="mobile-reserve">
      <button type="button" className="mobile-reserve-btn" onClick={() => openBooking()}>
        {t('cta.reserve')}
      </button>
    </div>
  )
}

// 푸터 "예약하기" — 모달을 연다.
function FooterReserve() {
  const { openBooking } = useBooking()
  const { t } = useI18n()
  return (
    <button type="button" className="foot-linkbtn" onClick={() => openBooking()}>
      {t('cta.reserve')}
    </button>
  )
}

// 부킹엔진 셸 — 상단 바 + 라우트. 예약은 어디서든 BookingProvider 의 모달로 통일한다.
function AppShell() {
  const { t } = useI18n()
  return (
    <BookingProvider>
      <div className="booking-shell">
        <a href="#main" className="skip-link">{t('a11y.skip')}</a>
        <header className="topbar">
          <Link to="/" className="brand">더 스테이</Link>
          <nav className="topnav topnav-main" aria-label={t('a11y.mainMenu')}>
            <Link to="/rooms">{t('nav.rooms')}</Link>
            <Link to="/packages">{t('nav.packages')}</Link>
            <Link to="/dining">{t('nav.dining')}</Link>
            <Link to="/facilities">{t('nav.facilities')}</Link>
            <Link to="/location">{t('nav.location')}</Link>
            <Link to="/gallery">{t('nav.gallery')}</Link>
            <Link to="/about">{t('nav.about')}</Link>
            {/* 후기 페이지 당분간 숨김 — 라우트·페이지는 유지, 링크만 감춤(복구 시 주석 해제) */}
            {/* <Link to="/reviews">{t('nav.reviews')}</Link> */}
          </nav>
          <nav className="topnav topnav-util" aria-label={t('a11y.utilMenu')}>
            <Link to="/faq">{t('nav.faq')}</Link>
            <Link to="/lookup">{t('nav.lookup')}</Link>
            <AuthNav />
            <LangToggle />
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
        <footer className="site-footer" aria-label={t('a11y.siteInfo')}>
          <div className="foot-grid">
            <div className="foot-col foot-about">
              <div className="foot-brand">더 스테이</div>
              <p className="foot-tagline">{t('footer.tagline')}</p>
            </div>
            <div className="foot-col">
              <h4>{t('footer.explore')}</h4>
              <Link to="/rooms">{t('nav.rooms')}</Link>
              <Link to="/packages">{t('nav.packages')}</Link>
              <Link to="/dining">{t('nav.dining')}</Link>
              <Link to="/facilities">{t('nav.facilities')}</Link>
              <Link to="/location">{t('nav.location')}</Link>
              <Link to="/gallery">{t('nav.gallery')}</Link>
              <Link to="/about">{t('nav.about')}</Link>
              {/* 후기 페이지 당분간 숨김 — 링크만 감춤(복구 시 주석 해제) */}
              {/* <Link to="/reviews">{t('nav.reviews')}</Link> */}
            </div>
            <div className="foot-col">
              <h4>{t('footer.booking')}</h4>
              <FooterReserve />
              <Link to="/lookup">{t('nav.lookup')}</Link>
              <Link to="/faq">{t('footer.faqLink')}</Link>
            </div>
            <div className="foot-col">
              <h4>{t('footer.contact')}</h4>
              <p className="foot-tagline">{t('footer.contactBody')}</p>
            </div>
          </div>
          <div className="foot-base">{t('footer.copyright')}</div>
        </footer>
        <MobileReserveBar />
      </div>
    </BookingProvider>
  )
}

export default AppShell
