import { isAxiosError } from 'axios'
import { apiClient } from '../../../shared/api/client'
import type { PortalLoginTokens } from '../../../shared/api/publicAuth'
import type { ApiEnvelope, PortalProfile } from '../types'

const BASE = '/api/v1/portal/profile'
const PORTAL_PROFILE_BOOTSTRAP_TTL_MS = 10_000

let portalProfileBootstrapCache: PortalProfile | null = null
let portalProfileBootstrapExpiresAt = 0
let portalProfileBootstrapInFlight: Promise<PortalProfile> | null = null

/** Revokes trusted-device cookie server-side (httpOnly); call before clearAuthSession on logout. */
export async function logoutPortalSession(): Promise<void> {
  try {
    await apiClient.post<ApiEnvelope<unknown>>(`${BASE}/logout`)
  } catch {
    // Local session is cleared regardless
  }
}

export function readApiErrorMessage(error: unknown): string {
  if (isAxiosError(error)) {
    const body = error.response?.data as ApiEnvelope<unknown> | undefined
    const msg = body?.error?.message
    if (typeof msg === 'string' && msg.length > 0) {
      return msg
    }
    return error.message
  }
  if (error instanceof Error) {
    return error.message
  }
  return 'Request failed'
}

function assertSuccessData<T>(body: ApiEnvelope<T>): T {
  if (!body.success) {
    throw new Error(body.error?.message ?? 'Request failed')
  }
  return body.data as T
}

function assertSuccessOnly(body: ApiEnvelope<unknown>): void {
  if (!body.success) {
    throw new Error(body.error?.message ?? 'Request failed')
  }
}

export async function fetchPortalProfile(): Promise<PortalProfile> {
  const { data } = await apiClient.get<ApiEnvelope<PortalProfile>>(BASE)
  return assertSuccessData(data)
}

export function primePortalProfileBootstrap(profile: PortalProfile): PortalProfile {
  portalProfileBootstrapCache = profile
  portalProfileBootstrapExpiresAt = Date.now() + PORTAL_PROFILE_BOOTSTRAP_TTL_MS
  return profile
}

export async function fetchPortalProfileBootstrap(forceRefresh = false): Promise<PortalProfile> {
  const now = Date.now()
  if (!forceRefresh && portalProfileBootstrapCache && portalProfileBootstrapExpiresAt > now) {
    return portalProfileBootstrapCache
  }
  if (!forceRefresh && portalProfileBootstrapInFlight) {
    return portalProfileBootstrapInFlight
  }
  portalProfileBootstrapInFlight = fetchPortalProfile()
    .then((profile) => primePortalProfileBootstrap(profile))
    .finally(() => {
      portalProfileBootstrapInFlight = null
    })
  return portalProfileBootstrapInFlight
}

export async function changePortalPassword(currentPassword: string, newPassword: string): Promise<void> {
  const { data } = await apiClient.post<ApiEnvelope<unknown>>(`${BASE}/password`, {
    currentPassword,
    newPassword,
  })
  assertSuccessOnly(data)
}

export async function sendPortalPasswordResetCode(): Promise<{ expiresInSeconds: number; resendInSeconds: number }> {
  const { data } = await apiClient.post<ApiEnvelope<{ expiresInSeconds: number; resendInSeconds: number }>>(
    `${BASE}/password/send-reset-code`,
  )
  return assertSuccessData(data)
}

export async function resetPortalPasswordForgot(verificationCode: string, newPassword: string): Promise<void> {
  const { data } = await apiClient.post<ApiEnvelope<unknown>>(`${BASE}/password/reset-forgot`, {
    verificationCode,
    newPassword,
  })
  assertSuccessOnly(data)
}

export async function sendPortalEmailChangeCode(newEmail: string): Promise<{
  expiresInSeconds: number
  resendInSeconds: number
}> {
  const { data } = await apiClient.post<ApiEnvelope<{ expiresInSeconds: number; resendInSeconds: number }>>(
    `${BASE}/email/send-code`,
    { newEmail },
  )
  return assertSuccessData(data)
}

export async function confirmPortalEmailChange(newEmail: string, verificationCode: string): Promise<PortalProfile> {
  const { data } = await apiClient.post<ApiEnvelope<PortalProfile>>(`${BASE}/email/confirm`, {
    newEmail,
    verificationCode,
  })
  return assertSuccessData(data)
}

export type UsernameAvailabilityResult = {
  normalizedUsername: string
  available: boolean
  suggestions: string[]
}

export type EmailAvailabilityResult = {
  normalizedEmail: string
  available: boolean
  blocked: boolean
  suggestions: string[]
}

