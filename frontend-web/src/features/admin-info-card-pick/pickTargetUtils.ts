const PICK_SKIP_SELECTOR =
  '[data-literacy-help-control], [data-admin-pick-control], .lit-help-banner, .lit-help-popover, .help-card-popover, .ic-modal-overlay, .portal-header'

const PICK_TARGET_SELECTOR =
  'button, a, label, th, td, [role="button"], [role="tab"], [role="link"], h1, h2, h3, h4, h5, h6, p, span, li, dt, dd, .lit-badge, .ic-chip'

export type PickTargetResult = {
  term: string
  label: string
  instrumentSymbol?: string
  elementId?: string
  i18nKey?: string
  i18nNs?: string
}

export function extractElementLabel(element: HTMLElement): string | null {
  const explicit = element.getAttribute('data-help-term')
  if (explicit?.trim()) {
    return explicit.trim()
  }
  const aria = element.getAttribute('aria-label')
  if (aria?.trim()) {
    return aria.trim()
  }
  const text = element.innerText?.trim().replace(/\s+/g, ' ')
  if (!text || text.length > 120) {
    return null
  }
  return text
}

export function resolvePickTargetFromEvent(event: MouseEvent): PickTargetResult | null {
  const selection = window.getSelection()?.toString().trim().replace(/\s+/g, ' ')
  if (selection && selection.length >= 2) {
    return {
      term: selection.slice(0, 120),
      label: selection.slice(0, 120),
    }
  }

  const target = event.target
  if (!(target instanceof HTMLElement)) {
    return null
  }

  const instrumentEl = target.closest<HTMLElement>('[data-help-instrument]')
  if (instrumentEl && !instrumentEl.closest(PICK_SKIP_SELECTOR)) {
    const symbol = instrumentEl.getAttribute('data-help-instrument')?.trim().toUpperCase()
    if (symbol) {
      const name = instrumentEl.getAttribute('data-help-instrument-name')?.trim()
      return {
        term: symbol,
        label: name ? `${symbol} — ${name}` : symbol,
        instrumentSymbol: symbol,
      }
    }
  }

  const candidate = target.closest<HTMLElement>(PICK_TARGET_SELECTOR)
  if (!candidate || candidate.closest(PICK_SKIP_SELECTOR)) {
    return null
  }

  const label = extractElementLabel(candidate)
  if (!label) {
    return null
  }

  const elementId = candidate.getAttribute('data-help-element-id') ?? undefined
  const i18nKey = candidate.getAttribute('data-help-i18n-key') ?? undefined
  const i18nNs = candidate.getAttribute('data-help-i18n-ns') ?? undefined

  return { term: label, label, elementId, i18nKey, i18nNs }
}

export function isAdminPickRouteAllowed(pathname: string): boolean {
  const normalized = pathname.replace(/\/+$/, '') || '/'
  if (normalized === '/app/bilgi-kartlari' || normalized.startsWith('/app/bilgi-kartlari/')) {
    return false
  }
  if (normalized === '/app/finansal-okuryazarlik' || normalized.startsWith('/app/finansal-okuryazarlik/')) {
    return false
  }
  return normalized.startsWith('/app')
}
