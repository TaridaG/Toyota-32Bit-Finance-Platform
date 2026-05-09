import { isAxiosError } from 'axios'
import { apiClient } from '../../../shared/api/client'

export type PortalUserMetrics = {
  totalUsers: number
  newUsersLast7Days: number
  newUsersPrevious7Days: number
  newUsersWeekOverWeekPercent: number
  newRegistrationsDailyLast7Utc: number[]
  generatedAt: string
}

type MetricsEnvelope<T> = {
  success: boolean
  data: T | null
  error?: { code?: string; message?: string }
}

function describePortalMetricsFailure(e: unknown): string {
  if (isAxiosError(e)) {
    const status = e.response?.status
    const body = e.response?.data as MetricsEnvelope<unknown> | undefined
    const serverMsg = body?.error?.message
    if (status === 403) {
      return serverMsg ?? 'Forbidden — ADMIN role required for /api/admin (check API gateway).'
    }
    if (status === 401) {
      return serverMsg ?? 'Unauthorized — sign in again.'
    }
    if (serverMsg) return serverMsg
    if (status != null) return `HTTP ${status}: ${e.message}`
  }
  if (e instanceof Error) return e.message
  return 'portal-users metrics unavailable'
}

export async function fetchPortalUserMetrics(): Promise<PortalUserMetrics> {
  try {
    const { data } = await apiClient.get<MetricsEnvelope<PortalUserMetrics>>('/api/admin/metrics/portal-users')
    if (!data.success || data.data == null) {
      const msg = data.error?.message ?? 'portal-users metrics unavailable'
      throw new Error(msg)
    }
    return data.data
  } catch (e) {
    throw new Error(describePortalMetricsFailure(e))
  }
}
