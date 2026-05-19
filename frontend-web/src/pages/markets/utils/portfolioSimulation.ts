import { inferNativeQuote, type NativeQuote } from '../../../features/markets/lib/marketDisplayConversion'
import {
  convertFiatAmount,
  type FxRateMap,
} from '../../../features/markets/lib/fxTryHubConversion'
import type { SupportedCurrency } from '../../../shared/preferences/preferences'
import type { CandlePoint } from '../../analysis/types'
import {
  findCandleOnOrBeforeDay,
  isoDayBounds,
  utcTimestampToIsoDay,
} from '../../analysis/utils/investmentSimulation'

export type PortfolioHolding = {
  id: string
  symbol: string
  name: string
  category: string | null
  weightPct: number
}

/** Per-asset period returns (1 unit of display currency invested at startDay). */
export type HoldingPeriodMetrics = {
  symbol: string
  nativeQuote: NativeQuote
  displayCurrency: SupportedCurrency
  entryDay: string
  exitDay: string
  assetReturnPct: number | null
  fxReturnPct: number | null
  totalReturnPct: number | null
  error: 'no_price' | 'no_fx' | null
}

export type PortfolioReturnSummary = {
  totalReturnPct: number
  entryDay: string
  exitDay: string
  holdings: Array<HoldingPeriodMetrics & { weightPct: number; name: string }>
}

const NOTIONAL = 1

function pctFromRatio(ratio: number): number {
  return (ratio - 1) * 100
}

function tryPerUsd(rates: FxRateMap): number | null {
  const v = rates.USDTRY
  return v != null && v > 0 ? v : null
}

/**
 * TRY-quoted price P = TRY per 1 unit of base (FX pair, metal, …).
 * Value of 1 base in USD via TRY hub: P / USDTRY.
 */
function baseInUsdFromTryQuote(pairTry: number, rates: FxRateMap): number | null {
  const usdTry = tryPerUsd(rates)
  if (usdTry == null || pairTry <= 0) {
    return null
  }
  return pairTry / usdTry
}

/**
 * TRY-quoted instrument shown in USD (e.g. GBPTRY, XAUTRY, USDTRY):
 * - Varlık: paritenin TRY cinsinden hareketi (P_exit / P_entry).
 * - Kur: bazın USD karşısındaki çapraz getirisi — (P/U)_exit / (P/U)_entry; varlığa göre değişir.
 * - Toplam: 1 USD ile giriş simülasyonu (çapraz getiri ile uyumlu).
 */
function decomposeTryQuotedInUsd(
  entryPrice: number,
  exitPrice: number,
  entryFx: FxRateMap,
  exitFx: FxRateMap,
): { assetReturnPct: number; fxReturnPct: number; totalReturnPct: number } | null {
  const crossEntry = baseInUsdFromTryQuote(entryPrice, entryFx)
  const crossExit = baseInUsdFromTryQuote(exitPrice, exitFx)
  if (crossEntry == null || crossExit == null || crossEntry <= 0) {
    return null
  }

  const assetReturnPct = pctFromRatio(exitPrice / entryPrice)
  const fxReturnPct = pctFromRatio(crossExit / crossEntry)

  const tryAtEntry = convertFiatAmount(NOTIONAL, 'USD', 'TRY', entryFx)
  const tryAtExit = tryAtEntry != null ? (tryAtEntry / entryPrice) * exitPrice : null
  const totalValue =
    tryAtExit != null ? convertFiatAmount(tryAtExit, 'TRY', 'USD', exitFx) : null
  const totalReturnPct =
    totalValue != null ? pctFromRatio(totalValue / NOTIONAL) : fxReturnPct

  return { assetReturnPct, fxReturnPct, totalReturnPct }
}

