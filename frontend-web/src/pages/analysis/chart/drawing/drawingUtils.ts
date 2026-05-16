import type { DrawingItem } from '../../types'
import type { ProjectedDrawing, ProjectedLine } from './drawingProjection'

export const FIB_LEVELS = [
  { level: 0, label: '0%' },
  { level: 0.236, label: '23.6%' },
  { level: 0.382, label: '38.2%' },
  { level: 0.5, label: '50%' },
  { level: 0.618, label: '61.8%' },
  { level: 0.786, label: '78.6%' },
  { level: 1, label: '100%' },
] as const

const HIT_THRESHOLD_PX = 8

function distPointToSegment(
  px: number,
  py: number,
  x1: number,
  y1: number,
  x2: number,
  y2: number,
): number {
  const dx = x2 - x1
  const dy = y2 - y1
  const lenSq = dx * dx + dy * dy
  if (lenSq < 0.0001) {
    return Math.hypot(px - x1, py - y1)
  }
  const t = Math.max(0, Math.min(1, ((px - x1) * dx + (py - y1) * dy) / lenSq))
  const projX = x1 + t * dx
  const projY = y1 + t * dy
  return Math.hypot(px - projX, py - projY)
}

function hitLine(line: ProjectedLine, x: number, y: number, threshold: number): boolean {
  return distPointToSegment(x, y, line.x1, line.y1, line.x2, line.y2) <= threshold
}

export function extendRaySegment(x1: number, y1: number, x2: number, y2: number, scale = 4) {
  const dx = x2 - x1
  const dy = y2 - y1
  if (Math.abs(dx) < 0.001 && Math.abs(dy) < 0.001) {
    return { x1, y1, x2, y2 }
  }
  return {
    x1: x1 - dx * scale,
    y1: y1 - dy * scale,
    x2: x2 + dx * scale,
    y2: y2 + dy * scale,
  }
}

export function fibLevelY(y1: number, y2: number, level: number): number {
  return y1 + (y2 - y1) * level
}

export function hitTestProjected(
  projected: ProjectedDrawing,
  item: DrawingItem,
  x: number,
  y: number,
  threshold = HIT_THRESHOLD_PX,
): boolean {
  for (const circle of projected.circles) {
    if (Math.hypot(x - circle.x, y - circle.y) <= threshold + circle.r) {
      return true
    }
  }

  for (const rect of projected.rects) {
    const onBorder =
      (Math.abs(x - rect.x) <= threshold && y >= rect.y && y <= rect.y + rect.height) ||
      (Math.abs(x - (rect.x + rect.width)) <= threshold && y >= rect.y && y <= rect.y + rect.height) ||
      (Math.abs(y - rect.y) <= threshold && x >= rect.x && x <= rect.x + rect.width) ||
      (Math.abs(y - (rect.y + rect.height)) <= threshold && x >= rect.x && x <= rect.x + rect.width)
    if (onBorder) return true
  }

  const linesToTest = item.type === 'ray' && projected.lines.length > 0 ? [projected.lines[0]] : projected.lines

  return linesToTest.some((line) => hitLine(line, x, y, threshold))
}

export function findProjectedDrawingAtPoint(
  projections: { item: DrawingItem; projected: ProjectedDrawing }[],
  x: number,
  y: number,
): DrawingItem | null {
  for (let i = projections.length - 1; i >= 0; i -= 1) {
    const entry = projections[i]
    if (hitTestProjected(entry.projected, entry.item, x, y)) {
      return entry.item
    }
  }
  return null
}

export function isTwoClickDrawTool(tool: string): boolean {
  return tool === 'trendline' || tool === 'ray' || tool === 'rect' || tool === 'fib'
}
