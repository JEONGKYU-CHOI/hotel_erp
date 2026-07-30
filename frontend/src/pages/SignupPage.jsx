import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { api } from '../api/client.js'
import { useAuth } from '../auth/AuthContext.jsx'

// 회원가입. 백엔드는 가입만 하고 로그인시키지 않으므로(D-009), 성공 후 곧바로 로그인까지
// 이어 붙여 사용자를 원래 흐름(from)으로 돌려보낸다.
export default function SignupPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const { state } = useLocation()
  const from = state?.from || '/'

  const [form, setForm] = useState({ email: '', password: '', name: '', phone: '' })
  const [error, setError] = useState(null)
  const [fields, setFields] = useState(null)
  const [submitting, setSubmitting] = useState(false)

  const set = (k) => (e) => setForm((f) => ({ ...f, [k]: e.target.value }))

  async function submit(e) {
    e.preventDefault()
    setError(null)
    setFields(null)
    setSubmitting(true)
    try {
      await api.signup(form)
      await login(form.email, form.password)
      navigate(from, { replace: true })
    } catch (err) {
      setError(err.message)
      setFields(err.fields || null)
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="card auth-card">
      <h1>회원가입</h1>
      <form className="book-form" onSubmit={submit}>
        <label>
          이메일
          <input type="email" value={form.email} onChange={set('email')}
                 autoComplete="email" required />
          {fields?.email && <span className="field-error">{fields.email}</span>}
        </label>
        <label>
          비밀번호 <span className="muted">(8자 이상)</span>
          <input type="password" value={form.password} onChange={set('password')}
                 autoComplete="new-password" minLength={8} required />
          {fields?.password && <span className="field-error">{fields.password}</span>}
        </label>
        <label>
          이름
          <input value={form.name} onChange={set('name')} required />
          {fields?.name && <span className="field-error">{fields.name}</span>}
        </label>
        <label>
          연락처
          <input value={form.phone} onChange={set('phone')}
                 placeholder="010-1234-5678" required />
          {fields?.phone && <span className="field-error">{fields.phone}</span>}
        </label>
        {error && <p className="error">⚠ {error}</p>}
        <button type="submit" className="cta" disabled={submitting}>
          {submitting ? '가입 중…' : '가입하고 로그인'}
        </button>
      </form>
      <p className="muted">
        이미 계정이 있으신가요? <Link to="/login" state={{ from }}>로그인</Link>
      </p>
    </div>
  )
}
