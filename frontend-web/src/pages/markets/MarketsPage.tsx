import { Fragment, useEffect, useMemo, useRef, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useDocumentTitle } from '../../shared/hooks/useDocumentTitle'
import { useMarkets } from '../../features/markets/hooks/useMarkets'
import { MarketsPortfolioSimulationCard } from './components/MarketsPortfolioSimulationCard'
import { MarketsPageSidebar } from './components/MarketsPageSidebar'
import { useMarketChampions } from './hooks/useMarketChampions'
import { addRowToMarketsPortfolioSimulation } from './lib/marketsPortfolioSimBridge'
import { MARKETS_ROW_DRAG_MIME, serializeMarketsRowDrag } from './lib/marketsRowDrag'
import { fetchInstrumentFundamentals } from '../../features/markets/api/marketService'
import type { InstrumentFundamentals, MarketCategory, MarketOverviewItem } from '../../shared/types/market'
import { useAppPreferences } from '../../shared/preferences/useAppPreferences'
import { marketSortFieldFromUrl, type MarketSortField } from '../../features/markets/lib/marketSort'
import { isAuthenticated } from '../../shared/auth/session'
import { addWatchlistItem, fetchWatchlist, removeWatchlistItem } from '../../features/markets/api/watchlistApi'
import { instrumentHelpRowProps } from '../../components/help/instrumentHelpAttrs'
import { resolveInstrumentDisplayLabel } from '../../features/markets/lib/tefasFundDisplay'

type SortDirection = 'asc' | 'desc'
const DEFAULT_PAGE = 0
const DEFAULT_SIZE = 10
const DEFAULT_CATEGORY = 'all'
const PRICE_FLASH_MS = 500
const PRICE_ANIMATION_MS = 300
const SPARKLINE_WIDTH = 64
const SPARKLINE_HEIGHT = 22
const SPARKLINE_PADDING = 2

type GlobalMarketStatus = 'LIVE' | 'DELAYED' | 'EMPTY'

function normalizeMarketCategory(raw: string | null): MarketCategory {
  const key = (raw ?? '').trim().toLowerCase()
  switch (key) {
    case 'crypto':
      return 'crypto'
    case 'bist':
      return 'bist'
    case 'nasdaq':
      return 'nasdaq'
    case 'forex':
    case 'fx':
      return 'forex'
    case 'metals':
      return 'metals'
    case 'globalfutures':
    case 'global_futures':
    case 'global-futures':
      return 'globalFutures'
    case 'funds':
      return 'funds'
    case 'bond':
    case 'bonds':
    case 'eurobond':
      return 'all'
    case 'all':
    default:
      return 'all'
  }
}

function isBondOverviewRow(row: MarketOverviewItem): boolean {
  const s = row.symbol.trim().toUpperCase()
  return (row.category ?? '').trim().toUpperCase() === 'BOND' || s.startsWith('TRBOND')
}

/** TRBOND1Y → 1; unknown pattern → null */
function trbondTenorYears(symbol: string): number | null {
  const up = symbol.trim().toUpperCase()
  const m = up.match(/^TRBOND(\d+)Y$/)
  if (!m) return null
  const y = Number(m[1])
  return Number.isFinite(y) ? y : null
}

function toSparklinePoints(row: MarketOverviewItem): number[] {
  const safePrice = Number.isFinite(row.price) && row.price > 0 ? row.price : 1
  const trend = (row.change1D ?? row.change24h ?? 0) / 100
  const wave = [0.18, -0.12, 0.1, -0.08, 0.06, -0.04, 0.03]
  const points = wave.map((w, index) => {
    const t = index / (wave.length - 1)
    const base = safePrice * (1 + trend * (t - 1))
    const wobble = safePrice * w * Math.max(Math.abs(trend), 0.01)
    return Math.max(0.0001, base + wobble)
  })
  points.push(safePrice)
  return points
}

function toSparklinePath(points: number[]): string {
  if (points.length === 0) {
    return ''
  }
  const min = Math.min(...points)
  const max = Math.max(...points)
  const range = max - min || 1
  return points
    .map((point, index) => {
      const x = SPARKLINE_PADDING + (index / Math.max(points.length - 1, 1)) * (SPARKLINE_WIDTH - SPARKLINE_PADDING * 2)
      const y =
        SPARKLINE_HEIGHT -
        SPARKLINE_PADDING -
        ((point - min) / range) * (SPARKLINE_HEIGHT - SPARKLINE_PADDING * 2)
      return `${index === 0 ? 'M' : 'L'}${x.toFixed(2)} ${y.toFixed(2)}`
    })
    .join(' ')
}

function formatContractCount(value: number | null | undefined, fmt: Intl.NumberFormat): string {
  if (value == null || !Number.isFinite(value)) {
    return '—'
  }
  return fmt.format(value)
}

function formatFuturesDayRange(row: MarketOverviewItem, usdFmt: Intl.NumberFormat): string {
  const lo = row.dayLow ?? row.low24h
  const hi = row.dayHigh ?? row.high24h
  if (lo == null || hi == null || !Number.isFinite(lo) || !Number.isFinite(hi)) {
    return '—'
  }
  return `${usdFmt.format(lo)} – ${usdFmt.format(hi)}`
}

function formatContractExpiry(iso: string | null | undefined, locale: string): string {
  if (!iso) {
    return '—'
  }
  const d = new Date(iso)
  if (!Number.isFinite(d.getTime())) {
    return '—'
  }
  return d.toLocaleDateString(locale, { year: 'numeric', month: 'short', day: 'numeric' })
}

function trendLabelText(label: MarketOverviewItem['trendLabel']): string {
  switch (label) {
    case 'WEAK':
      return 'Zayif'
    case 'STRONG':
      return 'Guclu'
    case 'VERY_STRONG':
      return 'Asiri Guclu'
    case 'NEUTRAL':
    default:
      return 'Notr'
  }
}

