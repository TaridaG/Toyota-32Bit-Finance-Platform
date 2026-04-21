import type { LineData, Time } from 'lightweight-charts'

export type OhlcLike = {
  time: Time
  close: number
}

/**
 * Simple moving average over `close`, aligned to each bar (uses all available closes up to `period`).
 */
export function calculateMovingAverage<T extends OhlcLike>(data: T[], period: number): LineData<Time>[] {
  if (period < 1 || data.length === 0) return []
  return data.map((bar, index) => {
    const start = Math.max(0, index - period + 1)
    const slice = data.slice(start, index + 1)
    const avg = slice.reduce((sum, p) => sum + p.close, 0) / slice.length
    return { time: bar.time, value: Number(avg.toFixed(6)) }
  })
}
