import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { API_VERSION_PREFIX, withApiVersion } from './apiVersion'
import { isAccountFrozenApiError, isAccountRemovedApiError, logoutFrozenAccount, logoutRemovedAccount } from '../auth/accountFrozen'
import {
  clearAuthSession,
  getAccessToken,
  getAccessTokenExpiryMs,
  getRefreshToken,
  isAuthenticated,
  isRememberMeEnabled,
  persistAuthSession,
} from '../auth/session'

const DEFAULT_API_BASE_URL = ''
const LANGUAGE_STORAGE_KEY = 'finance.locale'
const CURRENCY_STORAGE_KEY = 'finance.currency'

const configuredBaseUrl = import.meta.env.VITE_API_BASE_URL?.trim()

export const normalizedBaseUrl =
  configuredBaseUrl && configuredBaseUrl.length > 0
    ? configuredBaseUrl.replace(/\/+$/, '')
    : DEFAULT_API_BASE_URL

export { API_VERSION_PREFIX }

export const apiClient = axios.create({
  baseURL: normalizedBaseUrl,
  timeout: 15000,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
})

/** No response interceptor — used only for refresh to avoid recursion. */
const refreshClient = axios.create({
  baseURL: normalizedBaseUrl,
  timeout: 15000,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json',
  },
})

type AuthRetryConfig = InternalAxiosRequestConfig & { _authRetry?: boolean }

type RefreshEnvelope = {
  success: boolean
  data?: {
    accessToken: string
    refreshToken?: string | null
    refreshExpiresIn?: number | null
  }
  error?: { message?: string }
}

let refreshInFlight: Promise<string | null> | null = null

async function refreshAccessToken(): Promise<string | null> {
  if (refreshInFlight) {
    return refreshInFlight
  }
  refreshInFlight = (async () => {
    try {
      const rt = getRefreshToken()
      if (!rt) {
        return null
      }
      const { data } = await refreshClient.post<RefreshEnvelope>(
        `${API_VERSION_PREFIX}/public/refresh`,
        { refreshToken: rt },
      )
      if (!data.success || !data.data?.accessToken) {
        return null
      }
      persistAuthSession({
        accessToken: data.data.accessToken,
        refreshToken: data.data.refreshToken,
        rememberMe: isRememberMeEnabled(),
      })
      return data.data.accessToken
    } catch {
      return null
    } finally {
      refreshInFlight = null
    }
  })()
  return refreshInFlight
}

/**
 * Before protected `/app` routes, optionally refresh when access JWT is close to expiry.
 * - If `exp` cannot be read: do not block (avoid false logouts); 401 interceptor still applies.
 * - If there is no refresh token: allow navigation while access is not yet expired (Keycloak must issue refresh for sliding sessions).
 * - If access is expired or within {@code minComfortableTtlMs} of expiry and a refresh token exists: refresh once.
 */
export async function ensureFreshAccessToken(minComfortableTtlMs = 600_000): Promise<boolean> {
  if (!isAuthenticated()) {
    return false
  }
  const expMs = getAccessTokenExpiryMs()
  const now = Date.now()

  if (expMs == null) {
    return true
  }

  if (expMs <= now) {
    const rt = getRefreshToken()
    if (!rt) {
      return false
    }
    return (await refreshAccessToken()) != null
  }

  if (expMs - now > minComfortableTtlMs) {
    return true
  }

  const rt = getRefreshToken()
  if (!rt) {
    return true
  }

  const rotated = await refreshAccessToken()
  if (rotated != null) {
    return true
  }
  // Keycloak geçici hata / ağ: access hâlâ geçerliyse uygulamayı düşürme (yanlış "session expired").
  return expMs > now
}

/** Guest-readable news stream only — not user favorites (require JWT). */
function isPublicNewsCatalogUrl(url: string): boolean {
  if (url.includes('/api/news/favorites')) {
    return false
  }
  return url.includes('/api/news') && !url.includes('/api/news/admin')
}

function isPublicPortalInfoCardsUrl(url: string): boolean {
  return url.includes('/api/portal/info-cards')
}

/** 401 on these URLs should not force logout (public catalog / auth endpoints). */
function isPublicDataOrAuthUrl(url: string): boolean {
  return (
    url.includes('/api/public/') ||
    url.includes('/api/market') ||
    url.includes('/api/rates') ||
    isPublicNewsCatalogUrl(url) ||
    isPublicPortalInfoCardsUrl(url) ||
    url.includes('/api/instruments') ||
    url.includes('/api/analytics')
  )
}

/** These routes must work without a token; a stale Bearer would make the gateway return 401. */
function isPublicAnonymousApiRequest(config: { baseURL?: string; url?: string }): boolean {
  const path = (config.baseURL ?? '') + (config.url ?? '')
  return (
    path.includes('/api/public/register') ||
    path.includes('/api/public/login') ||
    path.includes('/api/public/refresh')
  )
}

