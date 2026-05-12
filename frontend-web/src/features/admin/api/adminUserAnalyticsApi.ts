import { isAxiosError } from 'axios'
import { apiClient } from '../../../shared/api/client'

export type UserAnalyticsPresetParam = '7d' | '14d' | '30d'

export type UserAnalyticsQuery =
  | { kind: 'preset'; preset: UserAnalyticsPresetParam }
  | { kind: 'custom'; from: string; to: string }

export type AdminUserDailyRegistration = {
  date: string
  newUsers: number
  deletionRequests: number
  rollingAverage7d: number
  deltaVsPreviousDay: number
}

export type AdminRecentRegistrationRow = {
  id: string
  displayName: string
  maskedEmail: string
  createdAt: string
  source: string
  device: string
  status: 'ACTIVE' | 'INACTIVE' | string
}

export type AdminUserInteractionMetrics = {
  measured: boolean
  averageSessionDurationSeconds: number | null
  sessionCount: number | null
  pageViews: number | null
  bounceRatePercent: number | null
}

export type AdminUserSegmentBucket = { label: string; count: number; percentage: number }

export type AdminUserSegments = { available: boolean; buckets: AdminUserSegmentBucket[] }

export type AdminUserHeatmapCell = { dayOfWeek: number; hour: number; value: number }

export type AdminUserHeatmap = { available: boolean; cells: AdminUserHeatmapCell[] }

export type AdminUserAnalyticsSummary = {
  totalUsers: number
  newUsersLast7Days: number
  newUsersPrevious7Days: number
  newUsersWeekOverWeekPercent: number
  userDeltaLast7VsPrev7: number
  newUsersCurrentPeriod: number
  newUsersPreviousPeriod: number
  growthPercentPeriodVsPrevious: number
  activeUsers: number
  activeRatePercent: number
  newUsersYesterday: number
  newUsersDayBeforeYesterday: number
  yesterdayVsPriorDayDelta: number
  todayNewUsersUtc: number | null
  /** New sign-ups in the previous ISO week (Mon UTC → next Mon UTC), excluding pending deletion. */
  newUsersPreviousIsoWeekUtc: number
  /** New sign-ups in the previous UTC calendar month, excluding pending deletion. */
  newUsersPreviousCalendarMonthUtc: number
  /** Approx. roster day-over-day % (see API docs). */
  totalUsersVsPriorDayPercentApprox: number
}

export type AdminUserAnalyticsDashboard = {
  preset: string
  chartRangeStartUtcInclusive: string
  chartRangeEndUtcExclusive: string
  summary: AdminUserAnalyticsSummary
  dailyRegistrations: AdminUserDailyRegistration[]
  recentRegistrations: AdminRecentRegistrationRow[]
  interaction: AdminUserInteractionMetrics
  segments: AdminUserSegments
  heatmap: AdminUserHeatmap
  generatedAt: string
}

type Envelope<T> = { success: boolean; data: T | null; error?: { message?: string } }

function failMessage(e: unknown): string {
  if (isAxiosError(e)) {
    const status = e.response?.status
    const body = e.response?.data as Envelope<unknown> | undefined
    if (body && body.success === false && body.error?.message) {
      return body.error.message
    }
    const serverMsg = body?.error?.message
    if (serverMsg) return serverMsg
    if (status != null) return `HTTP ${status}: ${e.message}`
    return e.message
  }
  return e instanceof Error ? e.message : 'user-analytics unavailable'
}

function queryParams(q: UserAnalyticsQuery): Record<string, string> {
  if (q.kind === 'preset') {
    return { preset: q.preset }
  }
  return { from: q.from, to: q.to }
}

export async function fetchAdminUserAnalytics(q: UserAnalyticsQuery): Promise<AdminUserAnalyticsDashboard> {
  try {
    const { data } = await apiClient.get<Envelope<AdminUserAnalyticsDashboard>>('/api/admin/metrics/user-analytics', {
      params: queryParams(q),
    })
    if (!data.success || data.data == null) {
      throw new Error(data.error?.message ?? 'user-analytics unavailable')
    }
    return normalizeDashboard(data.data)
  } catch (e) {
    throw new Error(failMessage(e))
  }
}

function normalizeDashboard(d: AdminUserAnalyticsDashboard): AdminUserAnalyticsDashboard {
  return {
    ...d,
    summary: {
      ...d.summary,
      totalUsers: Number(d.summary.totalUsers),
      newUsersLast7Days: Number(d.summary.newUsersLast7Days),
      newUsersPrevious7Days: Number(d.summary.newUsersPrevious7Days),
      newUsersWeekOverWeekPercent: Number(d.summary.newUsersWeekOverWeekPercent),
      userDeltaLast7VsPrev7: Number(d.summary.userDeltaLast7VsPrev7),
      newUsersCurrentPeriod: Number(d.summary.newUsersCurrentPeriod),
      newUsersPreviousPeriod: Number(d.summary.newUsersPreviousPeriod),
      growthPercentPeriodVsPrevious: Number(d.summary.growthPercentPeriodVsPrevious),
      activeUsers: Number(d.summary.activeUsers),
      activeRatePercent: Number(d.summary.activeRatePercent),
      newUsersYesterday: Number(d.summary.newUsersYesterday),
      newUsersDayBeforeYesterday: Number(d.summary.newUsersDayBeforeYesterday),
      yesterdayVsPriorDayDelta: Number(d.summary.yesterdayVsPriorDayDelta),
      todayNewUsersUtc: d.summary.todayNewUsersUtc == null ? null : Number(d.summary.todayNewUsersUtc),
      newUsersPreviousIsoWeekUtc: Number(d.summary.newUsersPreviousIsoWeekUtc ?? 0),
      newUsersPreviousCalendarMonthUtc: Number(d.summary.newUsersPreviousCalendarMonthUtc ?? 0),
      totalUsersVsPriorDayPercentApprox: Number(d.summary.totalUsersVsPriorDayPercentApprox ?? 0),
    },
    dailyRegistrations: (d.dailyRegistrations ?? []).map((row) => ({
      ...row,
      newUsers: Number(row.newUsers),
      deletionRequests: Number(row.deletionRequests),
      rollingAverage7d: Number(row.rollingAverage7d),
      deltaVsPreviousDay: Number(row.deltaVsPreviousDay),
    })),
    recentRegistrations: d.recentRegistrations ?? [],
    interaction:
      d.interaction ?? {
        measured: false,
        averageSessionDurationSeconds: null,
        sessionCount: null,
        pageViews: null,
        bounceRatePercent: null,
      },
    segments: d.segments ?? { available: false, buckets: [] },
    heatmap: d.heatmap ?? { available: false, cells: [] },
  }
}

/** Today as `yyyy-MM-dd` in UTC (for date inputs aligned with API). */
export function utcTodayIsoDate(): string {
  const d = new Date()
  const y = d.getUTCFullYear()
  const m = String(d.getUTCMonth() + 1).padStart(2, '0')
  const day = String(d.getUTCDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

/** Add signed whole UTC days to an ISO date `yyyy-MM-dd`. */
export function addUtcDaysIso(isoDate: string, deltaDays: number): string {
  const [y, mo, da] = isoDate.split('-').map((s) => Number(s))
  const t = Date.UTC(y, mo - 1, da) + deltaDays * 86400000
  const d = new Date(t)
  const yy = d.getUTCFullYear()
  const mm = String(d.getUTCMonth() + 1).padStart(2, '0')
  const dd = String(d.getUTCDate()).padStart(2, '0')
  return `${yy}-${mm}-${dd}`
}
