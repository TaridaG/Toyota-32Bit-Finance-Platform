import { useCallback, useEffect, useState } from 'react'
import {
  fetchAdminUserDirectory,
  type AdminUserDirectoryPage,
  type AdminUserDirectoryQuery,
} from '../api/adminUserDirectoryApi'

export type AdminUserDirectoryState =
  | { status: 'loading' }
  | { status: 'ok'; data: AdminUserDirectoryPage }
  | { status: 'error'; message: string }

export function useAdminUserDirectory(query: AdminUserDirectoryQuery) {
  const [state, setState] = useState<AdminUserDirectoryState>({ status: 'loading' })

  const load = useCallback(async () => {
    setState({ status: 'loading' })
    try {
      const data = await fetchAdminUserDirectory(query)
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
