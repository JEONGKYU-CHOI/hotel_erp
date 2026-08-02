import { useEffect, useState } from 'react'

// 남은 초 계산 — 만료 지났으면 0.
function remainingMs(expiresAt) {
  return Math.max(0, new Date(expiresAt).getTime() - Date.now())
}

/**
 * HOLD 결제 만료까지 남은 시간을 mm:ss 로 1초마다 센다.
 *
 * 만료가 가까우면(기본 2분 이하) 경고색으로, 만료되면 안내 문구로 바뀐다.
 * 백엔드 holdExpiresAt(ISO) 하나만 받으면 되고, 0 이 되는 순간 onExpire 를 한 번 부른다
 * — 화면이 만료 상태(결제 버튼 숨김 등)로 스스로 넘어갈 수 있게.
 */
export default function HoldCountdown({ expiresAt, warnSeconds = 120, onExpire }) {
  const [ms, setMs] = useState(() => remainingMs(expiresAt))

  useEffect(() => {
    setMs(remainingMs(expiresAt))
    const id = setInterval(() => {
      const next = remainingMs(expiresAt)
      setMs(next)
      if (next <= 0) clearInterval(id)
    }, 1000)
    return () => clearInterval(id)
  }, [expiresAt])

  // 만료 전→후로 넘어가는 순간에만 onExpire. 렌더 중 부수효과를 피해 effect 로 뺀다.
  useEffect(() => {
    if (ms <= 0) onExpire?.()
  }, [ms <= 0]) // eslint-disable-line react-hooks/exhaustive-deps

  if (ms <= 0) {
    return <span className="hold-countdown expired">만료됨</span>
  }

  const totalSec = Math.floor(ms / 1000)
  const mm = String(Math.floor(totalSec / 60)).padStart(2, '0')
  const ss = String(totalSec % 60).padStart(2, '0')
  const warn = totalSec <= warnSeconds

  return (
    <span className={`hold-countdown${warn ? ' warn' : ''}`}
          role="timer" aria-live={warn ? 'assertive' : 'off'}>
      {mm}:{ss}
    </span>
  )
}
