import { Link, Route, Routes, useNavigate } from 'react-router-dom'
import SearchPage from './pages/SearchPage.jsx'
import BookingPage from './pages/BookingPage.jsx'
import PaymentSuccessPage from './pages/PaymentSuccessPage.jsx'
import PaymentFailPage from './pages/PaymentFailPage.jsx'
import LookupPage from './pages/LookupPage.jsx'
import LoginPage from './pages/LoginPage.jsx'
import SignupPage from './pages/SignupPage.jsx'
import MyReservationsPage from './pages/MyReservationsPage.jsx'
import { useAuth } from './auth/AuthContext.jsx'
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
  return <Link to="/login">로그인</Link>
}

// 부킹엔진 셸 — 상단 바 + 라우트. 화면(가용→HOLD→결제→조회)은 단계별로 여기에 붙인다.
function App() {
  return (
    <div className="booking-shell">
      <header className="topbar">
        <Link to="/" className="brand">🏨 호텔 예약</Link>
        <nav className="topnav">
          <Link to="/">객실 검색</Link>
          <Link to="/lookup">예약 조회</Link>
          <AuthNav />
        </nav>
      </header>
      <main className="content">
        <Routes>
          <Route path="/" element={<SearchPage />} />
          <Route path="/book" element={<BookingPage />} />
          <Route path="/payment/success" element={<PaymentSuccessPage />} />
          <Route path="/payment/fail" element={<PaymentFailPage />} />
          <Route path="/lookup" element={<LookupPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/signup" element={<SignupPage />} />
          <Route path="/my-reservations" element={<MyReservationsPage />} />
        </Routes>
      </main>
    </div>
  )
}

export default App
