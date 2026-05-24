import { useCallback, useEffect, useLayoutEffect, useRef, useState } from 'react'
import type { LineData, MouseEventParams, Time, UTCTimestamp } from 'lightweight-charts'
import type {
  AssetNewsItem,
  AssetType,
  CandlePoint,
  ChartDisplayType,
  ChartTradeEvent,
  DrawTool,
  DrawingItem,
} from '../types'
import { DrawingToolsLayer } from '../components/DrawingToolsLayer'
import { ChartMeasureLayer } from '../components/ChartMeasureLayer'
import { useChart } from './hooks/useChart'
import { useCrosshairTooltip, type OhlcTooltipState } from './hooks/useCrosshairTooltip'
import { useCompareSeries, type ComparisonLine } from './hooks/useCompare'
import { useMovingAverageIndicators, useRsiIndicator } from './hooks/useIndicators'
import { useChartMarkers } from './hooks/useMarkers'
import { useMainPriceData, useMainPriceSeries } from './hooks/useSeries'
import { useNewsMarkerTooltip } from './hooks/useNewsMarkerTooltip'
import { useNewsMarkers } from './hooks/useNewsMarkers'
import { nearestCandleByTime } from './utils/nearestCandle'
import { useTranslation } from 'react-i18next'
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
  /** When true, chart clicks set simulation purchase date via `onBarClick`. */
  barClickEnabled?: boolean
  onBarClick?: (time: UTCTimestamp) => void
  onLiveOhlcForPanel?: (state: OhlcTooltipState) => void
  drawTool: DrawTool
  activeDrawColor: string | null
  drawings: DrawingItem[]
  selectedDrawingId: string | null
  onAddDrawing: (item: DrawingItem) => void
  onSelectDrawing: (id: string | null) => void
  onDrawComplete?: () => void
  locale: string
  currency: string
  assetType: AssetType
  chartType?: ChartDisplayType
  measureToolActive?: boolean
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
  barClickEnabled = false,
  onBarClick,
  onLiveOhlcForPanel,
  drawTool,
  activeDrawColor,
  drawings,
  selectedDrawingId,
  onAddDrawing,
  onSelectDrawing,
  onDrawComplete,
  locale,
  currency,
  assetType,
  chartType = 'candle',
  measureToolActive = false,
  embedded = false,
}: AnalysisChartProps) {
  const { t } = useTranslation('analysis')
  const newsMarkers = useNewsMarkers(newsItems, showEventMarkers)

  const [inlineTooltip, setInlineTooltip] = useState<OhlcTooltipState>(null)
  const showInlineTooltip = !embedded
  const { containerRef, chartMountRef, chart } = useChart()
  const newsTooltip = useNewsMarkerTooltip({
    chart,
    newsItems,
    enabled: showEventMarkers,
  })
  const seriesBundle = useMainPriceSeries(chart, chartType)

  const candlesRef = useRef(candles)
  const newsRef = useRef(newsItems)
  const onSelectNewsRef = useRef(onSelectNews)
  const barClickEnabledRef = useRef(barClickEnabled)
  const onBarClickRef = useRef(onBarClick)

  useLayoutEffect(() => {
    candlesRef.current = candles
    newsRef.current = newsItems
    onSelectNewsRef.current = onSelectNews
    barClickEnabledRef.current = barClickEnabled
    onBarClickRef.current = onBarClick
  }, [barClickEnabled, candles, newsItems, onBarClick, onSelectNews])

  useMainPriceData(chart, seriesBundle, candles, fitContentKey, assetType)

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
  useChartMarkers(seriesBundle?.main ?? null, {
    newsMarkers,
    trades: tradeEvents,
  })
  useCompareSeries(chart, comparisonLines, showCompare)

  const onInlineTooltip = useCallback((state: OhlcTooltipState) => {
    if (showInlineTooltip) setInlineTooltip(state)
  }, [showInlineTooltip])

  useCrosshairTooltip({
    chart,
    priceSeries: seriesBundle?.main ?? null,
    candles,
    ma20Data: movingAverageData.ma20,
    ma50Data: movingAverageData.ma50,
    rsiData,
    onChange: onInlineTooltip,
    onPanelSync: embedded ? onLiveOhlcForPanel : undefined,
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

  const drawToolRef = useRef(drawTool)
  drawToolRef.current = drawTool
  const measureActiveRef = useRef(measureToolActive)
  measureActiveRef.current = measureToolActive

  useEffect(() => {
    if (!chart) return

    const handler = (param: MouseEventParams<Time>) => {
      if (drawToolRef.current !== 'none' || measureActiveRef.current) return
      if (param.point === undefined) return

      const oid = param.hoveredObjectId
      if (typeof oid === 'string' && oid.length > 0) {
        const newsHit = newsRef.current.find((n) => n.id === oid)
        if (newsHit) {
          onSelectNewsRef.current(newsHit)
          return
        }
      }

      if (!barClickEnabledRef.current || !onBarClickRef.current) return

      const t = param.time
      if (t !== undefined && typeof t === 'number') {
        const pick = nearestCandleByTime(candlesRef.current, t)
        if (pick) {
          onBarClickRef.current(pick.time)
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
        <DrawingToolsLayer
          chart={chart}
          priceSeries={seriesBundle?.main ?? null}
          mountRef={chartMountRef}
          candles={candles}
          drawTool={drawTool}
          activeDrawColor={activeDrawColor}
          drawings={drawings}
          selectedDrawingId={selectedDrawingId}
          onAddDrawing={onAddDrawing}
          onSelectDrawing={onSelectDrawing}
          onDrawComplete={onDrawComplete}
        />
        <ChartMeasureLayer
          chart={chart}
          priceSeries={seriesBundle?.main ?? null}
          mountRef={chartMountRef}
          candles={candles}
          active={measureToolActive}
          locale={locale}
          currency={currency}
          assetType={assetType}
        />
        {newsTooltip ? (
          <div
            className="fi-chart-news-tooltip"
            role="status"
            style={{ left: newsTooltip.x + 12, top: newsTooltip.y + 12 }}
          >
            <strong>{newsTooltip.item.title}</strong>
            {newsTooltip.item.summary ? <p>{newsTooltip.item.summary}</p> : null}
            {newsTooltip.item.matchReasons && newsTooltip.item.matchReasons.length > 0 ? (
              <ul className="fi-chart-news-tooltip-tags">
                {newsTooltip.item.matchReasons.map((reason, index) => (
                  <li key={`${reason.kind}-${reason.categoryUi}-${index}`}>
                    {reason.kind === 'favorite'
                      ? t('chartNews.matchFavorite')
                      : reason.kind === 'asset' && reason.symbol
                        ? t('chartNews.matchAsset', { symbol: reason.symbol })
                        : t('chartNews.matchCategory', {
                            category: t(`news:categories.${reason.categoryUi}`, {
                              defaultValue: reason.categoryUi.toUpperCase(),
                            }),
                          })}
                  </li>
                ))}
              </ul>
            ) : null}
            <footer>
              <span>{newsTooltip.item.source}</span>
              {newsTooltip.item.nextDayChangePercent != null &&
              Number.isFinite(newsTooltip.item.nextDayChangePercent) ? (
                <span
                  className={
                    newsTooltip.item.nextDayChangePercent >= 0
                      ? 'fi-chart-news-tooltip-change-up'
                      : 'fi-chart-news-tooltip-change-down'
                  }
                >
                  {t('chartNews.nextDayChange', {
                    value: newsTooltip.item.nextDayChangePercent.toFixed(2),
                  })}
                </span>
              ) : null}
            </footer>
          </div>
        ) : null}
      </div>
      {showInlineTooltip && inlineTooltip ? (
        <div className="fi-crosshair-tooltip" role="status">
          <div className="fi-crosshair-tooltip-time">{inlineTooltip.timeLabel}</div>
          <div className={`fi-crosshair-direction ${(inlineTooltip.close ?? 0) >= (inlineTooltip.open ?? 0) ? 'up' : 'down'}`}>
            {(inlineTooltip.close ?? 0) >= (inlineTooltip.open ?? 0) ? '↑' : '↓'}
          </div>
          <section className="fi-crosshair-tooltip-section">
            <strong>OHLC</strong>
            <div className="fi-crosshair-tooltip-grid">
              <span>O</span>
              <span>{formatPrice(inlineTooltip.open, locale, currency, assetType)}</span>
              <span>H</span>
              <span>{formatPrice(inlineTooltip.high, locale, currency, assetType)}</span>
              <span>L</span>
              <span>{formatPrice(inlineTooltip.low, locale, currency, assetType)}</span>
              <span>C</span>
              <span>{formatPrice(inlineTooltip.close, locale, currency, assetType)}</span>
            </div>
          </section>
          <section className="fi-crosshair-tooltip-section">
            <strong>Indicators</strong>
            <div className="fi-crosshair-tooltip-grid">
              <span>MA20</span>
              <span>{formatPrice(inlineTooltip.ma20, locale, currency, assetType)}</span>
              <span>MA50</span>
              <span>{formatPrice(inlineTooltip.ma50, locale, currency, assetType)}</span>
              <span>RSI</span>
              <span>{formatNumber(inlineTooltip.rsi, locale, 2)}</span>
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
