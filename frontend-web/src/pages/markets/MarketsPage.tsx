import { Fragment, useEffect, useMemo, useRef, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useDocumentTitle } from '../../shared/hooks/useDocumentTitle'
import { useMarkets } from '../../features/markets/hooks/useMarkets'
import { useMarketInsights } from '../../features/markets/hooks/useMarketInsights'
import { fetchInstrumentFundamentals } from '../../features/markets/api/marketService'
import type { InstrumentFundamentals, MarketCategory, MarketOverviewItem } from '../../shared/types/market'
import { useAppPreferences } from '../../shared/preferences/useAppPreferences'
import { marketSortFieldFromUrl, type MarketSortField } from '../../features/markets/lib/marketSort'
import { isAuthenticated } from '../../shared/auth/session'
import { addWatchlistItem, fetchWatchlist, removeWatchlistItem } from '../../features/markets/api/watchlistApi'

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
      return 'forex'
    case 'metals':
      return 'metals'
    case 'globalfutures':
    case 'global_futures':
    case 'global-futures':
      return 'globalFutures'
    case 'funds':
      return 'funds'
    case 'all':
    default:
      return 'all'
  }
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

  const { rows: backendRows, loading, error, refetch, totalElements, totalPages } = useMarkets({
    page,
    size,
    category: selectedCategory,
    searchTerm,
    sort: sortQuery,
    displayCurrency: currency,
  })
  const {
    topGainers,
    topLosers,
    loading: insightsLoading,
    error: insightsError,
    refetch: refetchInsights,
  } = useMarketInsights()

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

  const clampedPage = Math.min(Math.max(page, 0), Math.max(totalPages - 1, 0))

  const showFavoriteLoginNotice = () => {
    const message =
      i18n.language?.toLowerCase().startsWith('tr')
        ? 'Please sign in to use favorites.'
        : i18n.language?.toLowerCase().startsWith('de')
          ? 'Please sign in to use favorites.'
          : 'Please sign in to use favorites.'
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
  }, [fundamentalsBySymbol, fundamentalsPrefetchingSymbols, visibleRows])

  const toggleFundamentals = (symbol: string) => {
    if (expandedSymbol === symbol) {
      setExpandedSymbol(null)
      return
    }
    setExpandedSymbol(symbol)
    void loadFundamentals(symbol)
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
      <div className="markets-hero">
        <p className="markets-kicker">{t('kicker')}</p>
        <h2>{t('title')}</h2>
        <p>{t('lead')}</p>
      </div>
      <div className={`markets-status markets-status-${globalMarketStatus.toLowerCase()}`}>
        <span className="markets-status-dot" />
        <span>{marketStatusLabel}</span>
      </div>

      <div className="markets-layout">
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
              {(['all', 'crypto', 'bist', 'nasdaq', 'forex', 'metals', 'globalFutures', 'funds'] as const).map((category) => (
                <button
                  key={category}
                  type="button"
                  className={`markets-filter${selectedCategory === category ? ' markets-filter-active' : ''}`}
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
              onClick={() => setShowFavoritesOnly((prev) => !prev)}
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
              <thead>
                <tr>
                  <th />
                  <th />
                  <th>
                    <button type="button" className="markets-sort-button" onClick={() => handleSort('symbol')}>
                      {t('table.symbol')}
                      {sortIndicator('symbol')}
                    </button>
                  </th>
                  <th>
                    <button type="button" className="markets-sort-button">
                      Piyasa Degeri
                    </button>
                  </th>
                  <th>
                    <button
                      type="button"
                      className="markets-sort-button"
                      onClick={() => handleSort('price')}
                      title={t('table.priceHint')}
                    >
                      {t('table.price')}
                      {sortIndicator('price')}
                    </button>
                  </th>
                  <th className="markets-th-currency" title={currency} scope="col">
                    <button
                      type="button"
                      className="markets-sort-button markets-th-currency-button"
                      onClick={() => handleSort('displayAmount')}
                      title={t('table.priceConvertedSort')}
                      aria-label={t('table.priceConvertedSort')}
                    >
                      <span className="markets-th-currency-symbol">{headerCurrencySymbol}</span>
                      {sortIndicator('displayAmount')}
                    </button>
                  </th>
                  <th>
                    <button type="button" className="markets-sort-button" onClick={() => handleSort('change1D')}>
                      1D
                      {sortIndicator('change1D')}
                    </button>
                  </th>
                  <th>
                    <button type="button" className="markets-sort-button" onClick={() => handleSort('change1M')}>
                      1M
                      {sortIndicator('change1M')}
                    </button>
                  </th>
                  <th>
                    <button type="button" className="markets-sort-button" onClick={() => handleSort('change3M')}>
                      3M
                      {sortIndicator('change3M')}
                    </button>
                  </th>
                  <th>
                    <button type="button" className="markets-sort-button" onClick={() => handleSort('change6M')}>
                      6M
                      {sortIndicator('change6M')}
                    </button>
                  </th>
                  <th>
                    <button type="button" className="markets-sort-button" onClick={() => handleSort('change1Y')}>
                      1Y
                      {sortIndicator('change1Y')}
                    </button>
                  </th>
                  <th>
                    <button type="button" className="markets-sort-button" onClick={() => handleSort('trendScore')}>
                      Trend Skoru
                      {sortIndicator('trendScore')}
                    </button>
                  </th>
                </tr>
              </thead>
              <tbody>
                {loading ? (
                  Array.from({ length: Math.min(size, 6) }).map((_, idx) => (
                    <tr key={`skeleton-${idx}`}>
                      <td colSpan={12}>
                        <div className="markets-skeleton-row" />
                      </td>
                    </tr>
                  ))
                ) : error ? (
                  <tr>
                    <td colSpan={12} className="markets-empty">
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
                    const isPositive = (row.change24h ?? 0) >= 0
                    const percentDisplay = row.category === 'FX' ? percentFormatFx : percentFormat
                    const animatedNat = animatedPriceBySymbol[row.symbol] ?? row.price
                    const nativeQ = row.nativeQuote ?? 'USD'
                    const nativeFmt = nativeQ === 'TRY' ? tryNativeFormat : usdNativeFormat
                    const redundantCol =
                      (nativeQ === 'TRY' && currency === 'TRY') || (nativeQ === 'USD' && currency === 'USD')
                    const ratio =
                      row.displayAmount != null && row.price > 0 ? row.displayAmount / row.price : null
                    const animatedDisplay =
                      ratio != null && Number.isFinite(ratio) ? ratio * animatedNat : row.displayAmount
                    const showConverted =
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
                        <tr>
                          <td>
                            <button
                              type="button"
                              aria-label={expandedSymbol === row.symbol ? 'Detayları kapat' : 'Detayları aç'}
                              className={`markets-expand-toggle${expandedSymbol === row.symbol ? ' markets-expand-toggle-open' : ''}`}
                              onClick={() => toggleFundamentals(row.symbol)}
                            >
                              {expandedSymbol === row.symbol ? '⌄' : '›'}
                            </button>
                          </td>
                          <td>
                          <button
                            type="button"
                            aria-label={isRowFavorite(row) ? t('unfavorite') : t('favorite')}
                            className={`markets-star${isRowFavorite(row) ? ' markets-star-active' : ''}`}
                            onClick={() => void toggleFavorite(row)}
                            disabled={row.instrumentId != null && favoritePendingIds.includes(row.instrumentId)}
                          >
                            {isRowFavorite(row) ? '★' : '☆'}
                          </button>
                        </td>
                        <td>
                          <div className="markets-symbol-cell">
                            <strong>
                              {row.symbol}
                              {row.freshness === 'STALE' ? (
                                <span className="markets-freshness-badge">Delayed data</span>
                              ) : null}
                            </strong>
                            <span>{row.name}</span>
                          </div>
                        </td>
                        <td className={`markets-price-native-cell${flashClass ? ` ${flashClass}` : ''}`}>
                          {formatMarketCap(
                            fundamentalsBySymbol[row.symbol]?.marketCapitalization,
                            fundamentalsBySymbol[row.symbol]?.currency ?? row.nativeQuote,
                          )}
                        </td>
                        <td className={`markets-price-native-cell${flashClass ? ` ${flashClass}` : ''}`}>
                          {nativeFmt.format(animatedNat)}
                        </td>
                        <td className="markets-price-converted-cell">
                          {redundantCol ? (
                            selectedCurrencyFormat.format(animatedNat)
                          ) : showConverted ? (
                            selectedCurrencyFormat.format(animatedDisplay)
                          ) : (
                            <span className="markets-price-converted-missing">—</span>
                          )}
                        </td>
                        <td className={isPositive ? 'markets-positive' : 'markets-negative'}>
                          {percentDisplay.format(row.change1D ?? 0)}
                        </td>
                        <td className={(row.change1M ?? 0) >= 0 ? 'markets-positive' : 'markets-negative'}>
                          {percentDisplay.format(row.change1M ?? 0)}
                        </td>
                        <td className={(row.change3M ?? 0) >= 0 ? 'markets-positive' : 'markets-negative'}>
                          {percentDisplay.format(row.change3M ?? 0)}
                        </td>
                        <td className={(row.change6M ?? 0) >= 0 ? 'markets-positive' : 'markets-negative'}>
                          {percentDisplay.format(row.change6M ?? 0)}
                        </td>
                        <td className={(row.change1Y ?? 0) >= 0 ? 'markets-positive' : 'markets-negative'}>
                          {percentDisplay.format(row.change1Y ?? 0)}
                        </td>
                        <td>
                          {(() => {
                            const points = toSparklinePoints(row)
                            const path = toSparklinePath(points)
                            const isTrendUp = points[points.length - 1] >= points[0]
                            const score = row.trendScore ?? 0
                            const trendClass =
                              score < 35 ? 'markets-trend-badge-weak' : score < 65 ? 'markets-trend-badge-neutral' : 'markets-trend-badge-strong'
                            const tooltip = `P${(row.trendPercentile ?? 0).toFixed(0)} | Medyana gore ${percentDisplay.format(
                              row.trendRelativeWeekly ?? 0,
                            )}`
                            return (
                              <div className="markets-trend-cell" title={tooltip}>
                                <span className={`markets-trend-badge ${trendClass}`}>
                                  {score.toFixed(0)} · {trendLabelText(row.trendLabel)}
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
                        </tr>
                        {expandedSymbol === row.symbol ? (
                          <tr className="markets-fundamentals-row">
                            <td colSpan={12}>
                              <div className="markets-fundamentals-panel">
                                {fundamentalsLoadingSymbol === row.symbol ? (
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
                    <td colSpan={12} className="markets-empty">
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

        <aside className="markets-insights-column">
          <article className="card markets-insights-card">
            <h3>{t('insights.topGainers')}</h3>
            {insightsLoading ? (
              <p className="markets-insights-empty">{t('common:loading')}</p>
            ) : insightsError ? (
              <div className="markets-error-wrap">
                <span>{t('insights.loadError')}</span>
                <button type="button" className="markets-filter" onClick={() => void refetchInsights()}>
                  {t('common:retry')}
                </button>
              </div>
            ) : topGainers.length === 0 ? (
              <p className="markets-insights-empty">{t('insights.noGainers')}</p>
            ) : (
              <ul className="markets-insights-list">
                {topGainers.map((item) => (
                  <li key={`gainer-${item.symbol}`} className="markets-insights-item">
                    <div>
                      <strong>
                        {item.symbol}
                        {item.freshness === 'STALE' ? (
                          <span className="markets-freshness-badge">Delayed data</span>
                        ) : null}
                      </strong>
                      <span>{item.name}</span>
                    </div>
                    <b className="markets-positive">
                      {item.change24h == null ? '-' : percentFormat.format(item.change24h)}
                    </b>
                  </li>
                ))}
              </ul>
            )}
          </article>

          <article className="card markets-insights-card">
            <h3>{t('insights.topLosers')}</h3>
            {insightsLoading ? (
              <p className="markets-insights-empty">{t('common:loading')}</p>
            ) : insightsError ? (
              <div className="markets-error-wrap">
                <span>{t('insights.loadError')}</span>
                <button type="button" className="markets-filter" onClick={() => void refetchInsights()}>
                  {t('common:retry')}
                </button>
              </div>
            ) : topLosers.length === 0 ? (
              <p className="markets-insights-empty">{t('insights.noLosers')}</p>
            ) : (
              <ul className="markets-insights-list">
                {topLosers.map((item) => (
                  <li key={`loser-${item.symbol}`} className="markets-insights-item">
                    <div>
                      <strong>
                        {item.symbol}
                        {item.freshness === 'STALE' ? (
                          <span className="markets-freshness-badge">Delayed data</span>
                        ) : null}
                      </strong>
                      <span>{item.name}</span>
                    </div>
                    <b className="markets-negative">
                      {item.change24h == null ? '-' : percentFormat.format(item.change24h)}
                    </b>
                  </li>
                ))}
              </ul>
            )}
          </article>
        </aside>
      </div>
    </section>
  )
}
