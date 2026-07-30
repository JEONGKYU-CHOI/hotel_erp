import { Link, Route, Routes } from 'react-router-dom'
import SearchPage from './pages/SearchPage.jsx'
import './booking.css'

// 부킹엔진 셸 — 상단 바 + 라우트. 화면(가용→HOLD→결제→조회)은 단계별로 여기에 붙인다.
function App() {
  return (
    <div className="booking-shell">
      <header className="topbar">
        <Link to="/" className="brand">🏨 호텔 예약</Link>
      </header>
      <main className="content">
        <Routes>
          <Route path="/" element={<SearchPage />} />
        </Routes>
      </main>
    </div>
  )
}

export default App
