import type { IChartApi, ISeriesApi } from 'lightweight-charts'
import type { CandlePoint, DrawingItem } from '../../types'
import { anchorToPixel, priceToCoordinate, timeToCoordinate, type PixelPoint } from './chartCoordinates'
import { extendRaySegment, fibLevelY, FIB_LEVELS } from './drawingUtils'

export type ProjectedLine = {
  x1: number
  y1: number
  x2: number
  y2: number
}

export type ProjectedDrawing = {
  id: string
  type: DrawingItem['type']
  lines: ProjectedLine[]
  circles: { x: number; y: number; r: number }[]
  rects: { x: number; y: number; width: number; height: number }[]
  labels: { x: number; y: number; text: string }[]
}

function boxFromPoints(a: PixelPoint, b: PixelPoint) {
  return {
    left: Math.min(a.x, b.x),
    right: Math.max(a.x, b.x),
    top: Math.min(a.y, b.y),
    bottom: Math.max(a.y, b.y),
  }
}

function clamp(value: number, min: number, max: number): number {
  return Math.max(min, Math.min(max, value))
}

function clipLine(line: ProjectedLine, width: number, height: number): ProjectedLine {
  return {
    x1: clamp(line.x1, 0, width),
    y1: clamp(line.y1, 0, height),
    x2: clamp(line.x2, 0, width),
    y2: clamp(line.y2, 0, height),
  }
}

export function projectDrawing(
  item: DrawingItem,
  chart: IChartApi,
  series: ISeriesApi<'Candlestick'> | ISeriesApi<'Line'>,
  width: number,
  height: number,
  candles: CandlePoint[] = [],
): ProjectedDrawing | null {
  const base: ProjectedDrawing = {
    id: item.id,
    type: item.type,
    lines: [],
    circles: [],
    rects: [],
    labels: [],
  }

  if (item.type === 'point') {
    const p = anchorToPixel(chart, series, item.anchor, candles)
    if (!p) return null
    base.circles.push({ x: p.x, y: p.y, r: 4 })
    return base
  }

  if (item.type === 'hline') {
    const y = priceToCoordinate(series, item.price)
    if (y == null) return null
    base.lines.push(clipLine({ x1: 0, y1: y, x2: width, y2: y }, width, height))
    return base
  }

  if (item.type === 'vline') {
    const x = timeToCoordinate(chart, item.time, candles)
    if (x == null) return null
    base.lines.push({ x1: x, y1: 0, x2: x, y2: height })
    return base
  }

  if (item.type === 'trendline') {
    const a = anchorToPixel(chart, series, item.a, candles)
    const b = anchorToPixel(chart, series, item.b, candles)
    if (!a || !b) return null
    base.lines.push(clipLine({ x1: a.x, y1: a.y, x2: b.x, y2: b.y }, width, height))
    return base
  }

  if (item.type === 'ray') {
    const a = anchorToPixel(chart, series, item.a, candles)
    const b = anchorToPixel(chart, series, item.b, candles)
    if (!a || !b) return null
    base.lines.push(clipLine({ x1: a.x, y1: a.y, x2: b.x, y2: b.y }, width, height))
    const ray = extendRaySegment(a.x, a.y, b.x, b.y, 3)
    base.lines.push(clipLine({ x1: ray.x1, y1: ray.y1, x2: ray.x2, y2: ray.y2 }, width, height))
    base.circles.push({ x: a.x, y: a.y, r: 3 })
    return base
  }

  if (item.type === 'rect' || item.type === 'fib') {
    const a = anchorToPixel(chart, series, item.a, candles)
    const b = anchorToPixel(chart, series, item.b, candles)
    if (!a || !b) return null
    const box = boxFromPoints(a, b)

    if (item.type === 'rect') {
      base.rects.push({
        x: box.left,
        y: box.top,
        width: Math.max(1, box.right - box.left),
        height: Math.max(1, box.bottom - box.top),
      })
      return base
    }

    base.rects.push({
      x: box.left,
      y: box.top,
      width: Math.max(1, box.right - box.left),
      height: Math.max(1, box.bottom - box.top),
    })

    const yTop = priceToCoordinate(series, item.a.price)
    const yBottom = priceToCoordinate(series, item.b.price)
    if (yTop == null || yBottom == null) return base

    for (const { level, label } of FIB_LEVELS) {
      const price = fibLevelY(item.a.price, item.b.price, level)
      const y = priceToCoordinate(series, price)
      if (y == null) continue
      base.lines.push(
        clipLine({ x1: box.left, y1: y, x2: clamp(box.right, 0, width), y2: y }, width, height),
      )
      base.labels.push({ x: box.left + 4, y: y + 3, text: label })
    }
    return base
  }

  return null
}

