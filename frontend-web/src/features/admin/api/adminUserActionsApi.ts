import { isAxiosError } from 'axios'
import { apiClient } from '../../../shared/api/client'

type AdminEnvelope = { success: boolean; error?: { message?: string } }

const BASE = '/api/v1/admin/metrics/user-directory'

function describeFailure(e: unknown): string {
  if (isAxiosError(e)) {
    const body = e.response?.data as AdminEnvelope | undefined
    if (body?.error?.message) return body.error.message
    if (e.response?.status != null) return `HTTP ${e.response.status}`
  }
  if (e instanceof Error) return e.message
  return 'Request failed'
}

export async function sendAdminUserMessage(userId: string, message: string): Promise<void> {
  try {
    const { data } = await apiClient.post<AdminEnvelope>(`${BASE}/${userId}/message`, { message })
    if (!data.success) throw new Error(data.error?.message ?? 'Request failed')
  } catch (e) {
    throw new Error(describeFailure(e))
  }
}

export async function freezeAdminUser(userId: string, reason?: string): Promise<void> {
  try {
    const { data } = await apiClient.post<AdminEnvelope>(`${BASE}/${userId}/freeze`, reason ? { reason } : {})
    if (!data.success) throw new Error(data.error?.message ?? 'Request failed')
  } catch (e) {
    throw new Error(describeFailure(e))
  }
}

export async function unfreezeAdminUser(userId: string): Promise<void> {
  try {
    const { data } = await apiClient.post<AdminEnvelope>(`${BASE}/${userId}/unfreeze`)
    if (!data.success) throw new Error(data.error?.message ?? 'Request failed')
  } catch (e) {
    throw new Error(describeFailure(e))
  }
}

export async function deleteAdminUser(userId: string, blockEmail: boolean): Promise<void> {
  try {
    const { data } = await apiClient.post<AdminEnvelope>(`${BASE}/${userId}/delete`, { blockEmail })
    if (!data.success) throw new Error(data.error?.message ?? 'Request failed')
  } catch (e) {
    throw new Error(describeFailure(e))
  }
}
