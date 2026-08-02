import { createContext, useContext, useEffect, useState } from 'react'
import { MESSAGES } from './messages.js'

const STORAGE_KEY = 'hotel.lang'
const I18nContext = createContext(null)

// 저장된 언어 → 브라우저 언어 → 한국어 순으로 초기값을 정한다.
function initialLang() {
  const saved = localStorage.getItem(STORAGE_KEY)
  if (saved === 'ko' || saved === 'en') return saved
  return navigator.language?.startsWith('en') ? 'en' : 'ko'
}

export function I18nProvider({ children }) {
  const [lang, setLangState] = useState(initialLang)

  // <html lang> 을 실제 언어와 맞춘다(스크린리더·SEO).
  useEffect(() => {
    document.documentElement.lang = lang
    localStorage.setItem(STORAGE_KEY, lang)
  }, [lang])

  const setLang = (l) => setLangState(l)
  const toggle = () => setLangState((l) => (l === 'ko' ? 'en' : 'ko'))

  // 키로 번역을 찾고, {name} 자리표시자를 vars 로 채운다. 키가 없으면 키 자체를 돌려준다.
  const t = (key, vars) => {
    let s = MESSAGES[lang]?.[key] ?? MESSAGES.ko[key] ?? key
    if (vars) for (const k of Object.keys(vars)) s = s.replaceAll(`{${k}}`, vars[k])
    return s
  }

  return (
    <I18nContext.Provider value={{ lang, setLang, toggle, t }}>
      {children}
    </I18nContext.Provider>
  )
}

export function useI18n() {
  const ctx = useContext(I18nContext)
  if (!ctx) throw new Error('useI18n must be used within I18nProvider')
  return ctx
}
