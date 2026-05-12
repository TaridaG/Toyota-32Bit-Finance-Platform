import { isAxiosError } from 'axios'
import { apiClient } from '../../../shared/api/client'

export type AdminNewsDashboardMetrics = {
  totalArticles: number
  distinctSourceCount: number
  articlesPublishedDailyLast7Utc: number[]
  generatedAt: string
}

type MetricsEnvelope<T> = {
  success: boolean
  data: T | null
  error?: { code?: string; message?: string }
}

function describeFailure(e: unknown): string {
  if (isAxiosError(e)) {
    const status = e.response?.status
    const body = e.response?.data as MetricsEnvelope<unknown> | undefined
    const serverMsg = body?.error?.message
    if (status === 403) return serverMsg ?? 'Forbidden — ADMIN role required for news admin metrics.'
    if (status === 401) return serverMsg ?? 'Unauthorized.'
    if (serverMsg) return serverMsg
    if (status != null) return `HTTP ${status}`
  }
  if (e instanceof Error) return e.message
  return 'news metrics unavailable'
}

export async function fetchAdminNewsDashboardMetrics(): Promise<AdminNewsDashboardMetrics> {
  try {
    const { data } = await apiClient.get<MetricsEnvelope<AdminNewsDashboardMetrics>>('/api/news/admin/metrics/dashboard')
    if (!data.success || data.data == null) {
      throw new Error(data.error?.message ?? 'news metrics unavailable')
    }
    return data.data
  } catch (e) {
    throw new Error(describeFailure(e))
  }
}
