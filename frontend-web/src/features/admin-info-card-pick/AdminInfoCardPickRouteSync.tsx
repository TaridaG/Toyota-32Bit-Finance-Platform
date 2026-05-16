import { useEffect } from 'react'
import { useLocation } from 'react-router-dom'
import { useLiteracyHelpMode } from '../literacy-help/LiteracyHelpModeContext'
import { useAdminInfoCardPick } from './AdminInfoCardPickContext'
import { isAdminPickRouteAllowed } from './pickTargetUtils'

export function AdminInfoCardPickRouteSync() {
  const { pathname } = useLocation()
  const { setCanPickOnRoute, pickModeActive, deactivatePickMode } = useAdminInfoCardPick()
  const { active: helpActive, deactivate: deactivateHelp } = useLiteracyHelpMode()

  useEffect(() => {
    const allowed = isAdminPickRouteAllowed(pathname)
    setCanPickOnRoute(allowed)
    if (!allowed && pickModeActive) {
      deactivatePickMode()
    }
  }, [pathname, setCanPickOnRoute, pickModeActive, deactivatePickMode])

  useEffect(() => {
    if (pickModeActive && helpActive) {
      deactivateHelp()
    }
  }, [pickModeActive, helpActive, deactivateHelp])

  return null
}
