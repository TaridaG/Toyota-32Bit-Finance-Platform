import type { UTCTimestamp } from 'lightweight-charts'
import { useEffect, useMemo, useRef, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useDocumentTitle } from '../../shared/hooks/useDocumentTitle'
import { useTranslation } from 'react-i18next'
import { useAppPreferences } from '../../shared/preferences/useAppPreferences'
import type { AssetDefinition, AssetNewsItem, AssetType, CandlePoint, DrawTool, DrawingItem, TimeRange } from './types'
import { AssetSelector } from './components/AssetSelector'
import { AnalysisChart } from './components/AnalysisChart'
import { AnalysisChartFrame } from './components/AnalysisChartFrame'
import { AnalysisTickerBar } from './components/AnalysisTickerBar'
import { PerformanceTable } from './components/PerformanceTable'
import { fetchCandles } from '../../features/analysis/api/analysisService'
import { useCandles } from '../../features/analysis/hooks/useCandles'
import { useIndicators } from '../../features/analysis/hooks/useIndicators'
import { useMarkets } from '../../features/markets/hooks/useMarkets'
import { useNews } from '../../features/news/hooks/useNews'
import type { MarketOverviewItem } from '../../shared/types/market'

const comparePalette = ['#f59e0b', '#8b5cf6', '#14b8a6', '#f97316', '#22c55e']

type CompareSlotTuple = [string | null, string | null, string | null]
const EMPTY_COMPARE_SLOTS: CompareSlotTuple = [null, null, null]

function normalizeSymbol(symbol: string): string {
  return symbol.replace(/[^A-Za-z0-9]/g, '').toUpperCase()
}

function mapCategoryToAssetType(category: string | null | undefined): AssetType {
  const c = (category ?? 'STOCK').toUpperCase()
  switch (c) {
    case 'CRYPTO':
      return 'crypto'
    case 'FX':
      return 'fx'
    case 'FUND':
      return 'fund'
    case 'METAL':
      return 'commodity'
    case 'STOCK':
    default:
      return 'stock'
  }
}

function mapMarketRowToAsset(row: MarketOverviewItem): AssetDefinition {
  const symbol = row.symbol.toUpperCase()
  const wire = (row.category ?? 'STOCK').toUpperCase()
  return {
    id: symbol.toLowerCase(),
    symbol,
    name: row.name || symbol,
    type: mapCategoryToAssetType(row.category),
    wireCategory: wire,
  }
}

