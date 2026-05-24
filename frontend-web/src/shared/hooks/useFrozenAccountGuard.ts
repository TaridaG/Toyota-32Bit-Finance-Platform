import { useEffect } from 'react'
import { useLocation } from 'react-router-dom'
import { fetchPortalProfile } from '../../features/profile/api/portalProfileApi'
import { logoutTerminatedPortalAccount } from '../auth/accountFrozen'
import { isAuthenticated } from '../auth/session'

const POLL_MS = 8_000

/**
 * While the user is in the app, periodically hits a protected endpoint so freeze/delete takes effect
 * within seconds even if they are not navigating (JWT alone does not expire immediately).
 */
export function useFrozenAccountGuard(enabled: boolean): void {
  const location = useLocation()

  useEffect(() => {
    if (!enabled || !isAuthenticated()) {
      return
    }
    const path = location.pathname
    if (!path.startsWith('/app')) {
      return
    }

    let cancelled = false

    const probe = async () => {
      try {
        await fetchPortalProfile()
      } catch (error) {
        if (!cancelled && logoutTerminatedPortalAccount(error)) {
          return
        }
      }
    }

    void probe()
    const timer = window.setInterval(() => {
      void probe()
    }, POLL_MS)

    const onFocus = () => {
      void probe()
    }
    window.addEventListener('focus', onFocus)

    return () => {
      cancelled = true
      window.clearInterval(timer)
      window.removeEventListener('focus', onFocus)
    }
  }, [enabled, location.pathname])
}
