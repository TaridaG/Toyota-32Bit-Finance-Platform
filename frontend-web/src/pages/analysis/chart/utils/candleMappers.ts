import type { CandlestickData, HistogramData, LineData, Time, WhitespaceData } from 'lightweight-charts'
import type { CandlePoint } from '../../types'

const FUND_NAV_MAX_GAP_SEC = 10 * 86400

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

/** Breaks the line at long NAV gaps so Feb→May does not draw a misleading vertical spike. */
export function toLineDataWithGapBreaks(
  candles: CandlePoint[],
  maxGapSec = FUND_NAV_MAX_GAP_SEC,
): (LineData<Time> | WhitespaceData<Time>)[] {
  const out: (LineData<Time> | WhitespaceData<Time>)[] = []
  for (let i = 0; i < candles.length; i += 1) {
    if (i > 0) {
      const gapSec = Number(candles[i].time) - Number(candles[i - 1].time)
      if (gapSec > maxGapSec) {
        const breakTime = (Number(candles[i].time) - 1) as Time
        out.push({ time: breakTime })
      }
    }
    out.push(toLineData(candles[i]))
  }
  return out
}

export function toVolumeHistogramData(candle: CandlePoint, prevClose?: number): HistogramData<Time> {
  const ref = prevClose ?? candle.open
  return {
    time: candle.time,
    value: candle.volume,
    color: candle.close >= ref ? 'rgba(34,197,94,0.45)' : 'rgba(239,68,68,0.45)',
  }
}
