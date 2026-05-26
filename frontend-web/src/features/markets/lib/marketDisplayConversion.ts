import type { SupportedCurrency } from '../../../shared/preferences/preferences'
import { normalizeFxQuotePrice } from './fxTryHubConversion'
/** US equities on Finnhub in MDS {@code tracked-stocks}; fallback when overview rows omit exchange/source. */
const US_LISTED_EQUITY_SYMBOLS = new Set([
  'AAPL',
  'AMZN',
  'NVDA',
  'MSFT',
  'GOOGL',
  'TSLA',
  'META',
  'AVGO',
  'AMD',
  'NFLX',
  'INTC',
  'CSCO',
])

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
    const rawMid = toNum(item.mid ?? item.ask ?? item.bid)
    if (!rawMid) continue
    const mid = sym === 'JPYTRY' || sym === 'JPYUSD' ? normalizeFxQuotePrice(sym, rawMid) : rawMid
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

export function inferNativeQuote(
  symbol: string,
  category: string | null | undefined,
  source?: string | null,
  listedExchange?: string | null,
): NativeQuote {
  const cat = (category ?? 'STOCK').toUpperCase()
  const s = symbol.trim().toUpperCase()
  const src = (source ?? '').trim().toUpperCase()
  const ex = (listedExchange ?? '').trim().toUpperCase()

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
    return s.startsWith('FUND_') ? 'TRY' : 'USD'
  }
  if (cat === 'DEPOSIT') {
    return 'TRY'
  }
  if (cat === 'STOCK') {
    if (ex === 'BIST' || ex === 'TEFAS') return 'TRY'
    if (ex === 'NASDAQ' || ex === 'FINNHUB' || ex === 'BINANCE') return 'USD'
    if (src === 'YAHOO') return 'TRY'
    if (src === 'FINNHUB') return 'USD'
    if (s.endsWith('.IS')) return 'TRY'
    // Overview API omits source/exchange; default BIST (TRY), not USD.
    return US_LISTED_EQUITY_SYMBOLS.has(s) ? 'USD' : 'TRY'
  }
  if (cat === 'METAL' && s.endsWith('TRY')) {
    return 'TRY'
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
