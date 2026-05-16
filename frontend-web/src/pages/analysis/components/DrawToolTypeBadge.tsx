import type { ReactElement } from 'react'
import type { DrawTool } from '../types'
import { normalizeDrawColor } from '../chart/drawing/drawColors'
import {
  DrawIconFib,
  DrawIconHline,
  DrawIconPoint,
  DrawIconRay,
  DrawIconRect,
  DrawIconTrend,
  DrawIconVline,
} from './chartDrawIcons'

const TOOL_ICONS: Partial<Record<DrawTool, () => ReactElement>> = {
  trendline: DrawIconTrend,
  ray: DrawIconRay,
  hline: DrawIconHline,
  vline: DrawIconVline,
  rect: DrawIconRect,
  fib: DrawIconFib,
  point: DrawIconPoint,
}

type DrawToolTypeBadgeProps = {
  tool: DrawTool
  color?: string
}

export function DrawToolTypeBadge({ tool, color }: DrawToolTypeBadgeProps) {
  const Icon = TOOL_ICONS[tool]
  if (!Icon) return null
  const swatch = normalizeDrawColor(color)
  return (
    <span
      className="fi-chart-drawing-type-badge"
      title={tool}
      style={{ color: swatch, borderColor: swatch }}
    >
      <Icon />
    </span>
  )
}
