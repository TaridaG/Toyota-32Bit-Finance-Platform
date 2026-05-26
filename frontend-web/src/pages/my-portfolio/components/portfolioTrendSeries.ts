import type { PortfolioTradeFlowPoint, PortfolioValueSnapshot } from '../../../shared/types/portfolio'
import type { ValueChartRange } from './portfolioChartShared'
import { RANGE_TO_MS } from './portfolioChartShared'

export type PortfolioTrendPoint = { t: number; v: number }

export type PortfolioTrendSummary = {
  startValue: number
  endValue: number
  changePct: number | null
  startMs: number
  endMs: number
  pointCount: number
}

function parseSnapshotValue(raw: unknown): number | null {
  if (raw == null) return null
  if (typeof raw === 'number') return Number.isFinite(raw) ? raw : null
  if (typeof raw === 'string') {
    const n = Number(raw.trim().replace(/\s/g, '').replace(',', '.'))
    return Number.isFinite(n) ? n : null
  }
  return null
}

function utcDayKey(sec: number): string {
  return new Date(sec * 1000).toISOString().slice(0, 10)
}

function secFromUtcDay(day: string): number {
  return Math.floor(Date.parse(`${day}T12:00:00Z`) / 1000)
}

function addUtcDays(day: string, delta: number): string {
  const d = new Date(`${day}T12:00:00Z`)
  d.setUTCDate(d.getUTCDate() + delta)
  return d.toISOString().slice(0, 10)
}

export type DailyFillMode = 'carry' | 'zero'

/** İlk günden bugüne her takvim günü (portföy: taşı, işlem: 0). */
export function fillDailyCalendarSeries(
  points: PortfolioTrendPoint[],
  opts?: {
    liveValue?: number | null
    maxDays?: number
    fillMode?: DailyFillMode
  },
): PortfolioTrendPoint[] {
  const maxDays = opts?.maxDays ?? 60
  const fillMode = opts?.fillMode ?? 'carry'
  const byDay = new Map<string, number>()
  for (const p of points) {
    const day = utcDayKey(p.t)
    byDay.set(day, p.v)
  }

  const today = new Date().toISOString().slice(0, 10)
  if (opts?.liveValue != null && Number.isFinite(opts.liveValue)) {
    byDay.set(today, opts.liveValue)
  }

  const knownDays = [...byDay.keys()].sort()
  if (knownDays.length === 0) return []

  const endDay = today >= knownDays[knownDays.length - 1]! ? today : knownDays[knownDays.length - 1]!
  const windowStart = addUtcDays(endDay, -(maxDays - 1))
  const firstDay = knownDays[0]!
  const startDay = windowStart > firstDay ? windowStart : firstDay

  const out: PortfolioTrendPoint[] = []
  let lastV = byDay.get(startDay) ?? (fillMode === 'carry' ? byDay.get(firstDay) ?? 0 : 0)

  for (let day = startDay; day <= endDay; day = addUtcDays(day, 1)) {
    if (byDay.has(day)) {
      lastV = byDay.get(day)!
    } else if (fillMode === 'zero') {
      lastV = 0
    }
    out.push({ t: secFromUtcDay(day), v: lastV })
  }

  return out
}

