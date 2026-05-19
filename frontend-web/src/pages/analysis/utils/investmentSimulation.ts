import type { UTCTimestamp } from 'lightweight-charts'
import {
  convertFiatAmount as convertFiat,
  fxRateBetween,
  type FxRateMap,
} from '../../../features/markets/lib/fxTryHubConversion'
import type { NativeQuote } from '../../../features/markets/lib/marketDisplayConversion'
import type { SupportedCurrency } from '../../../shared/preferences/preferences'
import type { CandlePoint } from '../types'

export type InvestmentSimulationBreakdown = {
  entryFxRate: number | null
  exitFxRate: number | null
  amountInNative: number | null
  valueInNative: number | null
  nativeQuote: NativeQuote
  paymentCurrency: SupportedCurrency
}

export type InvestmentSimulationResult = {
  entryPrice: number
  exitPrice: number
  units: number
  cost: number
  currentValue: number
  pnl: number
  pnlPct: number
  entryDay: string
  exitDay: string
  breakdown: InvestmentSimulationBreakdown
}

export function utcTimestampToIsoDay(ts: UTCTimestamp): string {
  return new Date(ts * 1000).toISOString().slice(0, 10)
}

export function isoDayBounds(series: CandlePoint[]): { min: string; max: string } | null {
  if (series.length === 0) return null
  const days = series.map((p) => utcTimestampToIsoDay(p.time)).sort()
  return { min: days[0], max: days[days.length - 1] }
}

/** Last candle on or before end of `isoDay` (UTC). */
export function findCandleOnOrBeforeDay(series: CandlePoint[], isoDay: string): CandlePoint | null {
  if (!isoDay || series.length === 0) return null
  const endSec = Math.floor(Date.parse(`${isoDay}T23:59:59Z`) / 1000)
  let best: CandlePoint | null = null
  for (const p of series) {
    if (p.time <= endSec) {
      best = p
    } else {
      break
    }
  }
  return best
}

export type InvestmentSimulationAttempt = {
  result: InvestmentSimulationResult | null
  partial: InvestmentSimulationBreakdown | null
  failure: 'no_price' | 'no_entry_fx' | 'no_exit_fx' | null
}

export function computeInvestmentSimulation(
  series: CandlePoint[],
  purchaseDay: string,
  paymentAmount: number,
  paymentCurrency: SupportedCurrency,
  nativeQuote: NativeQuote,
  entryFx: FxRateMap,
  exitFx: FxRateMap,
): InvestmentSimulationAttempt {
  const breakdownBase: InvestmentSimulationBreakdown = {
    entryFxRate: null,
    exitFxRate: null,
    amountInNative: null,
    valueInNative: null,
    nativeQuote,
    paymentCurrency,
  }

  if (!purchaseDay || !Number.isFinite(paymentAmount) || paymentAmount <= 0 || series.length === 0) {
    return { result: null, partial: breakdownBase, failure: 'no_price' }
  }

  const sorted = [...series].sort((a, b) => a.time - b.time)
  const entry = findCandleOnOrBeforeDay(sorted, purchaseDay)
  const exit = sorted[sorted.length - 1]
  if (!entry || !exit || entry.close <= 0 || exit.close <= 0) {
    return { result: null, partial: breakdownBase, failure: 'no_price' }
  }
  if (utcTimestampToIsoDay(entry.time) > purchaseDay) {
    return { result: null, partial: breakdownBase, failure: 'no_price' }
  }

  const instrumentCurrency = nativeQuote
  const entryFxRate = fxRateBetween(paymentCurrency, instrumentCurrency, entryFx)
  const exitFxRate = fxRateBetween(instrumentCurrency, paymentCurrency, exitFx)

  const partial: InvestmentSimulationBreakdown = {
    ...breakdownBase,
    entryFxRate,
    exitFxRate,
  }

  if (entryFxRate == null || entryFxRate <= 0) {
    return { result: null, partial, failure: 'no_entry_fx' }
  }

  const amountInNative = paymentAmount * entryFxRate
  const units = amountInNative / entry.close
  const valueInNative = units * exit.close

  partial.amountInNative = amountInNative
  partial.valueInNative = valueInNative

  const currentValue = convertFiat(valueInNative, instrumentCurrency, paymentCurrency, exitFx)
  if (currentValue == null) {
    return { result: null, partial, failure: 'no_exit_fx' }
  }

  const cost = paymentAmount
  const pnl = currentValue - cost
  const pnlPct = cost > 0 ? (pnl / cost) * 100 : 0

  return {
    result: {
      entryPrice: entry.close,
      exitPrice: exit.close,
      units,
      cost,
      currentValue,
      pnl,
      pnlPct,
      entryDay: utcTimestampToIsoDay(entry.time),
      exitDay: utcTimestampToIsoDay(exit.time),
      breakdown: partial,
    },
    partial,
    failure: null,
  }
}
