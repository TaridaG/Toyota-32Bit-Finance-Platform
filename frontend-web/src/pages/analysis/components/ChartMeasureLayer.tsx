import { useCallback, useEffect, useId, useMemo, useRef, useState } from 'react'
import type { IChartApi, ISeriesApi } from 'lightweight-charts'
import { useTranslation } from 'react-i18next'
import { anchorToPixel, clientToAnchor } from '../chart/drawing/chartCoordinates'
import { getPricePaneBounds } from '../chart/drawing/chartPaneLayout'
import { useDrawingLayoutSync } from '../chart/drawing/useDrawingLayoutSync'
import {
  computeMeasureStats,
  formatMeasureDuration,
} from '../chart/measure/computeMeasureStats'
import type { AssetType, CandlePoint, ChartAnchor } from '../types'
import { formatNumber, formatPrice } from '../../../shared/format/number'

type ChartMeasureLayerProps = {
  chart: IChartApi | null
  priceSeries: ISeriesApi<'Candlestick'> | ISeriesApi<'Line'> | null
  mountRef: React.RefObject<HTMLDivElement | null>
  candles: CandlePoint[]
  active: boolean
  locale: string
  currency: string
  assetType: AssetType
}

type MeasureRange = { a: ChartAnchor; b: ChartAnchor }

