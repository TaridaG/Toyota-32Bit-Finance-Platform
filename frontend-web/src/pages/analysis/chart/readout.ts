import type { CandlePoint } from '../types'

export type ChartReadout = {
  source: 'hover' | 'pin'
  timeLabel: string
  open: number
  high: number
  low: number
  close: number
}

export function candleToReadout(bar: CandlePoint, source: 'hover' | 'pin'): ChartReadout {
  return {
    source,
    timeLabel: new Date(Number(bar.time) * 1000).toLocaleString(),
    open: bar.open,
    high: bar.high,
    low: bar.low,
    close: bar.close,
  }
}