function requestPathForPublicRule(config: { baseURL?: string; url?: string }): string {
  const raw = `${config.baseURL ?? ''}${config.url ?? ''}`
  try {
    if (/^https?:\/\//i.test(raw)) {
      return normalizeApiPathForRules(new URL(raw).pathname)
    }
  } catch {
    /* ignore */
  }
  return normalizeApiPathForRules(raw)
}

/** Align public-route rules with gateway rewrite (/api/v1 → /api). */
function normalizeApiPathForRules(path: string): string {
  return path.replace(/\/api\/v1\//g, '/api/').replace(/\/api\/v1$/g, '/api')
}

/** Public catalog GETs: never send Bearer (stale JWT breaks gateway/resource-server before permitAll). */
function isPublicCatalogGetRequest(config: InternalAxiosRequestConfig): boolean {
  const method = (config.method ?? 'get').toLowerCase()
  if (method !== 'get') {
    return false
  }
  const path = requestPathForPublicRule(config)
  return (
    path.includes('/api/market') ||
    path.includes('/api/rates') ||
    path.includes('api/rates') ||
    isPublicNewsCatalogUrl(path) ||
    isPublicPortalInfoCardsUrl(path) ||
    path.includes('/api/instruments') ||
    path.includes('/api/analytics')
  )
}

function stripAuthorizationHeader(config: InternalAxiosRequestConfig) {
  const headers = config.headers
  if (!headers) {
    return
  }
  if (typeof headers.delete === 'function') {
    headers.delete('Authorization')
    headers.delete('authorization')
    return
  }
  const h = headers as Record<string, unknown>
  delete h.Authorization
  delete h.authorization
}

function hasHeaderValue(config: InternalAxiosRequestConfig, name: string): boolean {
  const headers = config.headers
  if (!headers) {
    return false
  }
  if (typeof headers.get === 'function') {
    const value = headers.get(name) ?? headers.get(name.toLowerCase())
    return value != null && String(value).trim().length > 0
  }
  const h = headers as Record<string, unknown>
  const value = h[name] ?? h[name.toLowerCase()]
  return value != null && String(value).trim().length > 0
}

function attachLocaleHeaders(config: InternalAxiosRequestConfig) {
  const language = window.localStorage.getItem(LANGUAGE_STORAGE_KEY)?.trim()
  const currency = window.localStorage.getItem(CURRENCY_STORAGE_KEY)?.trim()
  if (language && !hasHeaderValue(config, 'X-Language')) {
    config.headers['X-Language'] = language
  }
  if (currency && !hasHeaderValue(config, 'X-Currency')) {
    config.headers['X-Currency'] = currency
  }
}

apiClient.interceptors.request.use((config) => {
  if (config.url) {
    config.url = withApiVersion(config.url)
  }
  attachLocaleHeaders(config)

  if (isPublicAnonymousApiRequest(config)) {
    stripAuthorizationHeader(config)
    return config
  }

  if (isPublicCatalogGetRequest(config)) {
    stripAuthorizationHeader(config)
    return config
  }

  const token = getAccessToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

refreshClient.interceptors.request.use((config) => {
  attachLocaleHeaders(config)
  stripAuthorizationHeader(config)
  return config
})

apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const original = error.config as AuthRetryConfig | undefined
    const status = error.response?.status
    if (status === 403 && original) {
      const url = `${original.baseURL ?? ''}${original.url ?? ''}`
      if (!url.includes('/api/admin/')) {
        if (isAccountFrozenApiError(error)) {
          logoutFrozenAccount()
          return Promise.reject(error)
        }
        if (isAccountRemovedApiError(error)) {
          logoutRemovedAccount()
          return Promise.reject(error)
        }
      }
    }
    if (status !== 401 || !original) {
      return Promise.reject(error)
    }

    const url = `${original.baseURL ?? ''}${original.url ?? ''}`
    if (
      url.includes('/api/public/login') ||
      url.includes('/api/public/register') ||
      url.includes('/api/public/refresh')
    ) {
      return Promise.reject(error)
    }

    if (original._authRetry) {
      return Promise.reject(error)
    }
    original._authRetry = true

    const newAccess = await refreshAccessToken()
    if (!newAccess) {
      if (!isPublicDataOrAuthUrl(url)) {
        const expMs = getAccessTokenExpiryMs()
        const now = Date.now()
        const clientThinksAccessAlive = expMs != null && expMs > now + 60_000
        if (!clientThinksAccessAlive) {
          clearAuthSession()
          if (!window.location.pathname.includes('/login') && !window.location.pathname.includes('/register')) {
            window.location.assign('/login?session=expired')
          }
        }
      }
      return Promise.reject(error)
    }

    stripAuthorizationHeader(original)
    return apiClient.request(original)
  },
)
