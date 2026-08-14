import { useRef, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { api } from '../api/client.js'
import { useAuth } from '../auth/AuthContext.jsx'
import { useI18n } from '../i18n/I18nContext.jsx'
import { formatKoreanMobile, isValidEmail, isValidKoreanMobile, isValidPassword } from '../validation.js'

// 검증 실패 시 포커스를 옮길 순서. 화면에 보이는 위 → 아래 순서와 같게 유지한다.
const FIELD_ORDER = ['email', 'password', 'passwordConfirm', 'name', 'phone', 'gender', 'birthDate']

// 회원가입. 백엔드는 가입만 하고 로그인시키지 않으므로(D-009), 성공 후 곧바로 로그인까지
// 이어 붙여 사용자를 원래 흐름(from)으로 돌려보낸다.
export default function SignupPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const { state } = useLocation()
  const from = state?.from || '/'
  const { t } = useI18n()

  const [form, setForm] = useState({
    email: '', password: '', passwordConfirm: '', name: '', phone: '',
    gender: '', birthDate: '', marketingConsent: false,
  })
  const [error, setError] = useState(null)
  const [fields, setFields] = useState(null)
  const [submitting, setSubmitting] = useState(false)
  const [showPassword, setShowPassword] = useState(false)
  // 검증 실패한 첫 칸으로 포커스를 옮기기 위한 참조 모음.
  const refs = useRef({})

  const passwordOk = isValidPassword(form.password)
  const passwordMatch = form.passwordConfirm.length > 0 && form.passwordConfirm === form.password

  const set = (k) => (e) => setForm((f) => ({
    ...f,
    [k]: k === 'phone' ? formatKoreanMobile(e.target.value) : e.target.value,
  }))

  const genderLabel = (g) => t(`auth.gender.${g.toLowerCase()}`)

  async function submit(e) {
    e.preventDefault()
    setError(null)
    setFields(null)
    const today = new Date().toISOString().slice(0, 10)
    const v = {}
    // 빈 칸이면 "입력해주세요", 형식이 틀리면 형식 안내로 구분한다.
    if (!form.email.trim()) v.email = '이메일을 입력해주세요.'
    else if (!isValidEmail(form.email)) v.email = '이메일 형식이 올바르지 않습니다.'

    if (!form.password) v.password = '비밀번호를 입력해주세요.'
    else if (!isValidPassword(form.password)) v.password = '특수문자 + 숫자 포함 8자 이상 입력해주세요.'

    if (!form.passwordConfirm) v.passwordConfirm = '비밀번호 확인을 입력해주세요.'
    else if (form.passwordConfirm !== form.password) v.passwordConfirm = '비밀번호가 일치하지 않습니다.'

    if (!form.name.trim()) v.name = '이름을 입력해주세요.'

    if (!form.phone.trim()) v.phone = '연락처를 입력해주세요.'
    else if (!isValidKoreanMobile(form.phone)) v.phone = '휴대전화는 010-1234-5678 형식으로 입력해주세요.'

    if (!form.gender) v.gender = '성별을 선택해주세요.'

    if (!form.birthDate) v.birthDate = '생년월일을 입력해주세요.'
    else if (form.birthDate >= today) v.birthDate = '생년월일은 과거 날짜여야 합니다.'

    if (Object.keys(v).length) {
      setFields(v)
      // 위에서부터 처음으로 걸린 칸으로 포커스를 옮긴다.
      const firstKey = FIELD_ORDER.find((k) => v[k])
      const el = refs.current[firstKey]
      if (el && el.focus) el.focus()
      return
    }
    setSubmitting(true)
    try {
      const { passwordConfirm, ...payload } = form
      await api.signup(payload)
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
      <h1>{t('auth.signup.title')}</h1>
      <form className="book-form" onSubmit={submit} noValidate>
        <label>
          {t('auth.field.email')}
          <input type="email" value={form.email} onChange={set('email')}
                 ref={(el) => (refs.current.email = el)}
                 autoComplete="email" />
          {fields?.email && <span className="field-error">{fields.email}</span>}
        </label>
        <label>
          {t('auth.field.password')}
          <span className="password-wrap">
            <input type={showPassword ? 'text' : 'password'}
                   value={form.password} onChange={set('password')}
                   ref={(el) => (refs.current.password = el)}
                   placeholder={t('auth.password.placeholder')}
                   autoComplete="new-password" />
            {passwordOk && (
              <span className="password-check" aria-label="조건 충족" title="조건 충족">✓</span>
            )}
            <button type="button" className="password-toggle"
                    onClick={() => setShowPassword((s) => !s)}
                    aria-label={showPassword ? '비밀번호 숨기기' : '비밀번호 표시'}
                    title={showPassword ? '숨기기' : '표시'}>
              {showPassword ? '🙈' : '👁'}
            </button>
          </span>
          {fields?.password && <span className="field-error">{fields.password}</span>}
        </label>
        <label>
          {t('auth.field.passwordConfirm')}
          <span className="password-wrap">
            <input type={showPassword ? 'text' : 'password'}
                   value={form.passwordConfirm} onChange={set('passwordConfirm')}
                   ref={(el) => (refs.current.passwordConfirm = el)}
                   placeholder={t('auth.passwordConfirm.placeholder')}
                   autoComplete="new-password" style={{ paddingRight: '2.6rem' }} />
            {passwordMatch && (
              <span className="password-check confirm-check" aria-label="일치" title="일치">✓</span>
            )}
          </span>
          {fields?.passwordConfirm
            ? <span className="field-error">{fields.passwordConfirm}</span>
            : passwordMatch && <span className="field-ok">비밀번호가 일치합니다.</span>}
        </label>
        <label>
          {t('auth.field.name')}
          <input value={form.name} onChange={set('name')}
                 ref={(el) => (refs.current.name = el)} />
          {fields?.name && <span className="field-error">{fields.name}</span>}
        </label>
        <label>
          {t('auth.field.phone')}
          <input value={form.phone} onChange={set('phone')}
                 ref={(el) => (refs.current.phone = el)}
                 placeholder="010-1234-5678" inputMode="tel" autoComplete="tel"
                 maxLength={13} />
          {fields?.phone && <span className="field-error">{fields.phone}</span>}
        </label>
        <fieldset className="radio-group">
          <legend>{t('auth.field.gender')}</legend>
          <div className="radio-options">
            {['MALE', 'FEMALE'].map((g, i) => (
              <label key={g} className="radio-option">
                <input type="radio" name="gender" value={g}
                       checked={form.gender === g}
                       onChange={set('gender')}
                       ref={i === 0 ? (el) => (refs.current.gender = el) : undefined} />
                <span>{genderLabel(g)}</span>
              </label>
            ))}
          </div>
          {form.gender
            ? <span className="field-ok">{genderLabel(form.gender)} {t('auth.gender.selectedSuffix')}</span>
            : fields?.gender && <span className="field-error">{fields.gender}</span>}
        </fieldset>
        <label>
          {t('auth.field.birthDate')}
          <input type="date" value={form.birthDate} onChange={set('birthDate')}
                 ref={(el) => (refs.current.birthDate = el)}
                 max={new Date().toISOString().slice(0, 10)} autoComplete="bday" />
          {fields?.birthDate && <span className="field-error">{fields.birthDate}</span>}
        </label>
        <label className="checkbox-row">
          <input type="checkbox" checked={form.marketingConsent}
                 onChange={(e) => setForm((f) => ({ ...f, marketingConsent: e.target.checked }))} />
          <span>{t('auth.marketingConsent')}</span>
        </label>
        {error && <p className="error">⚠ {error}</p>}
        <button type="submit" className="cta" disabled={submitting}>
          {submitting ? t('auth.signingUp') : t('auth.signup.submit')}
        </button>
      </form>
      <p className="muted">
        {t('auth.haveAccount')} <Link to="/member-login" state={{ from }}>{t('auth.loginLink')}</Link>
      </p>
    </div>
  )
}
