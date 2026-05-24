import { describe, expect, it } from 'vitest'
import { computeMeasureStats } from './computeMeasureStats'
import type { CandlePoint } from '../../types'

const candles: CandlePoint[] = [
  { time: 1000 as CandlePoint['time'], open: 10, high: 11, low: 9, close: 10, volume: 100 },
  { time: 2000 as CandlePoint['time'], open: 11, high: 12, low: 10, close: 11, volume: 120 },
  { time: 3000 as CandlePoint['time'], open: 12, high: 13, low: 11, close: 12, volume: 140 },
]

describe('computeMeasureStats', () => {
  it('computes delta, percent, bars and duration', () => {
    const stats = computeMeasureStats(
      { time: 1000 as CandlePoint['time'], price: 10 },
      { time: 3000 as CandlePoint['time'], price: 12 },
      candles,
    )
    expect(stats.priceDelta).toBe(2)
    expect(stats.percentChange).toBe(20)
    expect(stats.barCount).toBe(2)
    expect(stats.durationSec).toBe(2000)
    expect(stats.isUp).toBe(true)
  })
})
