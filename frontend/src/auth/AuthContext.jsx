import { createContext, useContext, useEffect, useState } from 'react'
import { api, tokenStore } from '../api/client.js'

// 로그인 상태·회원정보를 앱 전역에 공유한다. 토큰은 client.js 의 tokenStore(localStorage)가
// 진실의 원천이고, 이 컨텍스트는 그 위에 "현재 회원(me)" 를 얹어 화면들이 구독하게 한다.
const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [member, setMember] = useState(null)
  // 앱 시작 시 저장된 토큰으로 me 를 확인하는 동안 true. 이 사이 보호 화면은 깜빡임을 피한다.
  const [loading, setLoading] = useState(Boolean(tokenStore.get()))

  // 새로고침 후에도 토큰이 남아 있으면 me 로 세션을 복원한다. 토큰이 만료/무효면(401)
  // 조용히 로그아웃 상태로 떨어뜨린다.
  useEffect(() => {
    if (!tokenStore.get()) return
    api.me()
      .then(setMember)
      .catch(() => tokenStore.clear())
      .finally(() => setLoading(false))
  }, [])

  async function login(email, password) {
    const { accessToken } = await api.login({ email, password })
    tokenStore.set(accessToken)
    const me = await api.me()
    setMember(me)
    return me
  }

  function logout() {
    tokenStore.clear()
    setMember(null)
  }

  return (
    <AuthContext.Provider value={{ member, loading, login, logout, setMember }}>
      {children}
    </AuthContext.Provider>
  )
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth 는 AuthProvider 안에서만 쓸 수 있습니다.')
  return ctx
}