/** Günlük son değer + canlı toplam; dünkü kapanış overview’dan eklenebilir. */
export function buildPortfolioTrendSeries(
  snapshots: PortfolioValueSnapshot[],
  liveTotalValue: number | null,
  priorDayValue?: number | null,
): PortfolioTrendPoint[] {
  const byDay = new Map<string, number>()
  for (const s of snapshots) {
    const v = parseSnapshotValue(s.totalValue)
    if (v == null) continue
    const day = s.createdAt.slice(0, 10)
    if (!day) continue
    byDay.set(day, v)
  }

  const today = new Date().toISOString().slice(0, 10)
  const yesterday = addUtcDays(today, -1)
  if (
    priorDayValue != null &&
    Number.isFinite(priorDayValue) &&
    Math.abs(priorDayValue) >= 1e-6 &&
    !byDay.has(yesterday)
  ) {
    byDay.set(yesterday, priorDayValue)
  }

  const points: PortfolioTrendPoint[] = [...byDay.entries()]
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([day, v]) => ({
      t: Math.floor(Date.parse(`${day}T12:00:00`) / 1000),
      v,
    }))

  const nowSec = Math.floor(Date.now() / 1000)
  if (liveTotalValue != null && Number.isFinite(liveTotalValue)) {
    const last = points[points.length - 1]
    if (!last) {
      points.push({ t: nowSec, v: liveTotalValue })
    } else if (nowSec - last.t > 3600) {
      points.push({ t: nowSec, v: liveTotalValue })
    } else {
      last.v = liveTotalValue
      last.t = nowSec
    }
  }

  let start = 0
  for (let i = 0; i < points.length - 1; i++) {
    if (Math.abs(points[i].v) < 1e-6 && Math.abs(points[i + 1].v) >= 1e-6) {
      start = i + 1
    } else if (Math.abs(points[i].v) >= 1e-6) {
      break
    }
  }

  return points.slice(start)
}

/** Filled günlük portföy değerini bir önceki güne göre yüzde değişime çevirir. */
export function buildDayOverDayPctSeries(points: PortfolioTrendPoint[]): PortfolioTrendPoint[] {
  if (points.length < 2) return []

  const out: PortfolioTrendPoint[] = []
  for (let i = 1; i < points.length; i++) {
    const prev = points[i - 1]!
    const cur = points[i]!

    let pct = 0
    if (Math.abs(prev.v) >= 1e-6) {
      pct = ((cur.v - prev.v) / prev.v) * 100
    } else if (Math.abs(cur.v) >= 1e-6) {
      continue
    }

    if (Number.isFinite(pct)) {
      out.push({ t: cur.t, v: pct })
    }
  }
  return out
}

/** Seçilen aralığa göre filtre; az kayıtta bile pencere boyunca çizgi üretir. */
export function filterTrendByRange(
  series: PortfolioTrendPoint[],
  range: ValueChartRange,
  nowMs: number = Date.now(),
): PortfolioTrendPoint[] {
  if (series.length === 0) {
    return []
  }
  const rangeMs = RANGE_TO_MS[range]
  const fromSec = Math.floor((nowMs - rangeMs) / 1000)
  const nowSec = Math.floor(nowMs / 1000)

  const inRange = series.filter((p) => p.t >= fromSec && p.t <= nowSec)
  if (inRange.length >= 2) {
    return inRange
  }

  const history = series.filter((p) => p.t <= nowSec)
  if (history.length === 0) {
    return []
  }

  const last = history[history.length - 1]
  const carry =
    [...history].reverse().find((p) => p.t <= fromSec) ??
    history[0]

  return [
    { t: fromSec, v: carry.v },
    { t: nowSec, v: last.v },
  ]
}

/** İşlem bazlı kümülatif net nakit akışı (detay grafikleri). */
export function buildTradeFlowTrendSeries(points: PortfolioTradeFlowPoint[]): PortfolioTrendPoint[] {
  const sorted = [...points]
    .filter((p) => Number.isFinite(Date.parse(p.createdAt)))
    .sort((a, b) => {
      const ta = Date.parse(a.createdAt)
      const tb = Date.parse(b.createdAt)
      if (ta !== tb) return ta - tb
      return a.transactionId - b.transactionId
    })

  let cum = 0
  const out: PortfolioTrendPoint[] = []
  for (const p of sorted) {
    cum += p.signedAmount
    const sec = Math.floor(Date.parse(p.createdAt) / 1000)
    const prev = out[out.length - 1]
    if (prev?.t === sec) {
      prev.v = cum
    } else {
      out.push({ t: sec, v: cum })
    }
  }
  return out
}

