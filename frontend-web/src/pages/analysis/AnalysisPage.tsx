import type { UTCTimestamp } from 'lightweight-charts'
import { useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useDocumentTitle } from '../../shared/hooks/useDocumentTitle'
import { useTranslation } from 'react-i18next'
import { useAppPreferences } from '../../shared/preferences/useAppPreferences'
import {
  assets,
  candleSeriesByAsset,
  getPerformancePercent,
  tradeEventsByAsset,
} from './mockData'
import type { AssetNewsItem, DrawTool, DrawingItem, TimeRange } from './types'
import { AssetSelector } from './components/AssetSelector'
import { ComparisonSelector } from './components/ComparisonSelector'
import { AnalysisChart } from './components/AnalysisChart'
import type { OhlcTooltipState } from './chart/hooks/useCrosshairTooltip'
import { candleToReadout } from './chart/readout'
import { AssetStatsPanel } from './components/AssetStatsPanel'
import { NewsPanel } from './components/NewsPanel'
import { PerformanceTable } from './components/PerformanceTable'
import { useCandles } from '../../features/analysis/hooks/useCandles'
import { useIndicators } from '../../features/analysis/hooks/useIndicators'
import { useNews } from '../../features/news/hooks/useNews'

const comparePalette = ['#f59e0b', '#8b5cf6', '#14b8a6', '#f97316', '#22c55e']

