import { Outlet } from 'react-router-dom'

/**
 * Authenticated app chrome only. Token refresh runs on 401 via {@code apiClient};
 * boot-time refresh here caused false "session expired" redirects (Strict Mode + races).
 */
export function AppLayout() {
  return (
    <div className="app-shell">
      <main className="page-content">
        <Outlet />
      </main>
    </div>
  )
}
