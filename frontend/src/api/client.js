// 부킹엔진 백엔드(/api) 호출 래퍼.
//
// 개발 중엔 Vite 프록시가 /api 를 localhost:8080 으로 넘긴다(vite.config.js). 배포 시엔
// 같은 도메인이라 어느 쪽이든 상대경로 하나로 동작한다 — CORS 가 없다(D-001).
//
// 백엔드 오류 본문은 { code, message, fields } 형태다(ApiError). 실패하면 그 code/message 를
// 담은 Error 를 던져, 화면이 code 로 안내를 분기할 수 있게 한다.

const BASE = '/api'

async function request(path, { method = 'GET', body, params } = {}) {
  let url = BASE + path
  if (params) {
    const qs = new URLSearchParams(params).toString()
    if (qs) url += '?' + qs
  }

  const res = await fetch(url, {
    method,
    headers: body ? { 'Content-Type': 'application/json' } : undefined,
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
}
