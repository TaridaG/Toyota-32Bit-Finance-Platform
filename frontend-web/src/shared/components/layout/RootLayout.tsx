import { useEffect } from 'react'
import { Outlet, useNavigate } from 'react-router-dom'
import { PortalHeader } from './PortalHeader'
import { clearAuthSession, isAuthenticated } from '../../auth/session'
import { normalizeLocale } from '../../i18n'
import { useAppPreferences } from '../../preferences/useAppPreferences'
import { fetchPortalProfile } from '../../../features/profile/api/portalProfileApi'
import { SUPPORTED_CURRENCIES } from '../../preferences/preferences'
import { LiteracyHelpLayer } from '../../../features/literacy-help/LiteracyHelpLayer'
import { AdminInfoCardPickLayer } from '../../../features/admin-info-card-pick/AdminInfoCardPickLayer'
import { AdminInfoCardPickEditor } from '../../../features/admin-info-card-pick/AdminInfoCardPickEditor'
import { AdminInfoCardPickRouteSync } from '../../../features/admin-info-card-pick/AdminInfoCardPickRouteSync'

export function RootLayout() {
  const navigate = useNavigate()
  const authenticated = isAuthenticated()
  const { setLanguage, setCurrency } = useAppPreferences()

  useEffect(() => {
    let cancelled = false
    let retryTimer: number | null = null

    if (!authenticated) {
      return
    }

    const hydrateFromProfile = async () => {
      try {
        const profile = await fetchPortalProfile()
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
      } catch {
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
  }, [authenticated, setCurrency, setLanguage])

  const handleLogout = () => {
    clearAuthSession()
    navigate('/', { replace: true })
  }

  return (
    <>
      <AdminInfoCardPickRouteSync />
      <PortalHeader isAuthenticated={authenticated} onLogout={handleLogout} />
      <LiteracyHelpLayer />
      <AdminInfoCardPickLayer />
      <AdminInfoCardPickEditor />
      <Outlet />
    </>
  )
}

