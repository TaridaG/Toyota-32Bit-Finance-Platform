import { isAxiosError } from 'axios'
import { apiClient } from '../../../shared/api/client'

export type PortfolioAnalyticsPresetParam = '7d' | '14d' | '30d'

export type PortfolioAnalyticsQuery =
  | { kind: 'preset'; preset: PortfolioAnalyticsPresetParam }
  | { kind: 'custom'; from: string; to: string }

export type AdminPortfolioDailyCreation = {
  date: string
  portfoliosCreated: number
  rollingAverage7d: number
  deltaVsPreviousDay: number
}

export type AdminPortfolioAnalyticsSummary = {
  totalPortfolios: number
  portfoliosCreatedPreviousIsoWeekUtc: number
  portfoliosCreatedPreviousCalendarMonthUtc: number
  averageOpenLotsPerPortfolio: number
  portfoliosPerRosterUser: number
  rosterUsersTotal: number
  openPositionLotsTotal: number
  portfoliosCreatedYesterdayUtc: number
  portfoliosCreatedDayBeforeYesterdayUtc: number
  totalPortfoliosVsPriorDayPercentApprox: number
}

export type AdminRecentPortfolioRow = {
  id: number
  name: string
  baseCurrency: string
  ownerUsername: string
  createdAt: string | null
}

export type AdminPortfolioAnalyticsDashboard = {
  preset: string
  chartRangeStartUtcInclusive: string
  chartRangeEndUtcExclusive: string
  summary: AdminPortfolioAnalyticsSummary
  dailyCreations: AdminPortfolioDailyCreation[]
  recentPortfolios: AdminRecentPortfolioRow[]
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
    if (status === 403) return serverMsg ?? 'Forbidden — ADMIN role required.'
    if (status === 401) return serverMsg ?? 'Unauthorized.'
    if (status != null) return `HTTP ${status}: ${e.message}`
    return e.message
  }
  return e instanceof Error ? e.message : 'portfolio-analytics unavailable'
}

function queryParams(q: PortfolioAnalyticsQuery): Record<string, string> {
  if (q.kind === 'preset') {
    return { preset: q.preset }
  }
  return { from: q.from, to: q.to }
}

export async function fetchAdminPortfolioAnalytics(
  q: PortfolioAnalyticsQuery,
): Promise<AdminPortfolioAnalyticsDashboard> {
  try {
    const { data } = await apiClient.get<Envelope<AdminPortfolioAnalyticsDashboard>>(
      '/api/v1/admin/metrics/portfolio-analytics',
      { params: queryParams(q) },
    )
    if (!data.success || data.data == null) {
      throw new Error(data.error?.message ?? 'portfolio-analytics unavailable')
    }
    return normalizeDashboard(data.data)
  } catch (e) {
    throw new Error(failMessage(e))
  }
}

function normalizeDashboard(d: AdminPortfolioAnalyticsDashboard): AdminPortfolioAnalyticsDashboard {
  return {
    ...d,
    summary: {
      totalPortfolios: Number(d.summary.totalPortfolios),
      portfoliosCreatedPreviousIsoWeekUtc: Number(d.summary.portfoliosCreatedPreviousIsoWeekUtc),
      portfoliosCreatedPreviousCalendarMonthUtc: Number(d.summary.portfoliosCreatedPreviousCalendarMonthUtc),
      averageOpenLotsPerPortfolio: Number(d.summary.averageOpenLotsPerPortfolio),
      portfoliosPerRosterUser: Number(d.summary.portfoliosPerRosterUser),
      rosterUsersTotal: Number(d.summary.rosterUsersTotal),
      openPositionLotsTotal: Number(d.summary.openPositionLotsTotal),
      portfoliosCreatedYesterdayUtc: Number(d.summary.portfoliosCreatedYesterdayUtc),
      portfoliosCreatedDayBeforeYesterdayUtc: Number(d.summary.portfoliosCreatedDayBeforeYesterdayUtc),
      totalPortfoliosVsPriorDayPercentApprox: Number(d.summary.totalPortfoliosVsPriorDayPercentApprox),
    },
    dailyCreations: (d.dailyCreations ?? []).map((row) => ({
      ...row,
      portfoliosCreated: Number(row.portfoliosCreated),
      rollingAverage7d: Number(row.rollingAverage7d),
      deltaVsPreviousDay: Number(row.deltaVsPreviousDay),
    })),
    recentPortfolios: (d.recentPortfolios ?? []).map((r) => ({
      ...r,
      id: Number(r.id),
    })),
  }
}

export function utcTodayIsoDate(): string {
  const d = new Date()
  const y = d.getUTCFullYear()
  const m = String(d.getUTCMonth() + 1).padStart(2, '0')
  const day = String(d.getUTCDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

export function addUtcDaysIso(isoDate: string, deltaDays: number): string {
  const [y, mo, da] = isoDate.split('-').map((s) => Number(s))
  const t = Date.UTC(y, mo - 1, da) + deltaDays * 86400000
  const d = new Date(t)
  const yy = d.getUTCFullYear()
  const mm = String(d.getUTCMonth() + 1).padStart(2, '0')
  const dd = String(d.getUTCDate()).padStart(2, '0')
  return `${yy}-${mm}-${dd}`
}
