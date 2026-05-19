import type { UTCTimestamp } from 'lightweight-charts'
import { useEffect, useMemo, useRef, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useDocumentTitle } from '../../shared/hooks/useDocumentTitle'
import { useTranslation } from 'react-i18next'
import { useAppPreferences } from '../../shared/preferences/useAppPreferences'
import type {
  AssetDefinition,
  AssetNewsItem,
  CandlePoint,
  ChartDisplayType,
  DrawTool,
  DrawingItem,
  TimeRange,
} from './types'
import { AssetSelector } from './components/AssetSelector'
import type { OhlcTooltipState } from './chart/hooks/useCrosshairTooltip'
import { createDefaultDrawColorsByTool, type DrawableTool } from './chart/drawing/drawColors'
import { AnalysisChart } from './components/AnalysisChart'
import { AnalysisChartFrame } from './components/AnalysisChartFrame'
import { ChartDrawingHistoryModal } from './components/ChartDrawingHistoryModal'
import { ChartDrawingLoginPrompt } from './components/ChartDrawingLoginPrompt'
import { ChartDrawingSaveModal } from './components/ChartDrawingSaveModal'
import { ChartHoverInsightCard } from './components/ChartHoverInsightCard'
import { AnalysisTickerBar } from './components/AnalysisTickerBar'
import { AnalysisInflationDepositCard } from './components/AnalysisInflationDepositCard'
import { AnalysisInvestmentSimulationCard } from './components/AnalysisInvestmentSimulationCard'
import { utcTimestampToIsoDay } from './utils/investmentSimulation'
import {
  createChartDrawingSave,
  fetchChartDrawingSave,
  hydrateSavedDrawings,
  type ChartDrawingSaveSummary,
} from '../../features/analysis/api/chartDrawingService'
import { fetchCandles } from '../../features/analysis/api/analysisService'
import { isAuthenticated } from '../../shared/auth/session'
import { useCandles } from '../../features/analysis/hooks/useCandles'
import { useIndicators } from '../../features/analysis/hooks/useIndicators'
import { useAnalysisInstrumentCatalog } from './hooks/useAnalysisInstrumentCatalog'
import { fetchMarketOverviewItemBySymbol } from '../../features/markets/api/marketService'
import { fetchNewsForChart, type NewsApiItem } from '../../features/news/api/newsService'
import {
  catalogRowToOverview,
  overviewRowToAsset,
  resolveAnalysisQuoteCurrency,
} from './utils/analysisCatalog'
import { mapNewsToChartItems, resolveChartNewsCategoryUi } from './utils/chartNews'
import { computeHorizonReturns, trailingCalendarReturnPercent } from './utils/horizonReturns'
import type { MarketCategory, MarketOverviewItem } from '../../shared/types/market'

const comparePalette = ['#f59e0b', '#8b5cf6', '#14b8a6', '#f97316', '#22c55e']

type CompareSlotTuple = [string | null, string | null, string | null]
const EMPTY_COMPARE_SLOTS: CompareSlotTuple = [null, null, null]

function normalizeSymbol(symbol: string): string {
  return symbol.replace(/[^A-Za-z0-9]/g, '').toUpperCase()
}

