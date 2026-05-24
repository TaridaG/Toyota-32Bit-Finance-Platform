import { apiClient } from '../../../shared/api/client'
import type { ApiEnvelope } from '../types'

export type PortalMfaStatus = {
  enabled: boolean
  enabledAt: string | null
}

export type PortalMfaSetup = {
  secret: string
  otpAuthUri: string
  qrCodeBase64: string
}

const BASE = '/api/portal/profile/mfa'

export async function fetchPortalMfaStatus(): Promise<PortalMfaStatus> {
  const { data } = await apiClient.get<ApiEnvelope<PortalMfaStatus>>(BASE)
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Could not load two-factor status')
  }
  return data.data
}

export async function beginPortalMfaSetup(): Promise<PortalMfaSetup> {
  const { data } = await apiClient.post<ApiEnvelope<PortalMfaSetup>>(`${BASE}/setup`)
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Could not start two-factor setup')
  }
  return data.data
}

export async function confirmPortalMfaSetup(code: string): Promise<PortalMfaStatus> {
  const { data } = await apiClient.post<ApiEnvelope<PortalMfaStatus>>(`${BASE}/confirm`, { code })
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Could not enable two-factor authentication')
  }
  return data.data
}

export async function disablePortalMfa(password: string, code: string): Promise<PortalMfaStatus> {
  const { data } = await apiClient.post<ApiEnvelope<PortalMfaStatus>>(`${BASE}/disable`, { password, code })
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Could not disable two-factor authentication')
  }
  return data.data
}
