import type { ChartAnchor, DrawingItem } from '../../types'

export function collectDrawingAnchors(item: DrawingItem): ChartAnchor[] {
  switch (item.type) {
    case 'point':
      return [item.anchor]
    case 'trendline':
    case 'ray':
    case 'rect':
    case 'fib':
      return [item.a, item.b]
    default:
      return []
  }
}
