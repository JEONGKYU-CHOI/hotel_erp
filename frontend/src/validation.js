export const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/
export const KOREAN_MOBILE_PATTERN = /^010\d{8}$/

export function normalizePhone(value = '') {
  return value.replace(/\D/g, '').slice(0, 11)
}

export function formatKoreanMobile(value = '') {
  const digits = normalizePhone(value)
  if (digits.length <= 3) return digits
  if (digits.length <= 7) return `${digits.slice(0, 3)}-${digits.slice(3)}`
  return `${digits.slice(0, 3)}-${digits.slice(3, 7)}-${digits.slice(7)}`
}

export function isValidEmail(value = '') {
  return EMAIL_PATTERN.test(value.trim())
}

export function isValidKoreanMobile(value = '') {
  return KOREAN_MOBILE_PATTERN.test(normalizePhone(value))
}

