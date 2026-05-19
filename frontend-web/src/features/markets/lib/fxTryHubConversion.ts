import type { SupportedCurrency } from '../../../shared/preferences/preferences'
import type { FxMidRow } from './marketDisplayConversion'

/** Raw FX mid quotes keyed by canonical symbol (USDTRY, EURTRY, …). */
export type FxRateMap = Partial<Record<string, number>>

function toNum(v: unknown): number | null {
  if (v === null || v === undefined || v === '') return null
  const n = Number(v)
  return Number.isFinite(n) && n > 0 ? n : null
}

const FX_SYMBOL_LOAD_ORDER = [
  'USDTRY',
  'EURTRY',
  'GBPTRY',
  'JPYTRY',
  'AEDTRY',
  'EURUSD',
  'GBPUSD',
  'JPYUSD',
] as const

/**
 * TCMB / EVDS conventions — mirrors backend {@code CurrencyConversionServiceImpl}.
 */
export function normalizeFxMidForTryHub(symbol: string, raw: number): number | null {
  if (!Number.isFinite(raw) || raw <= 0) return null
  const sym = symbol.trim().toUpperCase()
  if (sym === 'JPYTRY') {
    return raw > 1 ? raw / 100 : raw
  }
  if (sym === 'JPYUSD') {
    let r = raw
    if (r > 10) r = 1 / r
    if (r > 0.03 && r < 1) r = r / 100
    return r
  }
  return raw
}

/** Canonical TRY/USD quote for display, simulation, and FX hub (1 JPY, not 100 JPY). */
export function normalizeFxQuotePrice(symbol: string, price: number): number {
  if (!Number.isFinite(price) || price <= 0) {
    return price
  }
  return normalizeFxMidForTryHub(symbol.trim().toUpperCase(), price) ?? price
}

/** OHLC bar with UTC seconds timestamp (chart / simulation candles). */
export type FxOhlcBar = {
  time: number
  open: number
  high: number
  low: number
  close: number
}

/**
 * TCMB lists JPY as TRY per 100 JPY (~28); TRY hub and charts use TRY per 1 JPY (~0.28).
 * Normalize each bar independently so mixed history rows still share one scale.
 */
export function normalizeFxCandleSeries<T extends FxOhlcBar>(symbol: string, candles: T[]): T[] {
  const sym = symbol.trim().toUpperCase()
  if (sym !== 'JPYTRY' && sym !== 'JPYUSD') {
    return candles
  }
  if (candles.length === 0) {
    return candles
  }

  const scale = (price: number) => {
    if (!Number.isFinite(price) || price <= 0) {
      return price
    }
    return normalizeFxMidForTryHub(sym, price) ?? price
  }

  return candles.map((c) => ({
    ...c,
    open: scale(c.open),
    high: scale(c.high),
    low: scale(c.low),
    close: scale(c.close),
  }))
}

export function buildFxRateMapFromHistories(
  histories: Record<string, number | null | undefined>,
): FxRateMap {
  const map: FxRateMap = {}
  for (const symbol of FX_SYMBOL_LOAD_ORDER) {
    const raw = histories[symbol]
    if (raw == null) continue
    const n = normalizeFxMidForTryHub(symbol, raw)
    if (n != null && n > 0) map[symbol] = n
  }
  return map
}

function multiply(a: number, b: number | undefined): number | null {
  if (!Number.isFinite(a) || b == null || !Number.isFinite(b) || b <= 0) return null
  return a * b
}

function divide(a: number, b: number | undefined): number | null {
  if (!Number.isFinite(a) || b == null || !Number.isFinite(b) || b <= 0) return null
  return a / b
}

function multiplyChain(a: number, first: number | undefined, second: number | undefined): number | null {
  const step = multiply(a, first)
  return step == null ? null : multiply(step, second)
}

function firstNonNull(a: number | null, b: number | null): number | null {
  return a != null ? a : b
}

function toTryAmount(amount: number, from: SupportedCurrency, rates: FxRateMap): number | null {
  switch (from) {
    case 'TRY':
      return amount
    case 'USD':
      return multiply(amount, rates.USDTRY)
    case 'EUR':
      return firstNonNull(
        multiply(amount, rates.EURTRY),
        multiplyChain(amount, rates.EURUSD, rates.USDTRY),
      )
    case 'GBP':
      return firstNonNull(
        multiply(amount, rates.GBPTRY),
        multiplyChain(amount, rates.GBPUSD, rates.USDTRY),
      )
    case 'JPY':
      return firstNonNull(
        multiply(amount, rates.JPYTRY),
        multiplyChain(amount, rates.JPYUSD, rates.USDTRY),
      )
    case 'AED':
      return multiply(amount, rates.AEDTRY)
    default:
      return null
  }
}

