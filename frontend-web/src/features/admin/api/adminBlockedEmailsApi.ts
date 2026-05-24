import { isAxiosError } from 'axios'
import { apiClient } from '../../../shared/api/client'

type AdminEnvelope<T> = { success: boolean; data?: T; error?: { message?: string } }

export type BlockedEmailRow = {
  id: number
  email: string
  blockedAt: string
  sourceUserId: string | null
}

export type BlockedEmailsPage = {
  content: BlockedEmailRow[]
  totalElements: number
  totalPages: number
  page: number
  size: number
}

const BASE = '/api/admin/metrics/blocked-emails'

export function readBlockedEmailsApiError(e: unknown): string {
  if (isAxiosError(e)) {
    const body = e.response?.data as AdminEnvelope<unknown> | undefined
    if (body?.error?.message) return body.error.message
    if (e.response?.status != null) return `HTTP ${e.response.status}`
  }
  if (e instanceof Error) return e.message
  return 'Request failed'
}

export async function fetchBlockedEmails(page: number, size = 10): Promise<BlockedEmailsPage> {
  const { data } = await apiClient.get<AdminEnvelope<BlockedEmailsPage>>(BASE, {
    params: { page, size },
  })
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Request failed')
  }
  return data.data
}

export async function unblockBlockedEmail(id: number): Promise<void> {
  const { data } = await apiClient.post<AdminEnvelope<null>>(`${BASE}/${id}/unblock`)
  if (!data.success) {
    throw new Error(data.error?.message ?? 'Request failed')
  }
}
