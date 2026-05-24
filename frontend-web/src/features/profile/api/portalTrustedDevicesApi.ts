import { apiClient } from '../../../shared/api/client'
import type { ApiEnvelope } from '../types'

export type PortalTrustedDeviceRow = {
  id: string
  createdAt: string
  lastUsedAt: string | null
  expiresAt: string
  currentDevice: boolean
}

export type PortalTrustedDevicesResponse = {
  featureEnabled: boolean
  devices: PortalTrustedDeviceRow[]
}

const BASE = '/api/portal/profile/trusted-devices'

export async function fetchPortalTrustedDevices(): Promise<PortalTrustedDevicesResponse> {
  const { data } = await apiClient.get<ApiEnvelope<PortalTrustedDevicesResponse>>(BASE)
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Could not load trusted devices')
  }
  return data.data
}

export async function revokePortalTrustedDevice(deviceId: string): Promise<void> {
  const { data } = await apiClient.delete<ApiEnvelope<unknown>>(`${BASE}/${deviceId}`)
  if (!data.success) {
    throw new Error(data.error?.message ?? 'Could not remove trusted device')
  }
}

export async function revokeAllPortalTrustedDevices(): Promise<void> {
  const { data } = await apiClient.delete<ApiEnvelope<unknown>>(BASE)
  if (!data.success) {
    throw new Error(data.error?.message ?? 'Could not remove trusted devices')
  }
}