/** USD-quoted instrument shown in TRY: cross = P * USDTRY (value in TRY per 1 unit). */
function decomposeUsdQuotedInTry(
  entryPrice: number,
  exitPrice: number,
  entryFx: FxRateMap,
  exitFx: FxRateMap,
): { assetReturnPct: number; fxReturnPct: number; totalReturnPct: number } | null {
  const usdTryEntry = tryPerUsd(entryFx)
  const usdTryExit = tryPerUsd(exitFx)
  if (usdTryEntry == null || usdTryExit == null || entryPrice <= 0) {
    return null
  }

  const crossEntry = entryPrice * usdTryEntry
  const crossExit = exitPrice * usdTryExit
  const assetReturnPct = pctFromRatio(exitPrice / entryPrice)
  const fxReturnPct = pctFromRatio(crossExit / crossEntry)

  const usdAtEntry = convertFiatAmount(NOTIONAL, 'TRY', 'USD', entryFx)
  const units = usdAtEntry != null ? usdAtEntry / entryPrice : null
  const tryAtExit =
    units != null ? convertFiatAmount(units * exitPrice, 'USD', 'TRY', exitFx) : null
  const totalReturnPct =
    tryAtExit != null ? pctFromRatio(tryAtExit / NOTIONAL) : fxReturnPct

  return { assetReturnPct, fxReturnPct, totalReturnPct }
}

export function computeHoldingPeriodMetrics(
  series: CandlePoint[],
  startDay: string,
  displayCurrency: SupportedCurrency,
  nativeQuote: NativeQuote,
  entryFx: FxRateMap,
  exitFx: FxRateMap,
): HoldingPeriodMetrics {
  const base: HoldingPeriodMetrics = {
    symbol: '',
    nativeQuote,
    displayCurrency,
    entryDay: startDay,
    exitDay: startDay,
    assetReturnPct: null,
    fxReturnPct: null,
    totalReturnPct: null,
    error: null,
  }

  if (!startDay || series.length === 0) {
    return { ...base, error: 'no_price' }
  }

  const sorted = [...series].sort((a, b) => a.time - b.time)
  const entry = findCandleOnOrBeforeDay(sorted, startDay)
  const exit = sorted[sorted.length - 1]
  if (!entry || !exit || entry.close <= 0 || exit.close <= 0) {
    return { ...base, error: 'no_price' }
  }

  const entryDay = utcTimestampToIsoDay(entry.time)
  const exitDay = utcTimestampToIsoDay(exit.time)
  const entryPrice = entry.close
  const exitPrice = exit.close

  if (displayCurrency === nativeQuote) {
    const totalReturnPct = pctFromRatio(exitPrice / entryPrice)
    return {
      ...base,
      entryDay,
      exitDay,
      assetReturnPct: totalReturnPct,
      fxReturnPct: null,
      totalReturnPct,
      error: null,
    }
  }

  if (nativeQuote === 'TRY' && displayCurrency === 'USD') {
    const legs = decomposeTryQuotedInUsd(entryPrice, exitPrice, entryFx, exitFx)
    if (!legs) {
      return { ...base, entryDay, exitDay, error: 'no_fx' }
    }
    return {
      ...base,
      entryDay,
      exitDay,
      assetReturnPct: legs.assetReturnPct,
      fxReturnPct: legs.fxReturnPct,
      totalReturnPct: legs.totalReturnPct,
      error: null,
    }
  }

  if (nativeQuote === 'USD' && displayCurrency === 'TRY') {
    const legs = decomposeUsdQuotedInTry(entryPrice, exitPrice, entryFx, exitFx)
    if (!legs) {
      return { ...base, entryDay, exitDay, error: 'no_fx' }
    }
    return {
      ...base,
      entryDay,
      exitDay,
      assetReturnPct: legs.assetReturnPct,
      fxReturnPct: legs.fxReturnPct,
      totalReturnPct: legs.totalReturnPct,
      error: null,
    }
  }

  return { ...base, entryDay, exitDay, error: 'no_fx' }
}

