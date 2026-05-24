import type { CandlePoint, ChartAnchor } from '../../types'

export type MeasureStats = {
  priceDelta: number
  percentChange: number | null
  barCount: number
  durationSec: number
  isUp: boolean
}

function candleIndexByTime(candles: CandlePoint[], time: number): number {
  if (candles.length === 0) return 0
  let best = 0
  let bestDist = Math.abs(Number(candles[0].time) - time)
  for (let i = 1; i < candles.length; i += 1) {
    const dist = Math.abs(Number(candles[i].time) - time)
    if (dist < bestDist) {
      bestDist = dist
      best = i
    }
  }
  return best
}

export function computeMeasureStats(
  a: ChartAnchor,
  b: ChartAnchor,
  candles: CandlePoint[],
): MeasureStats {
  const priceDelta = b.price - a.price
  const base = a.price
  const percentChange =
    base !== 0 && Number.isFinite(base) ? (priceDelta / base) * 100 : null
  const tMin = Math.min(Number(a.time), Number(b.time))
  const tMax = Math.max(Number(a.time), Number(b.time))
  const durationSec = Math.max(0, tMax - tMin)

  let barCount = 0
  if (candles.length > 0) {
    const idxA = candleIndexByTime(candles, Number(a.time))
    const idxB = candleIndexByTime(candles, Number(b.time))
    barCount = Math.abs(idxB - idxA)
  }

  return {
    priceDelta,
    percentChange,
    barCount,
    durationSec,
    isUp: priceDelta >= 0,
  }
}

export function formatMeasureDuration(seconds: number, locale: string): string {
  if (seconds < 60) {
    return new Intl.NumberFormat(locale, { maximumFractionDigits: 0 }).format(seconds) + 's'
  }
  if (seconds < 3600) {
    const m = Math.round(seconds / 60)
    return new Intl.NumberFormat(locale, { maximumFractionDigits: 0 }).format(m) + 'm'
  }
  if (seconds < 86400) {
    const h = Math.round(seconds / 3600)
    return new Intl.NumberFormat(locale, { maximumFractionDigits: 0 }).format(h) + 'h'
  }
  const d = Math.round(seconds / 86400)
  return new Intl.NumberFormat(locale, { maximumFractionDigits: 0 }).format(d) + 'd'
}
