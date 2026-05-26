import type { IChartApi, UTCTimestamp } from 'lightweight-charts'

export type TradingRightBoundary = 'now' | 'lastData'

export function nowUnixSec(): number {
  return Math.floor(Date.now() / 1000)
}

/** Görünür pencere: [endSec − span, endSec]; shortcut / ilk açılış. */
export function setVisibleWindowEndingAt(chart: IChartApi, endSec: number, spanSec: number): void {
  const span = Math.max(60, Math.floor(spanSec))
  const end = Math.max(0, Math.floor(endSec))
  const from = Math.max(0, end - span) as UTCTimestamp
  const to = end as UTCTimestamp
  try {
    chart.timeScale().setVisibleRange({ from, to })
  } catch {
    try {
      chart.timeScale().fitContent()
    } catch {
      /* */
    }
  }
}

/** Görünür pencere: [now − span, now]; legacy helper. */
export function setVisibleWindowAlignedToNow(chart: IChartApi, spanSec: number): void {
  setVisibleWindowEndingAt(chart, nowUnixSec(), spanSec)
}

export function computeTimeScaleBounds(
  nowSec: number,
  lastDataSec: number,
  maxHistorySec: number,
  rightBoundary: TradingRightBoundary,
): { minUnix: number; maxUnix: number } {
  const maxTo = rightBoundary === 'lastData' ? Math.min(nowSec, lastDataSec) : nowSec
  const endForLeft = Math.min(nowSec, lastDataSec)
  const minFrom = Math.max(0, endForLeft - Math.max(60, maxHistorySec))
  return { minUnix: minFrom, maxUnix: maxTo }
}