export function computeWeightedPortfolioReturn(
  holdings: PortfolioHolding[],
  metricsByHoldingId: Map<string, HoldingPeriodMetrics>,
): number | null {
  if (holdings.length === 0) {
    return null
  }
  let weighted = 0
  for (const holding of holdings) {
    const metrics = metricsByHoldingId.get(holding.id)
    if (!metrics || metrics.error || metrics.totalReturnPct == null) {
      return null
    }
    weighted += (Math.max(0, holding.weightPct) / 100) * metrics.totalReturnPct
  }
  return weighted
}

export function buildPortfolioReturnSummary(
  holdings: PortfolioHolding[],
  metricsByHoldingId: Map<string, HoldingPeriodMetrics>,
  weightsOk: boolean,
): PortfolioReturnSummary | null {
  if (!weightsOk || holdings.length === 0) {
    return null
  }
  const totalReturnPct = computeWeightedPortfolioReturn(holdings, metricsByHoldingId)
  if (totalReturnPct == null) {
    return null
  }

  let entryDay = holdings[0] ? metricsByHoldingId.get(holdings[0].id)?.entryDay ?? '' : ''
  let exitDay = entryDay
  const rows: PortfolioReturnSummary['holdings'] = []

  for (const holding of holdings) {
    const metrics = metricsByHoldingId.get(holding.id)
    if (!metrics) {
      return null
    }
    if (metrics.entryDay && metrics.entryDay < entryDay) {
      entryDay = metrics.entryDay
    }
    if (metrics.exitDay && metrics.exitDay > exitDay) {
      exitDay = metrics.exitDay
    }
    rows.push({
      ...metrics,
      weightPct: holding.weightPct,
      name: holding.name,
      symbol: holding.symbol,
    })
  }

  return { totalReturnPct, entryDay, exitDay, holdings: rows }
}

export function equalizeWeights(holdings: PortfolioHolding[]): PortfolioHolding[] {
  if (holdings.length === 0) {
    return holdings
  }
  const each = Math.round((100 / holdings.length) * 100) / 100
  let remainder = 100
  return holdings.map((holding, index) => {
    const weight = index === holdings.length - 1 ? remainder : each
    remainder -= weight
    return { ...holding, weightPct: weight }
  })
}

export function normalizeWeights(holdings: PortfolioHolding[]): PortfolioHolding[] {
  const sum = holdings.reduce((acc, h) => acc + Math.max(0, h.weightPct), 0)
  if (sum <= 0) {
    return equalizeWeights(holdings)
  }
  return holdings.map((h) => ({
    ...h,
    weightPct: Math.round((Math.max(0, h.weightPct) / sum) * 10000) / 100,
  }))
}

export function portfolioDateBounds(seriesBySymbol: Record<string, CandlePoint[]>): {
  min: string
  max: string
} | null {
  let min: string | null = null
  let max: string | null = null
  for (const series of Object.values(seriesBySymbol)) {
    const bounds = isoDayBounds(series)
    if (!bounds) {
      continue
    }
    if (min == null || bounds.min < min) {
      min = bounds.min
    }
    if (max == null || bounds.max > max) {
      max = bounds.max
    }
  }
  if (min == null || max == null) {
    return null
  }
  return { min, max }
}

export function defaultStartDay(bounds: { min: string; max: string } | null): string {
  if (!bounds) {
    return new Date().toISOString().slice(0, 10)
  }
  const maxTs = Date.parse(`${bounds.max}T12:00:00Z`)
  const yearAgo = new Date(maxTs)
  yearAgo.setUTCFullYear(yearAgo.getUTCFullYear() - 1)
  const candidate = yearAgo.toISOString().slice(0, 10)
  return candidate < bounds.min ? bounds.min : candidate
}

export function isStartDayValid(seriesBySymbol: Record<string, CandlePoint[]>, startDay: string): boolean {
  if (!startDay) {
    return false
  }
  return Object.values(seriesBySymbol).some((series) => findCandleOnOrBeforeDay(series, startDay) != null)
}

export function inferHoldingNativeQuote(holding: PortfolioHolding): NativeQuote {
  return inferNativeQuote(holding.symbol, holding.category)
}