/** Dashboard: günlük net alım/satım (o günkü işlemlerin toplamı). */
export function buildTradeFlowDailySeries(points: PortfolioTradeFlowPoint[]): PortfolioTrendPoint[] {
  const byDay = new Map<string, number>()
  for (const p of points) {
    if (!Number.isFinite(Date.parse(p.createdAt))) continue
    const day = p.createdAt.slice(0, 10)
    if (!day) continue
    byDay.set(day, (byDay.get(day) ?? 0) + p.signedAmount)
  }
  return [...byDay.entries()]
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([day, v]) => ({ t: secFromUtcDay(day), v }))
}

export function summarizePortfolioTrend(series: PortfolioTrendPoint[]): PortfolioTrendSummary | null {
  if (series.length === 0) return null
  const start = series[0]
  const end = series[series.length - 1]
  const startValue = start.v
  const endValue = end.v
  let changePct: number | null = null
  if (Math.abs(startValue) >= 1e-6) {
    changePct = ((endValue - startValue) / startValue) * 100
  } else if (Math.abs(endValue) >= 1e-6) {
    changePct = null
  } else {
    changePct = 0
  }
  return {
    startValue,
    endValue,
    changePct,
    startMs: start.t * 1000,
    endMs: end.t * 1000,
    pointCount: series.length,
  }
}

export type SparklineScaleMode = 'zero' | 'data'

/** Portföy: veri aralığı; işlem: 0 tabanı. */
function sparklineYDomain(values: number[], scaleMode: SparklineScaleMode): { min: number; max: number } {
  const dataMin = Math.min(...values)
  const dataMax = Math.max(...values)
  if (scaleMode === 'data') {
    const span = dataMax - dataMin
    if (span < Math.max(Math.abs(dataMax) * 0.001, 1)) {
      const mid = dataMax
      const pad = Math.max(Math.abs(mid) * 0.015, 50)
      return { min: mid - pad, max: mid + pad }
    }
    const pad = span * 0.1
    return { min: dataMin - pad, max: dataMax + pad }
  }
  const min = Math.min(0, dataMin)
  let max = Math.max(0, dataMax)
  if (max > min) max += (max - min) * 0.1
  else if (max > 0) max *= 1.1
  return { min, max }
}

function sparklineYRange(min: number, max: number): number {
  return Math.max(max - min, Math.max(Math.abs(max), Math.abs(min)) * 0.02, 1e-6)
}

function sparklineYCoord(
  value: number,
  min: number,
  range: number,
  height: number,
  padY: number,
): number {
  const yn = (value - min) / range
  return height - padY - yn * (height - 2 * padY)
}

export function seriesToSparklinePath(
  values: number[],
  width: number,
  height: number,
  padX = 2,
  padY = 4,
  scaleMode: SparklineScaleMode = 'zero',
): { line: string; area: string } {
  if (values.length === 0) {
    return { line: '', area: '' }
  }

  const { min, max } = sparklineYDomain(values, scaleMode)
  const range = sparklineYRange(min, max)
  const baseY = height - padY

  if (values.length === 1) {
    const v = values[0]!
    const y = sparklineYCoord(v, min, range, height, padY)
    const x0 = padX
    const x1 = width - padX
    const line = `M${x0},${y.toFixed(2)} L${x1},${y.toFixed(2)}`
    return { line, area: `${line} L${x1},${baseY} L${x0},${baseY} Z` }
  }

  const coords = values.map((v, i) => {
    const x = padX + (i / (values.length - 1)) * (width - 2 * padX)
    const y = sparklineYCoord(v, min, range, height, padY)
    return { x, y }
  })

  const line = coords.map((c, i) => `${i === 0 ? 'M' : 'L'}${c.x.toFixed(2)},${c.y.toFixed(2)}`).join(' ')
  const area = `${line} L${coords[coords.length - 1].x.toFixed(2)},${baseY} L${coords[0].x.toFixed(2)},${baseY} Z`
  return { line, area }
}