export function AnalysisPage() {
  const { t } = useTranslation('analysis')
  const { language, currency } = useAppPreferences()
  useDocumentTitle(t('titleDoc'))
  const [searchParams, setSearchParams] = useSearchParams()

  const [chartSegment, setChartSegment] = useState<MarketCategory>('bist')
  const [pickerCategory, setPickerCategory] = useState<MarketCategory>('bist')
  const [pickerSelectedId, setPickerSelectedId] = useState<string | null>(null)
  const [timeRange, setTimeRange] = useState<TimeRange>('24h')
  const [showNewsOnChart, setShowNewsOnChart] = useState(false)
  const [chartNewsFeed, setChartNewsFeed] = useState<NewsApiItem[]>([])
  const [chartNewsLoading, setChartNewsLoading] = useState(false)
  const [showMA20, setShowMA20] = useState(true)
  const [showMA50, setShowMA50] = useState(true)
  const [showRsi, setShowRsi] = useState(true)
  const [compareSlotIds, setCompareSlotIds] = useState<CompareSlotTuple>(EMPTY_COMPARE_SLOTS)
  const [drawTool, setDrawTool] = useState<DrawTool>('none')
  const [drawColorsByTool, setDrawColorsByTool] = useState(createDefaultDrawColorsByTool)
  const [drawings, setDrawings] = useState<DrawingItem[]>([])
  const [selectedDrawingId, setSelectedDrawingId] = useState<string | null>(null)
  const [showVolume, setShowVolume] = useState(true)
  const [chartDisplayType, setChartDisplayType] = useState<ChartDisplayType>('candle')
  const [chartHoverReadout, setChartHoverReadout] = useState<OhlcTooltipState>(null)
  const tickerShellRef = useRef<HTMLDivElement>(null)
  const [instrumentPickerOpen, setInstrumentPickerOpen] = useState(false)
  const [selectedNews, setSelectedNews] = useState<AssetNewsItem | null>(null)
  const [simPurchaseDate, setSimPurchaseDate] = useState('')
  const [simChartPickActive, setSimChartPickActive] = useState(false)
  const [comparisonSeriesByAsset, setComparisonSeriesByAsset] = useState<Record<string, CandlePoint[]>>({})
  const [drawingSaveModalOpen, setDrawingSaveModalOpen] = useState(false)
  const [drawingHistoryModalOpen, setDrawingHistoryModalOpen] = useState(false)
  const [drawingLoginPromptOpen, setDrawingLoginPromptOpen] = useState(false)
  const [drawingSaveLoading, setDrawingSaveLoading] = useState(false)
  const [drawingSaveError, setDrawingSaveError] = useState<string | null>(null)
  const [drawingHistoryLoadingId, setDrawingHistoryLoadingId] = useState<number | null>(null)

  const chartDrawingAuth = isAuthenticated()

  const { assets: catalogAssets, rows: catalogRows, loading: catalogLoading } =
    useAnalysisInstrumentCatalog(chartSegment)

  const {
    assets: pickerCatalogAssets,
    loading: pickerCatalogLoading,
  } = useAnalysisInstrumentCatalog(pickerCategory)

  const [deepLinkedRow, setDeepLinkedRow] = useState<MarketOverviewItem | null>(null)
  const [deepLinkedAsset, setDeepLinkedAsset] = useState<AssetDefinition | null>(null)
  const [deepLinkLoading, setDeepLinkLoading] = useState(false)

  const selectedSymbol = searchParams.get('symbol')?.toUpperCase()

  useEffect(() => {
    if (!selectedSymbol) {
      setDeepLinkedRow(null)
      setDeepLinkLoading(false)
      return
    }
    const alreadyListed = catalogAssets.some(
      (asset) => normalizeSymbol(asset.symbol) === normalizeSymbol(selectedSymbol),
    )
    if (alreadyListed) {
      setDeepLinkedRow(null)
      setDeepLinkedAsset(null)
      setDeepLinkLoading(false)
      return
    }
    let cancelled = false
    setDeepLinkLoading(true)
    void fetchMarketOverviewItemBySymbol(selectedSymbol, currency)
      .then((row) => {
        if (!cancelled && row) {
          setDeepLinkedRow(row)
          setDeepLinkedAsset(overviewRowToAsset(row))
        }
      })
      .finally(() => {
        if (!cancelled) {
          setDeepLinkLoading(false)
        }
      })
    return () => {
      cancelled = true
    }
  }, [catalogAssets, currency, chartSegment, selectedSymbol])

  const assets = useMemo<AssetDefinition[]>(() => {
    if (!deepLinkedAsset) {
      return catalogAssets
    }
    if (catalogAssets.some((a) => a.id === deepLinkedAsset.id)) {
      return catalogAssets
    }
    return [deepLinkedAsset, ...catalogAssets]
  }, [catalogAssets, deepLinkedAsset])

  const assetsById = useMemo(() => new Map(assets.map((asset) => [asset.id, asset])), [assets])
  const marketBySymbol = useMemo(() => {
    const map = new Map<string, MarketOverviewItem>()
    catalogRows.forEach((row) => {
      map.set(row.symbol.toUpperCase(), catalogRowToOverview(row))
    })
    if (deepLinkedRow) {
      map.set(deepLinkedRow.symbol.toUpperCase(), deepLinkedRow)
    }
    return map
  }, [catalogRows, deepLinkedRow])

  const comparisonCandidates = useMemo(() => {
    return [...assets].sort((a, b) => a.symbol.localeCompare(b.symbol))
  }, [assets])

  const selectedAsset = useMemo(() => {
    if (assets.length === 0) {
      return null
    }
    if (selectedSymbol) {
      const found = assets.find((asset) => normalizeSymbol(asset.symbol) === normalizeSymbol(selectedSymbol))
      if (found) {
        return found
      }
      if (deepLinkLoading) {
        return null
      }
      return null
    }
    return assets[0]
  }, [assets, deepLinkLoading, selectedSymbol])

  const overview = useMemo(
    () => (selectedAsset ? (marketBySymbol.get(selectedAsset.symbol.toUpperCase()) ?? null) : null),
    [marketBySymbol, selectedAsset],
  )

  const catalogRowBySymbol = useMemo(() => {
    const map = new Map<string, (typeof catalogRows)[number]>()
    catalogRows.forEach((row) => {
      map.set(row.symbol.toUpperCase(), row)
    })
    return map
  }, [catalogRows])

  const quoteCurrency = useMemo(
    () =>
      resolveAnalysisQuoteCurrency(
        selectedAsset,
        overview ?? deepLinkedRow,
        selectedAsset ? (catalogRowBySymbol.get(selectedAsset.symbol.toUpperCase()) ?? null) : null,
      ),
    [catalogRowBySymbol, deepLinkedRow, overview, selectedAsset],
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
    { wireCategory: selectedAsset?.wireCategory ?? null },
  )

  const {
    indicators,
    loading: indicatorsLoading,
    error: indicatorsError,
  } = useIndicators(selectedWindowSeries)

  useEffect(() => {
    setChartHoverReadout(null)
  }, [selectedAsset?.id, timeRange])

  useEffect(() => {
    setDrawings([])
    setSelectedDrawingId(null)
    setDrawTool('none')
  }, [selectedAsset?.id, timeRange])

  const handleDeleteSelectedDrawing = () => {
    const targetId = selectedDrawingId ?? drawings[drawings.length - 1]?.id ?? null
    if (!targetId) return
    setDrawings((prev) => prev.filter((item) => item.id !== targetId))
    setSelectedDrawingId(null)
  }

  const handleDrawColorChange = (tool: DrawableTool, color: string) => {
    setDrawColorsByTool((prev) => ({ ...prev, [tool]: color }))
  }

  const activeDrawColor = drawTool === 'none' ? null : drawColorsByTool[drawTool]

  const handleAddDrawing = (item: DrawingItem) => {
    setDrawings((prev) => [...prev, item])
    setSelectedDrawingId(null)
  }

  const requireChartDrawingAuth = () => {
    if (!chartDrawingAuth) {
      setDrawingLoginPromptOpen(true)
      return false
    }
    return true
  }

  const handleSaveDrawingsClick = () => {
    if (!requireChartDrawingAuth()) return
    if (drawings.length === 0) {
      setDrawingSaveError(t('chartDrawings.saveEmpty'))
      setDrawingSaveModalOpen(true)
      return
    }
    setDrawingSaveError(null)
    setDrawingSaveModalOpen(true)
  }

  const handleDrawingHistoryClick = () => {
    if (!requireChartDrawingAuth()) return
    if (!selectedAsset) return
    setDrawingHistoryModalOpen(true)
  }

  const handleConfirmDrawingSave = async (name: string) => {
    if (!selectedAsset) return
    setDrawingSaveLoading(true)
    setDrawingSaveError(null)
    try {
      await createChartDrawingSave({
        assetKey: selectedAsset.id,
        assetSymbol: selectedAsset.symbol,
        assetType: selectedAsset.type,
        name,
        drawings,
      })
      setDrawingSaveModalOpen(false)
    } catch {
      setDrawingSaveError(t('chartDrawings.saveError'))
    } finally {
      setDrawingSaveLoading(false)
    }
  }

  const handleSelectSavedDrawing = async (summary: ChartDrawingSaveSummary) => {
    setDrawingHistoryLoadingId(summary.id)
    try {
      const detail = await fetchChartDrawingSave(summary.id)
      setDrawings(hydrateSavedDrawings(detail.drawings))
      setSelectedDrawingId(null)
      setDrawTool('none')
      setDrawingHistoryModalOpen(false)
    } catch {
      /* keep modal open */
    } finally {
      setDrawingHistoryLoadingId(null)
    }
  }

  useEffect(() => {
    const onKeyDown = (event: KeyboardEvent) => {
      const tag = (event.target as HTMLElement | null)?.tagName
      if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT') return

      if ((event.key === 'Delete' || event.key === 'Backspace') && selectedDrawingId) {
        event.preventDefault()
        const id = selectedDrawingId
        setDrawings((prev) => prev.filter((item) => item.id !== id))
        setSelectedDrawingId(null)
      }
      if (event.key === 'Escape') {
        setSelectedDrawingId(null)
      }
    }
    window.addEventListener('keydown', onKeyDown)
    return () => window.removeEventListener('keydown', onKeyDown)
  }, [selectedDrawingId])

  const latestBarReadout = useMemo((): OhlcTooltipState => {
    const last = selectedWindowSeries[selectedWindowSeries.length - 1]
    if (!last) return null
    const ma20Point = indicators.ma20[indicators.ma20.length - 1]
    const ma50Point = indicators.ma50[indicators.ma50.length - 1]
    const rsiPoint = indicators.rsi[indicators.rsi.length - 1]
    return {
      timeLabel: new Date(last.time * 1000).toLocaleString(language),
      open: last.open,
      high: last.high,
      low: last.low,
      close: last.close,
      volume: last.volume,
      ma20: ma20Point && Number.isFinite(ma20Point.value) ? ma20Point.value : undefined,
      ma50: ma50Point && Number.isFinite(ma50Point.value) ? ma50Point.value : undefined,
      rsi: rsiPoint && Number.isFinite(rsiPoint.value) ? rsiPoint.value : undefined,
    }
  }, [indicators.ma20, indicators.ma50, indicators.rsi, language, selectedWindowSeries])

  const chartInsightReadout = chartHoverReadout ?? latestBarReadout
  const chartInsightMode = chartHoverReadout ? 'crosshair' : latestBarReadout ? 'latest' : 'empty'

  const candleWindow = useMemo(() => {
    if (selectedWindowSeries.length === 0) {
      return null
    }
    return {
      fromSec: Number(selectedWindowSeries[0].time),
      toSec: Number(selectedWindowSeries[selectedWindowSeries.length - 1].time),
    }
  }, [selectedWindowSeries])

  useEffect(() => {
    if (!showNewsOnChart || !selectedAsset || !candleWindow) {
      return
    }
    const categoryUi = resolveChartNewsCategoryUi(selectedAsset)
    let cancelled = false
    setChartNewsLoading(true)
    void fetchNewsForChart({
      symbol: selectedAsset.symbol,
      categoryUi,
      fromSec: Math.max(0, candleWindow.fromSec - 3_600),
      toSec: candleWindow.toSec + 86_400,
      language,
    })
      .then((items) => {
        if (!cancelled) {
          setChartNewsFeed(items)
        }
      })
      .catch(() => {
        if (!cancelled) {
          setChartNewsFeed([])
        }
      })
      .finally(() => {
        if (!cancelled) {
          setChartNewsLoading(false)
        }
      })
    return () => {
      cancelled = true
    }
  }, [candleWindow, language, selectedAsset, showNewsOnChart])

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

  const isFundAsset = selectedAsset?.type === 'fund'

  const stats = useMemo(() => {
    const current = selectedWindowSeries[selectedWindowSeries.length - 1]
    const overview = selectedAsset ? marketBySymbol.get(selectedAsset.symbol.toUpperCase()) ?? null : null
    const daily = getPerformancePercent(sliceLast(selectedWindowSeries, 24))
    const weekly = isFundAsset
      ? (trailingCalendarReturnPercent(selectedWindowSeries, 7) ?? 0)
      : getPerformancePercent(sliceLast(selectedWindowSeries, 7 * 24))
    const monthly = isFundAsset
      ? (trailingCalendarReturnPercent(selectedWindowSeries, 30) ?? overview?.change1M ?? 0)
      : (overview?.change1M ?? getPerformancePercent(sliceLast(selectedWindowSeries, 30 * 24)))
    const yearly = isFundAsset
      ? (trailingCalendarReturnPercent(selectedWindowSeries, 365) ?? overview?.change1Y ?? 0)
      : (overview?.change1Y ?? getPerformancePercent(sliceLast(selectedWindowSeries, 365 * 24)))
    return {
      currentPrice: current?.close ?? overview?.price ?? 0,
      daily: overview?.change1D ?? daily,
      weekly,
      monthly,
      yearly,
    }
  }, [isFundAsset, marketBySymbol, selectedAsset, selectedWindowSeries])

  const horizonReturns = useMemo(() => {
    const ov = selectedAsset ? (marketBySymbol.get(selectedAsset.symbol.toUpperCase()) ?? null) : null
    return computeHorizonReturns(selectedWindowSeries, ov, isFundAsset)
  }, [isFundAsset, marketBySymbol, selectedAsset, selectedWindowSeries])

  const relatedNews = useMemo(() => {
    if (!showNewsOnChart || !selectedAsset || selectedWindowSeries.length === 0) {
      return []
    }
    const categoryUi = resolveChartNewsCategoryUi(selectedAsset)
    return mapNewsToChartItems(chartNewsFeed, selectedAsset, categoryUi, selectedWindowSeries)
  }, [chartNewsFeed, selectedAsset, selectedWindowSeries, showNewsOnChart])

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

  const handleChartBarClick = (time: UTCTimestamp) => {
    if (!simChartPickActive) return
    setSimPurchaseDate(utcTimestampToIsoDay(time))
    setSimChartPickActive(false)
  }

  useEffect(() => {
    setSimChartPickActive(false)
    setSimPurchaseDate('')
  }, [selectedAsset?.id])

  const handleAssetChange = (id: string, sourceAssets: AssetDefinition[] = assets) => {
    setSelectedNews(null)
    const asset = sourceAssets.find((item) => item.id === id)
    if (asset) {
      if (asset.marketSegment && asset.marketSegment !== 'all') {
        setChartSegment(asset.marketSegment)
      }
      const next = new URLSearchParams(searchParams)
      next.set('symbol', asset.symbol.replace('/', '').toUpperCase())
      setSearchParams(next, { replace: true })
    }
  }

  const handlePickAssetFromPopover = (id: string) => {
    handleAssetChange(id, pickerCatalogAssets)
    setInstrumentPickerOpen(false)
  }

  const handlePickerCategoryChange = (category: MarketCategory) => {
    setPickerCategory(category)
    setPickerSelectedId(null)
  }

  const lastSyncedSymbolRef = useRef<string | null>(null)
  const pickerOpenSyncedRef = useRef(false)

  useEffect(() => {
    if (!instrumentPickerOpen) {
      pickerOpenSyncedRef.current = false
      return
    }
    if (pickerOpenSyncedRef.current || !selectedAsset) {
      return
    }
    pickerOpenSyncedRef.current = true
    const segment =
      selectedAsset.marketSegment && selectedAsset.marketSegment !== 'all'
        ? selectedAsset.marketSegment
        : chartSegment
    setPickerCategory(segment)
  }, [chartSegment, instrumentPickerOpen, selectedAsset?.id])

  useEffect(() => {
    if (!instrumentPickerOpen || pickerCatalogLoading || !selectedAsset) {
      return
    }
    setPickerSelectedId((prev) => {
      if (prev && pickerCatalogAssets.some((a) => a.id === prev)) {
        return prev
      }
      return pickerCatalogAssets.some((a) => a.id === selectedAsset.id) ? selectedAsset.id : null
    })
  }, [instrumentPickerOpen, pickerCatalogAssets, pickerCatalogLoading, selectedAsset])

  useEffect(() => {
    if (deepLinkedAsset?.marketSegment && deepLinkedAsset.marketSegment !== 'all') {
      setChartSegment(deepLinkedAsset.marketSegment)
    }
  }, [deepLinkedAsset])

  useEffect(() => {
    if (catalogLoading || assets.length === 0) {
      return
    }
    if (selectedSymbol) {
      const found = assets.find((asset) => normalizeSymbol(asset.symbol) === normalizeSymbol(selectedSymbol))
      if (found?.marketSegment && found.marketSegment !== 'all' && lastSyncedSymbolRef.current !== selectedSymbol) {
        setChartSegment(found.marketSegment)
        lastSyncedSymbolRef.current = selectedSymbol
      }
      return
    }
    if (assets.length > 0) {
      const first = assets[0]
      const next = new URLSearchParams(searchParams)
      next.set('symbol', first.symbol.replace('/', '').toUpperCase())
      setSearchParams(next, { replace: true })
    }
  }, [assets, catalogLoading, searchParams, selectedSymbol, setSearchParams])

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
                    currencyCode={quoteCurrency}
                    scopeTag={scopeTag}
                    locale={language}
                    currency={quoteCurrency}
                    assetType={selectedAsset.type}
                    instrumentPickerOpen={instrumentPickerOpen}
                    onInstrumentTriggerClick={() => setInstrumentPickerOpen((open) => !open)}
                    horizonReturns={horizonReturns}
                  />
                ) : deepLinkLoading && selectedSymbol ? (
                  <p className="fi-empty">{t('common:loading')}</p>
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
                      assets={pickerCatalogAssets}
                      selectedAssetId={pickerSelectedId ?? ''}
                      instrumentCategory={pickerCategory}
                      onInstrumentCategoryChange={handlePickerCategoryChange}
                      onAssetChange={handlePickAssetFromPopover}
                      catalogLoading={pickerCatalogLoading}
                      retainOffListSelection={false}
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
                  chartNewsLoading={chartNewsLoading}
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
                  drawColorsByTool={drawColorsByTool}
                  onDrawColorChange={handleDrawColorChange}
                  selectedDrawingId={selectedDrawingId}
                  hasDrawings={drawings.length > 0}
                  showDrawingLibrary={chartDrawingAuth}
                  canSaveDrawings={drawings.length > 0}
                  onSaveDrawingsClick={handleSaveDrawingsClick}
                  onDrawingHistoryClick={handleDrawingHistoryClick}
                  onDeleteSelectedDrawing={handleDeleteSelectedDrawing}
                  compareSlots={compareSlotAssets}
                  compareCandidates={comparisonCandidates}
                  mainAssetId={selectedAsset?.id ?? null}
                  onCompareSlotSet={handleCompareSlotSet}
                  chartType={chartDisplayType}
                  onChartTypeChange={setChartDisplayType}
                >
                  <AnalysisChart
                    embedded
                    candles={selectedWindowSeries}
                    chartType={chartDisplayType}
                    fitContentKey={`${selectedAsset?.id ?? 'none'}-${timeRange}-${chartDisplayType}`}
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
                    barClickEnabled={simChartPickActive}
                    onBarClick={handleChartBarClick}
                    drawTool={drawTool}
                    activeDrawColor={activeDrawColor}
                    drawings={drawings}
                    selectedDrawingId={selectedDrawingId}
                    onAddDrawing={handleAddDrawing}
                    onSelectDrawing={setSelectedDrawingId}
                    onDrawComplete={() => setDrawTool('none')}
                    locale={language}
                    currency={quoteCurrency}
                    assetType={selectedAsset?.type ?? 'stock'}
                    onLiveOhlcForPanel={setChartHoverReadout}
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
              <ChartHoverInsightCard
                symbol={selectedAsset?.symbol ?? null}
                readout={chartInsightReadout}
                mode={chartInsightMode}
                showMA20={showMA20 && !indicatorsError}
                showMA50={showMA50 && !indicatorsError}
                showRsi={showRsi && !indicatorsError}
                locale={language}
                currency={quoteCurrency}
                assetType={selectedAsset?.type ?? 'stock'}
              />
            </div>
          </div>
        </div>
      </div>

      <div className="fi-analysis-bottom-grid">
        <AnalysisInvestmentSimulationCard
          asset={selectedAsset}
          fallbackSeries={selectedWindowSeries}
          displayCurrency={quoteCurrency}
          purchaseDate={simPurchaseDate}
          onPurchaseDateChange={setSimPurchaseDate}
          chartPickActive={simChartPickActive}
          onChartPickActiveChange={setSimChartPickActive}
        />
        <AnalysisInflationDepositCard />
      </div>

      {drawingSaveModalOpen && selectedAsset ? (
        <ChartDrawingSaveModal
          symbol={selectedAsset.symbol}
          drawingCount={drawings.length}
          saving={drawingSaveLoading}
          error={drawingSaveError}
          onClose={() => {
            setDrawingSaveModalOpen(false)
            setDrawingSaveError(null)
          }}
          onSave={(name) => {
            void handleConfirmDrawingSave(name)
          }}
        />
      ) : null}

      {drawingHistoryModalOpen && selectedAsset ? (
        <ChartDrawingHistoryModal
          assetKey={selectedAsset.id}
          symbol={selectedAsset.symbol}
          loadingId={drawingHistoryLoadingId}
          onClose={() => setDrawingHistoryModalOpen(false)}
          onSelect={(summary) => {
            void handleSelectSavedDrawing(summary)
          }}
        />
      ) : null}

      {drawingLoginPromptOpen ? (
        <ChartDrawingLoginPrompt
          onClose={() => setDrawingLoginPromptOpen(false)}
        />
      ) : null}
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
