import { useCallback, useEffect, useLayoutEffect, useRef, useState } from 'react'
import type { LineData, MouseEventParams, Time, UTCTimestamp } from 'lightweight-charts'
import type { AssetNewsItem, AssetType, CandlePoint, ChartTradeEvent, DrawTool, DrawingItem } from '../types'
import { DrawingToolsLayer } from '../components/DrawingToolsLayer'
import { useChart } from './hooks/useChart'
import { useCrosshairTooltip, type OhlcTooltipState } from './hooks/useCrosshairTooltip'
import { useCompareSeries, type ComparisonLine } from './hooks/useCompare'
import { useMovingAverageIndicators, useRsiIndicator } from './hooks/useIndicators'
import { useChartMarkers } from './hooks/useMarkers'
import { useCandleVolumeData, useCandleVolumeSeries } from './hooks/useSeries'
import { useNewsMarkers } from './hooks/useNewsMarkers'
import { nearestCandleByTime } from './utils/nearestCandle'
import { formatNumber, formatPrice } from '../../../shared/format/number'

export type { ComparisonLine } from './hooks/useCompare'

type AnalysisChartProps = {
  candles: CandlePoint[]
  fitContentKey: string
  comparisonLines: ComparisonLine[]
  showCompare: boolean
  showMA20: boolean
  showMA50: boolean
  showRSI: boolean
  movingAverageData: { ma20: LineData<Time>[]; ma50: LineData<Time>[] }
  rsiData: LineData<Time>[]
  showEventMarkers: boolean
  showVolume?: boolean
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
  locale: string
  currency: string
  assetType: AssetType
  /** Inside chart workbench: fills plot cell, no outer card chrome. */
  embedded?: boolean
}

export function AnalysisChart({
  candles,
  fitContentKey,
  comparisonLines,
  showCompare,
  showMA20,
  showMA50,
  showRSI,
  movingAverageData,
  rsiData,
  showEventMarkers,
  showVolume = true,
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
  locale,
  currency,
  assetType,
  embedded = false,
}: AnalysisChartProps) {
  const newsMarkers = useNewsMarkers(newsItems, showEventMarkers)

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

  useEffect(() => {
    if (!seriesBundle?.volume) return
    try {
      seriesBundle.volume.applyOptions({ visible: showVolume })
    } catch {
      /* chart teardown */
    }
  }, [seriesBundle, showVolume])

  useMovingAverageIndicators(chart, seriesBundle, movingAverageData, {
    ma20Visible: showMA20,
    ma50Visible: showMA50,
  })
  useRsiIndicator(chart, seriesBundle, rsiData, showRSI)
  useChartMarkers(seriesBundle?.candle ?? null, {
    newsMarkers,
    trades: tradeEvents,
    selectedBarTime,
  })
  useCompareSeries(chart, comparisonLines, showCompare)

  const onTooltip = useCallback((state: OhlcTooltipState) => {
    setTooltip(state)
  }, [])

  useCrosshairTooltip({
    chart,
    candleSeries: seriesBundle?.candle ?? null,
    candles,
    ma20Data: movingAverageData.ma20,
    ma50Data: movingAverageData.ma50,
    rsiData,
    onChange: onTooltip,
    onPanelSync: onLiveOhlcForPanel,
  })

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
    return () => {
      try {
        chart.unsubscribeClick(handler)
      } catch {
        /* chart.remove() may have run in useChart passive cleanup */
      }
    }
  }, [chart])

  const chartInner = (
    <>
      <div
        ref={containerRef}
        className="fi-chart-container"
        style={
          embedded
            ? { minHeight: 280, height: '100%', flex: 1, minWidth: 0 }
            : { minHeight: 460, height: 'clamp(320px, 52vh, 720px)' }
        }
      >
        <div ref={chartMountRef} className="fi-chart-mount" />
        {candles.length === 0 ? (
          <div className="fi-chart-empty-state" role="status">
            <strong>No chart data yet</strong>
            <span>Try another symbol or time range.</span>
          </div>
        ) : null}
        <DrawingToolsLayer drawTool={drawTool} drawings={drawings} onAddDrawing={onAddDrawing} />
      </div>
      {tooltip ? (
        <div className="fi-crosshair-tooltip" role="status">
          <div className="fi-crosshair-tooltip-time">{tooltip.timeLabel}</div>
          <div className={`fi-crosshair-direction ${(tooltip.close ?? 0) >= (tooltip.open ?? 0) ? 'up' : 'down'}`}>
            {(tooltip.close ?? 0) >= (tooltip.open ?? 0) ? '↑' : '↓'}
          </div>
          <section className="fi-crosshair-tooltip-section">
            <strong>OHLC</strong>
            <div className="fi-crosshair-tooltip-grid">
              <span>O</span>
              <span>{formatPrice(tooltip.open, locale, currency, assetType)}</span>
              <span>H</span>
              <span>{formatPrice(tooltip.high, locale, currency, assetType)}</span>
              <span>L</span>
              <span>{formatPrice(tooltip.low, locale, currency, assetType)}</span>
              <span>C</span>
              <span>{formatPrice(tooltip.close, locale, currency, assetType)}</span>
            </div>
          </section>
          <section className="fi-crosshair-tooltip-section">
            <strong>Indicators</strong>
            <div className="fi-crosshair-tooltip-grid">
              <span>MA20</span>
              <span>{formatPrice(tooltip.ma20, locale, currency, assetType)}</span>
              <span>MA50</span>
              <span>{formatPrice(tooltip.ma50, locale, currency, assetType)}</span>
              <span>RSI</span>
              <span>{formatNumber(tooltip.rsi, locale, 2)}</span>
            </div>
          </section>
          <div className="fi-crosshair-tooltip-rsi-hint">
            RSI is shown as plain number
          </div>
        </div>
      ) : null}
    </>
  )

  if (embedded) {
    return (
      <article className="fi-analysis-chart-card fi-analysis-chart-card--embedded">{chartInner}</article>
    )
  }

  return <article className="card fi-analysis-chart-card">{chartInner}</article>
}
