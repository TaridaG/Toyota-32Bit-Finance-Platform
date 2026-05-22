import { useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { fetchCandles, type AnalysisRange } from '../../../features/analysis/api/analysisService'
import { fetchMarketPricesSummary } from '../../../features/markets/api/marketService'
import { fetchWatchlist, type WatchlistItem } from '../../../features/markets/api/watchlistApi'
import { resolveInstrumentDisplayLabel } from '../../../features/markets/lib/tefasFundDisplay'
import { instrumentHelpRowProps } from '../../../components/help/instrumentHelpAttrs'

const FAVORITE_PAGE_SIZE = 10
const chartRanges: AnalysisRange[] = ['24h', '7d', '30d', '90d', '1y']

function chartPathFromPrices(prices: number[], width = 920, height = 260): string {
  if (prices.length === 0) {
    return ''
  }
  const min = Math.min(...prices)
  const max = Math.max(...prices)
  const range = max - min || 1
  return prices
    .map((price, index) => {
      const x = (index / Math.max(1, prices.length - 1)) * width
      const y = height - ((price - min) / range) * height
      return `${index === 0 ? 'M' : 'L'}${x.toFixed(2)} ${y.toFixed(2)}`
    })
    .join(' ')
}

type FavoriteRow = WatchlistItem & {
  price: number
  change1D: number
  change1M: number
  change1Y: number
}

type Props = {
  currencyFormat: Intl.NumberFormat
  percentFormat: Intl.NumberFormat
}

function sortFavorites(items: WatchlistItem[]): WatchlistItem[] {
  return [...items].sort((a, b) => {
    const aTime = a.createdAt ? Date.parse(a.createdAt) : 0
    const bTime = b.createdAt ? Date.parse(b.createdAt) : 0
    if (bTime !== aTime) return bTime - aTime
    return a.symbol.localeCompare(b.symbol)
  })
}

export function MyPortfolioWatchlistSection({ currencyFormat, percentFormat }: Props) {
  const { t } = useTranslation('portfolio')
  const [favorites, setFavorites] = useState<WatchlistItem[]>([])
  const [pageRows, setPageRows] = useState<FavoriteRow[]>([])
  const [page, setPage] = useState(0)
  const [listLoading, setListLoading] = useState(false)
  const [pricesLoading, setPricesLoading] = useState(false)
  const [listError, setListError] = useState<string | null>(null)
  const [expandedSymbol, setExpandedSymbol] = useState<string | null>(null)
  const [expandedRange, setExpandedRange] = useState<AnalysisRange>('30d')
  const [expandedPrices, setExpandedPrices] = useState<number[]>([])
  const [expandedChartLoading, setExpandedChartLoading] = useState(false)
  const [expandedChartError, setExpandedChartError] = useState<string | null>(null)

  const sortedFavorites = useMemo(() => sortFavorites(favorites), [favorites])
  const totalElements = sortedFavorites.length
  const totalPages = totalElements === 0 ? 0 : Math.ceil(totalElements / FAVORITE_PAGE_SIZE)
  const pageSlice = useMemo(
    () => sortedFavorites.slice(page * FAVORITE_PAGE_SIZE, page * FAVORITE_PAGE_SIZE + FAVORITE_PAGE_SIZE),
    [page, sortedFavorites],
  )

  useEffect(() => {
    setListLoading(true)
    setListError(null)
    void fetchWatchlist()
      .then((rows) => {
        setFavorites(rows)
        setPage(0)
        setExpandedSymbol(null)
      })
      .catch(() => {
        setListError(t('watchlist.loadError'))
        setFavorites([])
      })
      .finally(() => {
        setListLoading(false)
      })
  }, [t])

  useEffect(() => {
    if (totalPages > 0 && page >= totalPages) {
      setPage(Math.max(0, totalPages - 1))
    }
  }, [page, totalPages])

  useEffect(() => {
    if (pageSlice.length === 0) {
      setPageRows([])
      return
    }
    let cancelled = false
    setPricesLoading(true)
    const symbols = pageSlice.map((item) => item.symbol)
    void fetchMarketPricesSummary(symbols)
      .then((summaryBySymbol) => {
        if (cancelled) return
        setPageRows(
          pageSlice.map((item) => {
            const summary = summaryBySymbol[item.symbol] ?? summaryBySymbol[item.symbol.toUpperCase()]
            return {
              ...item,
              price: summary?.price ?? 0,
              change1D: summary?.change1D ?? 0,
              change1M: summary?.change1M ?? 0,
              change1Y: summary?.change1Y ?? 0,
            }
          }),
        )
      })
      .catch(() => {
        if (!cancelled) {
          setPageRows(
            pageSlice.map((item) => ({
              ...item,
              price: 0,
              change1D: 0,
              change1M: 0,
              change1Y: 0,
            })),
          )
        }
      })
      .finally(() => {
        if (!cancelled) setPricesLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [pageSlice])

  useEffect(() => {
    if (!expandedSymbol) {
      setExpandedPrices([])
      setExpandedChartError(null)
      return
    }
    setExpandedChartLoading(true)
    setExpandedChartError(null)
    void fetchCandles(expandedSymbol, expandedRange)
      .then((candles) => {
        setExpandedPrices(candles.map((c) => c.close))
      })
      .catch(() => {
        setExpandedChartError(t('watchlist.chartLoadError'))
        setExpandedPrices([])
      })
      .finally(() => {
        setExpandedChartLoading(false)
      })
  }, [expandedRange, expandedSymbol, t])

  const expandedRow = useMemo(
    () => pageRows.find((row) => row.symbol === expandedSymbol) ?? sortedFavorites.find((row) => row.symbol === expandedSymbol) ?? null,
    [expandedSymbol, pageRows, sortedFavorites],
  )
  const expandedPath = useMemo(() => chartPathFromPrices(expandedPrices), [expandedPrices])
  const loading = listLoading || (pricesLoading && pageSlice.length > 0)

  return (
    <article className="card">
      <div className="my-portfolio-card-head my-portfolio-favorites-head">
        <h3>
          <span className="my-portfolio-favorites-star" aria-hidden>
            ★
          </span>{' '}
          {t('watchlist.title')}
        </h3>
      </div>

      {loading ? (
        <div className="markets-skeleton-row" />
      ) : listError ? (
        <div className="markets-error-wrap">
          <span>{listError}</span>
        </div>
      ) : totalElements === 0 ? (
        <p className="markets-empty">{t('watchlist.empty')}</p>
      ) : (
        <>
          <div className="my-portfolio-watchlist-table-wrap">
            <table className="my-portfolio-watchlist-table">
              <colgroup>
                <col style={{ width: '16%' }} />
                <col style={{ width: '34%' }} />
                <col style={{ width: '16%' }} />
                <col style={{ width: '11%' }} />
                <col style={{ width: '11%' }} />
                <col style={{ width: '12%' }} />
              </colgroup>
              <thead>
                <tr>
                  <th>{t('watchlist.columns.symbol')}</th>
                  <th>{t('watchlist.columns.name')}</th>
                  <th>{t('watchlist.columns.price')}</th>
                  <th>1D</th>
                  <th>1M</th>
                  <th>1Y</th>
                </tr>
              </thead>
              <tbody>
                {pageRows.map((row) => {
                  const displayLabel = resolveInstrumentDisplayLabel(row.symbol, row.name ?? row.symbol)
                  return (
                    <tr
                      key={row.instrumentId}
                      {...instrumentHelpRowProps(row.symbol, row.name ?? row.symbol)}
                      className={`instrument-help-row${
                        expandedSymbol === row.symbol ? ' my-portfolio-watchlist-active-row' : ''
                      }`}
                      onClick={() => {
                        setExpandedSymbol((prev) => (prev === row.symbol ? null : row.symbol))
                      }}
                      style={{ cursor: 'pointer' }}
                    >
                      <td>
                        <strong>{displayLabel.symbol}</strong>
                      </td>
                      <td>{displayLabel.name}</td>
                      <td>{currencyFormat.format(row.price)}</td>
                      <td className={row.change1D >= 0 ? 'markets-positive' : 'markets-negative'}>
                        {percentFormat.format(row.change1D)}
                      </td>
                      <td className={row.change1M >= 0 ? 'markets-positive' : 'markets-negative'}>
                        {percentFormat.format(row.change1M)}
                      </td>
                      <td className={row.change1Y >= 0 ? 'markets-positive' : 'markets-negative'}>
                        {percentFormat.format(row.change1Y)}
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>

          {totalElements > 0 ? (
            <div className="fi-pagination">
              <button
                type="button"
                className="fi-filter-chip"
                disabled={page <= 0}
                onClick={() => {
                  setExpandedSymbol(null)
                  setPage((prev) => Math.max(prev - 1, 0))
                }}
              >
                ‹ {t('watchlist.prev')}
              </button>
              <span className="fi-pagination-summary">
                {t('watchlist.paginationSummary', {
                  page: page + 1,
                  totalPages: Math.max(totalPages, 1),
                  count: totalElements,
                })}
              </span>
              <button
                type="button"
                className="fi-filter-chip"
                disabled={page >= Math.max(totalPages - 1, 0)}
                onClick={() => {
                  setExpandedSymbol(null)
                  setPage((prev) => Math.min(prev + 1, Math.max(totalPages - 1, 0)))
                }}
              >
                {t('watchlist.next')} ›
              </button>
            </div>
          ) : null}

          {expandedSymbol ? (
            <section className="my-portfolio-watchlist-chart">
              <div className="my-portfolio-card-head">
                <h3>
                  {expandedRow
                    ? (() => {
                        const label = resolveInstrumentDisplayLabel(expandedRow.symbol, expandedRow.name ?? expandedRow.symbol)
                        return `${label.symbol} — ${label.name}`
                      })()
                    : expandedSymbol}
                </h3>
                <div style={{ display: 'inline-flex', gap: '0.35rem' }}>
                  {chartRanges.map((range) => (
                    <button
                      key={range}
                      type="button"
                      className={`fi-range-chip${expandedRange === range ? ' fi-range-chip-active' : ''}`}
                      onClick={() => setExpandedRange(range)}
                    >
                      {range}
                    </button>
                  ))}
                </div>
              </div>

              {expandedChartLoading ? (
                <div className="markets-skeleton-row" />
              ) : expandedChartError ? (
                <div className="markets-error-wrap">
                  <span>{expandedChartError}</span>
                </div>
              ) : expandedPrices.length === 0 ? (
                <p className="markets-empty">{t('watchlist.chartEmpty')}</p>
              ) : (
                <svg viewBox="0 0 920 260" className="my-portfolio-watchlist-chart-svg" aria-label={expandedSymbol}>
                  <path d={expandedPath} className="my-portfolio-watchlist-chart-line" />
                </svg>
              )}
            </section>
          ) : null}
        </>
      )}
    </article>
  )
}
