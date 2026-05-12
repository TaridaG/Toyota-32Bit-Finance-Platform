import { useCallback, useEffect, useState } from 'react'
import { fetchAdminNewsDashboardMetrics } from '../api/adminNewsMetrics'
import { formatAdminInteger, padSevenDayInts } from '../formatAdminNumbers'
import { isAdminUser } from '../../../shared/auth/session'

export type NewsKpiLive =
  | { kind: 'loading' }
  | { kind: 'error'; message: string }
  | {
      kind: 'live'
      totalArticlesFormatted: string
      distinctSourcesFormatted: string
      errors: number
      sparkline: number[]
    }

/**
 * Live news volume for the admin dashboard. When the user is not an admin, returns `undefined` so the UI keeps mock KPIs.
 */
export function useAdminNewsKpi(language: string) {
  const enabled = isAdminUser()
  const [state, setState] = useState<NewsKpiLive | undefined>(() => (enabled ? { kind: 'loading' } : undefined))

  const load = useCallback(async () => {
    if (!enabled) return
    setState((prev) => (prev?.kind === 'live' ? prev : { kind: 'loading' }))
    try {
      const d = await fetchAdminNewsDashboardMetrics()
      setState({
        kind: 'live',
        totalArticlesFormatted: formatAdminInteger(d.totalArticles, language),
        distinctSourcesFormatted: formatAdminInteger(d.distinctSourceCount, language),
        errors: 0,
        sparkline: padSevenDayInts(d.articlesPublishedDailyLast7Utc),
      })
    } catch (e) {
      const message = e instanceof Error ? e.message : 'load failed'
      setState((prev) => (prev?.kind === 'live' ? prev : { kind: 'error', message }))
    }
  }, [enabled, language])

  useEffect(() => {
    if (!enabled) {
      setState(undefined)
      return
    }
    setState({ kind: 'loading' })
    void load()
  }, [enabled, load])

  return { newsLive: state, refetchNews: load }
}