export function ChartMeasureLayer({
  chart,
  priceSeries,
  mountRef,
  candles,
  active,
  locale,
  currency,
  assetType,
}: ChartMeasureLayerProps) {
  const { t } = useTranslation('analysis')
  const [layoutTick, setLayoutTick] = useState(0)
  const [dragRange, setDragRange] = useState<MeasureRange | null>(null)
  const [committedRange, setCommittedRange] = useState<MeasureRange | null>(null)
  const draggingRef = useRef(false)
  const dragRangeRef = useRef<MeasureRange | null>(null)
  dragRangeRef.current = dragRange
  const clipId = useId().replace(/:/g, '')

  const displayRange = dragRange ?? committedRange

  const bump = useCallback(() => setLayoutTick((v) => v + 1), [])

  useEffect(() => {
    if (!active) {
      setDragRange(null)
      draggingRef.current = false
    }
  }, [active])

  useEffect(() => {
    const onKey = (ev: KeyboardEvent) => {
      if (ev.key === 'Escape') {
        setDragRange(null)
        setCommittedRange(null)
        draggingRef.current = false
      }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [])

  useDrawingLayoutSync({
    chart,
    priceSeries,
    mountRef,
    active: active && displayRange != null,
    onLayoutChange: bump,
  })

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

  useEffect(() => {
    const mount = mountRef.current
    if (!mount) return
    mount.classList.toggle('fi-chart-mount--measure', active)
    return () => mount.classList.remove('fi-chart-mount--measure')
  }, [active, mountRef])

  const paneLayout = useMemo(() => {
    void layoutTick
    const mount = mountRef.current
    if (!chart || !priceSeries || !mount) return null
    return getPricePaneBounds(priceSeries, mount)
  }, [chart, layoutTick, mountRef, priceSeries])

  const anchorFromClient = useCallback(
    (clientX: number, clientY: number): ChartAnchor | null => {
      const mount = mountRef.current
      if (!chart || !priceSeries || !mount) return null
      return clientToAnchor(chart, priceSeries, mount, clientX, clientY, candles)
    },
    [candles, chart, mountRef, priceSeries],
  )

  const onPointerDown = useCallback(
    (event: React.PointerEvent<SVGSVGElement>) => {
      if (!active || event.button !== 0) return
      const anchor = anchorFromClient(event.clientX, event.clientY)
      if (!anchor) return
      event.preventDefault()
      event.currentTarget.setPointerCapture(event.pointerId)
      draggingRef.current = true
      setCommittedRange(null)
      setDragRange({ a: anchor, b: anchor })
    },
    [active, anchorFromClient],
  )

  const onPointerMove = useCallback(
    (event: React.PointerEvent<SVGSVGElement>) => {
      if (!draggingRef.current) return
      const anchor = anchorFromClient(event.clientX, event.clientY)
      if (!anchor) return
      setDragRange((prev) => (prev ? { a: prev.a, b: anchor } : null))
    },
    [anchorFromClient],
  )

  const onPointerUp = useCallback((event: React.PointerEvent<SVGSVGElement>) => {
    if (!draggingRef.current) return
    draggingRef.current = false
    try {
      event.currentTarget.releasePointerCapture(event.pointerId)
    } catch {
      /* already released */
    }
    const prev = dragRangeRef.current
    setDragRange(null)
    if (!prev) return
    const same =
      prev.a.time === prev.b.time && Math.abs(prev.a.price - prev.b.price) < 1e-12
    if (!same) {
      setCommittedRange(prev)
    }
  }, [])

  const projection = useMemo(() => {
    if (!displayRange || !chart || !priceSeries || !paneLayout) return null
    const pa = anchorToPixel(chart, priceSeries, displayRange.a)
    const pb = anchorToPixel(chart, priceSeries, displayRange.b)
    if (!pa || !pb) return null

    const x1 = pa.x
    const y1 = pa.y
    const x2 = pb.x
    const y2 = pb.y
    const top = Math.min(y1, y2)
    const bottom = Math.max(y1, y2)
    const left = Math.min(x1, x2)
    const right = Math.max(x1, x2)
    const midX = (x1 + x2) / 2
    const midY = (y1 + y2) / 2

    const stats = computeMeasureStats(displayRange.a, displayRange.b, candles)
    const stroke = stats.isUp ? '#22c55e' : '#ef4444'
    const fill = stats.isUp ? 'rgba(34, 197, 94, 0.12)' : 'rgba(239, 68, 68, 0.12)'

    const priceDeltaLabel = formatPrice(Math.abs(stats.priceDelta), locale, currency, assetType)
    const signedDelta =
      (stats.priceDelta >= 0 ? '+' : '−') + priceDeltaLabel.replace(/^-/, '')
    const pctLabel =
      stats.percentChange == null
        ? '—'
        : `${stats.percentChange >= 0 ? '+' : ''}${formatNumber(stats.percentChange, locale, 2)}%`
    const barsLabel = t('chartMeasure.bars', { count: stats.barCount })
    const timeLabel = formatMeasureDuration(stats.durationSec, locale)

    const labelLines = [signedDelta, pctLabel, barsLabel, timeLabel]

    return {
      x1,
      y1,
      x2,
      y2,
      top,
      bottom,
      left,
      right,
      midX,
      midY,
      stats,
      stroke,
      fill,
      labelLines,
    }
  }, [assetType, candles, chart, currency, displayRange, locale, paneLayout, priceSeries, t])

  if (!active || !chart || !priceSeries || !paneLayout || paneLayout.width <= 0 || paneLayout.height <= 0) {
    return null
  }

  return (
    <svg
      className="fi-measure-layer"
      width={paneLayout.width}
      height={paneLayout.height}
      viewBox={`0 0 ${paneLayout.width} ${paneLayout.height}`}
      style={{
        left: paneLayout.left,
        top: paneLayout.top,
        width: paneLayout.width,
        height: paneLayout.height,
      }}
      onPointerDown={onPointerDown}
      onPointerMove={onPointerMove}
      onPointerUp={onPointerUp}
      onPointerCancel={onPointerUp}
    >
      <defs>
        <clipPath id={clipId}>
          <rect x={0} y={0} width={paneLayout.width} height={paneLayout.height} />
        </clipPath>
        <marker
          id={`${clipId}-arrow-up`}
          markerWidth="8"
          markerHeight="8"
          refX="4"
          refY="4"
          orient="auto-start-reverse"
        >
          <path d="M0,8 L4,0 L8,8 Z" fill="currentColor" />
        </marker>
        <marker
          id={`${clipId}-arrow-down`}
          markerWidth="8"
          markerHeight="8"
          refX="4"
          refY="4"
          orient="auto"
        >
          <path d="M0,0 L4,8 L8,0 Z" fill="currentColor" />
        </marker>
      </defs>
      {projection ? (
        <g clipPath={`url(#${clipId})`} style={{ pointerEvents: 'none' }}>
          <rect
            x={projection.left}
            y={projection.top}
            width={Math.max(1, projection.right - projection.left)}
            height={Math.max(1, projection.bottom - projection.top)}
            className="fi-measure-shade"
            fill={projection.fill}
            stroke={projection.stroke}
            strokeWidth={1}
            strokeDasharray="4 3"
            rx={2}
          />
          <line
            x1={projection.x1}
            y1={projection.top}
            x2={projection.x1}
            y2={projection.bottom}
            className="fi-measure-vline"
            stroke={projection.stroke}
            markerStart={
              projection.y1 <= projection.y2
                ? `url(#${clipId}-arrow-up)`
                : `url(#${clipId}-arrow-down)`
            }
            markerEnd={
              projection.y1 <= projection.y2
                ? `url(#${clipId}-arrow-down)`
                : `url(#${clipId}-arrow-up)`
            }
            style={{ color: projection.stroke }}
          />
          <line
            x1={projection.x2}
            y1={projection.top}
            x2={projection.x2}
            y2={projection.bottom}
            className="fi-measure-vline"
            stroke={projection.stroke}
            markerStart={
              projection.y2 <= projection.y1
                ? `url(#${clipId}-arrow-up)`
                : `url(#${clipId}-arrow-down)`
            }
            markerEnd={
              projection.y2 <= projection.y1
                ? `url(#${clipId}-arrow-down)`
                : `url(#${clipId}-arrow-up)`
            }
            style={{ color: projection.stroke }}
          />
          <line
            x1={projection.x1}
            y1={projection.y1}
            x2={projection.x2}
            y2={projection.y2}
            className="fi-measure-diagonal"
            stroke={projection.stroke}
          />
          <circle cx={projection.x1} cy={projection.y1} r={4} className="fi-measure-anchor" fill={projection.stroke} />
          <circle cx={projection.x2} cy={projection.y2} r={4} className="fi-measure-anchor" fill={projection.stroke} />
          <g transform={`translate(${projection.midX}, ${projection.midY})`}>
            <rect
              x={-72}
              y={-36}
              width={144}
              height={72}
              rx={6}
              className="fi-measure-label-box"
            />
            {projection.labelLines.map((line, index) => (
              <text
                key={line}
                x={0}
                y={-18 + index * 16}
                textAnchor="middle"
                className="fi-measure-label-text"
                style={{ fill: projection.stroke }}
              >
                {line}
              </text>
            ))}
          </g>
        </g>
      ) : null}
    </svg>
  )
}
