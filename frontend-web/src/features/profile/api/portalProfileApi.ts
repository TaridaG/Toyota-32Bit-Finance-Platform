import { isAxiosError } from 'axios'
import { apiClient } from '../../../shared/api/client'
import type { PortalLoginTokens } from '../../../shared/api/publicAuth'
import type { ApiEnvelope, PortalProfile } from '../types'

const BASE = '/api/portal/profile'

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

export async function changePortalPassword(currentPassword: string, newPassword: string): Promise<void> {
  const { data } = await apiClient.post<ApiEnvelope<unknown>>(`${BASE}/password`, {
    currentPassword,
    newPassword,
  })
  assertSuccessOnly(data)
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
  notifyProductUpdates: boolean,
): Promise<PortalProfile> {
  const { data } = await apiClient.put<ApiEnvelope<PortalProfile>>(`${BASE}/notifications`, {
    notifySecurityAlerts,
    notifyProductUpdates,
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
