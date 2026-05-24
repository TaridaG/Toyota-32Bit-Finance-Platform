const TOKEN_KEYS = ['finance.authToken', 'authToken', 'token']
const REFRESH_TOKEN_KEY = 'finance.refreshToken'
const STORAGE_MODE_KEY = 'finance.authStorageMode'
/** Legacy key (browser-only avatar); removed on logout / migration. */
const LEGACY_PROFILE_AVATAR_KEY = 'finance.profileAvatarDataUrl'

export type AuthStorageMode = 'local' | 'session'

export type AuthClaims = {
  sub?: string
  name?: string
  preferred_username?: string
  email?: string
  picture?: string
}

function resolveStorage(mode: AuthStorageMode): Storage {
  return mode === 'session' ? sessionStorage : localStorage
}

function readTokenFromStorage(storage: Storage): string | null {
  for (const key of TOKEN_KEYS) {
    const value = storage.getItem(key)
    if (typeof value === 'string' && value.trim().length > 0) {
      return value.trim()
    }
  }
  return null
}

function getStorageMode(): AuthStorageMode {
  return localStorage.getItem(STORAGE_MODE_KEY) === 'session' ? 'session' : 'local'
}

function getStoredToken(): string | null {
  const mode = getStorageMode()
  const primary = resolveStorage(mode)
  const found = readTokenFromStorage(primary)
  if (found) {
    return found
  }
  const fallback = resolveStorage(mode === 'session' ? 'local' : 'session')
  return readTokenFromStorage(fallback)
}

/** True when tokens are stored in localStorage with refresh (remember-me session). */
export function isRememberMeEnabled(): boolean {
  return getStorageMode() === 'local'
}

/** JWT `exp` in milliseconds since epoch, or null if missing / not a JWT. */
export function getAccessTokenExpiryMs(): number | null {
  const token = getStoredToken()
  if (!token) {
    return null
  }
  const payload = decodeJwtPayload(token)
  if (!payload) {
    return null
  }
  const raw = payload.exp
  const expSec =
    typeof raw === 'number' && Number.isFinite(raw)
      ? raw
      : typeof raw === 'string' && /^\d+$/.test(raw.trim())
        ? Number(raw.trim())
        : NaN
  if (Number.isFinite(expSec)) {
    return expSec * 1000
  }
  return null
}

function decodeJwtPayload(token: string): Record<string, unknown> | null {
  try {
    const parts = token.split('.')
    if (parts.length < 2) {
      return null
    }
    const segment = parts[1].replace(/-/g, '+').replace(/_/g, '/')
    const padded = segment.padEnd(segment.length + ((4 - (segment.length % 4)) % 4), '=')
    const json = atob(padded)
    return JSON.parse(json) as Record<string, unknown>
  } catch {
    return null
  }
}

function asRoleArray(value: unknown): string[] {
  if (!Array.isArray(value)) {
    return []
  }
  return value
    .filter((item): item is string => typeof item === 'string' && item.trim().length > 0)
    .map((item) => item.trim().toUpperCase())
}

/**
 * Realm / client roles from the access token (Keycloak: realm_access.roles, resource_access.*.roles).
 */
export function getAuthRoles(): string[] {
  const token = getStoredToken()
  if (!token) {
    return []
  }
  const payload = decodeJwtPayload(token)
  if (!payload) {
    return []
  }
  const roles = new Set<string>()
  const realmAccess = payload.realm_access
  if (realmAccess && typeof realmAccess === 'object' && realmAccess !== null && 'roles' in realmAccess) {
    asRoleArray((realmAccess as { roles?: unknown }).roles).forEach((r) => roles.add(r))
  }
  asRoleArray(payload.roles).forEach((r) => roles.add(r))
  const resourceAccess = payload.resource_access
  if (resourceAccess && typeof resourceAccess === 'object' && resourceAccess !== null) {
    for (const entry of Object.values(resourceAccess)) {
      if (entry && typeof entry === 'object' && entry !== null && 'roles' in entry) {
        asRoleArray((entry as { roles?: unknown }).roles).forEach((r) => roles.add(r))
      }
    }
  }
  return [...roles]
}

