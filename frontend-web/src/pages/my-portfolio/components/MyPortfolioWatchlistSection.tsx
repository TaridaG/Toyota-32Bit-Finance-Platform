import { useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { fetchCandles, type AnalysisRange } from '../../../features/analysis/api/analysisService'
import { addWatchlistItem, fetchWatchlist } from '../../../features/markets/api/watchlistApi'
import { useMarkets } from '../../../features/markets/hooks/useMarkets'
import type { MarketOverviewItem } from '../../../shared/types/market'
import { instrumentHelpRowProps } from '../../../components/help/instrumentHelpAttrs'

const watchlistRanges: AnalysisRange[] = ['24h', '7d', '30d', '90d', '1y']

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

type Props = {
  currencyFormat: Intl.NumberFormat
  percentFormat: Intl.NumberFormat
}

export function MyPortfolioWatchlistSection({ currencyFormat, percentFormat }: Props) {
  const { t } = useTranslation('portfolio')
  const [watchlistSymbols, setWatchlistSymbols] = useState<string[]>([])
  const [watchlistLoading, setWatchlistLoading] = useState(false)
  const [watchlistError, setWatchlistError] = useState<string | null>(null)
  const [expandedSymbol, setExpandedSymbol] = useState<string | null>(null)
  const [expandedRange, setExpandedRange] = useState<AnalysisRange>('30d')
  const [expandedPrices, setExpandedPrices] = useState<number[]>([])
  const [expandedChartLoading, setExpandedChartLoading] = useState(false)
  const [expandedChartError, setExpandedChartError] = useState<string | null>(null)
  const [catalogSearch, setCatalogSearch] = useState('')
  const [catalogCategory, setCatalogCategory] = useState<'all' | 'bist' | 'nasdaq' | 'crypto' | 'forex' | 'metals' | 'funds'>('all')
  const [catalogPendingId, setCatalogPendingId] = useState<number | null>(null)
  const [catalogNotice, setCatalogNotice] = useState<string | null>(null)

  const { rows: marketRows, loading: marketRowsLoading } = useMarkets({
    page: 0,
    size: 300,
    category: 'all',
    searchTerm: '',
    sort: 'change1D,desc',
    displayCurrency: 'USD',
  })

  const watchlistRows = useMemo<MarketOverviewItem[]>(
    () => marketRows.filter((row) => watchlistSymbols.includes(row.symbol.toUpperCase())),
    [marketRows, watchlistSymbols],
  )
  const catalogRows = useMemo(() => {
    const q = catalogSearch.trim().toLowerCase()
    return marketRows
      .filter((row) => {
        if (catalogCategory === 'all') {
          return true
        }
        const c = (row.category ?? '').toUpperCase()
        const source = (row as { source?: string | null }).source?.toUpperCase() ?? ''
        if (catalogCategory === 'bist') return c === 'STOCK' && source === 'YAHOO'
        if (catalogCategory === 'nasdaq') return c === 'STOCK' && source === 'FINNHUB'
        if (catalogCategory === 'crypto') return c === 'CRYPTO'
        if (catalogCategory === 'forex') return c === 'FX'
        if (catalogCategory === 'funds') return c === 'FUND'
        if (catalogCategory === 'metals') return c === 'METAL'
        return true
      })
      .filter((row) => (q.length === 0 ? true : `${row.symbol} ${row.name}`.toLowerCase().includes(q)))
      .slice(0, 14)
  }, [catalogCategory, catalogSearch, marketRows])

  useEffect(() => {
    setWatchlistLoading(true)
    setWatchlistError(null)
    void fetchWatchlist()
      .then((rows) => {
        setWatchlistSymbols(rows.map((item) => item.symbol.toUpperCase()))
      })
      .catch(() => {
        setWatchlistError(t('watchlist.loadError'))
      })
      .finally(() => {
        setWatchlistLoading(false)
      })
  }, [t])

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
    () => watchlistRows.find((row) => row.symbol === expandedSymbol) ?? null,
    [expandedSymbol, watchlistRows],
  )
  const expandedPath = useMemo(() => chartPathFromPrices(expandedPrices), [expandedPrices])
  const addToWatchlist = async (row: MarketOverviewItem) => {
    if (row.instrumentId == null || watchlistSymbols.includes(row.symbol.toUpperCase()) || catalogPendingId != null) {
      return
    }
    setCatalogPendingId(row.instrumentId)
    setCatalogNotice(null)
    try {
      await addWatchlistItem(row.instrumentId)
      setWatchlistSymbols((prev) => (prev.includes(row.symbol.toUpperCase()) ? prev : [...prev, row.symbol.toUpperCase()]))
      setCatalogNotice(t('watchlist.addPanel.added'))
    } catch {
      setCatalogNotice(t('watchlist.addPanel.addError'))
    } finally {
      setCatalogPendingId(null)
    }
  }

  return (
    <article className="card">
      <div className="my-portfolio-card-head">
        <h3>{t('watchlist.title')}</h3>
        <span>{t('watchlist.subtitle')}</span>
      </div>

      {watchlistLoading || marketRowsLoading ? (
        <div className="markets-skeleton-row" />
      ) : watchlistError ? (
        <div className="markets-error-wrap">
          <span>{watchlistError}</span>
        </div>
      ) : (
        <div className="my-portfolio-watchlist-layout">
          <div className="my-portfolio-watchlist-left">
            {watchlistRows.length === 0 ? (
              <p className="markets-empty">{t('watchlist.empty')}</p>
            ) : (
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
                    {watchlistRows.map((row) => (
                      <tr
                        key={row.symbol}
                        {...instrumentHelpRowProps(row.symbol, row.name)}
                        className={`instrument-help-row${
                          expandedSymbol === row.symbol ? ' my-portfolio-watchlist-active-row' : ''
                        }`}
                        onClick={() => {
                          setExpandedSymbol((prev) => (prev === row.symbol ? null : row.symbol))
                        }}
                        style={{ cursor: 'pointer' }}
                      >
                        <td><strong>{row.symbol}</strong></td>
                        <td>{row.name}</td>
                        <td>{currencyFormat.format(row.price)}</td>
                        <td className={(row.change1D ?? 0) >= 0 ? 'markets-positive' : 'markets-negative'}>
                          {percentFormat.format(row.change1D ?? 0)}
                        </td>
                        <td className={(row.change1M ?? 0) >= 0 ? 'markets-positive' : 'markets-negative'}>
                          {percentFormat.format(row.change1M ?? 0)}
                        </td>
                        <td className={(row.change1Y ?? 0) >= 0 ? 'markets-positive' : 'markets-negative'}>
                          {percentFormat.format(row.change1Y ?? 0)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}

            {expandedSymbol ? (
              <section className="my-portfolio-watchlist-chart">
                <div className="my-portfolio-card-head">
                  <h3>
                    {expandedRow?.symbol} - {expandedRow?.name}
                  </h3>
                  <div style={{ display: 'inline-flex', gap: '0.35rem' }}>
                    {watchlistRanges.map((range) => (
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
          </div>
          <aside className="my-portfolio-watchlist-right">
            <h4>{t('watchlist.addPanel.title')}</h4>
            <input
              className="my-portfolio-watchlist-search"
              value={catalogSearch}
              onChange={(e) => setCatalogSearch(e.target.value)}
              placeholder={t('watchlist.addPanel.searchPlaceholder')}
            />
            <div className="my-portfolio-watchlist-cats">
              {(['all', 'bist', 'nasdaq', 'crypto', 'forex', 'metals', 'funds'] as const).map((cat) => (
                <button
                  key={cat}
                  type="button"
                  className={`markets-filter${catalogCategory === cat ? ' markets-filter-active' : ''}`}
                  onClick={() => setCatalogCategory(cat)}
                >
                  {t(`watchlist.addPanel.categories.${cat}`)}
                </button>
              ))}
            </div>
            {catalogNotice ? <p className="my-portfolio-watchlist-notice">{catalogNotice}</p> : null}
            <ul className="my-portfolio-watchlist-catalog">
              {catalogRows.map((row) => {
                const already = watchlistSymbols.includes(row.symbol.toUpperCase())
                return (
                  <li key={`catalog-${row.symbol}`}>
                    <div>
                      <strong>{row.symbol}</strong>
                      <small>{row.name}</small>
                    </div>
                    <button
                      type="button"
                      className="my-portfolio-watchlist-add-btn"
                      disabled={already || row.instrumentId == null || catalogPendingId === row.instrumentId}
                      onClick={() => void addToWatchlist(row)}
                    >
                      {already ? t('watchlist.addPanel.addedShort') : t('watchlist.addPanel.add')}
                    </button>
                  </li>
                )
              })}
            </ul>
            {catalogRows.length === 0 ? <p className="markets-empty">{t('watchlist.addPanel.noResults')}</p> : null}
          </aside>
        </div>
      )}
    </article>
  )
}