function fromTryAmount(tryAmount: number, to: SupportedCurrency, rates: FxRateMap): number | null {
  switch (to) {
    case 'TRY':
      return tryAmount
    case 'USD':
      return divide(tryAmount, rates.USDTRY)
    case 'EUR':
      return firstNonNull(
        divide(tryAmount, rates.EURTRY),
        divide(divide(tryAmount, rates.USDTRY) ?? NaN, rates.EURUSD),
      )
    case 'GBP':
      return firstNonNull(
        divide(tryAmount, rates.GBPTRY),
        divide(divide(tryAmount, rates.USDTRY) ?? NaN, rates.GBPUSD),
      )
    case 'JPY':
      return firstNonNull(
        divide(tryAmount, rates.JPYTRY),
        divide(divide(tryAmount, rates.USDTRY) ?? NaN, rates.JPYUSD),
      )
    case 'AED':
      return divide(tryAmount, rates.AEDTRY)
    default:
      return null
  }
}

function toUsdLegacy(amount: number, from: SupportedCurrency, rates: FxRateMap): number | null {
  switch (from) {
    case 'USD':
      return amount
    case 'TRY':
      return divide(amount, rates.USDTRY)
    case 'EUR':
      return multiply(amount, rates.EURUSD)
    case 'GBP':
      return multiply(amount, rates.GBPUSD)
    case 'JPY':
      return multiply(amount, rates.JPYUSD)
    default:
      return null
  }
}

function fromUsdLegacy(amountUsd: number, to: SupportedCurrency, rates: FxRateMap): number | null {
  switch (to) {
    case 'USD':
      return amountUsd
    case 'TRY':
      return multiply(amountUsd, rates.USDTRY)
    case 'EUR':
      return divide(amountUsd, rates.EURUSD)
    case 'GBP':
      return divide(amountUsd, rates.GBPUSD)
    case 'JPY':
      return divide(amountUsd, rates.JPYUSD)
    default:
      return null
  }
}

/** Converts amount between supported fiat currencies using TRY hub (+ USD legacy fallback). */
export function convertFiatAmount(
  amount: number,
  from: SupportedCurrency,
  to: SupportedCurrency,
  rates: FxRateMap,
): number | null {
  if (!Number.isFinite(amount)) return null
  if (from === to) return amount

  const inTry = toTryAmount(amount, from, rates)
  if (inTry != null) {
    const out = fromTryAmount(inTry, to, rates)
    if (out != null) return out
  }

  const inUsd = toUsdLegacy(amount, from, rates)
  if (inUsd == null) return null
  return fromUsdLegacy(inUsd, to, rates)
}

/** Units of `to` currency received per 1 unit of `from` currency. */
export function fxRateBetween(
  from: SupportedCurrency,
  to: SupportedCurrency,
  rates: FxRateMap,
): number | null {
  return convertFiatAmount(1, from, to, rates)
}

/** Build rate map from live `/api/market/fx` rows (normalized). */
export function buildFxRateMapFromMidRows(items: FxMidRow[]): FxRateMap {
  const raw: Record<string, number | null | undefined> = {}
  for (const item of items) {
    const sym = String(item.symbol ?? '')
      .trim()
      .toUpperCase()
    if (!FX_SYMBOL_LOAD_ORDER.includes(sym as (typeof FX_SYMBOL_LOAD_ORDER)[number])) continue
    const mid = toNum(item.mid ?? item.ask ?? item.bid)
    if (mid != null) raw[sym] = mid
  }
  return enrichFxRateMap(buildFxRateMapFromHistories(raw))
}

/** Fill missing cross pairs from TRY legs (same idea as backend hub fallbacks). */
export function enrichFxRateMap(map: FxRateMap): FxRateMap {
  const next: FxRateMap = { ...map }
  const usdTry = next.USDTRY
  if (usdTry && usdTry > 0) {
    if (!next.EURUSD && next.EURTRY) next.EURUSD = next.EURTRY / usdTry
    if (!next.GBPUSD && next.GBPTRY) next.GBPUSD = next.GBPTRY / usdTry
    if (!next.JPYUSD && next.JPYTRY) next.JPYUSD = next.JPYTRY / usdTry
    if (!next.EURTRY && next.EURUSD) next.EURTRY = next.EURUSD * usdTry
    if (!next.GBPTRY && next.GBPUSD) next.GBPTRY = next.GBPUSD * usdTry
    if (!next.JPYTRY && next.JPYUSD) next.JPYTRY = next.JPYUSD * usdTry
  }
  return next
}

export function mergeFxRateMaps(primary: FxRateMap, fallback: FxRateMap): FxRateMap {
  const merged: FxRateMap = { ...fallback, ...primary }
  for (const sym of FX_SYMBOL_LOAD_ORDER) {
    if (merged[sym] == null && fallback[sym] != null) merged[sym] = fallback[sym]
  }
  return enrichFxRateMap(merged)
}
