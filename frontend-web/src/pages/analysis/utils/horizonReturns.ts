import type { CandlePoint } from '../types'
import type { TickerHorizonReturns } from '../components/AnalysisTickerBar'

const DAY_SEC = 86400

/**
 * Trailing % for daily NAV / end-of-day series: compare latest close to the last point on or before
 * {@code calendarDays} ago. Returns null when a data gap would inflate the move (e.g. Feb → May).
 */
export function trailingCalendarReturnPercent(
  candles: CandlePoint[],
  calendarDays: number,
  maxSpanSlipDays = 12,
): number | null {
  if (candles.length < 2 || calendarDays < 1) {
    return null
  }
  const sorted = [...candles].sort((a, b) => Number(a.time) - Number(b.time))
  const last = sorted[sorted.length - 1]
  const lastTs = Number(last.time)
  const cutoffTs = lastTs - calendarDays * DAY_SEC

  let baseline = sorted[0]
  for (const point of sorted) {
    if (Number(point.time) <= cutoffTs) {
      baseline = point
    } else {
      break
    }
  }
  if (baseline.time === last.time) {
    return null
  }

  const spanDays = (lastTs - Number(baseline.time)) / DAY_SEC
  if (spanDays > calendarDays + maxSpanSlipDays) {
    return null
  }

  const base = baseline.close
  if (!Number.isFinite(base) || base === 0 || !Number.isFinite(last.close)) {
    return null
  }
  return ((last.close - base) / base) * 100
}

export function computeFundHorizonReturns(candles: CandlePoint[]): TickerHorizonReturns {
  return {
    weekly: trailingCalendarReturnPercent(candles, 7),
    monthly: trailingCalendarReturnPercent(candles, 30),
    threeMonth: trailingCalendarReturnPercent(candles, 90),
    sixMonth: trailingCalendarReturnPercent(candles, 180),
    yearly: trailingCalendarReturnPercent(candles, 365),
  }
}

/** Intraday / hourly buckets — last N bars, not calendar days. */
export function trailingBarWindowReturnPercent(candles: CandlePoint[], barCount: number): number | null {
  if (candles.length < 2 || barCount < 2) {
    return null
  }
  const slice = candles.length <= barCount ? candles : candles.slice(candles.length - barCount)
  const first = slice[0].close
  const last = slice[slice.length - 1].close
  if (!Number.isFinite(first) || first === 0 || !Number.isFinite(last)) {
    return null
  }
  return ((last - first) / first) * 100
}

export function computeIntradayHorizonReturns(
  candles: CandlePoint[],
  overview: {
    change1M?: number | null
    change3M?: number | null
    change6M?: number | null
    change1Y?: number | null
  } | null,
): TickerHorizonReturns {
  const pct = (bars: number) => trailingBarWindowReturnPercent(candles, bars)
  const pick = (apiVal: number | null | undefined, bars: number) => {
    if (apiVal != null && Number.isFinite(apiVal)) return apiVal
    return pct(bars)
  }
  return {
    weekly: pct(7 * 24),
    monthly: pick(overview?.change1M, 30 * 24),
    threeMonth: pick(overview?.change3M, 90 * 24),
    sixMonth: pick(overview?.change6M, 180 * 24),
    yearly: pick(overview?.change1Y, 365 * 24),
  }
}

export function medianBarSpacingSec(candles: CandlePoint[]): number {
  if (candles.length < 2) return DAY_SEC
  const gaps: number[] = []
  for (let i = 1; i < candles.length; i += 1) {
    gaps.push(Number(candles[i].time) - Number(candles[i - 1].time))
  }
  gaps.sort((a, b) => a - b)
  return gaps[Math.floor(gaps.length / 2)] ?? DAY_SEC
}

/** Pick fund vs intraday logic from bar spacing (daily NAV ≈ 24h+ gaps). */
export function computeHorizonReturns(
  candles: CandlePoint[],
  overview: {
    change1M?: number | null
    change3M?: number | null
    change6M?: number | null
    change1Y?: number | null
  } | null,
  preferFundCalendar = false,
): TickerHorizonReturns {
  const dailyLike = preferFundCalendar || medianBarSpacingSec(candles) >= 20 * 3600
  if (dailyLike) {
    return computeFundHorizonReturns(candles)
  }
  return computeIntradayHorizonReturns(candles, overview)
}