export function AnalysisPage() {
  const { t } = useTranslation('analysis')
  const { language, currency } = useAppPreferences()
  useDocumentTitle(t('titleDoc'))
  const [searchParams, setSearchParams] = useSearchParams()

  const [comparisonListCategory, setComparisonListCategory] = useState<AssetType | 'all'>('all')
  const [timeRange, setTimeRange] = useState<TimeRange>('24h')
  const [showNewsOnChart, setShowNewsOnChart] = useState(true)
  const [showMA20, setShowMA20] = useState(true)
  const [showMA50, setShowMA50] = useState(true)
  const [showRsi, setShowRsi] = useState(true)
  const [compareSlotIds, setCompareSlotIds] = useState<CompareSlotTuple>(EMPTY_COMPARE_SLOTS)
  const [drawTool, setDrawTool] = useState<DrawTool>('none')
  const [drawings, setDrawings] = useState<DrawingItem[]>([])
  const [showVolume, setShowVolume] = useState(true)
  const tickerShellRef = useRef<HTMLDivElement>(null)
  const [instrumentPickerOpen, setInstrumentPickerOpen] = useState(false)
  const [selectedNews, setSelectedNews] = useState<AssetNewsItem | null>(null)
  const [selectedBarTime, setSelectedBarTime] = useState<UTCTimestamp | null>(null)
  const [comparisonSeriesByAsset, setComparisonSeriesByAsset] = useState<Record<string, CandlePoint[]>>({})

  const { rows: marketRows } = useMarkets({
    page: 0,
    size: 800,
    category: 'all',
    searchTerm: '',
    sort: 'change1D,desc',
    displayCurrency: currency,
  })

  const assets = useMemo<AssetDefinition[]>(
    () => marketRows.map(mapMarketRowToAsset),
    [marketRows],
  )
  const assetsById = useMemo(() => new Map(assets.map((asset) => [asset.id, asset])), [assets])
  const marketBySymbol = useMemo(
    () => new Map(marketRows.map((row) => [row.symbol.toUpperCase(), row])),
    [marketRows],
  )

  const comparisonCandidates = useMemo(() => {
    return assets
      .filter((a) => comparisonListCategory === 'all' || a.type === comparisonListCategory)
      .sort((a, b) => a.symbol.localeCompare(b.symbol))
  }, [assets, comparisonListCategory])

  const selectedSymbol = searchParams.get('symbol')?.toUpperCase()
  const selectedAsset = useMemo(() => {
    if (assets.length === 0) return null
    if (selectedSymbol) {
      const found = assets.find((asset) => normalizeSymbol(asset.symbol) === normalizeSymbol(selectedSymbol))
      if (found) {
        return found
      }
    }
    return assets[0]
  }, [assets, selectedSymbol])

  const overview = useMemo(
    () => (selectedAsset ? (marketBySymbol.get(selectedAsset.symbol.toUpperCase()) ?? null) : null),
    [marketBySymbol, selectedAsset],
  )

  const comparisonAssetIds = useMemo(() => compareSlotIds.filter((id): id is string => id != null), [compareSlotIds])

  const compareSlotAssets = useMemo(
    () => compareSlotIds.map((id) => (id ? (assetsById.get(id) ?? null) : null)),
    [compareSlotIds, assetsById],
  )

  const scopeTag = useMemo(() => {
    const c = (overview?.category ?? '').toLowerCase()
    if (c.includes('bist')) return t('ticker.scopeBist')
    const w = (selectedAsset?.wireCategory ?? '').toUpperCase()
    if (w.includes('NASDAQ') || w.includes('NYSE') || w.includes('US')) return t('ticker.scopeUs')
    return t('ticker.scopeGlobal')
  }, [overview, selectedAsset, t])

  const categoryTag = selectedAsset ? t(`assetSelector.types.${selectedAsset.type}`) : ''

  const { candles: selectedWindowSeries, loading: candlesLoading, error: candlesError, refetch: refetchCandles } = useCandles(
    selectedAsset?.symbol ?? '',
    timeRange,
    { currencyKey: currency, wireCategory: selectedAsset?.wireCategory ?? null },
  )

  const {
    indicators,
    loading: indicatorsLoading,
    error: indicatorsError,
  } = useIndicators(selectedWindowSeries)
  const { data: newsFeed } = useNews(0, 50)

  useEffect(() => {
    if (!selectedAsset?.id) return
    setCompareSlotIds((prev) => {
      const next = prev.map((id) => (id === selectedAsset.id ? null : id)) as CompareSlotTuple
      if (next[0] === prev[0] && next[1] === prev[1] && next[2] === prev[2]) return prev
      return next
    })
  }, [selectedAsset?.id])

  useEffect(() => {
    let cancelled = false
    const targets = comparisonAssetIds
      .filter((assetId) => selectedAsset != null && assetId !== selectedAsset.id)
      .slice(0, 3)
      .map((assetId) => assetsById.get(assetId))
      .filter((asset): asset is AssetDefinition => asset != null)

    if (targets.length === 0) {
      setComparisonSeriesByAsset({})
      return
    }

    Promise.all(
      targets.map(async (asset) => ({
        id: asset.id,
        candles: await fetchCandles(asset.symbol, timeRange, { wireCategory: asset.wireCategory }),
      })),
    )
      .then((rows) => {
        if (cancelled) return
        const next: Record<string, CandlePoint[]> = {}
        rows.forEach((row) => {
          next[row.id] = row.candles
        })
        setComparisonSeriesByAsset(next)
      })
      .catch(() => {
        if (!cancelled) {
          setComparisonSeriesByAsset({})
        }
      })

    return () => {
      cancelled = true
    }
  }, [assetsById, comparisonAssetIds, selectedAsset, timeRange])

  const windowedTradeEvents = useMemo(() => [], [])

  const stats = useMemo(() => {
    const current = selectedWindowSeries[selectedWindowSeries.length - 1]
    const overview = selectedAsset ? marketBySymbol.get(selectedAsset.symbol.toUpperCase()) : null
    const daily = getPerformancePercent(sliceLast(selectedWindowSeries, 24))
    const weekly = getPerformancePercent(sliceLast(selectedWindowSeries, 7 * 24))
    const monthly = getPerformancePercent(sliceLast(selectedWindowSeries, 30 * 24))
    const yearly = getPerformancePercent(sliceLast(selectedWindowSeries, 365 * 24))
    return {
      currentPrice: current?.close ?? overview?.price ?? 0,
      daily: overview?.change1D ?? daily,
      weekly,
      monthly: overview?.change1M ?? monthly,
      yearly: overview?.change1Y ?? yearly,
    }
  }, [marketBySymbol, selectedAsset, selectedWindowSeries])

  const horizonReturns = useMemo(() => {
    const ov = selectedAsset ? (marketBySymbol.get(selectedAsset.symbol.toUpperCase()) ?? null) : null
    const s = selectedWindowSeries
    const pct = (points: typeof s) => {
      if (points.length < 2) return null
      const v = getPerformancePercent(points)
      return Number.isFinite(v) ? v : null
    }
    const pick = (apiVal: number | null | undefined, candlePoints: typeof s) => {
      if (apiVal != null && Number.isFinite(apiVal)) return apiVal
      return pct(candlePoints)
    }
    return {
      weekly: pct(sliceLast(s, 7 * 24)),
      monthly: pick(ov?.change1M, sliceLast(s, 30 * 24)),
      threeMonth: pick(ov?.change3M, sliceLast(s, 90 * 24)),
      sixMonth: pick(ov?.change6M, sliceLast(s, 180 * 24)),
      yearly: pick(ov?.change1Y, sliceLast(s, 365 * 24)),
    }
  }, [marketBySymbol, selectedAsset, selectedWindowSeries])

  const relatedNews = useMemo(() => {
    const target = selectedAsset ? normalizeSymbol(selectedAsset.symbol) : ''
    const mapped = newsFeed
      .filter((item) => (item.relatedSymbols ?? []).map((s) => normalizeSymbol(s)).includes(target))
      .map<AssetNewsItem>((item) => ({
        id: String(item.id),
        assetId: selectedAsset?.id ?? 'unknown',
        title: item.title ?? item.titleOriginal ?? '',
        summary: item.summary ?? item.summaryOriginal ?? '',
        source: item.sourceName,
        impact: item.sentiment,
        createdAt: Math.floor(Date.parse(item.publishedAt) / 1000) as UTCTimestamp,
        reactionPercent1h: item.reactionPercent1h ?? 0,
        relatedAssets: item.relatedSymbols ?? [],
      }))
      .filter((item) => Number.isFinite(item.createdAt))
    return [...mapped].sort((a, b) => b.createdAt - a.createdAt)
  }, [newsFeed, selectedAsset])

  const comparisonLines = useMemo(() => {
    if (selectedAsset == null) return []
    const compareTail = getComparisonTailCount(timeRange)
    return comparisonAssetIds
      .filter((assetId) => assetId !== selectedAsset.id)
      .slice(0, 3)
      .map((assetId, index) => {
        const windowed = sliceLast(comparisonSeriesByAsset[assetId] ?? [], compareTail)
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
      .filter((line) => line.data.length > 0)
  }, [comparisonAssetIds, comparisonSeriesByAsset, selectedAsset, timeRange])

  const tableRows = useMemo(
    () =>
      assets.slice(0, 8).map((asset) => {
        const summary = marketBySymbol.get(asset.symbol.toUpperCase())
        return {
          assetId: asset.id,
          daily: summary?.change1D ?? 0,
          weekly: summary?.change1M != null ? summary.change1M / 4 : 0,
          monthly: summary?.change1M ?? 0,
          yearly: summary?.change1Y ?? 0,
        }
      }),
    [assets, marketBySymbol],
  )

  const handleCompareSlotSet = (slotIndex: number, assetId: string | null) => {
    if (slotIndex < 0 || slotIndex > 2) return
    if (assetId != null && selectedAsset?.id === assetId) return
    setCompareSlotIds((prev) => {
      const next: CompareSlotTuple = [...prev] as CompareSlotTuple
      if (assetId != null) {
        for (let i = 0; i < 3; i++) {
          if (i !== slotIndex && next[i] === assetId) {
            next[i] = null
          }
        }
      }
      next[slotIndex] = assetId
      return next
    })
  }

  const handleAssetChange = (id: string) => {
    setSelectedBarTime(null)
    setSelectedNews(null)
    const asset = assets.find((item) => item.id === id)
    if (asset) {
      const next = new URLSearchParams(searchParams)
      next.set('symbol', asset.symbol.replace('/', '').toUpperCase())
      setSearchParams(next, { replace: true })
    }
  }

  const handlePickAssetFromPopover = (id: string) => {
    handleAssetChange(id)
    setInstrumentPickerOpen(false)
  }

  useEffect(() => {
    setInstrumentPickerOpen(false)
  }, [selectedAsset?.id])

  useEffect(() => {
    if (!instrumentPickerOpen) {
      return undefined
    }
    const onPointerDown = (ev: PointerEvent) => {
      const root = tickerShellRef.current
      if (root && !root.contains(ev.target as Node)) {
        setInstrumentPickerOpen(false)
      }
    }
    const onKeyDown = (ev: KeyboardEvent) => {
      if (ev.key === 'Escape') {
        setInstrumentPickerOpen(false)
      }
    }
    document.addEventListener('pointerdown', onPointerDown, true)
    document.addEventListener('keydown', onKeyDown)
    return () => {
      document.removeEventListener('pointerdown', onPointerDown, true)
      document.removeEventListener('keydown', onKeyDown)
    }
  }, [instrumentPickerOpen])

  const handleRangeChange = (range: TimeRange) => {
    setSelectedBarTime(null)
    setSelectedNews(null)
    setInstrumentPickerOpen(false)
    setTimeRange(range)
  }

  return (
    <section className="fi-analysis-page">
      <div className="fi-analysis-main-grid">
        <div className="fi-analysis-chart-column">
          <div className="fi-analysis-deck-grid">
            <div className="fi-analysis-deck-col fi-analysis-deck-col--ticker">
              <div className="fi-analysis-ticker-shell" ref={tickerShellRef}>
                {selectedAsset ? (
                  <AnalysisTickerBar
                    layout="terminal"
                    asset={selectedAsset}
                    price={stats.currentPrice}
                    dailyPct={stats.daily}
                    dailyHigh={overview?.high24h ?? null}
                    dailyLow={overview?.low24h ?? null}
                    weeklyPct={stats.weekly}
                    yearlyPct={stats.yearly}
                    trendScore={overview?.trendScore ?? null}
                    trendLabel={overview?.trendLabel ?? null}
                    categoryTag={categoryTag}
                    currencyCode={currency}
                    scopeTag={scopeTag}
                    locale={language}
                    currency={currency}
                    assetType={selectedAsset.type}
                    instrumentPickerOpen={instrumentPickerOpen}
                    onInstrumentTriggerClick={() => setInstrumentPickerOpen((open) => !open)}
                    horizonReturns={horizonReturns}
                  />
                ) : null}
                {instrumentPickerOpen && selectedAsset ? (
                  <div
                    id="fi-analysis-instrument-popover"
                    className="fi-analysis-instrument-popover fi-analysis-instrument-popover--bar"
                    role="dialog"
                    aria-modal="true"
                    aria-labelledby="fi-analysis-instrument-popover-title"
                  >
                    <div className="fi-analysis-instrument-popover-bar">
                      <span id="fi-analysis-instrument-popover-title" className="fi-analysis-instrument-popover-bar-title">
                        {t('ticker.instrumentPickerTitle')}
                      </span>
                      <button
                        type="button"
                        className="fi-analysis-instrument-popover-close"
                        onClick={() => setInstrumentPickerOpen(false)}
                        aria-label={t('ticker.closeInstrumentPicker')}
                      >
                        ×
                      </button>
                    </div>
                    <AssetSelector
                      variant="popover"
                      assets={assets}
                      selectedAssetId={selectedAsset.id}
                      comparisonListCategory={comparisonListCategory}
                      onAssetChange={handlePickAssetFromPopover}
                      onComparisonListCategoryChange={setComparisonListCategory}
                    />
                  </div>
                ) : null}
              </div>
            </div>
          </div>

          <div className="fi-analysis-chart-hero">
            <div className="fi-analysis-chart-body">
              <div className="fi-analysis-chart-stack">
                <AnalysisChartFrame
                  timeRange={timeRange}
                  onRangeChange={handleRangeChange}
                  showNewsOnChart={showNewsOnChart}
                  showMA20={showMA20}
                  showMA50={showMA50}
                  showRsi={showRsi}
                  showVolume={showVolume}
                  onToggleNews={() => setShowNewsOnChart((v) => !v)}
                  onToggleMA20={() => setShowMA20((v) => !v)}
                  onToggleMA50={() => setShowMA50((v) => !v)}
                  onToggleRsi={() => setShowRsi((v) => !v)}
                  onToggleVolume={() => setShowVolume((v) => !v)}
                  drawTool={drawTool}
                  onDrawToolChange={setDrawTool}
                  compareSlots={compareSlotAssets}
                  compareCandidates={comparisonCandidates}
                  mainAssetId={selectedAsset?.id ?? null}
                  onCompareSlotSet={handleCompareSlotSet}
                >
                  <AnalysisChart
                    embedded
                    candles={selectedWindowSeries}
                    fitContentKey={`${selectedAsset?.id ?? 'none'}-${timeRange}`}
                    comparisonLines={comparisonLines}
                    showCompare={comparisonLines.length > 0}
                    showMA20={showMA20 && !indicatorsError}
                    showMA50={showMA50 && !indicatorsError}
                    showRSI={showRsi && !indicatorsError}
                    movingAverageData={{ ma20: indicators.ma20, ma50: indicators.ma50 }}
                    rsiData={indicators.rsi}
                    showEventMarkers={showNewsOnChart}
                    showVolume={showVolume}
                    newsItems={relatedNews}
                    tradeEvents={windowedTradeEvents}
                    selectedNewsId={selectedNews?.id ?? null}
                    onSelectNews={setSelectedNews}
                    selectedBarTime={selectedBarTime}
                    onBarSelect={setSelectedBarTime}
                    drawTool={drawTool}
                    drawings={drawings}
                    onAddDrawing={(item) => setDrawings((prev) => [...prev, item])}
                    locale={language}
                    currency={currency}
                    assetType={selectedAsset?.type ?? 'stock'}
                  />
                </AnalysisChartFrame>
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
                {!candlesLoading && !candlesError && indicatorsLoading ? (
                  <p className="fi-empty">{t('analysis:indicatorsLoading')}</p>
                ) : null}
                {!candlesLoading && !candlesError && indicatorsError ? (
                  <p className="fi-empty">{t('analysis:indicatorsDisabled')}</p>
                ) : null}
                {!candlesLoading && !candlesError && selectedWindowSeries.length === 0 ? (
                  <div className="fi-empty-stack">
                    <p className="fi-empty">{t('seriesEmpty')}</p>
                    {selectedAsset?.type === 'fund' || selectedAsset?.type === 'commodity' ? (
                      <p className="fi-empty fi-empty-subtle">{t('seriesEmptyCatalogHint')}</p>
                    ) : null}
                  </div>
                ) : null}
                {assets.length === 0 ? <p className="fi-empty">{t('common:noData')}</p> : null}
              </div>
            </div>
          </div>
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

function getPerformancePercent(series: { close: number }[]) {
  if (series.length < 2) return 0
  const first = series[0].close
  const last = series[series.length - 1].close
  return ((last - first) / first) * 100
}

function getComparisonTailCount(range: TimeRange): number {
  switch (range) {
    case '1h':
      return 120
    case '6h':
      return 300
    case '24h':
      return 400
    case '7d':
      return 450
    case '30d':
      return 700
    case '90d':
      return 1000
    case '1y':
      return 1300
    case '5y':
      return 2500
    default:
      return 500
  }
}
