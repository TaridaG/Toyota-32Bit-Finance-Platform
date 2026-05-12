import type { LineData, Time, UTCTimestamp } from 'lightweight-charts'
import { timeToMs, timeToUnixSec } from '../../../shared/chart/timeUtils'

export type ValueChartRange = '1w' | '1m' | '3m' | '6m' | '1y' | '5y'

export { timeToMs, timeToUnixSec }

export const VALUE_CHART_RANGES: ValueChartRange[] = ['1w', '1m', '3m', '6m', '1y', '5y']

export function valueChartRangeLabel(r: ValueChartRange): string {
  switch (r) {
    case '1w':
      return '1W'
    case '1m':
      return '1M'
    case '3m':
      return '3M'
    case '6m':
      return '6M'
    case '1y':
      return '1Y'
    case '5y':
      return '5Y'
    default:
      return r
  }
}

export const RANGE_TO_MS: Record<ValueChartRange, number> = {
  '1w': 7 * 86_400_000,
  '1m': 30 * 86_400_000,
  '3m': 90 * 86_400_000,
  '6m': 180 * 86_400_000,
  '1y': 365 * 86_400_000,
  '5y': 5 * 365 * 86_400_000,
}

/** Kısayol butonları → imperative `setWindow` (saniye). */
export const RANGE_TO_SEC: Record<ValueChartRange, number> = {
  '1w': Math.floor(RANGE_TO_MS['1w'] / 1000),
  '1m': Math.floor(RANGE_TO_MS['1m'] / 1000),
  '3m': Math.floor(RANGE_TO_MS['3m'] / 1000),
  '6m': Math.floor(RANGE_TO_MS['6m'] / 1000),
  '1y': Math.floor(RANGE_TO_MS['1y'] / 1000),
  '5y': Math.floor(RANGE_TO_MS['5y'] / 1000),
}

const DAY_SEC = 86_400

/**
 * Zoom/pan + günlük omurga: en fazla bu kadar geri (dashboard time-series contract).
 * Sparse event noktaları yerine her zaman ~5y günlük grid üretilir.
 */
export const MAX_CHART_HISTORY_SEC = 5 * 365 * DAY_SEC

/** Boş / düz seri: viewport’tan bağımsız, sadece çizilebilir zaman aralığı (gün adımı). */
export const EMPTY_SERIES_DISPLAY_SPAN_SEC = 7 * DAY_SEC

/**
 * UTC gün bucket’ları + son `nowSec` noktası.
 * Lightweight Charts logical scale’in sparse data ile sıkışmasını önler.
 */
export function buildDailyBackboneUnixTimes(nowSec: number, maxHistorySec: number): number[] {
  const maxH = Math.max(DAY_SEC, Math.floor(maxHistorySec))
  const lo = Math.max(0, nowSec - maxH)
  const startDay = Math.floor(lo / DAY_SEC) * DAY_SEC
  const out: number[] = []
  for (let t = startDay; t < nowSec; t += DAY_SEC) {
    out.push(t)
  }
  if (out.length === 0 || out[out.length - 1] !== nowSec) {
    out.push(nowSec)
  }
  return out
}

/**
 * Portföy değeri: MAX_HISTORY günlük omurga + snapshot’ları forward-fill.
 * Tek/seyrek noktada bile eksen 1Y/5Y gibi aralıklarda doğru zaman bandına geçer.
 */
export function normalizePortfolioValueLineData(
  rawPoints: { t: number; v: number }[],
  nowSec: number,
  tailOverride: number | null,
  maxHistorySec: number = MAX_CHART_HISTORY_SEC,
): LineData<Time>[] {
  const now = Math.floor(nowSec)
  const times = buildDailyBackboneUnixTimes(now, maxHistorySec)

  const cleaned: { t: number; v: number }[] = []
  for (const p of rawPoints) {
    if (!Number.isFinite(p.t) || !Number.isFinite(p.v)) continue
    if (p.t > now) continue
    cleaned.push({ t: Math.floor(p.t), v: p.v })
  }
  cleaned.sort((a, b) => a.t - b.t)
  const deduped: { t: number; v: number }[] = []
  for (const p of cleaned) {
    const prev = deduped[deduped.length - 1]
    if (prev && prev.t === p.t) prev.v = p.v
    else deduped.push({ t: p.t, v: p.v })
  }

  if (deduped.length === 0) {
    if (tailOverride == null || !Number.isFinite(tailOverride)) return []
    return times.map((t) => ({ time: t as UTCTimestamp, value: tailOverride }))
  }

  let j = -1
  const out: LineData<Time>[] = []
  for (const t of times) {
    while (j + 1 < deduped.length && deduped[j + 1].t <= t) {
      j++
    }
    const carry = j < 0 ? deduped[0].v : deduped[j].v
    let v = carry
    if (t === now && tailOverride != null && Number.isFinite(tailOverride)) {
      v = tailOverride
    }
    out.push({ time: t as UTCTimestamp, value: v })
  }
  return out
}

