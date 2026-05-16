import { infoCardsApi } from '../../services/infoCardsApi'
import type { PortalPageKey } from '../../types/infoCards'

const SKIP_SELECTOR =
  '[data-literacy-help-control], [data-admin-pick-control], .lit-help-banner, .lit-help-popover, .help-card-popover, .ic-modal-overlay, .portal-header, nav.portal-nav'

const CANDIDATE_SELECTOR =
  'button, a, label, th, td, [role="button"], [role="tab"], [role="link"], h1, h2, h3, h4, h5, h6, p, span, li, dt, dd'

export function getActiveInstrumentsForPage(pageKey: PortalPageKey): Set<string> {
  const symbols = new Set<string>()
  for (const card of infoCardsApi.getAll()) {
    if (card.status !== 'ACTIVE' || !card.pages.includes(pageKey)) {
      continue
    }
    for (const symbol of card.targetInstrumentSymbols ?? []) {
      const normalized = symbol.trim().toUpperCase()
      if (normalized) {
        symbols.add(normalized)
      }
    }
  }
  return symbols
}

export function getActiveTermsForPage(pageKey: PortalPageKey): Set<string> {
  const terms = new Set<string>()
  for (const card of infoCardsApi.getAll()) {
    if (card.status !== 'ACTIVE' || !card.pages.includes(pageKey)) {
      continue
    }
    for (const term of card.targetTerms) {
      const normalized = term.trim()
      if (normalized) {
        terms.add(normalized)
      }
    }
  }
  return terms
}

export function normalizeTerm(value: string): string {
  return value.trim().replace(/\s+/g, ' ')
}

export function findTermFromElement(element: HTMLElement, pageKey: PortalPageKey): string | null {
  const terms = getActiveTermsForPage(pageKey)
  if (terms.size === 0) {
    return null
  }

  let node: HTMLElement | null = element
  while (node) {
    if (node.closest(SKIP_SELECTOR)) {
      return null
    }
    const dataTerm = node.getAttribute('data-help-term') ?? node.getAttribute('data-dynamic-help-term')
    if (dataTerm && terms.has(normalizeTerm(dataTerm))) {
      return normalizeTerm(dataTerm)
    }
    const text = normalizeTerm(node.innerText ?? '')
    if (text.length >= 2 && text.length <= 80 && terms.has(text)) {
      return text
    }
    node = node.parentElement
  }
  return null
}

export function markInstrumentHelpTargets(pageKey: PortalPageKey): () => void {
  const symbols = getActiveInstrumentsForPage(pageKey)
  const marked: HTMLElement[] = []

  if (symbols.size === 0) {
    return () => undefined
  }

  const root = document.getElementById('root')
  if (!root) {
    return () => undefined
  }

  for (const el of root.querySelectorAll<HTMLElement>('[data-help-instrument]')) {
    if (el.closest(SKIP_SELECTOR)) {
      continue
    }
    const symbol = el.getAttribute('data-help-instrument')?.trim().toUpperCase()
    if (!symbol || !symbols.has(symbol)) {
      continue
    }
    el.classList.add('literacy-help-target')
    marked.push(el)
  }

  return () => {
    for (const el of marked) {
      el.classList.remove('literacy-help-target')
    }
  }
}

export function markDynamicHelpTargets(pageKey: PortalPageKey): () => void {
  const terms = getActiveTermsForPage(pageKey)
  const marked: HTMLElement[] = []

  if (terms.size === 0) {
    return () => undefined
  }

  const root = document.getElementById('root')
  if (!root) {
    return () => undefined
  }

  for (const el of root.querySelectorAll<HTMLElement>(CANDIDATE_SELECTOR)) {
    if (el.closest(SKIP_SELECTOR)) {
      continue
    }
    const text = normalizeTerm(el.innerText ?? '')
    if (!text || text.length > 80) {
      continue
    }
    if (!terms.has(text)) {
      continue
    }
    if (el.classList.contains('help-term')) {
      continue
    }
    el.classList.add('literacy-help-target')
    el.setAttribute('data-dynamic-help-term', text)
    marked.push(el)
  }

  return () => {
    for (const el of marked) {
      el.classList.remove('literacy-help-target')
      el.removeAttribute('data-dynamic-help-term')
    }
  }
}
