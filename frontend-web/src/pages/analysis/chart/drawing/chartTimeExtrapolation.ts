import type { IChartApi, Logical, Time, UTCTimestamp } from 'lightweight-charts'
import type { CandlePoint } from '../../types'

/** Median step between recent bars (seconds). */
export function estimateBarIntervalSec(candles: CandlePoint[]): number {
  if (candles.length < 2) return 86_400
  const sampleCount = Math.min(8, candles.length - 1)
  const steps: number[] = []
  for (let i = candles.length - sampleCount; i < candles.length; i += 1) {
    const step = Number(candles[i].time) - Number(candles[i - 1].time)
    if (step > 0) steps.push(step)
  }
  if (steps.length === 0) return 86_400
  steps.sort((a, b) => a - b)
  return steps[Math.floor(steps.length / 2)] ?? 86_400
}

/** Map unix seconds to fractional bar index (0 = first candle). */
export function timeToLogicalIndex(timeSec: number, candles: CandlePoint[]): number {
  const len = candles.length
  if (len === 0) return 0

  const lastIdx = len - 1
  const firstT = Number(candles[0].time)
  const lastT = Number(candles[lastIdx].time)

  if (timeSec >= lastT) {
    const interval = estimateBarIntervalSec(candles)
    return lastIdx + (timeSec - lastT) / interval
  }
  if (timeSec <= firstT) {
    const interval = estimateBarIntervalSec(candles)
    return (timeSec - firstT) / interval
  }

  let lo = 0
  let hi = lastIdx
  while (lo < hi - 1) {
    const mid = Math.floor((lo + hi) / 2)
    if (Number(candles[mid].time) <= timeSec) lo = mid
    else hi = mid
  }

  const tLo = Number(candles[lo].time)
  const tHi = Number(candles[hi].time)
  if (tHi <= tLo) return lo
  const frac = (timeSec - tLo) / (tHi - tLo)
  return lo + frac
}

export function logicalIndexToTime(logical: number, candles: CandlePoint[]): UTCTimestamp {
  const len = candles.length
  if (len === 0) return Math.round(logical) as UTCTimestamp

  const lastIdx = len - 1
  if (logical <= 0) {
    const interval = estimateBarIntervalSec(candles)
    return (Number(candles[0].time) + logical * interval) as UTCTimestamp
  }
  if (logical >= lastIdx) {
    const interval = estimateBarIntervalSec(candles)
    const lastT = Number(candles[lastIdx].time)
    return (lastT + (logical - lastIdx) * interval) as UTCTimestamp
  }

  const i = Math.floor(logical)
  const frac = logical - i
  const t0 = Number(candles[i].time)
  const t1 = Number(candles[i + 1].time)
  return (t0 + frac * (t1 - t0)) as UTCTimestamp
}

export function coordinateToChartTime(
  chart: IChartApi,
  x: number,
  candles: CandlePoint[],
): UTCTimestamp | null {
  const direct = chart.timeScale().coordinateToTime(x)
  if (direct != null) {
    return Number(direct) as UTCTimestamp
  }

  if (candles.length === 0) return null

  const logical = chart.timeScale().coordinateToLogical(x)
  if (logical == null || !Number.isFinite(logical)) return null

  return logicalIndexToTime(logical, candles)
}

export function chartTimeToCoordinate(
  chart: IChartApi,
  time: UTCTimestamp,
  candles: CandlePoint[],
): number | null {
  const direct = chart.timeScale().timeToCoordinate(time as Time)
  if (direct != null) return direct

  if (candles.length === 0) return null

  const logical = timeToLogicalIndex(Number(time), candles) as Logical
  const x = chart.timeScale().logicalToCoordinate(logical)
  return x ?? null
}
