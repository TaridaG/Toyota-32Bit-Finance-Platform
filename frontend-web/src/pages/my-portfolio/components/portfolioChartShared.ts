import type { IChartApi, LineData, Time, UTCTimestamp } from 'lightweight-charts'

export type ValueChartRange = '1w' | '1m' | '3m' | '6m' | '1y'

export const RANGE_TO_MS: Record<ValueChartRange, number> = {
  '1w': 7 * 86_400_000,
  '1m': 30 * 86_400_000,
  '3m': 90 * 86_400_000,
  '6m': 180 * 86_400_000,
  '1y': 365 * 86_400_000,
}

export function timeToMs(t: Time): number {
  if (typeof t === 'number') return t * 1000
  if (typeof t === 'string') {
    const ms = Date.parse(t)
    return Number.isFinite(ms) ? ms : NaN
  }
  const o = t as { year: number; month: number; day: number }
  if (o && typeof o.year === 'number' && typeof o.month === 'number' && typeof o.day === 'number') {
    return Date.UTC(o.year, o.month - 1, o.day)
  }
  return NaN
}

export function formatAxisLabel(t: Time, range: ValueChartRange, locale: string): string {
  const ms = timeToMs(t)
  if (!Number.isFinite(ms)) return ''
  const d = new Date(ms)
  if (range === '1w') {
    return new Intl.DateTimeFormat(locale, { weekday: 'short', day: 'numeric', month: 'short' }).format(d)
  }
  if (range === '1m') {
    return new Intl.DateTimeFormat(locale, { day: 'numeric', month: 'short' }).format(d)
  }
  if (range === '3m' || range === '6m') {
    return new Intl.DateTimeFormat(locale, { day: 'numeric', month: 'short', year: '2-digit' }).format(d)
  }
  return new Intl.DateTimeFormat(locale, { month: 'short', year: 'numeric' }).format(d)
}

export function sortDedupeByTime(points: LineData<Time>[]): LineData<Time>[] {
  const m = new Map<number, number>()
  for (const p of points) {
    const t = p.time as number
    m.set(t, p.value)
  }
  return [...m.entries()]
    .sort((a, b) => a[0] - b[0])
    .map(([t, v]) => ({ time: t as UTCTimestamp, value: v }))
}

export function rangeStepSeconds(range: ValueChartRange): number {
  switch (range) {
    case '1w':
      return 86_400
    case '1m':
      return 86_400
    case '3m':
      return 2 * 86_400
    case '6m':
      return 4 * 86_400
    case '1y':
      return 7 * 86_400
    default:
      return 86_400
  }
}

export function applyVisibleWindow(chart: IChartApi, range: ValueChartRange) {
  const run = () => {
    const to = Math.floor(Date.now() / 1000) as UTCTimestamp
    const span = Math.floor(RANGE_TO_MS[range] / 1000)
    const from = Math.max(0, (to as number) - span) as UTCTimestamp
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
  requestAnimationFrame(() => {
    requestAnimationFrame(run)
  })
}
