import { useCallback, useEffect, useId, useMemo, useRef, useState } from 'react'
import type { IChartApi, ISeriesApi, MouseEventParams, Time, UTCTimestamp } from 'lightweight-charts'
import { anchorToPixel, projectPendingAnchor } from '../chart/drawing/chartCoordinates'
import { nearestCandleByTime } from '../chart/utils/nearestCandle'
import { collectDrawingAnchors } from '../chart/drawing/drawingAnchors'
import { getPricePaneBounds } from '../chart/drawing/chartPaneLayout'
import { projectDrawing } from '../chart/drawing/drawingProjection'
import { normalizeDrawColor, withAlpha } from '../chart/drawing/drawColors'
import { isTwoClickDrawTool } from '../chart/drawing/drawingUtils'
import { useDrawingLayoutSync } from '../chart/drawing/useDrawingLayoutSync'
import type { CandlePoint, ChartAnchor, DrawTool, DrawingItem } from '../types'

type DrawingToolsLayerProps = {
  chart: IChartApi | null
  priceSeries: ISeriesApi<'Candlestick'> | ISeriesApi<'Line'> | null
  mountRef: React.RefObject<HTMLDivElement | null>
  candles: CandlePoint[]
  drawTool: DrawTool
  activeDrawColor: string | null
  drawings: DrawingItem[]
  selectedDrawingId: string | null
  onAddDrawing: (item: DrawingItem) => void
  onSelectDrawing: (id: string | null) => void
  onDrawComplete?: () => void
}

function drawingStrokeColor(item: DrawingItem): string {
  return normalizeDrawColor(item.color)
}

function drawingFillColor(item: DrawingItem, selected: boolean): string {
  return withAlpha(drawingStrokeColor(item), selected ? 0.16 : 0.1)
}

function drawingSelectionStyle(color: string, selected: boolean): {
  strokeWidth: number
  filter?: string
} {
  return selected
    ? { strokeWidth: 2.75, filter: `drop-shadow(0 0 5px ${withAlpha(color, 0.75)})` }
    : { strokeWidth: 2 }
}

function isAnchoredDrawing(item: DrawingItem): boolean {
  if (item.type === 'point') return 'anchor' in item
  if (item.type === 'hline') return typeof item.price === 'number'
  if (item.type === 'vline') return typeof item.time === 'number'
  return 'a' in item && 'b' in item
}

