const TOKEN_KEY = 'token'
const REFRESH_KEY = 'refreshToken'
const USER_KEY = 'user'
const TOKENS_CHANGED = 'railflow-tokens-changed'

export { USER_KEY, TOKENS_CHANGED }

export function getToken() {
  return sessionStorage.getItem(TOKEN_KEY) || ''
}

export function getRefreshToken() {
  return sessionStorage.getItem(REFRESH_KEY) || ''
}

export function setTokens(token, refreshToken) {
  if (token) sessionStorage.setItem(TOKEN_KEY, token)
  else sessionStorage.removeItem(TOKEN_KEY)

  if (refreshToken) sessionStorage.setItem(REFRESH_KEY, refreshToken)
  else sessionStorage.removeItem(REFRESH_KEY)

  window.dispatchEvent(new Event(TOKENS_CHANGED))
}

export function clearTokens() {
  sessionStorage.removeItem(TOKEN_KEY)
  sessionStorage.removeItem(REFRESH_KEY)
  sessionStorage.removeItem(USER_KEY)
  window.dispatchEvent(new Event(TOKENS_CHANGED))
}
