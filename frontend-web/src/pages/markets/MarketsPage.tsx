import { useEffect, useMemo, useState } from 'react'
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

export function MarketsPage() {
  const { t, i18n } = useTranslation('markets')
  const { currency } = useAppPreferences()
  const [searchParams, setSearchParams] = useSearchParams()
  const [showFavoritesOnly, setShowFavoritesOnly] = useState(false)
  const [favorites, setFavorites] = useState<string[]>(['BTCUSDT', 'ETHUSDT', 'ASELS'])
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

  return (
    <section className="markets-page">
      <div className="markets-hero">
        <p className="markets-kicker">{t('kicker')}</p>
        <h2>{t('title')}</h2>
        <p>{t('lead')}</p>
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
                      {t('table.change24h')}
                      {sortIndicator('change24h')}
                    </button>
                  </th>
                  <th>
                    <button type="button" className="markets-sort-button">
                      {t('table.high24h')}
                    </button>
                  </th>
                  <th>
                    <button type="button" className="markets-sort-button">
                      {t('table.low24h')}
                    </button>
                  </th>
                  <th>
                    <button type="button" className="markets-sort-button">
                      {t('table.exchange')}
                    </button>
                  </th>
                </tr>
              </thead>
              <tbody>
                {loading ? (
                  Array.from({ length: Math.min(size, 6) }).map((_, idx) => (
                    <tr key={`skeleton-${idx}`}>
                      <td colSpan={7}>
                        <div className="markets-skeleton-row" />
                      </td>
                    </tr>
                  ))
                ) : error ? (
                  <tr>
                    <td colSpan={7} className="markets-empty">
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
                            <strong>{row.symbol}</strong>
                            <span>{row.name}</span>
                          </div>
                        </td>
                        <td>{priceFormat.format(row.price)}</td>
                        <td className={isPositive ? 'markets-positive' : 'markets-negative'}>
                          {row.change24h == null ? '-' : percentFormat.format(row.change24h)}
                        </td>
                        <td>{row.high24h == null ? '-' : priceFormat.format(row.high24h)}</td>
                        <td>{row.low24h == null ? '-' : priceFormat.format(row.low24h)}</td>
                        <td>{row.category ?? '-'}</td>
                      </tr>
                    )
                  })
                ) : (
                  <tr>
                    <td colSpan={7} className="markets-empty">
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
                      <strong>{item.symbol}</strong>
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
                      <strong>{item.symbol}</strong>
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
