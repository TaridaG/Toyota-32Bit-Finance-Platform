import { useCallback, useEffect, useState } from 'react'
import {
  fetchAdminNewsAnalytics,
  type AdminNewsAnalyticsDashboard,
  type NewsAnalyticsQuery,
} from '../api/adminNewsAnalyticsApi'

export type AdminNewsAnalyticsState =
  | { status: 'loading' }
  | { status: 'error'; message: string }
  | { status: 'ok'; data: AdminNewsAnalyticsDashboard }

export function useAdminNewsAnalytics(query: NewsAnalyticsQuery) {
  const [state, setState] = useState<AdminNewsAnalyticsState>({ status: 'loading' })

  const load = useCallback(async () => {
    setState({ status: 'loading' })
    try {
      const data = await fetchAdminNewsAnalytics(query)
      setState({ status: 'ok', data })
    } catch (e) {
      const message = e instanceof Error ? e.message : 'news-analytics unavailable'
      setState({ status: 'error', message })
    }
  }, [query])

  useEffect(() => {
    void load()
  }, [load])

  return { state, refetch: load }
}