export async function checkPortalEmailAvailability(
  email: string,
  currentEmail?: string,
): Promise<EmailAvailabilityResult> {
  const { data } = await apiClient.get<
    ApiEnvelope<{ normalizedEmail: string; available: boolean; blocked: boolean; suggestions: string[] }>
  >(`${BASE}/email-availability`, { params: { email } })
  const result = assertSuccessData(data)
  const normalized = result.normalizedEmail || email.trim().toLowerCase()
  const isCurrent = currentEmail != null && normalized === currentEmail.trim().toLowerCase()
  return {
    normalizedEmail: normalized,
    available: result.available || isCurrent,
    blocked: result.blocked,
    suggestions: isCurrent ? [] : Array.isArray(result.suggestions) ? result.suggestions : [],
  }
}

export async function checkPortalUsernameAvailability(
  username: string,
  currentUsername?: string,
): Promise<UsernameAvailabilityResult> {
  try {
    const { data } = await apiClient.get<
      ApiEnvelope<{ normalizedUsername: string; available: boolean; suggestions: string[] }>
    >(`${BASE}/username-availability`, { params: { username } })
    const result = assertSuccessData(data)
    return {
      normalizedUsername: result.normalizedUsername,
      available: result.available,
      suggestions: Array.isArray(result.suggestions) ? result.suggestions : [],
    }
  } catch {
    const { checkUsernameAvailability } = await import('../../../shared/api/publicRegistration')
    const pub = await checkUsernameAvailability(username)
    const normalized = pub.normalizedUsername || username.trim().toLowerCase()
    const isCurrent =
      currentUsername != null && normalized === currentUsername.trim().toLowerCase()
    return {
      normalizedUsername: normalized,
      available: pub.available || isCurrent,
      suggestions: isCurrent ? [] : pub.suggestions,
    }
  }
}

export async function changePortalUsername(
  newUsername: string,
  currentPassword: string,
): Promise<PortalLoginTokens> {
  const { data } = await apiClient.post<ApiEnvelope<PortalLoginTokens>>(`${BASE}/username`, {
    newUsername,
    currentPassword,
  })
  return assertSuccessData(data)
}

export async function updatePortalPhone(phone: string): Promise<PortalProfile> {
  const { data } = await apiClient.put<ApiEnvelope<PortalProfile>>(`${BASE}/phone`, { phone })
  return assertSuccessData(data)
}

export async function updatePortalNotifications(
  notifySecurityAlerts: boolean,
  notifyWatchlistAlerts: boolean,
  notifyAlarmAlerts: boolean,
): Promise<PortalProfile> {
  const { data } = await apiClient.put<ApiEnvelope<PortalProfile>>(`${BASE}/notifications`, {
    notifySecurityAlerts,
    notifyWatchlistAlerts,
    notifyAlarmAlerts,
  })
  return assertSuccessData(data)
}

export async function updatePortalPreferences(
  preferredLocale: string,
  preferredCurrency: string,
): Promise<PortalProfile> {
  const { data } = await apiClient.put<ApiEnvelope<PortalProfile>>(`${BASE}/preferences`, {
    preferredLocale,
    preferredCurrency,
  })
  return assertSuccessData(data)
}

export async function uploadPortalAvatar(file: File): Promise<PortalProfile> {
  const formData = new FormData()
  formData.append('file', file)
  const { data } = await apiClient.post<ApiEnvelope<PortalProfile>>(`${BASE}/avatar`, formData, {
    timeout: 120_000,
    transformRequest: [
      (body, headers) => {
        if (body instanceof FormData) {
          const h = headers as Record<string, string | undefined>
          delete h['Content-Type']
          delete h['content-type']
        }
        return body
      },
    ],
  })
  return assertSuccessData(data)
}

export async function deletePortalAvatar(): Promise<PortalProfile> {
  const { data } = await apiClient.delete<ApiEnvelope<PortalProfile>>(`${BASE}/avatar`)
  return assertSuccessData(data)
}

/** Loads the current user's JPEG avatar using Authorization (for blob/object URLs). */
export async function fetchPortalAvatarBlob(): Promise<Blob | null> {
  try {
    const res = await apiClient.get<ArrayBuffer>(`${BASE}/avatar`, {
      responseType: 'arraybuffer',
      validateStatus: (status) => status === 200 || status === 404,
    })
    if (res.status === 404 || !res.data || res.data.byteLength === 0) {
      return null
    }
    return new Blob([res.data], { type: 'image/jpeg' })
  } catch {
    return null
  }
}
