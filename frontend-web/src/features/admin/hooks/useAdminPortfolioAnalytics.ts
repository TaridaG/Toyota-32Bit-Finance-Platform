import { useCallback, useEffect, useState } from 'react'
import {
  fetchAdminPortfolioAnalytics,
  type AdminPortfolioAnalyticsDashboard,
  type PortfolioAnalyticsQuery,
} from '../api/adminPortfolioAnalyticsApi'

export type AdminPortfolioAnalyticsState =
  | { status: 'loading' }
  | { status: 'error'; message: string }
  | { status: 'ok'; data: AdminPortfolioAnalyticsDashboard }

export function useAdminPortfolioAnalytics(query: PortfolioAnalyticsQuery) {
  const [state, setState] = useState<AdminPortfolioAnalyticsState>({ status: 'loading' })

  const load = useCallback(async () => {
    setState({ status: 'loading' })
    try {
      const data = await fetchAdminPortfolioAnalytics(query)
      setState({ status: 'ok', data })
    } catch (e) {
      const message = e instanceof Error ? e.message : 'portfolio-analytics unavailable'
      setState({ status: 'error', message })
    }
  }, [query])

  useEffect(() => {
    void load()
  }, [load])

  return { state, refetch: load }
}
