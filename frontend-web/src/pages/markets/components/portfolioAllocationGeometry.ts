/** Donut slice geometry (angles in radians, 0 = 3 o'clock; start at 12 o'clock). */
export const ALLOCATION_PALETTE = [
  '#6366f1',
  '#22c55e',
  '#f59e0b',
  '#ec4899',
  '#06b6d4',
  '#a855f7',
  '#f97316',
  '#14b8a6',
  '#ef4444',
  '#84cc16',
  '#3b82f6',
  '#eab308',
] as const

export const MIN_SEGMENT_PCT = 3

const TAU = Math.PI * 2
const START = -Math.PI / 2

export function polar(cx: number, cy: number, r: number, angle: number): [number, number] {
  return [cx + r * Math.cos(angle), cy + r * Math.sin(angle)]
}

export function slicePath(
  cx: number,
  cy: number,
  rOut: number,
  rIn: number,
  a0: number,
  a1: number,
): string {
  if (a1 - a0 >= TAU - 1e-6) {
    a1 = a0 + TAU - 1e-5
  }
  const largeArc = a1 - a0 > Math.PI ? 1 : 0
  const [x0o, y0o] = polar(cx, cy, rOut, a0)
  const [x1o, y1o] = polar(cx, cy, rOut, a1)
  const [x1i, y1i] = polar(cx, cy, rIn, a1)
  const [x0i, y0i] = polar(cx, cy, rIn, a0)
  return `M ${x0o} ${y0o} A ${rOut} ${rOut} 0 ${largeArc} 1 ${x1o} ${y1o} L ${x1i} ${y1i} A ${rIn} ${rIn} 0 ${largeArc} 0 ${x0i} ${y0i} Z`
}

export type AllocationSegment = {
  id: string
  symbol: string
  name: string
  weightPct: number
  color: string
  startAngle: number
  endAngle: number
}

export function buildAllocationSegments(
  holdings: { id: string; symbol: string; name: string; weightPct: number }[],
): AllocationSegment[] {
  const total = holdings.reduce((sum, h) => sum + Math.max(0, h.weightPct), 0)
  const scale = total > 0 ? 100 / total : 0
  let cursor = START
  return holdings.map((holding, index) => {
    const share = Math.max(0, holding.weightPct) * scale
    const sweep = (share / 100) * TAU
    const startAngle = cursor
    const endAngle = cursor + sweep
    cursor = endAngle
    return {
      id: holding.id,
      symbol: holding.symbol,
      name: holding.name,
      weightPct: holding.weightPct,
      color: ALLOCATION_PALETTE[index % ALLOCATION_PALETTE.length],
      startAngle,
      endAngle,
    }
  })
}

/** Pointer angle from chart center; 0% cumulative at 12 o'clock, clockwise. */
export function pointerToCumulativePct(clientX: number, clientY: number, rect: DOMRect): number {
  const cx = rect.left + rect.width / 2
  const cy = rect.top + rect.height / 2
  const rad = Math.atan2(clientY - cy, clientX - cx)
  let deg = (rad * 180) / Math.PI + 90
  if (deg < 0) {
    deg += 360
  }
  return (deg / 360) * 100
}

export function cumulativePctToAngle(pct: number): number {
  return START + (pct / 100) * TAU
}

export function roundWeight(value: number): number {
  return Math.round(value * 10) / 10
}
