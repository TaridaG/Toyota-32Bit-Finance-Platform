import { useEffect, useRef } from 'react'
import { Outlet, useNavigate } from 'react-router-dom'
import { PortalHeader } from './PortalHeader'
import { clearAuthSession, isAuthenticated } from '../../auth/session'
import { syncLocaleFromBackend } from '../../i18n'

export function RootLayout() {
  const navigate = useNavigate()
  const authenticated = isAuthenticated()
  const localeSyncDoneRef = useRef(false)

  useEffect(() => {
    if (!authenticated) {
      localeSyncDoneRef.current = false
      return
    }

    if (localeSyncDoneRef.current) {
      return
    }

    localeSyncDoneRef.current = true
    void syncLocaleFromBackend()
  }, [authenticated])

  const handleLogout = () => {
    clearAuthSession()
    navigate('/', { replace: true })
  }

  return (
    <>
      <PortalHeader isAuthenticated={authenticated} onLogout={handleLogout} />
      <Outlet />
    </>
  )
}

