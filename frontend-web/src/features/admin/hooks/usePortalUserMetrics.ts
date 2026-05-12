import { useCallback, useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { fetchPortalUserMetrics, type PortalAdminDashboardMetrics } from '../api/portalUserMetrics'
import { formatAdminInteger, formatWowPercent, padSevenDayInts } from '../formatAdminNumbers'

export type PortalUserMetricsState =
  | { status: 'loading' }
  | { status: 'ok'; data: PortalAdminDashboardMetrics }
  | { status: 'error'; message: string }

/** Live snapshot for the admin dashboard “total users” KPI card (never falls back to mock on error). */
export type UsersKpiLive =
  | { kind: 'loading' }
  | { kind: 'error'; message: string }
  | {
      kind: 'live'
      valueFormatted: string
      deltaFormatted: string
      trend: 'up' | 'down' | 'flat'
      /** Raw UTC-day bucket counts (sparkline scales internally). */
      sparkline: number[]
      sparklineSecondary?: number[]
      footCaptionKey: string
    }

/** “Piyasa varlıkları” / instruments catalog count on the dashboard. */
export type StreamsKpiLive =
  | { kind: 'loading' }
  | { kind: 'error'; message: string }
  | { kind: 'live'; totalFormatted: string; sparkline: number[] }

export function usePortalUserMetrics() {
  const [state, setState] = useState<PortalUserMetricsState>({ status: 'loading' })

  const load = useCallback(async () => {
    setState((prev) => (prev.status === 'ok' ? prev : { status: 'loading' }))
    try {
      const data = await fetchPortalUserMetrics()
      setState({ status: 'ok', data })
    } catch (e) {
      const message = e instanceof Error ? e.message : 'load failed'
      setState((prev) => (prev.status === 'ok' ? prev : { status: 'error', message }))
    }
  }, [])

  useEffect(() => {
    void load()
  }, [load])

  return { state, refetch: load }
}

/** One {@link usePortalUserMetrics} fetch drives both “total users” and “active portfolios” KPI cards. */
export function usePortalAdminDashboardKpis(language: string) {
  const { t } = useTranslation('admin')
  const { state, refetch } = usePortalUserMetrics()

  const usersLive = useMemo((): UsersKpiLive => {
    if (state.status === 'loading') return { kind: 'loading' }
    if (state.status === 'error') return { kind: 'error', message: state.message }
    const d = state.data
    const pct = d.newUsersWeekOverWeekPercent
    const trend: 'up' | 'down' | 'flat' =
      pct > 0.0001 ? 'up' : pct < -0.0001 ? 'down' : 'flat'
    return {
      kind: 'live',
      valueFormatted: formatAdminInteger(d.totalUsers, language),
      deltaFormatted: formatWowPercent(pct, language),
      trend,
      sparkline: padSevenDayInts(d.newRegistrationsDailyLast7Utc),
      sparklineSecondary: padSevenDayInts(d.userDeletionRequestsDailyLast7Utc),
      footCaptionKey: 'dashboard.kpi.userFlowSparkFoot',
    }
  }, [state, language])

  const portfoliosLive = useMemo((): UsersKpiLive => {
    if (state.status === 'loading') return { kind: 'loading' }
    if (state.status === 'error') return { kind: 'error', message: state.message }
    const d = state.data
    if (d.portfolioMetricsAvailable === false) {
      return { kind: 'error', message: t('dashboard.kpi.portfoliosMetricLoadError') }
    }
    if (typeof d.totalPortfolios !== 'number') {
      return {
        kind: 'error',
        message:
          'Portfolio metrics missing from API response — deploy finance-api with combined /portal-users payload.',
      }
    }
    const pct = d.newPortfoliosWeekOverWeekPercent
    const trend: 'up' | 'down' | 'flat' =
      pct > 0.0001 ? 'up' : pct < -0.0001 ? 'down' : 'flat'
    return {
      kind: 'live',
      valueFormatted: formatAdminInteger(d.totalPortfolios, language),
      deltaFormatted: formatWowPercent(pct, language),
      trend,
      sparkline: padSevenDayInts(d.newPortfoliosDailyLast7Utc),
      sparklineSecondary: padSevenDayInts(d.portfolioUpdatesExistingDailyLast7Utc),
      footCaptionKey: 'dashboard.kpi.portfolioFlowSparkFoot',
    }
  }, [state, language, t])

  const instrumentsStreamsLive = useMemo((): StreamsKpiLive => {
    if (state.status === 'loading') return { kind: 'loading' }
    if (state.status === 'error') return { kind: 'error', message: state.message }
    const d = state.data
    const raw: unknown = d.totalInstruments
    const n =
      typeof raw === 'number'
        ? raw
        : typeof raw === 'string' && /^\d+$/.test(raw.trim())
          ? Number(raw.trim())
          : NaN
    if (!Number.isFinite(n)) {
      return {
        kind: 'error',
        message: t('dashboard.kpi.instrumentsMetricLoadError'),
      }
    }
    return {
      kind: 'live',
      totalFormatted: formatAdminInteger(n, language),
      sparkline: padSevenDayInts(d.instrumentDistinctWithPriceDailyLast7Utc),
    }
  }, [state, language, t])

  return { usersLive, portfoliosLive, instrumentsStreamsLive, refetchPortalDashboard: refetch }
}

/** @deprecated Prefer {@link usePortalAdminDashboardKpis} on the overview page (single refetch). */
export function useAdminPortalUsersKpiLive(language: string) {
  const { usersLive, refetchPortalDashboard } = usePortalAdminDashboardKpis(language)
  return { usersLive, refetchPortalUsers: refetchPortalDashboard }
}
