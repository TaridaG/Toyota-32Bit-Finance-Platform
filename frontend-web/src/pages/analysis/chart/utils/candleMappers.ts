import type { CandlestickData, HistogramData, Time } from 'lightweight-charts'
import type { CandlePoint } from '../../types'

export function toCandlestickData(candle: CandlePoint): CandlestickData<Time> {
  return {
    time: candle.time,
    open: candle.open,
    high: candle.high,
    low: candle.low,
    close: candle.close,
  }
}

export function toVolumeHistogramData(candle: CandlePoint): HistogramData<Time> {
  return {
    time: candle.time,
    value: candle.volume,
    color: candle.close >= candle.open ? 'rgba(34,197,94,0.45)' : 'rgba(239,68,68,0.45)',
  }
}