export function AnalysisPage() {
  const { t } = useTranslation('analysis')
  const { language, currency } = useAppPreferences()
  useDocumentTitle(t('titleDoc'))
  const [searchParams, setSearchParams] = useSearchParams()

  const [selectedAssetType, setSelectedAssetType] = useState<'all' | 'stock' | 'crypto' | 'fx' | 'commodity' | 'index'>('all')
  const [timeRange, setTimeRange] = useState<TimeRange>('24h')
  const [showNewsOnChart, setShowNewsOnChart] = useState(true)
  const [showMA20, setShowMA20] = useState(true)
  const [showMA50, setShowMA50] = useState(true)
  const [showRsi, setShowRsi] = useState(true)
  const [showCompareOnChart, setShowCompareOnChart] = useState(true)
  const [comparisonAssets, setComparisonAssets] = useState<string[]>(['bist', 'gold', 'btc'])
  const [drawTool, setDrawTool] = useState<DrawTool>('none')
  const [drawings, setDrawings] = useState<DrawingItem[]>([])
  const [selectedNews, setSelectedNews] = useState<AssetNewsItem | null>(null)
  const [newsSort, setNewsSort] = useState<'time' | 'impact'>('time')
  const [liveHoverOhlc, setLiveHoverOhlc] = useState<OhlcTooltipState>(null)
  const [selectedBarTime, setSelectedBarTime] = useState<UTCTimestamp | null>(null)

  const selectedSymbol = searchParams.get('symbol')?.toUpperCase()
  const selectedAsset = assets.find((asset) => asset.symbol.replace('/', '').toUpperCase() === selectedSymbol) ?? assets[0]
  const { candles: selectedWindowSeries, loading: candlesLoading, error: candlesError, refetch: refetchCandles } = useCandles(
    selectedAsset.symbol,
    timeRange,
    currency,
  )
  const {
    indicators,
    loading: indicatorsLoading,
    error: indicatorsError,
  } = useIndicators(selectedWindowSeries)
  const { data: newsFeed } = useNews(0, 50)

  const pinnedBar = useMemo(() => {
    if (selectedBarTime == null) return null
    return selectedWindowSeries.find((c) => c.time === selectedBarTime) ?? null
  }, [selectedBarTime, selectedWindowSeries])

  const chartReadout = useMemo(() => {
    if (liveHoverOhlc) {
      return { source: 'hover' as const, ...liveHoverOhlc }
    }
    if (pinnedBar) {
      return candleToReadout(pinnedBar, 'pin')
    }
    return null
  }, [liveHoverOhlc, pinnedBar])

  const windowedTradeEvents = useMemo(() => {
    const all = tradeEventsByAsset[selectedAsset.id] ?? []
    if (selectedWindowSeries.length === 0) return all
    const from = selectedWindowSeries[0].time
    const to = selectedWindowSeries[selectedWindowSeries.length - 1].time
    return all.filter((e) => e.time >= from && e.time <= to)
  }, [selectedAsset.id, selectedWindowSeries])

  const stats = useMemo(() => {
    const current = selectedWindowSeries[selectedWindowSeries.length - 1]
    const daily = getPerformancePercent(sliceLast(selectedWindowSeries, 24))
    const weekly = getPerformancePercent(sliceLast(selectedWindowSeries, 7 * 24))
    const monthly = getPerformancePercent(sliceLast(selectedWindowSeries, 30 * 24))
    const yearly = getPerformancePercent(sliceLast(selectedWindowSeries, 365 * 24))
    return {
      currentPrice: current?.close ?? 0,
      volume: current?.volume ?? 0,
      daily,
      weekly,
      monthly,
      yearly,
    }
  }, [selectedWindowSeries])

  const relatedNews = useMemo(() => {
    const target = selectedAsset.symbol.replace('/', '').toUpperCase()
    const mapped = newsFeed
      .filter((item) => (item.relatedSymbols ?? []).map((s) => s.replace('/', '').toUpperCase()).includes(target))
      .map<AssetNewsItem>((item) => ({
        id: String(item.id),
        assetId: selectedAsset.id,
        title: item.title,
        summary: item.summary ?? '',
        source: item.sourceName,
        impact: item.sentiment,
        createdAt: Math.floor(Date.parse(item.publishedAt) / 1000) as UTCTimestamp,
        reactionPercent1h: item.reactionPercent1h ?? 0,
        relatedAssets: item.relatedSymbols ?? [],
      }))
      .filter((item) => Number.isFinite(item.createdAt))
    if (newsSort === 'impact') {
      return [...mapped].sort((a, b) => Math.abs(b.reactionPercent1h) - Math.abs(a.reactionPercent1h))
    }
    return [...mapped].sort((a, b) => b.createdAt - a.createdAt)
  }, [newsFeed, newsSort, selectedAsset.id, selectedAsset.symbol])

  const comparisonLines = useMemo(() => {
    if (!showCompareOnChart) return []
    return comparisonAssets
      .filter((assetId) => assetId !== selectedAsset.id)
      .slice(0, 4)
      .map((assetId, index) => {
        const windowed = sliceLast(candleSeriesByAsset[assetId], 96)
        const base = windowed[0]?.close || 1
        return {
          id: assetId,
          color: comparePalette[index % comparePalette.length],
          data: windowed.map((point) => ({
            time: point.time,
            value: Number(((point.close / base) * 100).toFixed(4)),
          })),
        }
      })
  }, [comparisonAssets, selectedAsset.id, showCompareOnChart])

  const tableRows = useMemo(
    () =>
      [selectedAsset.id, 'gold', 'usdtry', 'bist', 'btc'].map((assetId) => {
        const series = candleSeriesByAsset[assetId]
        return {
          assetId,
          daily: getPerformancePercent(sliceLast(series, 24)),
          weekly: getPerformancePercent(sliceLast(series, 7 * 24)),
          monthly: getPerformancePercent(sliceLast(series, 30 * 24)),
          yearly: getPerformancePercent(sliceLast(series, 365 * 24)),
        }
      }),
    [selectedAsset.id],
  )

  const toggleComparison = (assetId: string) => {
    setComparisonAssets((prev) => (prev.includes(assetId) ? prev.filter((id) => id !== assetId) : [...prev, assetId]))
  }

  const handleAssetChange = (id: string) => {
    setSelectedBarTime(null)
    const asset = assets.find((item) => item.id === id)
    if (asset) {
      const next = new URLSearchParams(searchParams)
      next.set('symbol', asset.symbol.replace('/', '').toUpperCase())
      setSearchParams(next, { replace: true })
    }
  }

  const handleRangeChange = (range: TimeRange) => {
    setSelectedBarTime(null)
    setTimeRange(range)
  }

  return (
    <section className="fi-analysis-page">
      <header className="fi-analysis-top-grid">
        <AssetSelector
          assets={assets}
          selectedAssetId={selectedAsset.id}
          selectedAssetType={selectedAssetType}
          onAssetChange={handleAssetChange}
          onAssetTypeChange={setSelectedAssetType}
        />
        <ComparisonSelector assets={assets} selected={comparisonAssets} onToggle={toggleComparison} />
        <section className="card fi-analysis-controls">
          <div>
            {(['1h', '6h', '24h', '7d'] as const).map((range) => (
              <button
                key={range}
                type="button"
                className={`fi-range-chip${timeRange === range ? ' fi-range-chip-active' : ''}`}
                onClick={() => handleRangeChange(range)}
              >
                {range}
              </button>
            ))}
          </div>
          <div>
            <button
              type="button"
              className={`fi-toggle-chip${showNewsOnChart ? ' fi-toggle-chip-active' : ''}`}
              onClick={() => setShowNewsOnChart((prev) => !prev)}
            >
              {t('controls.showNews')}
            </button>
            <button
              type="button"
              className={`fi-toggle-chip${showMA20 ? ' fi-toggle-chip-active' : ''}`}
              onClick={() => setShowMA20((prev) => !prev)}
            >
              {t('controls.showMA20')}
            </button>
            <button
              type="button"
              className={`fi-toggle-chip${showMA50 ? ' fi-toggle-chip-active' : ''}`}
              onClick={() => setShowMA50((prev) => !prev)}
            >
              {t('controls.showMA50')}
            </button>
            <button
              type="button"
              className={`fi-toggle-chip${showRsi ? ' fi-toggle-chip-active' : ''}`}
              onClick={() => setShowRsi((prev) => !prev)}
            >
              {t('controls.showRSI')}
            </button>
            <button
              type="button"
              className={`fi-toggle-chip${showCompareOnChart ? ' fi-toggle-chip-active' : ''}`}
              onClick={() => setShowCompareOnChart((prev) => !prev)}
            >
              {t('controls.showCompare')}
            </button>
            <select value={drawTool} onChange={(event) => setDrawTool(event.target.value as DrawTool)}>
              <option value="none">{t('controls.drawNone')}</option>
              <option value="trendline">{t('controls.drawTrend')}</option>
              <option value="point">{t('controls.drawMarker')}</option>
              <option value="hline">{t('controls.drawHLevel')}</option>
            </select>
          </div>
        </section>
      </header>

      <div className="fi-analysis-main-grid">
        <AnalysisChart
          candles={selectedWindowSeries}
          fitContentKey={`${selectedAsset.id}-${timeRange}`}
          comparisonLines={comparisonLines}
          showCompare={showCompareOnChart}
          showMA20={showMA20 && !indicatorsError}
          showMA50={showMA50 && !indicatorsError}
          showRSI={showRsi && !indicatorsError}
          movingAverageData={{ ma20: indicators.ma20, ma50: indicators.ma50 }}
          rsiData={indicators.rsi}
          showEventMarkers={showNewsOnChart}
          newsItems={relatedNews}
          tradeEvents={windowedTradeEvents}
          selectedNewsId={selectedNews?.id ?? null}
          onSelectNews={setSelectedNews}
          selectedBarTime={selectedBarTime}
          onBarSelect={setSelectedBarTime}
          onLiveOhlcForPanel={setLiveHoverOhlc}
          drawTool={drawTool}
          drawings={drawings}
          onAddDrawing={(item) => setDrawings((prev) => [...prev, item])}
          locale={language}
          currency={currency}
          assetType={selectedAsset.type}
        />
        {candlesLoading && selectedWindowSeries.length === 0 ? (
          <div className="markets-skeleton-row" aria-label={t('common:loading')} />
        ) : null}
        {candlesError ? (
          <div className="markets-error-wrap">
            <span>{t(candlesError)}</span>
            <button
              type="button"
              className="markets-filter"
              onClick={() => {
                void refetchCandles()
              }}
            >
              {t('common:retry')}
            </button>
          </div>
        ) : null}
        {!candlesLoading && !candlesError && indicatorsLoading ? <p className="fi-empty">{t('analysis:indicatorsLoading')}</p> : null}
        {!candlesLoading && !candlesError && indicatorsError ? <p className="fi-empty">{t('analysis:indicatorsDisabled')}</p> : null}
        {!candlesLoading && !candlesError && selectedWindowSeries.length === 0 ? <p className="fi-empty">{t('common:noData')}</p> : null}

        <div className="fi-analysis-right-col">
          <AssetStatsPanel
            currentPrice={stats.currentPrice}
            daily={stats.daily}
            weekly={stats.weekly}
            monthly={stats.monthly}
            yearly={stats.yearly}
            volume={stats.volume}
            marketCap={selectedAsset.marketCap}
            chartReadout={chartReadout}
          />
          <section className="card fi-news-sorter">
            <label>
              {t('sortNews')}
              <select value={newsSort} onChange={(event) => setNewsSort(event.target.value as 'time' | 'impact')}>
                <option value="time">{t('sort.byTime')}</option>
                <option value="impact">{t('sort.byImpact')}</option>
              </select>
            </label>
          </section>
          <NewsPanel items={relatedNews} selectedId={selectedNews?.id ?? null} onSelect={setSelectedNews} />
        </div>
      </div>

      <PerformanceTable titleRange={timeRange} assets={assets} rows={tableRows} />
    </section>
  )
}

function sliceLast<T>(series: T[], count: number): T[] {
  if (series.length <= count) {
    return series
  }
  return series.slice(series.length - count)
}
