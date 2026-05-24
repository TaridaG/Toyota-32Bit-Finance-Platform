import i18n from '../../shared/i18n'
import { PORTAL_PAGE_ELEMENTS } from '../../data/portalPageElements'
import { SUPPORTED_LOCALES, type SupportedLocale } from '../../shared/i18n'
import type { PortalPageKey } from '../../types/infoCards'

const MARKET_CATEGORY_KEYS = [
  'all',
  'crypto',
  'bist',
  'nasdaq',
  'stocks',
  'forex',
  'metals',
  'funds',
  'bonds',
  'eurobond',
] as const

export type PickLocaleTitleInput = {
  label: string
  term: string
  pageKey?: PortalPageKey
  elementId?: string
  instrumentSymbol?: string
  i18nKey?: string
  i18nNs?: string
}

function normalize(value: string): string {
  return value.trim().toLowerCase().replace(/\s+/g, ' ')
}

function translateKey(ns: string, key: string, locale: SupportedLocale): string | null {
  const fixed = i18n.getFixedT(locale, ns)
  const translated = fixed(key, { defaultValue: '' })
  if (!translated || translated === key) {
    return null
  }
  return translated
}

function titlesFromI18n(ns: string, key: string): Record<SupportedLocale, string> | null {
  const titles = {} as Record<SupportedLocale, string>
  let found = 0
  for (const locale of SUPPORTED_LOCALES) {
    const value = translateKey(ns, key, locale)
    if (value) {
      titles[locale] = value
      found += 1
    }
  }
  return found > 0 ? titles : null
}

function inferMarketsCategoryKey(label: string, term: string): string | null {
  const candidates = [label, term].map(normalize).filter(Boolean)
  if (candidates.length === 0) {
    return null
  }
  for (const categoryKey of MARKET_CATEGORY_KEYS) {
    for (const locale of SUPPORTED_LOCALES) {
      const translated = translateKey('markets', `categories.${categoryKey}`, locale)
      if (translated && candidates.includes(normalize(translated))) {
        return categoryKey
      }
    }
  }
  return null
}

function titlesFromPortalElement(elementId: string): Record<SupportedLocale, string> | null {
  const element = PORTAL_PAGE_ELEMENTS.find((item) => item.id === elementId)
  if (!element?.labelI18nKey) {
    return null
  }
  const ns = element.labelI18nNs ?? 'common'
  return titlesFromI18n(ns, element.labelI18nKey)
}

export function resolvePickLocaleTitles(input: PickLocaleTitleInput): Record<SupportedLocale, string> {
  const fallback = input.label.trim() || input.term.trim()

  if (input.i18nKey?.trim()) {
    const ns = input.i18nNs?.trim() || 'common'
    const fromKey = titlesFromI18n(ns, input.i18nKey.trim())
    if (fromKey) {
      return fromKey
    }
  }

  if (input.elementId) {
    const fromElement = titlesFromPortalElement(input.elementId)
    if (fromElement) {
      return fromElement
    }
  }

  const categoryKey = inferMarketsCategoryKey(input.label, input.term)
  if (categoryKey) {
    const fromCategory = titlesFromI18n('markets', `categories.${categoryKey}`)
    if (fromCategory) {
      return fromCategory
    }
  }

  if (input.instrumentSymbol?.trim()) {
    const symbol = input.instrumentSymbol.trim().toUpperCase()
    const titles = {} as Record<SupportedLocale, string>
    for (const locale of SUPPORTED_LOCALES) {
      titles[locale] = symbol
    }
    return titles
  }

  const titles = {} as Record<SupportedLocale, string>
  for (const locale of SUPPORTED_LOCALES) {
    titles[locale] = fallback
  }
  return titles
}

export function uniqueTargetTerms(
  term: string,
  label: string,
  localeTitles: Record<SupportedLocale, string>,
  instrumentSymbol?: string,
): string[] {
  const values = new Set<string>()
  if (instrumentSymbol?.trim()) {
    values.add(instrumentSymbol.trim().toUpperCase())
  }
  for (const value of [term, label, ...Object.values(localeTitles)]) {
    const trimmed = value.trim()
    if (trimmed) {
      values.add(trimmed)
    }
  }
  return [...values]
}
