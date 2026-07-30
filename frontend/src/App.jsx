import { Link, Route, Routes } from 'react-router-dom'
import SearchPage from './pages/SearchPage.jsx'
import BookingPage from './pages/BookingPage.jsx'
import PaymentSuccessPage from './pages/PaymentSuccessPage.jsx'
import PaymentFailPage from './pages/PaymentFailPage.jsx'
import LookupPage from './pages/LookupPage.jsx'
import './booking.css'

// 부킹엔진 셸 — 상단 바 + 라우트. 화면(가용→HOLD→결제→조회)은 단계별로 여기에 붙인다.
function App() {
  return (
    <div className="booking-shell">
      <header className="topbar">
        <Link to="/" className="brand">🏨 호텔 예약</Link>
        <nav className="topnav">
          <Link to="/">객실 검색</Link>
          <Link to="/lookup">예약 조회</Link>
        </nav>
      </header>
      <main className="content">
        <Routes>
          <Route path="/" element={<SearchPage />} />
          <Route path="/book" element={<BookingPage />} />
          <Route path="/payment/success" element={<PaymentSuccessPage />} />
          <Route path="/payment/fail" element={<PaymentFailPage />} />
          <Route path="/lookup" element={<LookupPage />} />
        </Routes>
      </main>
    </div>
  )
}

export default App
