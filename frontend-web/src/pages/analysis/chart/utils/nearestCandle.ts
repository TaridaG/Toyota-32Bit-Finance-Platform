import type { CandlePoint } from '../../types'

/** Nearest candle by absolute time distance (series is sorted by time ascending). */
export function nearestCandleByTime(candles: CandlePoint[], timeSec: number): CandlePoint | null {
  if (candles.length === 0) return null
  let lo = 0
  let hi = candles.length - 1
  while (lo < hi) {
    const mid = Math.floor((lo + hi) / 2)
    const mt = Number(candles[mid].time)
    if (mt < timeSec) lo = mid + 1
    else hi = mid
  }
  const i = lo
  const prev = i > 0 ? candles[i - 1] : null
  const curr = candles[i]
  if (!prev) return curr
  const dPrev = Math.abs(Number(prev.time) - timeSec)
  const dCurr = Math.abs(Number(curr.time) - timeSec)
  return dPrev <= dCurr ? prev : curr
}
