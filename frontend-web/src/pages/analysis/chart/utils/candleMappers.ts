import type { CandlestickData, HistogramData, LineData, Time } from 'lightweight-charts'
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

export function toLineData(candle: CandlePoint): LineData<Time> {
  return {
    time: candle.time,
    value: candle.close,
  }
}

export function toVolumeHistogramData(candle: CandlePoint, prevClose?: number): HistogramData<Time> {
  const ref = prevClose ?? candle.open
  return {
    time: candle.time,
    value: candle.volume,
    color: candle.close >= ref ? 'rgba(34,197,94,0.45)' : 'rgba(239,68,68,0.45)',
  }
}
