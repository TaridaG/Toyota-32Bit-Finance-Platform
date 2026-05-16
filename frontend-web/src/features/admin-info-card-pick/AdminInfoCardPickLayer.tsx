import { useEffect } from 'react'
import { createPortal } from 'react-dom'
import { useLocation } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { resolvePageKeyFromPath } from '../../data/portalPages'
import { useLiteracyHelpMode } from '../literacy-help/LiteracyHelpModeContext'
import { useAdminInfoCardPick } from './AdminInfoCardPickContext'
import { resolveElementIdsForPick } from './resolvePickElementIds'
import { isAdminPickRouteAllowed, resolvePickTargetFromEvent } from './pickTargetUtils'

export function AdminInfoCardPickLayer() {
  const { t } = useTranslation('common')
  const { pathname } = useLocation()
  const { deactivate: deactivateHelp } = useLiteracyHelpMode()
  const { pickModeActive, canPickOnRoute, deactivatePickMode, openEditorFromPick } = useAdminInfoCardPick()

  const pageKey = resolvePageKeyFromPath(pathname)
  const active = pickModeActive && canPickOnRoute && pageKey != null

  useEffect(() => {
    document.body.classList.toggle('admin-info-card-pick-mode', active)
    return () => {
      document.body.classList.remove('admin-info-card-pick-mode')
    }
  }, [active])

  useEffect(() => {
    if (!active) {
      return
    }
    deactivateHelp()
  }, [active, deactivateHelp])

  useEffect(() => {
    if (!active || !pageKey) {
      return
    }

    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        deactivatePickMode()
      }
    }

    const onMouseUp = (event: MouseEvent) => {
      const pick = resolvePickTargetFromEvent(event)
      if (!pick) {
        return
      }
      event.preventDefault()
      event.stopPropagation()
      const targetElementIds = pick.instrumentSymbol
        ? []
        : resolveElementIdsForPick(pageKey, pick.label, pick.term, pick.elementId)
      openEditorFromPick({
        pageKey,
        targetTerms: pick.instrumentSymbol ? [pick.instrumentSymbol] : [pick.term],
        targetElementIds,
        targetInstrumentSymbols: pick.instrumentSymbol ? [pick.instrumentSymbol] : [],
        title: pick.instrumentSymbol
          ? pick.label.includes('—')
            ? pick.label.split('—')[1]?.trim() || pick.term
            : pick.term
          : pick.term,
        pickLabel: pick.label,
      })
    }

    const onClickCapture = (event: MouseEvent) => {
      const target = event.target
      if (!(target instanceof HTMLElement)) {
        return
      }
      if (target.closest('[data-admin-pick-control], [data-literacy-help-control]')) {
        return
      }
      if (target.closest('.admin-pick-banner')) {
        return
      }
      event.preventDefault()
      event.stopPropagation()
    }

    document.addEventListener('keydown', onKeyDown)
    document.addEventListener('mouseup', onMouseUp, true)
    document.addEventListener('click', onClickCapture, true)

    return () => {
      document.removeEventListener('keydown', onKeyDown)
      document.removeEventListener('mouseup', onMouseUp, true)
      document.removeEventListener('click', onClickCapture, true)
    }
  }, [active, pageKey, deactivatePickMode, openEditorFromPick])

  useEffect(() => {
    if (!pickModeActive) {
      return
    }
    if (!isAdminPickRouteAllowed(pathname)) {
      deactivatePickMode()
    }
  }, [pathname, pickModeActive, deactivatePickMode])

  if (!active) {
    return null
  }

  return createPortal(
    <>
      <div className="admin-pick-blur" aria-hidden="true" />
      <div className="admin-pick-banner" role="status" aria-live="polite">
        <span className="admin-pick-banner-icon" aria-hidden="true">
          ?+
        </span>
        <span>{t('header.adminPick.banner')}</span>
        <button type="button" className="admin-pick-banner-exit" onClick={deactivatePickMode}>
          {t('header.adminPick.exit')}
        </button>
      </div>
    </>,
    document.body,
  )
}
