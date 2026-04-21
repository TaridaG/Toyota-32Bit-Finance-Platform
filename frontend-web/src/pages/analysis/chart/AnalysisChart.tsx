import { useCallback, useEffect, useLayoutEffect, useRef, useState } from 'react'
import type { MouseEventParams, Time, UTCTimestamp } from 'lightweight-charts'
import type { AssetNewsItem, CandlePoint, ChartTradeEvent, DrawTool, DrawingItem } from '../types'
import { DrawingToolsLayer } from '../components/DrawingToolsLayer'
import { useChart } from './hooks/useChart'
import { useCrosshairTooltip, type OhlcTooltipState } from './hooks/useCrosshairTooltip'
import { useCompareSeries, type ComparisonLine } from './hooks/useCompare'
import { useMovingAverageIndicators } from './hooks/useIndicators'
import { useChartMarkers } from './hooks/useMarkers'
import { useCandleVolumeData, useCandleVolumeSeries } from './hooks/useSeries'
import { nearestCandleByTime } from './utils/nearestCandle'

export type { ComparisonLine } from './hooks/useCompare'

type AnalysisChartProps = {
  candles: CandlePoint[]
  fitContentKey: string
  comparisonLines: ComparisonLine[]
  showCompare: boolean
  showMovingAverages: boolean
  showEventMarkers: boolean
  newsItems: AssetNewsItem[]
  tradeEvents: ChartTradeEvent[]
  selectedNewsId: string | null
  onSelectNews: (item: AssetNewsItem) => void
  selectedBarTime: UTCTimestamp | null
  onBarSelect: (time: UTCTimestamp | null) => void
  onLiveOhlcForPanel?: (state: OhlcTooltipState) => void
  drawTool: DrawTool
  drawings: DrawingItem[]
  onAddDrawing: (item: DrawingItem) => void
}

function fmt(n: number) {
  return n.toLocaleString(undefined, { maximumFractionDigits: 4 })
}

export function AnalysisChart({
  candles,
  fitContentKey,
  comparisonLines,
  showCompare,
  showMovingAverages,
  showEventMarkers,
  newsItems,
  tradeEvents,
  selectedNewsId,
  onSelectNews,
  selectedBarTime,
  onBarSelect,
  onLiveOhlcForPanel,
  drawTool,
  drawings,
  onAddDrawing,
}: AnalysisChartProps) {
  const [tooltip, setTooltip] = useState<OhlcTooltipState>(null)
  const { containerRef, chartMountRef, chart } = useChart()
  const seriesBundle = useCandleVolumeSeries(chart)

  const candlesRef = useRef(candles)
  const newsRef = useRef(newsItems)
  const onSelectNewsRef = useRef(onSelectNews)
  const onBarSelectRef = useRef(onBarSelect)
  const selectedBarTimeRef = useRef(selectedBarTime)

  useLayoutEffect(() => {
    candlesRef.current = candles
    newsRef.current = newsItems
    onSelectNewsRef.current = onSelectNews
    onBarSelectRef.current = onBarSelect
    selectedBarTimeRef.current = selectedBarTime
  }, [candles, newsItems, onSelectNews, onBarSelect, selectedBarTime])

  useCandleVolumeData(chart, seriesBundle, candles, fitContentKey)
  useMovingAverageIndicators(chart, seriesBundle, candles, showMovingAverages)
  useChartMarkers(seriesBundle?.candle ?? null, {
    news: newsItems,
    trades: tradeEvents,
    visible: showEventMarkers,
    selectedBarTime,
  })
  useCompareSeries(chart, comparisonLines, showCompare)

  const onTooltip = useCallback((state: OhlcTooltipState) => {
    setTooltip(state)
  }, [])

  useCrosshairTooltip(chart, seriesBundle?.candle ?? null, onTooltip, onLiveOhlcForPanel)

  useEffect(() => {
    if (!chart || !selectedNewsId) return
    const match = newsRef.current.find((item) => item.id === selectedNewsId)
    if (!match) return
    chart.timeScale().setVisibleRange({
      from: (match.createdAt - 3600 * 24 * 4) as Time,
      to: (match.createdAt + 3600 * 24 * 4) as Time,
    })
  }, [chart, newsItems, selectedNewsId])

  useEffect(() => {
    if (!chart) return

    const handler = (param: MouseEventParams<Time>) => {
      if (param.point === undefined) return

      const oid = param.hoveredObjectId
      if (typeof oid === 'string' && oid.length > 0) {
        const newsHit = newsRef.current.find((n) => n.id === oid)
        if (newsHit) {
          onSelectNewsRef.current(newsHit)
          return
        }
      }

      const t = param.time
      if (t !== undefined && typeof t === 'number') {
        const pick = nearestCandleByTime(candlesRef.current, t)
        if (pick) {
          const next = pick.time
          onBarSelectRef.current(selectedBarTimeRef.current === next ? null : next)
        }
      }
    }

    chart.subscribeClick(handler)
    return () => chart.unsubscribeClick(handler)
  }, [chart])

  return (
    <article className="card fi-analysis-chart-card">
      <div
        ref={containerRef}
        className="fi-chart-container"
        style={{ minHeight: 460, height: 'clamp(320px, 52vh, 720px)' }}
      >
        <div ref={chartMountRef} className="fi-chart-mount" />
        <DrawingToolsLayer drawTool={drawTool} drawings={drawings} onAddDrawing={onAddDrawing} />
      </div>
      {tooltip ? (
        <div className="fi-crosshair-tooltip" role="status">
          <div className="fi-crosshair-tooltip-time">{tooltip.timeLabel}</div>
          <div className="fi-crosshair-tooltip-grid">
            <span>O</span>
            <span>{fmt(tooltip.open)}</span>
            <span>H</span>
            <span>{fmt(tooltip.high)}</span>
            <span>L</span>
            <span>{fmt(tooltip.low)}</span>
            <span>C</span>
            <span>{fmt(tooltip.close)}</span>
          </div>
        </div>
      ) : null}
    </article>
  )
}
