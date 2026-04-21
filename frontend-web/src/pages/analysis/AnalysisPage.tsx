import type { UTCTimestamp } from 'lightweight-charts'
import { useMemo, useState } from 'react'
import { useDocumentTitle } from '../../shared/hooks/useDocumentTitle'
import { useTranslation } from 'react-i18next'
import {
  assets,
  candleSeriesByAsset,
  getPerformancePercent,
  getWindowedSeries,
  newsByAsset,
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

const comparePalette = ['#f59e0b', '#8b5cf6', '#14b8a6', '#f97316', '#22c55e']

export function AnalysisPage() {
  const { t } = useTranslation()
  useDocumentTitle(t('analysisPage.titleDoc'))

  const [selectedAssetId, setSelectedAssetId] = useState('thy')
  const [selectedAssetType, setSelectedAssetType] = useState<'all' | 'stock' | 'crypto' | 'fx' | 'commodity' | 'index'>('all')
  const [timeRange, setTimeRange] = useState<TimeRange>('1M')
  const [showNewsOnChart, setShowNewsOnChart] = useState(true)
  const [showMovingAverages, setShowMovingAverages] = useState(true)
  const [showCompareOnChart, setShowCompareOnChart] = useState(true)
  const [comparisonAssets, setComparisonAssets] = useState<string[]>(['bist', 'gold', 'btc'])
  const [drawTool, setDrawTool] = useState<DrawTool>('none')
  const [drawings, setDrawings] = useState<DrawingItem[]>([])
  const [selectedNews, setSelectedNews] = useState<AssetNewsItem | null>(null)
  const [newsSort, setNewsSort] = useState<'time' | 'impact'>('time')
  const [liveHoverOhlc, setLiveHoverOhlc] = useState<OhlcTooltipState>(null)
  const [selectedBarTime, setSelectedBarTime] = useState<UTCTimestamp | null>(null)

  const selectedAsset = assets.find((asset) => asset.id === selectedAssetId) ?? assets[0]
  const selectedSeries = candleSeriesByAsset[selectedAsset.id]
  const selectedWindowSeries = useMemo(
    () => getWindowedSeries(selectedSeries, timeRange),
    [selectedSeries, timeRange],
  )

  const pinnedBar = useMemo(() => {
    if (selectedBarTime == null) return null
    return selectedSeries.find((c) => c.time === selectedBarTime) ?? null
  }, [selectedBarTime, selectedSeries])

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
    const daily = getPerformancePercent(getWindowedSeries(selectedSeries, '1D'))
    const weekly = getPerformancePercent(getWindowedSeries(selectedSeries, '1W'))
    const monthly = getPerformancePercent(getWindowedSeries(selectedSeries, '1M'))
    const yearly = getPerformancePercent(getWindowedSeries(selectedSeries, '1Y'))
    const current = selectedSeries[selectedSeries.length - 1]
    return {
      currentPrice: current?.close ?? 0,
      volume: current?.volume ?? 0,
      daily,
      weekly,
      monthly,
      yearly,
    }
  }, [selectedSeries])

  const relatedNews = useMemo(() => {
    const items = newsByAsset.filter((item) => item.assetId === selectedAsset.id)
    if (newsSort === 'impact') {
      return [...items].sort((a, b) => Math.abs(b.reactionPercent1h) - Math.abs(a.reactionPercent1h))
    }
    return [...items].sort((a, b) => b.createdAt - a.createdAt)
  }, [newsSort, selectedAsset.id])

  const comparisonLines = useMemo(() => {
    if (!showCompareOnChart) return []
    return comparisonAssets
      .filter((assetId) => assetId !== selectedAsset.id)
      .slice(0, 4)
      .map((assetId, index) => {
        const windowed = getWindowedSeries(candleSeriesByAsset[assetId], timeRange)
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
  }, [comparisonAssets, selectedAsset.id, showCompareOnChart, timeRange])

  const tableRows = useMemo(
    () =>
      [selectedAsset.id, 'gold', 'usdtry', 'bist', 'btc'].map((assetId) => {
        const series = candleSeriesByAsset[assetId]
        return {
          assetId,
          daily: getPerformancePercent(getWindowedSeries(series, '1D')),
          weekly: getPerformancePercent(getWindowedSeries(series, '1W')),
          monthly: getPerformancePercent(getWindowedSeries(series, '1M')),
          yearly: getPerformancePercent(getWindowedSeries(series, '1Y')),
        }
      }),
    [selectedAsset.id],
  )

  const toggleComparison = (assetId: string) => {
    setComparisonAssets((prev) => (prev.includes(assetId) ? prev.filter((id) => id !== assetId) : [...prev, assetId]))
  }

  const handleAssetChange = (id: string) => {
    setSelectedBarTime(null)
    setSelectedAssetId(id)
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
            {(['1D', '1W', '1M', '3M', '1Y', 'ALL'] as const).map((range) => (
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
              Show news
            </button>
            <button
              type="button"
              className={`fi-toggle-chip${showMovingAverages ? ' fi-toggle-chip-active' : ''}`}
              onClick={() => setShowMovingAverages((prev) => !prev)}
            >
              Show MA
            </button>
            <button
              type="button"
              className={`fi-toggle-chip${showCompareOnChart ? ' fi-toggle-chip-active' : ''}`}
              onClick={() => setShowCompareOnChart((prev) => !prev)}
            >
              Show compare
            </button>
            <select value={drawTool} onChange={(event) => setDrawTool(event.target.value as DrawTool)}>
              <option value="none">Draw: None</option>
              <option value="trendline">Draw: Trend line</option>
              <option value="point">Draw: Marker</option>
              <option value="hline">Draw: H-Level</option>
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
          showMovingAverages={showMovingAverages}
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
        />

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
              Sort news:
              <select value={newsSort} onChange={(event) => setNewsSort(event.target.value as 'time' | 'impact')}>
                <option value="time">By Time</option>
                <option value="impact">By Impact</option>
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
