import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext.jsx'
import { useI18n } from '../i18n/I18nContext.jsx'

// 로그인. 성공하면 이전 화면(from)으로 돌려보낸다 — 없으면 홈으로.
export default function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const { state } = useLocation()
  const from = state?.from || '/'
  const { t } = useI18n()

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  async function submit(e) {
    e.preventDefault()
    setError(null)
    setSubmitting(true)
    try {
      await login(email, password)
      navigate(from, { replace: true })
    } catch (err) {
      setError(err.message)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="card auth-card">
      <h1>{t('auth.login.title')}</h1>
      <form className="book-form" onSubmit={submit}>
        <label>
          {t('auth.field.email')}
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)}
                 autoComplete="email" required />
        </label>
        <label>
          {t('auth.field.password')}
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)}
                 autoComplete="current-password" required />
        </label>
        {error && <p className="error">⚠ {error}</p>}
        <button type="submit" className="cta" disabled={submitting}>
          {submitting ? t('auth.loggingIn') : t('auth.login.submit')}
        </button>
      </form>
      <p className="muted">
        {t('auth.noAccount')} <Link to="/signup" state={{ from }}>{t('auth.signupLink')}</Link>
      </p>
    </div>
  )
}
