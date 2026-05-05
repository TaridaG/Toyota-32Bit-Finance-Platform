import type { SupportedCurrency } from '../../../shared/preferences/preferences'
import { BIST_TRY_QUOTED_SYMBOLS } from './bistTrySymbols'

/** Minimal FX row shape (matches `/api/market/fx` entries). */
export type FxMidRow = {
  symbol?: string
  bid?: number | string
  ask?: number | string
  mid?: number | string
}

export type NativeQuote = 'TRY' | 'USD'

/** TRY per 1 unit of each major currency (from *TRY mid feeds). */
export type FxTryHub = {
  tryPerUsd: number
  tryPerEur?: number
  tryPerGbp?: number
  tryPerJpy?: number
  tryPerAed?: number
}

function toNum(v: unknown): number | null {
  if (v === null || v === undefined || v === '') return null
  const n = Number(v)
  return Number.isFinite(n) && n > 0 ? n : null
}

export function buildFxTryHub(fxItems: FxMidRow[]): FxTryHub | null {
  let tryPerUsd: number | null = null
  let tryPerEur: number | null = null
  let tryPerGbp: number | null = null
  let tryPerJpy: number | null = null
  let tryPerAed: number | null = null

  for (const item of fxItems) {
    const sym = String(item.symbol ?? '')
      .trim()
      .toUpperCase()
    const mid = toNum(item.mid ?? item.ask ?? item.bid)
    if (!mid) continue
    switch (sym) {
      case 'USDTRY':
        tryPerUsd = mid
        break
      case 'EURTRY':
        tryPerEur = mid
        break
      case 'GBPTRY':
        tryPerGbp = mid
        break
      case 'JPYTRY':
        tryPerJpy = mid
        break
      case 'AEDTRY':
        tryPerAed = mid
        break
      default:
        break
    }
  }

  if (!tryPerUsd) {
    return null
  }

  return {
    tryPerUsd,
    tryPerEur: tryPerEur ?? undefined,
    tryPerGbp: tryPerGbp ?? undefined,
    tryPerJpy: tryPerJpy ?? undefined,
    tryPerAed: tryPerAed ?? undefined,
  }
}

export function inferNativeQuote(symbol: string, category: string | null | undefined): NativeQuote {
  const cat = (category ?? 'STOCK').toUpperCase()
  const s = symbol.trim().toUpperCase()

  if (cat === 'CRYPTO') {
    return 'USD'
  }
  if (cat === 'FX') {
    if (s.length === 6 && s.endsWith('TRY')) {
      return 'TRY'
    }
    return 'USD'
  }
  if (cat === 'FUND') {
    return 'TRY'
  }
  if (cat === 'STOCK') {
    return BIST_TRY_QUOTED_SYMBOLS.has(s) ? 'TRY' : 'USD'
  }
  return 'USD'
}

function tryPerUnitOf(target: SupportedCurrency, hub: FxTryHub): number | null {
  switch (target) {
    case 'TRY':
      return 1
    case 'USD':
      return hub.tryPerUsd
    case 'EUR':
      return hub.tryPerEur ?? null
    case 'GBP':
      return hub.tryPerGbp ?? null
    case 'JPY':
      return hub.tryPerJpy ?? null
    case 'AED':
      return hub.tryPerAed ?? null
    default:
      return null
  }
}

/**
 * Converts instrument spot from native quote (TRY or USD) into header currency using TRY as bridge.
 */
export function convertToDisplayCurrency(
  price: number,
  native: NativeQuote,
  target: SupportedCurrency,
  hub: FxTryHub | null,
): number | null {
  if (!Number.isFinite(price) || price <= 0 || !hub) {
    return null
  }

  const valueTry = native === 'TRY' ? price : price * hub.tryPerUsd

  if (target === 'TRY') {
    return valueTry
  }

  const denom = tryPerUnitOf(target, hub)
  if (!denom || denom <= 0) {
    return null
  }

  return valueTry / denom
}
