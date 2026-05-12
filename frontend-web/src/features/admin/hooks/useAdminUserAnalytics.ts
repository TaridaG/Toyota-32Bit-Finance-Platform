import { useCallback, useEffect, useState } from 'react'
import {
  fetchAdminUserAnalytics,
  type AdminUserAnalyticsDashboard,
  type UserAnalyticsQuery,
} from '../api/adminUserAnalyticsApi'

export type AdminUserAnalyticsState =
  | { status: 'loading' }
  | { status: 'ok'; data: AdminUserAnalyticsDashboard }
  | { status: 'error'; message: string }

export function useAdminUserAnalytics(query: UserAnalyticsQuery) {
  const [state, setState] = useState<AdminUserAnalyticsState>({ status: 'loading' })

  const load = useCallback(async () => {
    setState({ status: 'loading' })
    try {
      const data = await fetchAdminUserAnalytics(query)
      setState({ status: 'ok', data })
    } catch (e) {
      const message = e instanceof Error ? e.message : 'load failed'
      setState({ status: 'error', message })
    }
  }, [query])

  useEffect(() => {
    void load()
  }, [load])

  return { state, refetch: load }
}
