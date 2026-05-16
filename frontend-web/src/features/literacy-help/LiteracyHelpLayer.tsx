import { useEffect } from 'react'
import { createPortal } from 'react-dom'
import { useLocation } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { resolvePageKeyFromPath } from '../../data/portalPages'
import {
  findTermFromElement,
  markDynamicHelpTargets,
  markInstrumentHelpTargets,
} from '../admin-info-card-pick/helpTargetMatching'
import { useLiteracyHelpMode } from './LiteracyHelpModeContext'
import { infoCardsApi } from '../../services/infoCardsApi'
import { HelpCardPopover } from '../../components/help/HelpCardPopover'

const INTERACTIVE_SELECTOR =
  'button, a, input, select, textarea, label, summary, [role="button"], [role="link"], [role="tab"], [role="menuitem"], .portal-icon-button'

function isEmptyAreaClick(target: HTMLElement): boolean {
  if (target.closest('[data-literacy-help-control]')) {
    return false
  }
  if (target.closest('.help-term')) {
    return false
  }
  if (target.closest('.literacy-help-target, [data-dynamic-help-term], [data-help-instrument]')) {
    return false
  }
  if (target.closest('[data-literacy-slug]')) {
    return false
  }
  if (target.closest('.lit-help-popover, .help-card-popover')) {
    return false
  }
  if (target.closest(INTERACTIVE_SELECTOR)) {
    return false
  }
  return true
}

export function LiteracyHelpLayer() {
  const { t } = useTranslation('common')
  const { pathname } = useLocation()
  const pageKey = resolvePageKeyFromPath(pathname)
  const { active, deactivate, openCardState, closeCard, openCard } = useLiteracyHelpMode()

  useEffect(() => {
    if (!active || !pageKey) {
      return
    }
    const cleanupDynamic = markDynamicHelpTargets(pageKey)
    const cleanupInstruments = markInstrumentHelpTargets(pageKey)
    return () => {
      cleanupDynamic()
      cleanupInstruments()
    }
  }, [active, pageKey, pathname])

  useEffect(() => {
    document.body.classList.toggle('literacy-help-mode', active)
    return () => {
      document.body.classList.remove('literacy-help-mode')
    }
  }, [active])

  useEffect(() => {
    if (!active) {
      return
    }

    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        if (openCardState) {
          closeCard()
        } else {
          deactivate()
        }
      }
    }

    const onClickCapture = (event: MouseEvent) => {
      const target = event.target
      if (!(target instanceof HTMLElement)) {
        return
      }

      if (target.closest('[data-literacy-help-control]')) {
        return
      }

      if (target.closest('.help-term')) {
        return
      }

      const instrumentRow = target.closest<HTMLElement>('[data-help-instrument]')
      if (instrumentRow && pageKey) {
        const instrumentSymbol = instrumentRow.getAttribute('data-help-instrument') ?? undefined
        const card = infoCardsApi.findForHelpTarget(pageKey, { instrumentSymbol })
        if (card) {
          openCard(card.id, instrumentRow.getBoundingClientRect())
          event.preventDefault()
          event.stopPropagation()
        }
        return
      }

      const helpTarget = target.closest<HTMLElement>(
        '.literacy-help-target, [data-dynamic-help-term]',
      )
      if (helpTarget && pageKey) {
        const elementId = helpTarget.getAttribute('data-help-element-id') ?? undefined
        const term =
          helpTarget.getAttribute('data-dynamic-help-term') ??
          helpTarget.getAttribute('data-help-term') ??
          findTermFromElement(target, pageKey) ??
          undefined
        const card = infoCardsApi.findForHelpTarget(pageKey, {
          term,
          elementId,
        })
        if (card) {
          openCard(card.id, helpTarget.getBoundingClientRect())
          event.preventDefault()
          event.stopPropagation()
        }
        return
      }

      const slugEl = target.closest('[data-literacy-slug]')
      if (slugEl) {
        const slug = slugEl.getAttribute('data-literacy-slug')
        if (slug) {
          void (async () => {
            const card = await infoCardsApi.getBySlug(slug)
            if (card) {
              openCard(card.id, slugEl.getBoundingClientRect())
            }
          })()
          event.preventDefault()
          event.stopPropagation()
        }
        return
      }

      if (target.closest('.lit-help-popover, .help-card-popover')) {
        return
      }

      if (isEmptyAreaClick(target)) {
        deactivate()
        return
      }

      closeCard()
    }

    document.addEventListener('keydown', onKeyDown)
    document.addEventListener('click', onClickCapture, true)

    return () => {
      document.removeEventListener('keydown', onKeyDown)
      document.removeEventListener('click', onClickCapture, true)
    }
  }, [active, deactivate, closeCard, openCard, openCardState, pageKey])

  if (!active) {
    return null
  }

  const card = openCardState ? infoCardsApi.getById(openCardState.cardId) : null

  return createPortal(
    <>
      <div className="literacy-help-blur" aria-hidden="true" />
      <div className="lit-help-banner" role="status" aria-live="polite">
        <span className="lit-help-banner-icon" aria-hidden="true">
          ?
        </span>
        <span>{t('header.literacyHelp.banner')}</span>
        <button type="button" className="lit-help-banner-exit" onClick={deactivate}>
          {t('header.literacyHelp.exit')}
        </button>
      </div>

      {card && openCardState ? (
        <HelpCardPopover
          card={card}
          anchor={openCardState.anchor}
          onClose={closeCard}
          onDeactivate={deactivate}
        />
      ) : null}
    </>,
    document.body,
  )
}