export function MarketsPage() {
  const { t, i18n } = useTranslation('markets')
  const { currency } = useAppPreferences()
  const authenticated = isAuthenticated()
  const [searchParams, setSearchParams] = useSearchParams()
  const [showFavoritesOnly, setShowFavoritesOnly] = useState(false)
  const [favoriteIds, setFavoriteIds] = useState<number[]>([])
  const [favoriteSymbols, setFavoriteSymbols] = useState<string[]>([])
  const [favoriteNotice, setFavoriteNotice] = useState<string | null>(null)
  const [favoritePendingIds, setFavoritePendingIds] = useState<number[]>([])
  const [expandedSymbol, setExpandedSymbol] = useState<string | null>(null)
  const [fundamentalsBySymbol, setFundamentalsBySymbol] = useState<Record<string, InstrumentFundamentals>>({})
  const [fundamentalsLoadingSymbol, setFundamentalsLoadingSymbol] = useState<string | null>(null)
  const [fundamentalsErrorBySymbol, setFundamentalsErrorBySymbol] = useState<Record<string, string>>({})
  const [fundamentalsPrefetchingSymbols, setFundamentalsPrefetchingSymbols] = useState<string[]>([])
  const [priceFlashBySymbol, setPriceFlashBySymbol] = useState<Record<string, 'up' | 'down'>>({})
  const [animatedPriceBySymbol, setAnimatedPriceBySymbol] = useState<Record<string, number>>({})
  const previousPriceBySymbolRef = useRef<Record<string, number>>({})
  const flashTimeoutsRef = useRef<Record<string, number>>({})
  const animationFrameBySymbolRef = useRef<Record<string, number>>({})
  const animatedPriceBySymbolRef = useRef<Record<string, number>>({})
  useDocumentTitle(t('titleDoc'))

  const page = Math.max(Number(searchParams.get('page') ?? DEFAULT_PAGE), 0)
  const size = Math.max(Number(searchParams.get('size') ?? DEFAULT_SIZE), 1)
  const selectedCategory = normalizeMarketCategory(searchParams.get('category') ?? DEFAULT_CATEGORY)
  const isGlobalFutures = selectedCategory === 'globalFutures'
  const tableColCount = 10
  const searchTerm = searchParams.get('q') ?? ''

  const rawSort = searchParams.get('sort') ?? 'change1D,desc'
  const [sortFieldRaw, sortDirectionRaw] = rawSort.split(',')
  const sortField: MarketSortField = marketSortFieldFromUrl(sortFieldRaw)
  const sortDirection: SortDirection = sortDirectionRaw === 'asc' ? 'asc' : 'desc'
  const sortQuery = `${sortField},${sortDirection}`

  const updateParams = (updater: (next: URLSearchParams) => void) => {
    const next = new URLSearchParams(searchParams)
    updater(next)
    setSearchParams(next)
  }

  useEffect(() => {
    const missingDefaults =
      !searchParams.has('page') || !searchParams.has('size') || !searchParams.has('category') || !searchParams.has('sort')
    if (!missingDefaults) {
      return
    }
    const next = new URLSearchParams(searchParams)
    if (!next.has('page')) next.set('page', String(DEFAULT_PAGE))
    if (!next.has('size')) next.set('size', String(DEFAULT_SIZE))
    if (!next.has('category')) next.set('category', 'ALL')
    if (!next.has('sort')) next.set('sort', 'change1D,desc')
    setSearchParams(next, { replace: true })
  }, [searchParams, setSearchParams])

  useEffect(() => {
    const raw = (searchParams.get('category') ?? '').trim().toUpperCase()
    if (raw === 'BOND' || raw === 'BONDS' || raw === 'EUROBOND') {
      const next = new URLSearchParams(searchParams)
      next.set('category', 'ALL')
      next.set('page', '0')
      setSearchParams(next, { replace: true })
    }
  }, [searchParams, setSearchParams])

  const { rows: backendRows, loading, error, refetch, totalElements, totalPages } = useMarkets({
    page,
    size,
    category: selectedCategory,
    searchTerm,
    sort: sortQuery,
    displayCurrency: currency,
  })
  const { champions: sidebarChampions, loading: championsLoading } = useMarketChampions(currency, selectedCategory)
  /** Header-selected currency (converted line). */
  const selectedCurrencyFormat = useMemo(
    () =>
      new Intl.NumberFormat(i18n.language, {
        style: 'currency',
        currency,
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
      }),
    [currency, i18n.language],
  )

  const tryNativeFormat = useMemo(
    () =>
      new Intl.NumberFormat(i18n.language, {
        style: 'currency',
        currency: 'TRY',
        currencyDisplay: 'narrowSymbol',
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
      }),
    [i18n.language],
  )

  /** JPYTRY: ~0.28 TRY per 1 JPY — show kuruş precision, not 28 (per 100 JPY). */
  const jpyTryNativeFormat = useMemo(
    () =>
      new Intl.NumberFormat(i18n.language, {
        style: 'currency',
        currency: 'TRY',
        currencyDisplay: 'narrowSymbol',
        minimumFractionDigits: 4,
        maximumFractionDigits: 4,
      }),
    [i18n.language],
  )

  const usdNativeFormat = useMemo(
    () =>
      new Intl.NumberFormat(i18n.language, {
        style: 'currency',
        currency: 'USD',
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
      }),
    [i18n.language],
  )

  const bondYieldNumberFormat = useMemo(
    () =>
      new Intl.NumberFormat(i18n.language, {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
      }),
    [i18n.language],
  )

  /** Narrow symbol for the selected header currency (second price column). */
  const headerCurrencySymbol = useMemo(() => {
    try {
      const parts = new Intl.NumberFormat(i18n.language, {
        style: 'currency',
        currency,
        currencyDisplay: 'narrowSymbol',
      }).formatToParts(1)
      return parts.find((p) => p.type === 'currency')?.value ?? currency
    } catch {
      return currency
    }
  }, [currency, i18n.language])

  const percentFormat = useMemo(
    () =>
      new Intl.NumberFormat(i18n.language, {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
        signDisplay: 'always',
      }),
    [i18n.language],
  )
  const percentFormatFx = useMemo(
    () =>
      new Intl.NumberFormat(i18n.language, {
        minimumFractionDigits: 3,
        maximumFractionDigits: 3,
        signDisplay: 'always',
      }),
    [i18n.language],
  )
  const compactIntegerFormat = useMemo(
    () =>
      new Intl.NumberFormat(i18n.language, {
        notation: 'compact',
        maximumFractionDigits: 2,
      }),
    [i18n.language],
  )

  const isRowFavorite = (row: MarketOverviewItem) =>
    (row.instrumentId != null && favoriteIds.includes(row.instrumentId)) || favoriteSymbols.includes(row.symbol)

  const visibleRows = useMemo(
    () =>
      showFavoritesOnly
        ? backendRows.filter(
            (row) =>
              (row.instrumentId != null && favoriteIds.includes(row.instrumentId)) || favoriteSymbols.includes(row.symbol),
          )
        : backendRows,
    [backendRows, favoriteIds, favoriteSymbols, showFavoritesOnly],
  )

  const showBondMaturityHeader = useMemo(
    () =>
      selectedCategory === 'bonds' ||
      (visibleRows.length > 0 && visibleRows.every((row) => isBondOverviewRow(row))),
    [selectedCategory, visibleRows],
  )

  const clampedPage = Math.min(Math.max(page, 0), Math.max(totalPages - 1, 0))

  const showFavoriteLoginNotice = () => {
    const message = t('favoritesLoginRequired')
    setFavoriteNotice(message)
    window.setTimeout(() => {
      setFavoriteNotice((current) => (current === message ? null : current))
    }, 3500)
  }

  const toggleFavorite = async (row: MarketOverviewItem) => {
    if (!authenticated) {
      showFavoriteLoginNotice()
      return
    }
    if (row.instrumentId == null) {
      setFavoriteNotice(
        i18n.language?.toLowerCase().startsWith('tr')
          ? 'Favorites are not available for this asset yet.'
          : i18n.language?.toLowerCase().startsWith('de')
            ? 'Favorites are not available for this asset yet.'
            : 'Favorites are not available for this asset yet.',
      )
      return
    }
    const instrumentId = row.instrumentId
    if (favoritePendingIds.includes(instrumentId)) {
      return
    }
    const currentlyFavorite = isRowFavorite(row)
    setFavoritePendingIds((prev) => [...prev, instrumentId])
    try {
      if (currentlyFavorite) {
        await removeWatchlistItem(instrumentId)
        setFavoriteIds((prev) => prev.filter((id) => id !== instrumentId))
        setFavoriteSymbols((prev) => prev.filter((s) => s !== row.symbol))
      } else {
        await addWatchlistItem(instrumentId)
        setFavoriteIds((prev) => (prev.includes(instrumentId) ? prev : [...prev, instrumentId]))
        setFavoriteSymbols((prev) => (prev.includes(row.symbol) ? prev : [...prev, row.symbol]))
      }
    } catch {
      setFavoriteNotice(
        i18n.language?.toLowerCase().startsWith('tr')
          ? 'Could not update favorite. Please try again.'
          : i18n.language?.toLowerCase().startsWith('de')
            ? 'Could not update favorite. Please try again.'
            : 'Could not update favorite. Please try again.',
      )
    } finally {
      setFavoritePendingIds((prev) => prev.filter((id) => id !== instrumentId))
    }
  }

  useEffect(() => {
    if (!authenticated) {
      setFavoriteIds([])
      setFavoriteSymbols([])
      return
    }
    void fetchWatchlist()
      .then((rows) => {
        setFavoriteIds(rows.map((item) => item.instrumentId))
        setFavoriteSymbols(rows.map((item) => item.symbol))
      })
      .catch(() => {
        // keep current client state if watchlist fetch fails
      })
  }, [authenticated])

  const handleSort = (field: MarketSortField) => {
    const nextDirection: SortDirection = sortField === field && sortDirection === 'asc' ? 'desc' : 'asc'
    updateParams((next) => {
      next.set('sort', `${field},${nextDirection}`)
      next.set('page', '0')
    })
  }

  const loadFundamentals = async (symbol: string) => {
    if (fundamentalsBySymbol[symbol]) {
      return
    }
    setFundamentalsLoadingSymbol(symbol)
    setFundamentalsErrorBySymbol((prev) => {
      const next = { ...prev }
      delete next[symbol]
      return next
    })
    try {
      const payload = await fetchInstrumentFundamentals(symbol)
      setFundamentalsBySymbol((prev) => ({ ...prev, [symbol]: payload }))
    } catch {
      setFundamentalsErrorBySymbol((prev) => ({ ...prev, [symbol]: 'Detaylı finansal veriler şu anda alınamıyor.' }))
    } finally {
      setFundamentalsLoadingSymbol((current) => (current === symbol ? null : current))
    }
  }

  useEffect(() => {
    if (isGlobalFutures) {
      return
    }
    const targets = visibleRows
      .map((row) => row.symbol)
      .filter(
        (symbol) =>
          !fundamentalsBySymbol[symbol] &&
          !fundamentalsPrefetchingSymbols.includes(symbol),
      )
    if (targets.length === 0) {
      return
    }
    const batch = targets.slice(0, 8)
    setFundamentalsPrefetchingSymbols((prev) => [...prev, ...batch])
    Promise.all(
      batch.map(async (symbol) => {
        try {
          const payload = await fetchInstrumentFundamentals(symbol)
          setFundamentalsBySymbol((prev) => ({ ...prev, [symbol]: payload }))
        } catch {
          // keep row without market cap when unavailable
        } finally {
          setFundamentalsPrefetchingSymbols((prev) => prev.filter((item) => item !== symbol))
        }
      }),
    ).catch(() => {
      // no-op
    })
  }, [fundamentalsBySymbol, fundamentalsPrefetchingSymbols, visibleRows, isGlobalFutures])

  const toggleFundamentals = (symbol: string) => {
    if (expandedSymbol === symbol) {
      setExpandedSymbol(null)
      return
    }
    setExpandedSymbol(symbol)
    if (!isGlobalFutures) {
      void loadFundamentals(symbol)
    }
  }

  const formatMetric = (value: number | null | undefined, suffix = '') => {
    if (value == null || !Number.isFinite(value)) return '—'
    return `${compactIntegerFormat.format(value)}${suffix}`
  }
  const currencySymbolOf = (currencyCode: string | null | undefined) => {
    const code = (currencyCode ?? '').trim().toUpperCase()
    if (!code) return ''
    try {
      const parts = new Intl.NumberFormat(i18n.language, {
        style: 'currency',
        currency: code,
        currencyDisplay: 'narrowSymbol',
      }).formatToParts(1)
      return parts.find((p) => p.type === 'currency')?.value ?? code
    } catch {
      return code
    }
  }
  const formatMarketCap = (value: number | null | undefined, currencyCode: string | null | undefined) => {
    if (value == null || !Number.isFinite(value)) return '—'
    const symbol = currencySymbolOf(currencyCode)
    return symbol ? `${symbol}${compactIntegerFormat.format(value)}` : compactIntegerFormat.format(value)
  }
  const hasEquityMetrics = (f: InstrumentFundamentals) =>
    f.marketCapitalization != null || f.peTtm != null || f.epsTtm != null || f.annualStatements.length > 0

  const sortIndicator = (field: MarketSortField) => {
    if (sortField !== field) return ''
    return sortDirection === 'asc' ? ' ▲' : ' ▼'
  }

  const globalMarketStatus = useMemo<GlobalMarketStatus>(() => {
    if (backendRows.length === 0) {
      return 'EMPTY'
    }
    if (backendRows.every((row) => row.freshness === 'LIVE')) {
      return 'LIVE'
    }
    if (backendRows.some((row) => row.freshness === 'STALE')) {
      return 'DELAYED'
    }
    return 'EMPTY'
  }, [backendRows])

  const delayedMinutes = useMemo(() => {
    const latestTimestampMs = backendRows.reduce((max, row) => {
      const ts = row.timestamp ? Date.parse(row.timestamp) : Number.NaN
      if (!Number.isFinite(ts)) {
        return max
      }
      return Math.max(max, ts)
    }, Number.NEGATIVE_INFINITY)
    if (!Number.isFinite(latestTimestampMs)) {
      return null
    }
    return Math.max(0, Math.floor((Date.now() - latestTimestampMs) / 60000))
  }, [backendRows])

  const marketStatusLabel =
    globalMarketStatus === 'LIVE'
      ? 'Live market data'
      : globalMarketStatus === 'DELAYED'
        ? `Delayed data (last update ${delayedMinutes ?? '-'} min ago)`
        : 'No market data available'

  useEffect(() => {
    const previousPrices = previousPriceBySymbolRef.current
    backendRows.forEach((row) => {
      const previousPrice = previousPrices[row.symbol]
      if (previousPrice != null && previousPrice !== row.price) {
        const direction: 'up' | 'down' = row.price > previousPrice ? 'up' : 'down'
        const existingTimeoutId = flashTimeoutsRef.current[row.symbol]
        if (existingTimeoutId != null) {
          window.clearTimeout(existingTimeoutId)
        }
        setPriceFlashBySymbol((prev) => ({ ...prev, [row.symbol]: direction }))
        flashTimeoutsRef.current[row.symbol] = window.setTimeout(() => {
          setPriceFlashBySymbol((prev) => {
            const next = { ...prev }
            delete next[row.symbol]
            return next
          })
          delete flashTimeoutsRef.current[row.symbol]
        }, PRICE_FLASH_MS)
      }
      previousPrices[row.symbol] = row.price
    })
  }, [backendRows])

  useEffect(() => {
    const animatedPrices = animatedPriceBySymbolRef.current
    backendRows.forEach((row) => {
      const existingAnimation = animationFrameBySymbolRef.current[row.symbol]
      if (existingAnimation != null) {
        window.cancelAnimationFrame(existingAnimation)
      }
      const from = animatedPrices[row.symbol] ?? row.price
      const to = row.price
      if (from === to) {
        animatedPrices[row.symbol] = to
        setAnimatedPriceBySymbol((prev) => (prev[row.symbol] === to ? prev : { ...prev, [row.symbol]: to }))
        return
      }
      const startTime = performance.now()
      const tick = (now: number) => {
        const progress = Math.min((now - startTime) / PRICE_ANIMATION_MS, 1)
        const nextValue = from + (to - from) * progress
        animatedPrices[row.symbol] = nextValue
        setAnimatedPriceBySymbol((prev) => ({ ...prev, [row.symbol]: nextValue }))
        if (progress < 1) {
          animationFrameBySymbolRef.current[row.symbol] = window.requestAnimationFrame(tick)
        } else {
          delete animationFrameBySymbolRef.current[row.symbol]
        }
      }
      animationFrameBySymbolRef.current[row.symbol] = window.requestAnimationFrame(tick)
    })
  }, [backendRows])

  useEffect(() => {
    return () => {
      Object.values(flashTimeoutsRef.current).forEach((timeoutId) => window.clearTimeout(timeoutId))
      flashTimeoutsRef.current = {}
      Object.values(animationFrameBySymbolRef.current).forEach((frameId) => window.cancelAnimationFrame(frameId))
      animationFrameBySymbolRef.current = {}
    }
  }, [])

  return (
    <section className="markets-page">
      <div className={`markets-status markets-status-${globalMarketStatus.toLowerCase()}`}>
        <span className="markets-status-dot" />
        <span>{marketStatusLabel}</span>
      </div>


      <MarketsPortfolioSimulationCard />

      <div className="fi-markets-layout">
        <div className="fi-markets-main">
        <article className="card markets-main-card">
          <label className="markets-search-field markets-search-row">
            <span>{t('searchLabel')}</span>
            <input
              type="text"
              value={searchTerm}
              onChange={(event) => {
                updateParams((next) => {
                  next.set('q', event.target.value)
                  next.set('page', '0')
                })
              }}
              placeholder={t('searchPlaceholder')}
            />
          </label>

          <div className="markets-toolbar">
            <div className="markets-filter-group" role="tablist" aria-label={t('categories.aria')}>
              {(
                [
                  'all',
                  'crypto',
                  'bist',
                  'nasdaq',
                  'forex',
                  'metals',
                  'globalFutures',
                  'funds',
                ] as const
              ).map((category) => (
                <button
                  key={category}
                  type="button"
                  className={`markets-filter${selectedCategory === category ? ' markets-filter-active' : ''}`}
                  data-help-i18n-key={`categories.${category}`}
                  data-help-i18n-ns="markets"
                  data-help-term={t(`categories.${category}`)}
                  onClick={() => {
                    updateParams((next) => {
                      next.set('category', category.toUpperCase())
                      next.set('page', '0')
                    })
                  }}
                >
                  {t(`categories.${category}`)}
                </button>
              ))}
            </div>

            <button
              type="button"
              className={`markets-filter${showFavoritesOnly ? ' markets-filter-active' : ''}`}
              onClick={() => {
                if (!authenticated) {
                  if (!showFavoritesOnly) {
                    showFavoriteLoginNotice()
                  } else {
                    setShowFavoritesOnly(false)
                  }
                  return
                }
                setShowFavoritesOnly((prev) => !prev)
              }}
            >
              {t('favoritesOnly')}
            </button>

            <select
              className="markets-size-select"
              value={size}
              onChange={(event) => {
                updateParams((next) => {
                  next.set('size', String(Number(event.target.value)))
                  next.set('page', '0')
                })
              }}
              aria-label={t('pagination.pageSizeAria')}
            >
              <option value={10}>{t('pagination.size10')}</option>
              <option value={20}>{t('pagination.size20')}</option>
              <option value={50}>{t('pagination.size50')}</option>
            </select>
          </div>
          {favoriteNotice ? (
            <div className="markets-status markets-status-delayed" style={{ marginBottom: '0.75rem' }}>
              <span className="markets-status-dot" />
              <span>{favoriteNotice}</span>
            </div>
          ) : null}

          <div className="markets-table-wrap">
            <table className="markets-table">
              <colgroup>
                <col className="markets-col-lead" />
                <col className="markets-col-mcap" />
                <col className="markets-col-price" />
                <col className="markets-col-converted" />
                <col className="markets-col-chg" />
                <col className="markets-col-chg" />
                <col className="markets-col-chg" />
                <col className="markets-col-chg" />
                <col className="markets-col-chg" />
                <col className="markets-col-trend" />
              </colgroup>
              <thead>
                <tr>
                  <th className="markets-th-symbol-lead">
                    <button type="button" className="markets-sort-button" onClick={() => handleSort('symbol')}>
                      {t('table.symbol')}
                      {sortIndicator('symbol')}
                    </button>
                  </th>
                  <th className="markets-col-numeric">
                    <button
                      type="button"
                      className="markets-sort-button markets-sort-button-end"
                      onClick={isGlobalFutures ? () => handleSort('volume24h') : undefined}
                    >
                      {isGlobalFutures ? t('table.futuresVolume') : t('table.marketCap', { defaultValue: 'Piyasa Degeri' })}
                      {isGlobalFutures ? sortIndicator('volume24h') : null}
                    </button>
                  </th>
                  {isGlobalFutures ? (
                    <th className="markets-col-numeric">
                      <button
                        type="button"
                        className="markets-sort-button markets-sort-button-end"
                        onClick={() => handleSort('openInterest')}
                      >
                        {t('table.futuresOpenInterest')}
                        {sortIndicator('openInterest')}
                      </button>
                    </th>
                  ) : null}
                  <th className="markets-col-numeric">
                    <button
                      type="button"
                      className="markets-sort-button markets-sort-button-end"
                      onClick={() => handleSort('price')}
                      title={t('table.priceHint')}
                    >
                      {t('table.price')}
                      {sortIndicator('price')}
                    </button>
                  </th>
                  <th className="markets-th-currency markets-col-numeric" title={currency} scope="col">
                    <button
                      type="button"
                      className="markets-sort-button markets-sort-button-end markets-th-currency-button"
                      onClick={() => handleSort('displayAmount')}
                      title={showBondMaturityHeader ? t('table.bondMaturitySort') : t('table.priceConvertedSort')}
                      aria-label={showBondMaturityHeader ? t('table.bondMaturitySort') : t('table.priceConvertedSort')}
                    >
                      <span className="markets-th-currency-symbol">
                        {showBondMaturityHeader ? t('table.bondMaturityColumn') : headerCurrencySymbol}
                      </span>
                      {sortIndicator('displayAmount')}
                    </button>
                  </th>
                  {isGlobalFutures ? (
                    <th className="markets-col-numeric">
                      <button
                        type="button"
                        className="markets-sort-button markets-sort-button-end"
                        onClick={() => handleSort('spotSpreadPct')}
                      >
                        {t('table.futuresSpotSpread')}
                        {sortIndicator('spotSpreadPct')}
                      </button>
                    </th>
                  ) : null}
                  <th className="markets-col-numeric">
                    <button
                      type="button"
                      className="markets-sort-button markets-sort-button-end"
                      onClick={() => handleSort('change1D')}
                    >
                      1D
                      {sortIndicator('change1D')}
                    </button>
                  </th>
                  <th className="markets-col-numeric">
                    <button
                      type="button"
                      className="markets-sort-button markets-sort-button-end"
                      onClick={() => handleSort('change1M')}
                    >
                      1M
                      {sortIndicator('change1M')}
                    </button>
                  </th>
                  <th className="markets-col-numeric">
                    <button
                      type="button"
                      className="markets-sort-button markets-sort-button-end"
                      onClick={() => handleSort('change3M')}
                    >
                      3M
                      {sortIndicator('change3M')}
                    </button>
                  </th>
                  {isGlobalFutures ? (
                    <th className="markets-col-numeric">{t('table.futuresDayRange')}</th>
                  ) : (
                    <>
                      <th className="markets-col-numeric">
                        <button
                          type="button"
                          className="markets-sort-button markets-sort-button-end"
                          onClick={() => handleSort('change6M')}
                        >
                          6M
                          {sortIndicator('change6M')}
                        </button>
                      </th>
                      <th className="markets-col-numeric">
                        <button
                          type="button"
                          className="markets-sort-button markets-sort-button-end"
                          onClick={() => handleSort('change1Y')}
                        >
                          1Y
                          {sortIndicator('change1Y')}
                        </button>
                      </th>
                      <th className="markets-col-trend markets-th-trend">
                        <button type="button" className="markets-sort-button" onClick={() => handleSort('trendScore')}>
                          Trend Skoru
                          {sortIndicator('trendScore')}
                        </button>
                      </th>
                    </>
                  )}
                </tr>
              </thead>
              <tbody>
                {loading ? (
                  Array.from({ length: Math.min(size, 6) }).map((_, idx) => (
                    <tr key={`skeleton-${idx}`}>
                      <td colSpan={tableColCount}>
                        <div className="markets-skeleton-row" />
                      </td>
                    </tr>
                  ))
                ) : error ? (
                  <tr>
                    <td colSpan={tableColCount} className="markets-empty">
                      <div className="markets-error-wrap">
                        <span>{error}</span>
                        <button type="button" className="markets-filter" onClick={() => void refetch()}>
                          {t('common:retry')}
                        </button>
                      </div>
                    </td>
                  </tr>
                ) : visibleRows.length > 0 ? (
                  visibleRows.map((row: MarketOverviewItem) => {
                    const displayLabel = resolveInstrumentDisplayLabel(row.symbol, row.name)
                    const isBond = isBondOverviewRow(row)
                    const isPositive = (row.change24h ?? 0) >= 0
                    const percentDisplay = row.category === 'FX' ? percentFormatFx : percentFormat
                    const animatedNat = animatedPriceBySymbol[row.symbol] ?? row.price
                    const nativeQ = row.nativeQuote ?? 'USD'
                    const nativeFmt =
                      row.symbol === 'JPYTRY'
                        ? jpyTryNativeFormat
                        : nativeQ === 'TRY'
                          ? tryNativeFormat
                          : usdNativeFormat
                    const redundantCol =
                      !isBond &&
                      ((nativeQ === 'TRY' && currency === 'TRY') || (nativeQ === 'USD' && currency === 'USD'))
                    const ratio =
                      row.displayAmount != null && row.price > 0 ? row.displayAmount / row.price : null
                    const animatedDisplay =
                      ratio != null && Number.isFinite(ratio) ? ratio * animatedNat : row.displayAmount
                    const showConverted =
                      !isBond &&
                      !redundantCol &&
                      animatedDisplay != null &&
                      Number.isFinite(animatedDisplay) &&
                      row.displayAmount != null
                    const flashClass =
                      priceFlashBySymbol[row.symbol] === 'up'
                        ? 'markets-price-flash-up'
                        : priceFlashBySymbol[row.symbol] === 'down'
                          ? 'markets-price-flash-down'
                          : undefined
                    return (
                      <Fragment key={row.symbol}>
                        <tr
                          {...instrumentHelpRowProps(row.symbol, row.name)}
                          className="instrument-help-row markets-table-row-draggable"
                          draggable
                          onDragStart={(event) => {
                            event.dataTransfer.setData(
                              MARKETS_ROW_DRAG_MIME,
                              serializeMarketsRowDrag({
                                symbol: row.symbol,
                                name: displayLabel.name,
                                category: row.category,
                              }),
                            )
                            event.dataTransfer.effectAllowed = 'copy'
                          }}
                        >
                          <td className="markets-symbol-lead-cell">
                            <div className="markets-symbol-lead">
                              <button
                                type="button"
                                aria-label={expandedSymbol === row.symbol ? 'Detayları kapat' : 'Detayları aç'}
                                className={`markets-expand-toggle${expandedSymbol === row.symbol ? ' markets-expand-toggle-open' : ''}`}
                                onClick={() => toggleFundamentals(row.symbol)}
                              >
                                {expandedSymbol === row.symbol ? '⌄' : '›'}
                              </button>
                              <button
                                type="button"
                                className="markets-add-to-sim"
                                aria-label={t('addToPortfolioSim', { symbol: displayLabel.symbol })}
                                title={t('addToPortfolioSim', { symbol: displayLabel.symbol })}
                                onMouseDown={(event) => event.stopPropagation()}
                                onClick={() =>
                                  addRowToMarketsPortfolioSimulation({
                                    symbol: row.symbol,
                                    name: displayLabel.name,
                                    category: row.category,
                                  })
                                }
                              >
                                +
                              </button>
                              <button
                                type="button"
                                aria-label={isRowFavorite(row) ? t('unfavorite') : t('favorite')}
                                className={`markets-star${isRowFavorite(row) ? ' markets-star-active' : ''}`}
                                onMouseDown={(event) => event.stopPropagation()}
                                onClick={() => void toggleFavorite(row)}
                                disabled={row.instrumentId != null && favoritePendingIds.includes(row.instrumentId)}
                              >
                                {isRowFavorite(row) ? '★' : '☆'}
                              </button>
                              <div className="markets-symbol-cell">
                                <strong>
                                  {displayLabel.symbol}
                                  {displayLabel.isTefasFund ? (
                                    <span className="markets-tefas-badge">TEFAS</span>
                                  ) : null}
                                  {row.freshness === 'STALE' ? (
                                    <span className="markets-freshness-badge">Delayed data</span>
                                  ) : null}
                                </strong>
                                <span>{displayLabel.name}</span>
                              </div>
                            </div>
                          </td>
                        <td className={`markets-col-numeric markets-price-native-cell${flashClass ? ` ${flashClass}` : ''}`}>
                          {isGlobalFutures
                            ? formatContractCount(row.volume24h, compactIntegerFormat)
                            : formatMarketCap(
                                fundamentalsBySymbol[row.symbol]?.marketCapitalization,
                                fundamentalsBySymbol[row.symbol]?.currency ?? row.nativeQuote,
                              )}
                        </td>
                        {isGlobalFutures ? (
                          <td className="markets-col-numeric">
                            {formatContractCount(row.openInterest, compactIntegerFormat)}
                          </td>
                        ) : null}
                        <td className={`markets-col-numeric markets-price-native-cell${flashClass ? ` ${flashClass}` : ''}`}>
                          {isBond
                            ? `%${bondYieldNumberFormat.format(animatedNat)}`
                            : nativeFmt.format(animatedNat)}
                        </td>
                        <td className="markets-col-numeric markets-price-converted-cell">
                          {isBond ? (
                            (() => {
                              const y = trbondTenorYears(row.symbol)
                              return y == null ? (
                                <span className="markets-price-converted-missing">—</span>
                              ) : (
                                <span className="markets-bond-tenor">{t('table.bondTenorYears', { years: y })}</span>
                              )
                            })()
                          ) : redundantCol ? (
                            selectedCurrencyFormat.format(animatedNat)
                          ) : showConverted ? (
                            selectedCurrencyFormat.format(animatedDisplay)
                          ) : (
                            <span className="markets-price-converted-missing">—</span>
                          )}
                        </td>
                        {isGlobalFutures ? (
                          <td
                            className={`markets-col-numeric ${
                              (row.spotSpreadPct ?? 0) >= 0 ? 'markets-positive' : 'markets-negative'
                            }`}
                          >
                            {row.spotSpreadPct != null && Number.isFinite(row.spotSpreadPct)
                              ? percentDisplay.format(row.spotSpreadPct)
                              : '—'}
                          </td>
                        ) : null}
                        <td className={`markets-col-numeric ${isPositive ? 'markets-positive' : 'markets-negative'}`}>
                          {percentDisplay.format(row.change1D ?? 0)}
                        </td>
                        <td
                          className={`markets-col-numeric ${(row.change1M ?? 0) >= 0 ? 'markets-positive' : 'markets-negative'}`}
                        >
                          {percentDisplay.format(row.change1M ?? 0)}
                        </td>
                        <td
                          className={`markets-col-numeric ${(row.change3M ?? 0) >= 0 ? 'markets-positive' : 'markets-negative'}`}
                        >
                          {percentDisplay.format(row.change3M ?? 0)}
                        </td>
                        {isGlobalFutures ? (
                          <td className="markets-col-numeric">{formatFuturesDayRange(row, usdNativeFormat)}</td>
                        ) : (
                          <>
                            <td
                              className={`markets-col-numeric ${(row.change6M ?? 0) >= 0 ? 'markets-positive' : 'markets-negative'}`}
                            >
                              {percentDisplay.format(row.change6M ?? 0)}
                            </td>
                            <td
                              className={`markets-col-numeric ${(row.change1Y ?? 0) >= 0 ? 'markets-positive' : 'markets-negative'}`}
                            >
                              {percentDisplay.format(row.change1Y ?? 0)}
                            </td>
                            <td className="markets-col-trend">
                              {(() => {
                                const points = toSparklinePoints(row)
                                const path = toSparklinePath(points)
                                const isTrendUp = points[points.length - 1] >= points[0]
                                const score = row.trendScore
                                const trendClass =
                                  score == null
                                    ? 'markets-trend-badge-neutral'
                                    : score < 35
                                      ? 'markets-trend-badge-weak'
                                      : score < 65
                                        ? 'markets-trend-badge-neutral'
                                        : 'markets-trend-badge-strong'
                                const tooltip = `P${(row.trendPercentile ?? 0).toFixed(0)} | Medyana gore ${percentDisplay.format(
                                  row.trendRelativeWeekly ?? 0,
                            )}`
                            return (
                              <div className="markets-trend-cell" title={tooltip}>
                                <span className={`markets-trend-badge ${trendClass}`}>
                                  {score != null
                                    ? `${score.toFixed(0)} · ${trendLabelText(row.trendLabel)}`
                                    : '—'}
                                </span>
                                <svg
                                  className="sparkline sparkline-compact"
                                  viewBox={`0 0 ${SPARKLINE_WIDTH} ${SPARKLINE_HEIGHT}`}
                                  aria-label={`${row.symbol} trend`}
                                >
                                  <path
                                    d={path}
                                    className={isTrendUp ? 'sparkline-line-positive' : 'sparkline-line-negative'}
                                  />
                                </svg>
                              </div>
                            )
                          })()}
                          </td>
                          </>
                        )}
                        </tr>
                        {expandedSymbol === row.symbol ? (
                          <tr className="markets-fundamentals-row">
                            <td colSpan={tableColCount}>
                              <div className="markets-fundamentals-panel">
                                {isGlobalFutures ? (
                                  <div className="markets-fundamentals-grid">
                                    <div>
                                      <span>{t('table.futuresExchange')}</span>
                                      <strong>{row.exchangeName ?? row.exchange ?? '—'}</strong>
                                    </div>
                                    <div>
                                      <span>{t('table.futuresUnderlying')}</span>
                                      <strong>{row.underlyingSymbol ?? '—'}</strong>
                                    </div>
                                    <div>
                                      <span>{t('table.futuresExpiry')}</span>
                                      <strong>{formatContractExpiry(row.contractExpiry, i18n.language)}</strong>
                                    </div>
                                    <div>
                                      <span>{t('table.futuresLinkedSpot')}</span>
                                      <strong>{row.linkedSpotSymbol ?? '—'}</strong>
                                    </div>
                                    <div>
                                      <span>{t('table.futuresDayOpen')}</span>
                                      <strong>
                                        {row.dayOpen != null && Number.isFinite(row.dayOpen)
                                          ? usdNativeFormat.format(row.dayOpen)
                                          : '—'}
                                      </strong>
                                    </div>
                                    <div>
                                      <span>{t('table.futuresSpotSpreadAbs')}</span>
                                      <strong>
                                        {row.spotSpreadAbs != null && Number.isFinite(row.spotSpreadAbs)
                                          ? tryNativeFormat.format(row.spotSpreadAbs)
                                          : '—'}
                                      </strong>
                                    </div>
                                  </div>
                                ) : fundamentalsLoadingSymbol === row.symbol ? (
                                  <div className="markets-skeleton-row" />
                                ) : fundamentalsErrorBySymbol[row.symbol] ? (
                                  <p className="markets-insights-empty">{fundamentalsErrorBySymbol[row.symbol]}</p>
                                ) : fundamentalsBySymbol[row.symbol] ? (
                                  <>
                                    <div className="markets-fundamentals-grid">
                                      <div>
                                        <span>Saglayici</span>
                                        <strong>{fundamentalsBySymbol[row.symbol].provider}</strong>
                                      </div>
                                      <div>
                                        <span>Sirket</span>
                                        <strong>{fundamentalsBySymbol[row.symbol].companyName ?? row.name}</strong>
                                      </div>
                                      <div>
                                        <span>Sektor</span>
                                        <strong>{fundamentalsBySymbol[row.symbol].industry ?? '—'}</strong>
                                      </div>
                                      <div>
                                        <span>Piyasa</span>
                                        <strong>{fundamentalsBySymbol[row.symbol].exchange ?? row.category ?? '—'}</strong>
                                      </div>
                                      <div>
                                        <span>Para Birimi</span>
                                        <strong>{fundamentalsBySymbol[row.symbol].currency ?? '—'}</strong>
                                      </div>
                                      <div>
                                        <span>Ulke</span>
                                        <strong>{fundamentalsBySymbol[row.symbol].country ?? '—'}</strong>
                                      </div>
                                      <div>
                                        <span>Kurulus / IPO</span>
                                        <strong>{fundamentalsBySymbol[row.symbol].ipoDate ?? '—'}</strong>
                                      </div>
                                      {fundamentalsBySymbol[row.symbol].website ? (
                                        <div>
                                          <span>Web</span>
                                          <strong>{fundamentalsBySymbol[row.symbol].website}</strong>
                                        </div>
                                      ) : null}
                                      {hasEquityMetrics(fundamentalsBySymbol[row.symbol]) ? (
                                        <>
                                          <div>
                                            <span>Piyasa Degeri</span>
                                            <strong>
                                              {formatMarketCap(
                                                fundamentalsBySymbol[row.symbol].marketCapitalization,
                                                fundamentalsBySymbol[row.symbol].currency ?? row.nativeQuote,
                                              )}
                                            </strong>
                                          </div>
                                          <div>
                                            <span>F/K (TTM)</span>
                                            <strong>{formatMetric(fundamentalsBySymbol[row.symbol].peTtm)}</strong>
                                          </div>
                                          <div>
                                            <span>EPS (TTM)</span>
                                            <strong>{formatMetric(fundamentalsBySymbol[row.symbol].epsTtm)}</strong>
                                          </div>
                                        </>
                                      ) : null}
                                    </div>
                                    {fundamentalsBySymbol[row.symbol].annualStatements.length > 0 ? (
                                      <div className="markets-fundamentals-financials">
                                        <h4>Yillik Finansal Ozet</h4>
                                        <div className="markets-fundamentals-financials-table">
                                          <table>
                                            <thead>
                                              <tr>
                                                <th>Yil</th>
                                                <th>Ciro</th>
                                                <th>Net Kar</th>
                                                <th>Varlik</th>
                                                <th>Yukumluluk</th>
                                                <th>Operasyonel Nakit Akisi</th>
                                              </tr>
                                            </thead>
                                            <tbody>
                                              {fundamentalsBySymbol[row.symbol].annualStatements.map((item, index) => (
                                                <tr key={`${row.symbol}-fin-${item.year ?? 'na'}-${index}`}>
                                                  <td>{item.year ?? '—'}</td>
                                                  <td>{formatMetric(item.revenue)}</td>
                                                  <td>{formatMetric(item.netIncome)}</td>
                                                  <td>{formatMetric(item.totalAssets)}</td>
                                                  <td>{formatMetric(item.totalLiabilities)}</td>
                                                  <td>{formatMetric(item.operatingCashFlow)}</td>
                                                </tr>
                                              ))}
                                            </tbody>
                                          </table>
                                        </div>
                                      </div>
                                    ) : null}
                                  </>
                                ) : (
                                  <p className="markets-insights-empty">Detayli veri bulunamadi.</p>
                                )}
                              </div>
                            </td>
                          </tr>
                        ) : null}
                      </Fragment>
                    )
                  })
                ) : (
                  <tr>
                    <td colSpan={tableColCount} className="markets-empty">
                      {t('noMatches')}
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
          <div className="markets-pagination">
            <button
              type="button"
              className="markets-filter"
              disabled={clampedPage <= 0}
              onClick={() =>
                updateParams((next) => {
                  next.set('page', String(clampedPage - 1))
                })
              }
            >
              {t('pagination.prev')}
            </button>
            <span>
              {t('pagination.summary', {
                page: clampedPage + 1,
                totalPages: Math.max(totalPages, 1),
                totalElements,
              })}
            </span>
            <button
              type="button"
              className="markets-filter"
              disabled={clampedPage >= Math.max(totalPages - 1, 0)}
              onClick={() =>
                updateParams((next) => {
                  next.set('page', String(clampedPage + 1))
                })
              }
            >
              {t('pagination.next')}
            </button>
          </div>
          <p className="markets-last-updated">{t('lastUpdated')}</p>
        </article>
        </div>
        <MarketsPageSidebar champions={sidebarChampions} loading={championsLoading} />
      </div>
    </section>
  )
}
