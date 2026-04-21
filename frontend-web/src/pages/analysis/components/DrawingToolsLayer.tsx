import { useMemo, useState } from 'react'
import type { MouseEvent } from 'react'
import type { DrawTool, DrawingItem } from '../types'

type DrawingToolsLayerProps = {
  drawTool: DrawTool
  drawings: DrawingItem[]
  onAddDrawing: (item: DrawingItem) => void
}

export function DrawingToolsLayer({ drawTool, drawings, onAddDrawing }: DrawingToolsLayerProps) {
  const [pendingTrendStart, setPendingTrendStart] = useState<{ x: number; y: number } | null>(null)

  const overlayCursor = useMemo(() => {
    if (drawTool === 'point') return 'crosshair'
    if (drawTool === 'trendline') return 'crosshair'
    if (drawTool === 'hline') return 'ns-resize'
    return 'default'
  }, [drawTool])

  const handleClick = (event: MouseEvent<SVGSVGElement>) => {
    if (drawTool === 'none') return
    const svg = event.currentTarget
    const rect = svg.getBoundingClientRect()
    const x = ((event.clientX - rect.left) / rect.width) * 100
    const y = ((event.clientY - rect.top) / rect.height) * 100

    if (drawTool === 'point') {
      onAddDrawing({ id: crypto.randomUUID(), type: 'point', x, y })
      return
    }

    if (drawTool === 'hline') {
      onAddDrawing({ id: crypto.randomUUID(), type: 'hline', y })
      return
    }

    if (drawTool === 'trendline') {
      if (!pendingTrendStart) {
        setPendingTrendStart({ x, y })
      } else {
        onAddDrawing({
          id: crypto.randomUUID(),
          type: 'trendline',
          x1: pendingTrendStart.x,
          y1: pendingTrendStart.y,
          x2: x,
          y2: y,
        })
        setPendingTrendStart(null)
      }
    }
  }

  const interactive = drawTool !== 'none'

  return (
    <svg
      className={`fi-drawing-layer${interactive ? ' fi-drawing-layer--interactive' : ''}`}
      style={{
        cursor: overlayCursor,
        pointerEvents: interactive ? 'auto' : 'none',
      }}
      onClick={handleClick}
    >
      {drawings.map((item) => {
        if (item.type === 'point') {
          return <circle key={item.id} cx={`${item.x}%`} cy={`${item.y}%`} r="4" className="fi-draw-point" />
        }
        if (item.type === 'hline') {
          return <line key={item.id} x1="0%" x2="100%" y1={`${item.y}%`} y2={`${item.y}%`} className="fi-draw-line" />
        }
        return (
          <line
            key={item.id}
            x1={`${item.x1}%`}
            y1={`${item.y1}%`}
            x2={`${item.x2}%`}
            y2={`${item.y2}%`}
            className="fi-draw-line"
          />
        )
      })}
      {pendingTrendStart ? <circle cx={`${pendingTrendStart.x}%`} cy={`${pendingTrendStart.y}%`} r="4" className="fi-draw-point" /> : null}
    </svg>
  )
}