export function DrawingToolsLayer({
  chart,
  priceSeries,
  mountRef,
  candles,
  drawTool,
  activeDrawColor,
  drawings,
  selectedDrawingId,
  onAddDrawing,
  onSelectDrawing,
  onDrawComplete,
}: DrawingToolsLayerProps) {
  const [layoutTick, setLayoutTick] = useState(0)
  const [pendingAnchor, setPendingAnchor] = useState<ChartAnchor | null>(null)
  const pendingAnchorRef = useRef<ChartAnchor | null>(null)
  const drawToolRef = useRef(drawTool)
  drawToolRef.current = drawTool
  const activeDrawColorRef = useRef(activeDrawColor)
  activeDrawColorRef.current = activeDrawColor
  const isDrawingMode = drawTool !== 'none'
  const isSelectMode = drawTool === 'none'

  pendingAnchorRef.current = pendingAnchor

  const bump = useCallback(() => setLayoutTick((v) => v + 1), [])
  const plotClipId = useId().replace(/:/g, '')

  const anchoredDrawings = useMemo(
    () => drawings.filter(isAnchoredDrawing),
    [drawings],
  )

  const needsLayoutSync =
    isDrawingMode || pendingAnchor != null || anchoredDrawings.length > 0

  useEffect(() => {
    setPendingAnchor(null)
  }, [drawTool])

  useEffect(() => {
    const mount = mountRef.current
    if (!chart || !mount) return

    const onLayout = () => bump()
    const ts = chart.timeScale()
    ts.subscribeVisibleLogicalRangeChange(onLayout)
    ts.subscribeVisibleTimeRangeChange(onLayout)
    const ro = new ResizeObserver(onLayout)
    ro.observe(mount)
    mount.addEventListener('wheel', onLayout, { passive: true })

    return () => {
      try {
        ts.unsubscribeVisibleLogicalRangeChange(onLayout)
        ts.unsubscribeVisibleTimeRangeChange(onLayout)
      } catch {
        /* disposed */
      }
      ro.disconnect()
      mount.removeEventListener('wheel', onLayout)
    }
  }, [bump, chart, mountRef])

  useDrawingLayoutSync({
    chart,
    priceSeries,
    mountRef,
    active: needsLayoutSync,
    onLayoutChange: bump,
  })

  const paneLayout = useMemo(() => {
    void layoutTick
    const mount = mountRef.current
    if (!chart || !priceSeries || !mount) return null
    return getPricePaneBounds(priceSeries, mount)
  }, [chart, layoutTick, mountRef, priceSeries])

  const projections = useMemo(() => {
    if (!chart || !priceSeries || !paneLayout) return []
    return anchoredDrawings
      .map((item) => {
        const projected = projectDrawing(
          item,
          chart,
          priceSeries,
          paneLayout.width,
          paneLayout.height,
        )
        return projected ? { item, projected } : null
      })
      .filter((entry): entry is { item: DrawingItem; projected: NonNullable<ReturnType<typeof projectDrawing>> } =>
        entry != null,
      )
  }, [anchoredDrawings, chart, paneLayout, priceSeries])

  const pendingPixel = useMemo(() => {
    if (!pendingAnchor || !chart || !priceSeries) return null
    return projectPendingAnchor(chart, priceSeries, pendingAnchor)
  }, [chart, pendingAnchor, priceSeries])

  const finishDraw = useCallback(() => {
    onDrawComplete?.()
  }, [onDrawComplete])

  const applyDrawAnchor = useCallback(
    (anchor: ChartAnchor) => {
      const tool = drawToolRef.current
      const color = normalizeDrawColor(activeDrawColorRef.current ?? undefined)
      onSelectDrawing(null)

      if (tool === 'point') {
        onAddDrawing({ id: crypto.randomUUID(), type: 'point', color, anchor })
        finishDraw()
        return
      }

      if (tool === 'hline') {
        onAddDrawing({ id: crypto.randomUUID(), type: 'hline', color, price: anchor.price })
        finishDraw()
        return
      }

      if (tool === 'vline') {
        onAddDrawing({ id: crypto.randomUUID(), type: 'vline', color, time: anchor.time })
        finishDraw()
        return
      }

      if (isTwoClickDrawTool(tool)) {
        const pending = pendingAnchorRef.current
        if (!pending) {
          setPendingAnchor(anchor)
          return
        }

        const id = crypto.randomUUID()
        if (tool === 'trendline') {
          onAddDrawing({ id, type: 'trendline', color, a: pending, b: anchor })
        } else if (tool === 'ray') {
          onAddDrawing({ id, type: 'ray', color, a: pending, b: anchor })
        } else if (tool === 'rect') {
          onAddDrawing({ id, type: 'rect', color, a: pending, b: anchor })
        } else {
          onAddDrawing({ id, type: 'fib', color, a: pending, b: anchor })
        }
        setPendingAnchor(null)
        finishDraw()
      }
    },
    [finishDraw, onAddDrawing, onSelectDrawing],
  )

  const anchorFromChartClick = useCallback(
    (param: MouseEventParams<Time>): ChartAnchor | null => {
      if (!chart || !priceSeries || param.point === undefined) return null

      const mount = mountRef.current
      const pane = mount ? getPricePaneBounds(priceSeries, mount) : null
      if (pane && (param.point.y < 0 || param.point.y > pane.height)) return null

      const price = priceSeries.coordinateToPrice(param.point.y)
      if (price == null || !Number.isFinite(price)) return null

      let time: UTCTimestamp
      if (typeof param.time === 'number') {
        time = param.time as UTCTimestamp
      } else {
        const raw = chart.timeScale().coordinateToTime(param.point.x)
        if (raw == null) return null
        time = Number(raw) as UTCTimestamp
      }

      if (candles.length === 0) {
        return { time, price }
      }

      const candle = nearestCandleByTime(candles, Number(time))
      if (!candle) {
        return { time, price }
      }

      return { time: candle.time, price }
    },
    [candles, chart, mountRef, priceSeries],
  )

  useEffect(() => {
    if (!chart || !priceSeries || !isDrawingMode) return

    const handler = (param: MouseEventParams<Time>) => {
      if (drawToolRef.current === 'none') return
      const anchor = anchorFromChartClick(param)
      if (!anchor) return
      applyDrawAnchor(anchor)
    }

    chart.subscribeClick(handler)
    return () => {
      try {
        chart.unsubscribeClick(handler)
      } catch {
        /* chart disposed */
      }
    }
  }, [anchorFromChartClick, applyDrawAnchor, chart, isDrawingMode, priceSeries])

  useEffect(() => {
    const mount = mountRef.current
    if (!mount) return
    mount.classList.toggle('fi-chart-mount--drawing', isDrawingMode)
    return () => mount.classList.remove('fi-chart-mount--drawing')
  }, [isDrawingMode, mountRef])

  if (!chart || !priceSeries || !paneLayout || paneLayout.width <= 0 || paneLayout.height <= 0) {
    return null
  }

  return (
    <svg
      className={`fi-drawing-layer fi-drawing-layer--select${isDrawingMode ? ' fi-drawing-layer--drawing' : ''}`}
      width={paneLayout.width}
      height={paneLayout.height}
      viewBox={`0 0 ${paneLayout.width} ${paneLayout.height}`}
      style={{
        left: paneLayout.left,
        top: paneLayout.top,
        width: paneLayout.width,
        height: paneLayout.height,
      }}
    >
      <defs>
        <clipPath id={plotClipId}>
          <rect x={0} y={0} width={paneLayout.width} height={paneLayout.height} />
        </clipPath>
      </defs>
      <g clipPath={`url(#${plotClipId})`}>
      {projections.map(({ item, projected }) => {
        const selected = item.id === selectedDrawingId
        const selectedClass = selected ? ' fi-draw-shape--selected' : ''
        const stroke = drawingStrokeColor(item)
        const fill = drawingFillColor(item, selected)
        const selectionStyle = drawingSelectionStyle(stroke, selected)
        const hitStrokeStyle = { pointerEvents: 'stroke' as const, cursor: 'pointer' as const }
        const noHitStyle = { pointerEvents: 'none' as const }

        return (
          <g key={item.id} data-drawing-id={item.id}>
            {projected.rects.map((rect, index) => (
              <rect
                key={`${item.id}-rect-${index}`}
                x={rect.x}
                y={rect.y}
                width={rect.width}
                height={rect.height}
                className={`fi-draw-rect${selectedClass}`}
                style={{
                  fill,
                  stroke,
                  strokeWidth: selectionStyle.strokeWidth,
                  filter: selectionStyle.filter,
                  ...(isSelectMode ? hitStrokeStyle : noHitStyle),
                }}
                onClick={
                  isSelectMode
                    ? (e) => {
                        e.stopPropagation()
                        onSelectDrawing(item.id)
                      }
                    : undefined
                }
              />
            ))}
            {projected.lines.map((line, index) => {
              const isRayCore = item.type === 'ray' && index === 0
              const lineClass =
                item.type === 'ray' && index > 0
                  ? `fi-draw-line fi-draw-line--ray${selectedClass}`
                  : `fi-draw-line${selectedClass}`
              const lineInteractive = isSelectMode && (item.type !== 'ray' || isRayCore)
              return (
                <g key={`${item.id}-line-${index}`}>
                  {lineInteractive ? (
                    <line
                      x1={line.x1}
                      y1={line.y1}
                      x2={line.x2}
                      y2={line.y2}
                      stroke="transparent"
                      strokeWidth={14}
                      style={hitStrokeStyle}
                      onClick={(e) => {
                        e.stopPropagation()
                        onSelectDrawing(item.id)
                      }}
                    />
                  ) : null}
                  <line
                    x1={line.x1}
                    y1={line.y1}
                    x2={line.x2}
                    y2={line.y2}
                    className={lineClass}
                    stroke={stroke}
                    strokeWidth={selectionStyle.strokeWidth}
                    filter={selectionStyle.filter}
                    style={noHitStyle}
                  />
                </g>
              )
            })}
            {collectDrawingAnchors(item).map((anchor, index) => {
              const px = anchorToPixel(chart, priceSeries, anchor)
              if (!px) return null
              const anchorStroke = drawingStrokeColor(item)
              return (
                <g key={`${item.id}-anchor-${index}`} style={{ pointerEvents: 'none' }}>
                  <circle
                    cx={px.x}
                    cy={px.y}
                    r={5}
                    className={`fi-draw-anchor${selectedClass}`}
                    style={{ fill: withAlpha(anchorStroke, 0.35), stroke: anchorStroke }}
                  />
                  <line
                    x1={px.x - 6}
                    y1={px.y}
                    x2={px.x + 6}
                    y2={px.y}
                    className={`fi-draw-anchor-cross${selectedClass}`}
                    style={{ stroke: anchorStroke }}
                  />
                  <line
                    x1={px.x}
                    y1={px.y - 6}
                    x2={px.x}
                    y2={px.y + 6}
                    className={`fi-draw-anchor-cross${selectedClass}`}
                    style={{ stroke: anchorStroke }}
                  />
                </g>
              )
            })}
            {projected.circles.map((circle, index) => (
              <circle
                key={`${item.id}-circle-${index}`}
                cx={circle.x}
                cy={circle.y}
                r={selected ? circle.r + 1.5 : circle.r}
                className={`fi-draw-point${selectedClass}${isSelectMode ? ' fi-draw-shape-hit' : ''}`}
                stroke={stroke}
                strokeWidth={selected ? 2.25 : 1.5}
                filter={selectionStyle.filter}
                style={{
                  fill: stroke,
                  ...(isSelectMode ? { pointerEvents: 'all', cursor: 'pointer' } : { pointerEvents: 'none' }),
                }}
                onClick={
                  isSelectMode
                    ? (e) => {
                        e.stopPropagation()
                        onSelectDrawing(item.id)
                      }
                    : undefined
                }
              />
            ))}
            {projected.labels.map((label, index) => (
              <text
                key={`${item.id}-label-${index}`}
                x={label.x}
                y={label.y}
                className="fi-draw-fib-label"
                style={{ pointerEvents: 'none', fill: stroke }}
              >
                {label.text}
              </text>
            ))}
          </g>
        )
      })}
      {pendingPixel ? (
        <g style={{ pointerEvents: 'none' }}>
          {(() => {
            const pendingColor = normalizeDrawColor(activeDrawColor ?? undefined)
            return (
              <>
                <circle
                  cx={pendingPixel.x}
                  cy={pendingPixel.y}
                  r={5}
                  className="fi-draw-anchor"
                  style={{ fill: withAlpha(pendingColor, 0.35), stroke: pendingColor }}
                />
                <line
                  x1={pendingPixel.x - 6}
                  y1={pendingPixel.y}
                  x2={pendingPixel.x + 6}
                  y2={pendingPixel.y}
                  className="fi-draw-anchor-cross"
                  style={{ stroke: pendingColor }}
                />
                <line
                  x1={pendingPixel.x}
                  y1={pendingPixel.y - 6}
                  x2={pendingPixel.x}
                  y2={pendingPixel.y + 6}
                  className="fi-draw-anchor-cross"
                  style={{ stroke: pendingColor }}
                />
              </>
            )
          })()}
        </g>
      ) : null}
      </g>
    </svg>
  )
}
