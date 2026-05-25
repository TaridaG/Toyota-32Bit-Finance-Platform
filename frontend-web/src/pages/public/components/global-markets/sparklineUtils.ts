import type { MarketOverviewItem } from '../../../../shared/types/market'

export const SPARKLINE_WIDTH = 52
export const SPARKLINE_HEIGHT = 18
export const SPARKLINE_PADDING = 2

export function toSparklinePoints(row: MarketOverviewItem): number[] {
  const safePrice = Number.isFinite(row.price) && row.price > 0 ? row.price : 1
  const trend = (row.change1D ?? row.change24h ?? 0) / 100
  const wave = [0.18, -0.12, 0.1, -0.08, 0.06, -0.04, 0.03]
  const points = wave.map((w, index) => {
    const t = index / (wave.length - 1)
    const base = safePrice * (1 + trend * (t - 1))
    const wobble = safePrice * w * Math.max(Math.abs(trend), 0.01)
    return Math.max(0.0001, base + wobble)
  })
  points.push(safePrice)
  return points
}

export function toSparklinePath(points: number[]): string {
  if (points.length === 0) {
    return ''
  }
  const min = Math.min(...points)
  const max = Math.max(...points)
  const range = max - min || 1
  return points
    .map((point, index) => {
      const x = SPARKLINE_PADDING + (index / Math.max(points.length - 1, 1)) * (SPARKLINE_WIDTH - SPARKLINE_PADDING * 2)
      const y =
        SPARKLINE_HEIGHT -
        SPARKLINE_PADDING -
        ((point - min) / range) * (SPARKLINE_HEIGHT - SPARKLINE_PADDING * 2)
      return `${index === 0 ? 'M' : 'L'}${x.toFixed(2)} ${y.toFixed(2)}`
    })
    .join(' ')
}

export function trendBadgeClass(score: number | null | undefined): string {
  if (score == null) {
    return 'markets-trend-badge-neutral'
  }
  if (score < 35) {
    return 'markets-trend-badge-weak'
  }
  if (score < 65) {
    return 'markets-trend-badge-neutral'
  }
  return 'markets-trend-badge-strong'
}
