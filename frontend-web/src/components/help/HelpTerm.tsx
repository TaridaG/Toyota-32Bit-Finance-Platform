import type { ReactNode, MouseEvent } from 'react'
import { infoCardsApi } from '../../services/infoCardsApi'
import { resolvePageKeyFromPath } from '../../data/portalPages'
import { useLiteracyHelpMode } from '../../features/literacy-help/LiteracyHelpModeContext'
import type { PortalPageKey } from '../../types/infoCards'
import { useLocation } from 'react-router-dom'

type HelpTermProps = {
  term: string
  pageKey?: PortalPageKey
  /** Stable id from portal page element catalog (admin Bilgi Kartları picker) */
  elementId?: string
  /** When set, admin pick resolves titles for tr/en/de from this i18n key */
  i18nKey?: string
  i18nNs?: string
  children: ReactNode
  className?: string
}

export function HelpTerm({ term, pageKey, elementId, i18nKey, i18nNs, children, className }: HelpTermProps) {
  const { pathname } = useLocation()
  const { active, openCard } = useLiteracyHelpMode()
  const resolvedPage = pageKey ?? resolvePageKeyFromPath(pathname)
  const card =
    resolvedPage && active
      ? infoCardsApi.findForHelpTarget(resolvedPage, { term, elementId })
      : undefined

  if (!active || !card) {
    return <span className={className}>{children}</span>
  }

  const handleClick = (event: MouseEvent<HTMLButtonElement>) => {
    event.preventDefault()
    event.stopPropagation()
    const rect = event.currentTarget.getBoundingClientRect()
    openCard(card.id, rect)
  }

  return (
    <button
      type="button"
      className={`help-term literacy-help-target${className ? ` ${className}` : ''}`}
      onClick={handleClick}
      title={card.title}
      data-help-term={term}
      data-help-element-id={elementId}
      data-help-i18n-key={i18nKey}
      data-help-i18n-ns={i18nNs}
    >
      {children}
    </button>
  )
}
