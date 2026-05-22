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

/** Günlük son değer + canlı toplam; 5 yıllık sıfır doldurma yok. */
export function buildPortfolioTrendSeries(
  snapshots: PortfolioValueSnapshot[],
  liveTotalValue: number | null,
): PortfolioTrendPoint[] {
  const byDay = new Map<string, number>()
  for (const s of snapshots) {
    const v = parseSnapshotValue(s.totalValue)
    if (v == null) continue
    const day = s.createdAt.slice(0, 10)
    if (!day) continue
    byDay.set(day, v)
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

/** İşlem bazlı kümülatif net nakit akışı (tüm geçmiş). */
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

export function seriesToSparklinePath(
  values: number[],
  width: number,
  height: number,
  padX = 2,
  padY = 4,
): { line: string; area: string } {
  if (values.length === 0) {
    return { line: '', area: '' }
  }
  if (values.length === 1) {
    const y = height / 2
    const x0 = padX
    const x1 = width - padX
    const line = `M${x0},${y} L${x1},${y}`
    return { line, area: `${line} L${x1},${height - padY} L${x0},${height - padY} Z` }
  }

  const min = Math.min(...values)
  const max = Math.max(...values)
  const range = Math.max(max - min, Math.max(Math.abs(max), Math.abs(min)) * 0.02, 1e-6)

  const coords = values.map((v, i) => {
    const x = padX + (i / (values.length - 1)) * (width - 2 * padX)
    const yn = (v - min) / range
    const y = height - padY - yn * (height - 2 * padY)
    return { x, y }
  })

  const line = coords.map((c, i) => `${i === 0 ? 'M' : 'L'}${c.x.toFixed(2)},${c.y.toFixed(2)}`).join(' ')
  const baseY = height - padY
  const area = `${line} L${coords[coords.length - 1].x.toFixed(2)},${baseY} L${coords[0].x.toFixed(2)},${baseY} Z`
  return { line, area }
}
