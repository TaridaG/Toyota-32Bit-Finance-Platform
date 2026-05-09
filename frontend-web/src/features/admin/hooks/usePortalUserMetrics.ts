import { useCallback, useEffect, useMemo, useState } from 'react'
import { fetchPortalUserMetrics, type PortalUserMetrics } from '../api/portalUserMetrics'
import { formatAdminInteger, formatWowPercent, sparklineFromDailyCounts } from '../formatAdminNumbers'

export type PortalUserMetricsState =
  | { status: 'loading' }
  | { status: 'ok'; data: PortalUserMetrics }
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
      sparkline: number[]
      footCaptionKey: string
    }

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

/** Fetches portal user metrics and maps them to dashboard KPI shape (with loading / error fallbacks). */
export function useAdminPortalUsersKpiLive(language: string) {
  const { state: portalUsersState, refetch: refetchPortalUsers } = usePortalUserMetrics()

  const usersLive = useMemo((): UsersKpiLive => {
    if (portalUsersState.status === 'loading') return { kind: 'loading' }
    if (portalUsersState.status === 'error') return { kind: 'error', message: portalUsersState.message }
    const d = portalUsersState.data
    const pct = d.newUsersWeekOverWeekPercent
    const trend: 'up' | 'down' | 'flat' =
      pct > 0.0001 ? 'up' : pct < -0.0001 ? 'down' : 'flat'
    return {
      kind: 'live',
      valueFormatted: formatAdminInteger(d.totalUsers, language),
      deltaFormatted: formatWowPercent(pct, language),
      trend,
      sparkline: sparklineFromDailyCounts(d.newRegistrationsDailyLast7Utc),
      footCaptionKey: 'dashboard.kpi.signupVelocityWow',
    }
  }, [portalUsersState, language])

  return { usersLive, refetchPortalUsers }
}
