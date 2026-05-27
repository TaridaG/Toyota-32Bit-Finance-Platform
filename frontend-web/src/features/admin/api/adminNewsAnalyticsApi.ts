import { isAxiosError } from 'axios'
import { apiClient } from '../../../shared/api/client'

export type NewsAnalyticsPresetParam = '7d' | '14d' | '30d'

export type NewsAnalyticsQuery =
  | { kind: 'preset'; preset: NewsAnalyticsPresetParam }
  | { kind: 'custom'; from: string; to: string }

export type AdminNewsDailyPublish = {
  date: string
  articlesPublished: number
  rollingAverage7d: number
  deltaVsPreviousDay: number
}

export type AdminNewsAnalyticsSummary = {
  totalArticles: number
  distinctSourceCount: number
  translationCompletionPercent: number
  translationMeasuredLanguage: string
  articlesPublishedPreviousIsoWeekUtc: number
  articlesPublishedPreviousCalendarMonthUtc: number
  articlesCreatedYesterdayUtc: number
  articlesCreatedDayBeforeYesterdayUtc: number
  totalArticlesVsPriorDayPercentApprox: number
}

export type AdminRecentNewsArticleRow = {
  id: number
  title: string
  sourceName: string
  category: string
  publishedAt: string
}

export type AdminNewsAnalyticsDashboard = {
  preset: string
  chartRangeStartUtcInclusive: string
  chartRangeEndUtcExclusive: string
  summary: AdminNewsAnalyticsSummary
  dailyPublished: AdminNewsDailyPublish[]
  recentArticles: AdminRecentNewsArticleRow[]
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
    if (status === 403) return serverMsg ?? 'Forbidden — ADMIN role required for news admin metrics.'
    if (status === 401) return serverMsg ?? 'Unauthorized.'
    if (status != null) return `HTTP ${status}: ${e.message}`
    return e.message
  }
  return e instanceof Error ? e.message : 'news-analytics unavailable'
}

function queryParams(q: NewsAnalyticsQuery): Record<string, string> {
  if (q.kind === 'preset') {
    return { preset: q.preset }
  }
  return { from: q.from, to: q.to }
}

export async function fetchAdminNewsAnalytics(q: NewsAnalyticsQuery): Promise<AdminNewsAnalyticsDashboard> {
  try {
    const { data } = await apiClient.get<Envelope<AdminNewsAnalyticsDashboard>>('/api/v1/news/admin/metrics/analytics', {
      params: queryParams(q),
    })
    if (!data.success || data.data == null) {
      throw new Error(data.error?.message ?? 'news-analytics unavailable')
    }
    return normalizeDashboard(data.data)
  } catch (e) {
    throw new Error(failMessage(e))
  }
}

function normalizeDashboard(d: AdminNewsAnalyticsDashboard): AdminNewsAnalyticsDashboard {
  return {
    ...d,
    summary: {
      totalArticles: Number(d.summary.totalArticles),
      distinctSourceCount: Number(d.summary.distinctSourceCount),
      translationCompletionPercent: Number(d.summary.translationCompletionPercent),
      translationMeasuredLanguage: String(d.summary.translationMeasuredLanguage ?? 'en'),
      articlesPublishedPreviousIsoWeekUtc: Number(d.summary.articlesPublishedPreviousIsoWeekUtc),
      articlesPublishedPreviousCalendarMonthUtc: Number(d.summary.articlesPublishedPreviousCalendarMonthUtc),
      articlesCreatedYesterdayUtc: Number(d.summary.articlesCreatedYesterdayUtc),
      articlesCreatedDayBeforeYesterdayUtc: Number(d.summary.articlesCreatedDayBeforeYesterdayUtc),
      totalArticlesVsPriorDayPercentApprox: Number(d.summary.totalArticlesVsPriorDayPercentApprox),
    },
    dailyPublished: (d.dailyPublished ?? []).map((row) => ({
      ...row,
      articlesPublished: Number(row.articlesPublished),
      rollingAverage7d: Number(row.rollingAverage7d),
      deltaVsPreviousDay: Number(row.deltaVsPreviousDay),
    })),
    recentArticles: (d.recentArticles ?? []).map((r) => ({
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
