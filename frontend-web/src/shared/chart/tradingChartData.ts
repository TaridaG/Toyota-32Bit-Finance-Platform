import type { LineData, Time, UTCTimestamp } from 'lightweight-charts'

export type TradingAreaPoint = { time: number; value: number }

export function tradingPointsShallowEqual(a: TradingAreaPoint[], b: TradingAreaPoint[]): boolean {
  if (a === b) return true
  if (a.length !== b.length) return false
  for (let i = 0; i < a.length; i++) {
    if (a[i].time !== b[i].time || a[i].value !== b[i].value) return false
  }
  return true
}

export function lineDataShallowEqual(a: LineData<Time>[], b: LineData<Time>[]): boolean {
  if (a === b) return true
  if (a.length !== b.length) return false
  for (let i = 0; i < a.length; i++) {
    if (a[i].time !== b[i].time || a[i].value !== b[i].value) return false
  }
  return true
}

/** Lightweight Charts serisi için zaman/değer çiftleri (saniye UTC). */
export function toLineDataSeries(points: TradingAreaPoint[]): LineData<Time>[] {
  const out: LineData<Time>[] = []
  for (const p of points) {
    if (!Number.isFinite(p.time) || !Number.isFinite(p.value)) continue
    out.push({ time: Math.floor(p.time) as UTCTimestamp, value: p.value })
  }
  return out
}