export function hasRealmRole(role: string): boolean {
  const normalized = role.trim().toUpperCase()
  if (!normalized) {
    return false
  }
  return getAuthRoles().includes(normalized)
}

/** Matches Keycloak realm role {@code ADMIN} used by the API gateway for /api/admin/**. */
export function isAdminUser(): boolean {
  return hasRealmRole('ADMIN')
}

export function getAuthClaims(): AuthClaims | null {
  const token = getStoredToken()
  if (!token) {
    return null
  }
  const payload = decodeJwtPayload(token)
  if (!payload) {
    return null
  }
  const str = (key: string) => (typeof payload[key] === 'string' ? (payload[key] as string) : undefined)
  return {
    sub: str('sub'),
    name: str('name'),
    preferred_username: str('preferred_username'),
    email: str('email'),
    picture: str('picture'),
  }
}

export function getProfileDisplayLabel(claims: AuthClaims | null): string {
  if (!claims) {
    return ''
  }
  if (claims.name?.trim()) {
    return claims.name.trim()
  }
  if (claims.preferred_username?.trim()) {
    return claims.preferred_username.trim()
  }
  if (claims.email?.trim()) {
    const local = claims.email.trim().split('@')[0]
    return local || claims.email.trim()
  }
  return ''
}

export function getProfileInitials(label: string): string {
  const t = label.trim()
  if (!t) {
    return '?'
  }
  const parts = t.split(/\s+/).filter(Boolean)
  if (parts.length >= 2) {
    const a = parts[0][0] ?? ''
    const b = parts[parts.length - 1][0] ?? ''
    return (a + b).toUpperCase().slice(0, 2)
  }
  return t.slice(0, 2).toUpperCase()
}

export type ProfileAvatarChangeDetail = { avatarUpdatedAt: string | null }

/**
 * Notifies the shell (header, etc.) that the server-side profile photo changed.
 * Optional {@link detail.avatarUpdatedAt} avoids an extra profile fetch.
 */
export function notifyProfileAvatarChanged(detail?: ProfileAvatarChangeDetail) {
  const payload: ProfileAvatarChangeDetail = detail ?? { avatarUpdatedAt: null }
  window.dispatchEvent(new CustomEvent('finance-profile-avatar', { detail: payload }))
}

/** Current access JWT from the active storage (local or session per remember-me). */
export function getAccessToken(): string | null {
  return getStoredToken()
}

export function isAuthenticated() {
  return getStoredToken() != null
}

export function getRefreshToken(): string | null {
  if (!isRememberMeEnabled()) {
    return null
  }
  const v = localStorage.getItem(REFRESH_TOKEN_KEY)
  return typeof v === 'string' && v.trim().length > 0 ? v.trim() : null
}

/** Updates access token only; leaves refresh token unchanged (e.g. partial token rotation). */
export function persistAuthToken(accessToken: string) {
  const storage = resolveStorage(getStorageMode())
  storage.setItem('finance.authToken', accessToken)
}

/**
 * Persists access token and optionally refresh token.
 * {@code rememberMe: false} → sessionStorage only, no refresh (browser tab session).
 */
export function persistAuthSession(tokens: {
  accessToken: string
  refreshToken?: string | null
  rememberMe?: boolean
}) {
  clearAuthSession()
  const remember = tokens.rememberMe === true
  const mode: AuthStorageMode = remember ? 'local' : 'session'
  localStorage.setItem(STORAGE_MODE_KEY, mode)
  const storage = resolveStorage(mode)
  storage.setItem('finance.authToken', tokens.accessToken)
  if (remember) {
    const refresh = tokens.refreshToken?.trim()
    if (refresh) {
      localStorage.setItem(REFRESH_TOKEN_KEY, refresh)
    }
  }
}

export function clearAuthSession() {
  for (const storage of [localStorage, sessionStorage]) {
    TOKEN_KEYS.forEach((key) => storage.removeItem(key))
    storage.removeItem(REFRESH_TOKEN_KEY)
  }
  localStorage.removeItem(STORAGE_MODE_KEY)
  try {
    localStorage.removeItem(LEGACY_PROFILE_AVATAR_KEY)
  } catch {
    // ignore
  }
}
