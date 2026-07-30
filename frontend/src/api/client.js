// 부킹엔진 백엔드(/api) 호출 래퍼.
//
// 개발 중엔 Vite 프록시가 /api 를 localhost:8080 으로 넘긴다(vite.config.js). 배포 시엔
// 같은 도메인이라 어느 쪽이든 상대경로 하나로 동작한다 — CORS 가 없다(D-001).
//
// 백엔드 오류 본문은 { code, message, fields } 형태다(ApiError). 실패하면 그 code/message 를
// 담은 Error 를 던져, 화면이 code 로 안내를 분기할 수 있게 한다.

const BASE = '/api'
const TOKEN_KEY = 'hotel.accessToken'

// Access Token 저장소(D-010, 리프레시 없음). localStorage 라 새로고침·탭 이동에도 로그인이
// 유지된다. 만료(1시간)되면 보호 경로가 401 을 주고, 화면이 재로그인으로 안내한다.
export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (t) => localStorage.setItem(TOKEN_KEY, t),
  clear: () => localStorage.removeItem(TOKEN_KEY),
}

async function request(path, { method = 'GET', body, params } = {}) {
  let url = BASE + path
  if (params) {
    const qs = new URLSearchParams(params).toString()
    if (qs) url += '?' + qs
  }

  // 토큰이 있으면 항상 실어 보낸다 — 백엔드는 /api 를 선택 인증으로 받으므로(비회원 경로 유지),
  // HOLD 등에 토큰이 붙으면 그 예약이 자동으로 회원과 연결된다(D-032, BookingApiController).
  const token = tokenStore.get()
  const headers = {}
  if (body) headers['Content-Type'] = 'application/json'
  if (token) headers['Authorization'] = `Bearer ${token}`

  const res = await fetch(url, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined,
  })

  const text = await res.text()
  const data = text ? JSON.parse(text) : null

  if (!res.ok) {
    const err = new Error(data?.message || `요청 실패 (${res.status})`)
    err.code = data?.code || 'ERROR'
    err.status = res.status
    err.fields = data?.fields
    throw err
  }
  return data
}

export const api = {
  roomTypes: () => request('/room-types'),
  ratePlans: (roomTypeId) => request(`/room-types/${roomTypeId}/rate-plans`),
  availability: (roomTypeId, checkIn, checkOut) =>
    request('/availability', { params: { roomTypeId, checkIn, checkOut } }),
  // HOLD 생성. 201 로 예약번호·총액·만료시각을 돌려준다.
  hold: (payload) => request('/reservations', { method: 'POST', body: payload }),
  // 결제창 초기화용 공개 클라이언트 키.
  paymentConfig: () => request('/payments/config'),
  // 결제 승인 → 예약 확정. 결제창 성공 후 paymentKey/orderId/amount 를 넘긴다.
  confirmPayment: (payload) => request('/payments/confirm', { method: 'POST', body: payload }),
  // 예약 조회 (예약번호 + 전화). 비회원 소유 확인 경로(D-028).
  lookup: (reservationNo, phone) =>
    request(`/reservations/${encodeURIComponent(reservationNo)}`, { params: { phone } }),

  // 회원 인증 (D-009). signup 은 201 로 MeResponse, login 은 TokenResponse 를 준다.
  signup: (payload) => request('/auth/signup', { method: 'POST', body: payload }),
  login: (payload) => request('/auth/login', { method: 'POST', body: payload }),
  // 현재 로그인 회원 · 회원 예약 목록. 둘 다 JWT 필요(없으면 401).
  me: () => request('/me'),
  myReservations: () => request('/me/reservations'),
}
