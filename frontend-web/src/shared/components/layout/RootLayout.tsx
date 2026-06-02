import { useEffect } from 'react'
import { Outlet, useLocation, useNavigate } from 'react-router-dom'
import { PortalHeader } from './PortalHeader'
import { fetchPortalProfileBootstrap, logoutPortalSession } from '../../../features/profile/api/portalProfileApi'
import { clearAuthSession, isAuthenticated } from '../../auth/session'
import { logoutTerminatedPortalAccount } from '../../auth/accountFrozen'
import { useFrozenAccountGuard } from '../../hooks/useFrozenAccountGuard'
import { normalizeLocale } from '../../i18n'
import { useAppPreferences } from '../../preferences/useAppPreferences'
import { SUPPORTED_CURRENCIES } from '../../preferences/preferences'
import { LiteracyHelpLayer } from '../../../features/literacy-help/LiteracyHelpLayer'
import { AdminInfoCardPickLayer } from '../../../features/admin-info-card-pick/AdminInfoCardPickLayer'
import { AdminInfoCardPickEditor } from '../../../features/admin-info-card-pick/AdminInfoCardPickEditor'
import { AdminInfoCardPickRouteSync } from '../../../features/admin-info-card-pick/AdminInfoCardPickRouteSync'
import { AlarmUiProvider } from '../../../features/alarms/AlarmUiContext'
import { CreateAlarmModal } from '../../../features/alarms/components/CreateAlarmModal'

export function RootLayout() {
  const navigate = useNavigate()
  const { pathname } = useLocation()
  const authenticated = isAuthenticated()
  const { setLanguage, setCurrency } = useAppPreferences()
  const isAdminRoute = pathname.startsWith('/admin')

  useFrozenAccountGuard(authenticated)

  useEffect(() => {
    let cancelled = false
    let retryTimer: number | null = null

    if (!authenticated || isAdminRoute) {
      return
    }

    const hydrateFromProfile = async () => {
      try {
        const profile = await fetchPortalProfileBootstrap()
        if (cancelled) {
          return
        }
        const normalizedLocale = normalizeLocale(profile.preferredLocale)
        if (normalizedLocale) {
          await setLanguage(normalizedLocale)
        }
        const normalizedCurrency = profile.preferredCurrency?.trim().toUpperCase()
        if (
          normalizedCurrency &&
          (SUPPORTED_CURRENCIES as readonly string[]).includes(normalizedCurrency)
        ) {
          setCurrency(normalizedCurrency as (typeof SUPPORTED_CURRENCIES)[number])
        }
      } catch (error) {
        if (!cancelled && logoutTerminatedPortalAccount(error)) {
          return
        }
        if (!cancelled) {
          retryTimer = window.setTimeout(() => {
            void hydrateFromProfile()
          }, 2000)
        }
      }
    }

    void hydrateFromProfile()

    return () => {
      cancelled = true
      if (retryTimer !== null) {
        window.clearTimeout(retryTimer)
      }
    }
  }, [authenticated, isAdminRoute, setCurrency, setLanguage])

  const handleLogout = () => {
    void (async () => {
      if (authenticated) {
        await logoutPortalSession()
      }
      clearAuthSession()
      navigate('/', { replace: true })
    })()
  }

  return (
    <AlarmUiProvider>
      <AdminInfoCardPickRouteSync />
      <PortalHeader isAuthenticated={authenticated} onLogout={handleLogout} />
      <LiteracyHelpLayer />
      <AdminInfoCardPickLayer />
      <AdminInfoCardPickEditor />
      <CreateAlarmModal />
      <Outlet />
    </AlarmUiProvider>
  )
}