/**
 * Alım satım: günlük omurga + kümülatif net akış forward-fill (işlemsiz = 0 flatline).
 */
export function normalizeTradeFlowLineData(
  rawPoints: { t: number; v: number }[],
  nowSec: number,
  maxHistorySec: number = MAX_CHART_HISTORY_SEC,
): LineData<Time>[] {
  const now = Math.floor(nowSec)
  const times = buildDailyBackboneUnixTimes(now, maxHistorySec)

  const cleaned: { t: number; v: number }[] = []
  for (const p of rawPoints) {
    if (!Number.isFinite(p.t) || !Number.isFinite(p.v)) continue
    if (p.t > now) continue
    cleaned.push({ t: Math.floor(p.t), v: p.v })
  }
  cleaned.sort((a, b) => a.t - b.t)
  const deduped: { t: number; v: number }[] = []
  for (const p of cleaned) {
    const prev = deduped[deduped.length - 1]
    if (prev && prev.t === p.t) prev.v = p.v
    else deduped.push({ t: p.t, v: p.v })
  }

  let j = -1
  const out: LineData<Time>[] = []
  for (const t of times) {
    while (j + 1 < deduped.length && deduped[j + 1].t <= t) {
      j++
    }
    const carry = j < 0 ? 0 : deduped[j].v
    out.push({ time: t as UTCTimestamp, value: carry })
  }
  return out
}

/**
 * Crosshair / yardımcı etiketler (görünür aralığa göre).
 * `visibleSpanSec` = görünür pencere genişliği (saniye).
 */
export function formatAxisLabelForVisibleSpan(visibleSpanSec: number, locale: string, t: Time): string {
  const ms = timeToMs(t)
  if (!Number.isFinite(ms)) return ''
  const d = new Date(ms)
  const span = Math.max(60, visibleSpanSec)
  if (span <= 10 * DAY_SEC) {
    return new Intl.DateTimeFormat(locale, { weekday: 'short', day: 'numeric', month: 'short' }).format(d)
  }
  if (span <= 45 * DAY_SEC) {
    return new Intl.DateTimeFormat(locale, { day: 'numeric', month: 'short' }).format(d)
  }
  if (span <= 200 * DAY_SEC) {
    return new Intl.DateTimeFormat(locale, { day: 'numeric', month: 'short', year: '2-digit' }).format(d)
  }
  if (span <= 380 * DAY_SEC) {
    return new Intl.DateTimeFormat(locale, { month: 'short', year: 'numeric' }).format(d)
  }
  return new Intl.DateTimeFormat(locale, { year: 'numeric' }).format(d)
}

export function formatAxisLabel(t: Time, range: ValueChartRange, locale: string): string {
  return formatAxisLabelForVisibleSpan(RANGE_TO_MS[range] / 1000, locale, t)
}

/** Zoom preset (1W→1M) değişince aynı noktalar yeni diziyle gelmesin diye. */
export function lineSeriesDataShallowEqual(a: LineData<Time>[], b: LineData<Time>[]): boolean {
  if (a === b) return true
  if (a.length !== b.length) return false
  for (let i = 0; i < a.length; i++) {
    if (a[i].time !== b[i].time || a[i].value !== b[i].value) return false
  }
  return true
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

/** İki örnek arası değer sabitken yatay segment (area serisinin yanlış eğim çizmemesi için). */
export function insertStaircaseHolds(points: LineData<Time>[]): LineData<Time>[] {
  if (points.length < 2) return points
  const out: LineData<Time>[] = []
  for (let i = 0; i < points.length; i++) {
    out.push(points[i])
    if (i >= points.length - 1) break
    const t0 = points[i].time as number
    const t1 = points[i + 1].time as number
    const v0 = points[i].value
    const v1 = points[i + 1].value
    if (t1 > t0 + 1 && v0 !== v1) {
      out.push({ time: (t1 - 1) as UTCTimestamp, value: v0 })
    }
  }
  return sortDedupeByTime(out)
}
