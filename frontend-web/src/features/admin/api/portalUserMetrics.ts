import { isAxiosError } from 'axios'
import { apiClient } from '../../../shared/api/client'

/** Combined payload from {@code GET /api/admin/metrics/portal-users} (users + external portfolios). */
export type PortalAdminDashboardMetrics = {
  totalUsers: number
  newUsersLast7Days: number
  newUsersPrevious7Days: number
  newUsersWeekOverWeekPercent: number
  newRegistrationsDailyLast7Utc: number[]
  /** Account deletion workflow started that UTC day (parallel 7 buckets). */
  userDeletionRequestsDailyLast7Utc?: number[]
  totalPortfolios: number
  newPortfoliosLast7Days: number
  newPortfoliosPrevious7Days: number
  newPortfoliosWeekOverWeekPercent: number
  newPortfoliosDailyLast7Utc: number[]
  /** External portfolios created before the day that received an update that UTC day. */
  portfolioUpdatesExistingDailyLast7Utc?: number[]
  generatedAt: string
  /** False when the server substituted empty portfolio metrics (see finance-api logs). */
  portfolioMetricsAvailable?: boolean
  /** Active rows in {@code instruments} (same scope as portal instrument list). */
  totalInstruments?: number
  /** Distinct instruments with ≥1 price row that UTC day (market data touch). */
  instrumentDistinctWithPriceDailyLast7Utc?: number[]
}

/** @deprecated Use {@link PortalAdminDashboardMetrics}. */
export type PortalUserMetrics = PortalAdminDashboardMetrics

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

export async function fetchPortalUserMetrics(): Promise<PortalAdminDashboardMetrics> {
  try {
    const { data } = await apiClient.get<MetricsEnvelope<PortalAdminDashboardMetrics>>('/api/admin/metrics/portal-users')
    if (!data.success || data.data == null) {
      const msg = data.error?.message ?? 'portal-users metrics unavailable'
      throw new Error(msg)
    }
    return data.data
  } catch (e) {
    throw new Error(describePortalMetricsFailure(e))
  }
}
