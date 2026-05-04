import { useEffect, useMemo, useRef, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useDocumentTitle } from '../../shared/hooks/useDocumentTitle'
import { useMarkets } from '../../features/markets/hooks/useMarkets'
import { useMarketInsights } from '../../features/markets/hooks/useMarketInsights'
import type { MarketCategory, MarketOverviewItem } from '../../shared/types/market'
import { useAppPreferences } from '../../shared/preferences/useAppPreferences'

type SortField = 'price' | 'change24h'
type SortDirection = 'asc' | 'desc'
const DEFAULT_PAGE = 0
const DEFAULT_SIZE = 20
const DEFAULT_CATEGORY = 'all'
const PRICE_FLASH_MS = 500
const PRICE_ANIMATION_MS = 300
const SPARKLINE_WIDTH = 80
const SPARKLINE_HEIGHT = 24
const SPARKLINE_PADDING = 2

type GlobalMarketStatus = 'LIVE' | 'DELAYED' | 'EMPTY'

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

export function MarketsPage() {
  const { t, i18n } = useTranslation('markets')
  const { currency } = useAppPreferences()
  const [searchParams, setSearchParams] = useSearchParams()
  const [showFavoritesOnly, setShowFavoritesOnly] = useState(false)
  const [favorites, setFavorites] = useState<string[]>(['BTCUSDT', 'ETHUSDT', 'ASELS'])
  const [priceFlashBySymbol, setPriceFlashBySymbol] = useState<Record<string, 'up' | 'down'>>({})
  const [animatedPriceBySymbol, setAnimatedPriceBySymbol] = useState<Record<string, number>>({})
  const previousPriceBySymbolRef = useRef<Record<string, number>>({})
  const flashTimeoutsRef = useRef<Record<string, number>>({})
  const animationFrameBySymbolRef = useRef<Record<string, number>>({})
  const animatedPriceBySymbolRef = useRef<Record<string, number>>({})
  useDocumentTitle(t('titleDoc'))

  const page = Math.max(Number(searchParams.get('page') ?? DEFAULT_PAGE), 0)
  const size = Math.max(Number(searchParams.get('size') ?? DEFAULT_SIZE), 1)
  const selectedCategory = (searchParams.get('category')?.toLowerCase() ?? DEFAULT_CATEGORY) as MarketCategory
  const searchTerm = searchParams.get('q') ?? ''

  const rawSort = searchParams.get('sort') ?? 'change24h,desc'
  const [sortFieldRaw, sortDirectionRaw] = rawSort.split(',')
  const sortField: SortField = sortFieldRaw === 'price' ? 'price' : 'change24h'
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
    if (!next.has('sort')) next.set('sort', 'change24h,desc')
    setSearchParams(next, { replace: true })
  }, [searchParams, setSearchParams])

  const { rows: backendRows, loading, error, refetch, totalElements, totalPages } = useMarkets({
    page,
    size,
    category: selectedCategory,
    searchTerm,
    sort: sortQuery,
  })
  const {
    topGainers,
    topLosers,
    loading: insightsLoading,
    error: insightsError,
    refetch: refetchInsights,
  } = useMarketInsights()

  const priceFormat = useMemo(
    () =>
      new Intl.NumberFormat(i18n.language, {
        style: 'currency',
        currency,
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
      }),
    [currency, i18n.language],
  )

  const percentFormat = useMemo(
    () =>
      new Intl.NumberFormat(i18n.language, {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
        signDisplay: 'always',
      }),
    [i18n.language],
  )

  const visibleRows = useMemo(
    () => (showFavoritesOnly ? backendRows.filter((row) => favorites.includes(row.symbol)) : backendRows),
    [backendRows, favorites, showFavoritesOnly],
  )

  const clampedPage = Math.min(Math.max(page, 0), Math.max(totalPages - 1, 0))

  const toggleFavorite = (symbol: string) => {
    setFavorites((prev) => (prev.includes(symbol) ? prev.filter((item) => item !== symbol) : [...prev, symbol]))
  }

  const handleSort = (field: SortField) => {
    const nextDirection: SortDirection = sortField === field && sortDirection === 'asc' ? 'desc' : 'asc'
    updateParams((next) => {
      next.set('sort', `${field},${nextDirection}`)
      next.set('page', '0')
    })
  }

  const sortIndicator = (field: SortField) => {
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
              {(['all', 'crypto', 'stocks', 'forex', 'commodities'] as const).map((category) => (
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

          <div className="markets-table-wrap">
            <table className="markets-table">
              <thead>
                <tr>
                  <th />
                  <th>
                    <button type="button" className="markets-sort-button">
                      {t('table.symbol')}
                    </button>
                  </th>
                  <th>
                    <button type="button" className="markets-sort-button" onClick={() => handleSort('price')}>
                      {t('table.price')}
                      {sortIndicator('price')}
                    </button>
                  </th>
                  <th>
                    <button type="button" className="markets-sort-button" onClick={() => handleSort('change24h')}>
                      1D
                      {sortIndicator('change24h')}
                    </button>
                  </th>
                  <th>
                    <button type="button" className="markets-sort-button">
                      1M
                    </button>
                  </th>
                  <th>
                    <button type="button" className="markets-sort-button">
                      3M
                    </button>
                  </th>
                  <th>
                    <button type="button" className="markets-sort-button">
                      6M
                    </button>
                  </th>
                  <th>
                    <button type="button" className="markets-sort-button">
                      1Y
                    </button>
                  </th>
                  <th>
                    <button type="button" className="markets-sort-button">
                      Trend
                    </button>
                  </th>
                </tr>
              </thead>
              <tbody>
                {loading ? (
                  Array.from({ length: Math.min(size, 6) }).map((_, idx) => (
                    <tr key={`skeleton-${idx}`}>
                      <td colSpan={9}>
                        <div className="markets-skeleton-row" />
                      </td>
                    </tr>
                  ))
                ) : error ? (
                  <tr>
                    <td colSpan={9} className="markets-empty">
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
                    return (
                      <tr key={row.symbol}>
                        <td>
                          <button
                            type="button"
                            aria-label={favorites.includes(row.symbol) ? t('unfavorite') : t('favorite')}
                            className={`markets-star${favorites.includes(row.symbol) ? ' markets-star-active' : ''}`}
                            onClick={() => toggleFavorite(row.symbol)}
                          >
                            ▲
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
                        <td
                          className={
                            priceFlashBySymbol[row.symbol] === 'up'
                              ? 'markets-price-flash-up'
                              : priceFlashBySymbol[row.symbol] === 'down'
                                ? 'markets-price-flash-down'
                                : undefined
                          }
                        >
                          {priceFormat.format(animatedPriceBySymbol[row.symbol] ?? row.price)}
                        </td>
                        <td className={isPositive ? 'markets-positive' : 'markets-negative'}>
                          {percentFormat.format(row.change1D ?? 0)}
                        </td>
                        <td className={(row.change1M ?? 0) >= 0 ? 'markets-positive' : 'markets-negative'}>
                          {percentFormat.format(row.change1M ?? 0)}
                        </td>
                        <td className={(row.change3M ?? 0) >= 0 ? 'markets-positive' : 'markets-negative'}>
                          {percentFormat.format(row.change3M ?? 0)}
                        </td>
                        <td className={(row.change6M ?? 0) >= 0 ? 'markets-positive' : 'markets-negative'}>
                          {percentFormat.format(row.change6M ?? 0)}
                        </td>
                        <td className={(row.change1Y ?? 0) >= 0 ? 'markets-positive' : 'markets-negative'}>
                          {percentFormat.format(row.change1Y ?? 0)}
                        </td>
                        <td>
                          {(() => {
                            const points = toSparklinePoints(row)
                            const path = toSparklinePath(points)
                            const isTrendUp = points[points.length - 1] >= points[0]
                            return (
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
                            )
                          })()}
                        </td>
                      </tr>
                    )
                  })
                ) : (
                  <tr>
                    <td colSpan={9} className="markets-empty">
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
