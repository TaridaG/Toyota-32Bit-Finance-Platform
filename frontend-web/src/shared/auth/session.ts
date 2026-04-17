const TOKEN_KEYS = ['finance.authToken', 'authToken', 'token']

export function isAuthenticated() {
  return TOKEN_KEYS.some((key) => {
    const value = window.localStorage.getItem(key)
    return typeof value === 'string' && value.trim().length > 0
  })
}

export function persistAuthToken(token: string) {
  window.localStorage.setItem('finance.authToken', token)
}

export function clearAuthSession() {
  TOKEN_KEYS.forEach((key) => window.localStorage.removeItem(key))
}

