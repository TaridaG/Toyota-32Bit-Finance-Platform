import { useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useDocumentTitle } from '../../shared/hooks/useDocumentTitle'

type MarketCategory = 'all' | 'crypto' | 'stocks' | 'forex' | 'commodities'
type SortKey =
  | 'symbol'
  | 'price'
  | 'previousClose'
  | 'changePercent'
  | 'high24h'
  | 'low24h'
  | 'weightedAverage'
  | 'lotVolume'
  | 'turnover'

type MarketRow = {
  symbol: string
  name: string
  category: Exclude<MarketCategory, 'all'>
  price: number
  changePercent: number
  lotVolumeLabel: string
  turnoverLabel: string
  high24h: number
  low24h: number
  sparkline: number[]
}

type OverviewKey = 'bist' | 'usd' | 'euro' | 'gold'

type OverviewItem = {
  key: OverviewKey
  label: string
  value: number
  changePercent: number
  changeValue: number
  open: number
  previousClose: number
  low: number
  high: number
  sparkline: number[]
}

const marketRows: MarketRow[] = [
  {
    symbol: 'BTC/USDT',
    name: 'Bitcoin',
    category: 'crypto',
    price: 108452.45,
    changePercent: 2.24,
    lotVolumeLabel: '55.767.709',
    turnoverLabel: '734,2 Mio',
    high24h: 109114.0,
    low24h: 106021.83,
    sparkline: [98, 101, 100, 104, 107, 106, 108, 111],
  },
  {
    symbol: 'ETH/USDT',
    name: 'Ethereum',
    category: 'crypto',
    price: 5325.84,
    changePercent: 1.71,
    lotVolumeLabel: '5.913.666',
    turnoverLabel: '120,7 Mio',
    high24h: 5390.2,
    low24h: 5172.2,
    sparkline: [74, 75, 76, 77, 76, 78, 80, 82],
  },
  {
    symbol: 'AAPL',
    name: 'Apple Inc.',
    category: 'stocks',
    price: 214.27,
    changePercent: -0.38,
    lotVolumeLabel: '391.211.670',
    turnoverLabel: '1.744,2 Mio',
    high24h: 216.01,
    low24h: 212.14,
    sparkline: [88, 89, 90, 89, 88, 88, 87, 87],
  },
  {
    symbol: 'TSLA',
    name: 'Tesla Inc.',
    category: 'stocks',
    price: 311.82,
    changePercent: 1.12,
    lotVolumeLabel: '7.278.027',
    turnoverLabel: '325,8 Mio',
    high24h: 315.62,
    low24h: 304.71,
    sparkline: [62, 63, 61, 64, 65, 64, 66, 67],
  },
  {
    symbol: 'EUR/USD',
    name: 'Euro / US Dollar',
    category: 'forex',
    price: 1.0834,
    changePercent: -0.14,
    lotVolumeLabel: '10.880.575',
    turnoverLabel: '149,6 Mio',
    high24h: 1.0872,
    low24h: 1.0817,
    sparkline: [82, 81, 80, 80, 81, 80, 79, 79],
  },
  {
    symbol: 'XAU/USD',
    name: 'Gold Spot',
    category: 'commodities',
    price: 3188.21,
    changePercent: 0.91,
    lotVolumeLabel: '10.025.025',
    turnoverLabel: '40,0 Mio',
    high24h: 3201.8,
    low24h: 3144.9,
    sparkline: [44, 45, 45, 46, 47, 48, 48, 49],
  },
]

const overviewItems: OverviewItem[] = [
  {
    key: 'bist',
    label: 'BIST',
    value: 14.375,
    changePercent: -0.76,
    changeValue: -109.51,
    open: 14.535,
    previousClose: 14.485,
    low: 14.375,
    high: 14.616,
    sparkline: [9.74, 9.7, 9.66, 9.62, 9.58, 9.56, 9.57, 9.55, 9.57, 9.59, 9.6, 9.61, 9.59],
  },
  {
    key: 'usd',
    label: 'USD',
    value: 38.42,
    changePercent: 0.32,
    changeValue: 0.12,
    open: 38.31,
    previousClose: 38.3,
    low: 38.22,
    high: 38.47,
    sparkline: [7.31, 7.32, 7.33, 7.34, 7.35, 7.34, 7.36, 7.38, 7.39, 7.4, 7.41, 7.42, 7.43],
  },
  {
    key: 'euro',
    label: 'EURO',
    value: 41.15,
    changePercent: -0.28,
    changeValue: -0.11,
    open: 41.21,
    previousClose: 41.26,
    low: 41.03,
    high: 41.34,
    sparkline: [7.84, 7.82, 7.8, 7.78, 7.76, 7.74, 7.73, 7.72, 7.71, 7.72, 7.73, 7.72, 7.71],
  },
  {
    key: 'gold',
    label: 'ALTIN',
    value: 4213.4,
    changePercent: 0.44,
    changeValue: 18.4,
    open: 4190.8,
    previousClose: 4195.0,
    low: 4178.4,
    high: 4232.9,
    sparkline: [6.4, 6.44, 6.46, 6.49, 6.53, 6.5, 6.55, 6.57, 6.6, 6.63, 6.61, 6.65, 6.69],
  },
]

function Sparkline({
  values,
  isPositive,
  compact = false,
}: {
  values: number[]
  isPositive: boolean
  compact?: boolean
}) {
  const width = compact ? 80 : 108
  const height = compact ? 24 : 30
  const min = Math.min(...values)
  const max = Math.max(...values)
  const spread = max - min || 1

  const points = values
    .map((value, index) => {
      const x = (index / (values.length - 1)) * width
      const y = height - ((value - min) / spread) * (height - 2)
      return `${x},${y}`
    })
    .join(' ')

  return (
    <svg className={`sparkline${compact ? ' sparkline-compact' : ''}`} viewBox={`0 0 ${width} ${height}`} aria-hidden>
      <polyline className={isPositive ? 'sparkline-line-positive' : 'sparkline-line-negative'} points={points} />
    </svg>
  )
}

export function MarketsPage() {
  const { t, i18n } = useTranslation()
  const [selectedCategory, setSelectedCategory] = useState<MarketCategory>('all')
  const [searchTerm, setSearchTerm] = useState('')
  const [showFavoritesOnly, setShowFavoritesOnly] = useState(false)
  const [selectedOverview, setSelectedOverview] = useState<OverviewKey>('bist')
  const [sortState, setSortState] = useState<{ key: SortKey; direction: 'asc' | 'desc' }>({
    key: 'changePercent',
    direction: 'desc',
  })
  const [favorites, setFavorites] = useState<string[]>(['BTC/USDT', 'ETH/USDT', 'AAPL'])
  useDocumentTitle(t('markets.titleDoc'))

  const priceFormat = useMemo(
    () =>
      new Intl.NumberFormat(i18n.language, {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
      }),
    [i18n.language],
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

  const filteredRows = useMemo(() => {
    const loweredSearch = searchTerm.trim().toLowerCase()

    const categoryFiltered =
      selectedCategory === 'all' ? marketRows : marketRows.filter((row) => row.category === selectedCategory)

    const searched = loweredSearch
      ? categoryFiltered.filter(
          (row) => row.symbol.toLowerCase().includes(loweredSearch) || row.name.toLowerCase().includes(loweredSearch),
        )
      : categoryFiltered

    const favoriteScoped = showFavoritesOnly ? searched.filter((row) => favorites.includes(row.symbol)) : searched

    const toNumberFromLot = (value: string) => Number(value.replaceAll('.', '').replaceAll(',', '.'))
    const toNumberFromTurnover = (value: string) => {
      const cleaned = value.toLowerCase().replaceAll(' ', '').replace('mio', '')
      return Number(cleaned.replace(',', '.'))
    }

    const sorted = [...favoriteScoped].sort((a, b) => {
      const aPreviousClose = a.price / (1 + a.changePercent / 100)
      const bPreviousClose = b.price / (1 + b.changePercent / 100)
      const aWeighted = (a.high24h + a.low24h + a.price) / 3
      const bWeighted = (b.high24h + b.low24h + b.price) / 3

      let result = 0
      switch (sortState.key) {
        case 'symbol':
          result = a.symbol.localeCompare(b.symbol)
          break
        case 'price':
          result = a.price - b.price
          break
        case 'previousClose':
          result = aPreviousClose - bPreviousClose
          break
        case 'changePercent':
          result = a.changePercent - b.changePercent
          break
        case 'high24h':
          result = a.high24h - b.high24h
          break
        case 'low24h':
          result = a.low24h - b.low24h
          break
        case 'weightedAverage':
          result = aWeighted - bWeighted
          break
        case 'lotVolume':
          result = toNumberFromLot(a.lotVolumeLabel) - toNumberFromLot(b.lotVolumeLabel)
          break
        case 'turnover':
          result = toNumberFromTurnover(a.turnoverLabel) - toNumberFromTurnover(b.turnoverLabel)
          break
      }

      return sortState.direction === 'asc' ? result : -result
    })

    return sorted
  }, [favorites, searchTerm, selectedCategory, showFavoritesOnly, sortState.direction, sortState.key])

  const toggleFavorite = (symbol: string) => {
    setFavorites((prev) => (prev.includes(symbol) ? prev.filter((item) => item !== symbol) : [...prev, symbol]))
  }

  const handleSort = (key: SortKey) => {
    setSortState((prev) => {
      if (prev.key === key) {
        return {
          key,
          direction: prev.direction === 'asc' ? 'desc' : 'asc',
        }
      }

      return { key, direction: 'desc' }
    })
  }

  const sortIndicator = (key: SortKey) => {
    if (sortState.key !== key) return ''
    return sortState.direction === 'asc' ? ' ▲' : ' ▼'
  }

  const selectedOverviewItem = overviewItems.find((item) => item.key === selectedOverview) ?? overviewItems[0]
  const overviewPositive = selectedOverviewItem.changePercent >= 0

  return (
    <section className="markets-page">
      <div className="markets-hero">
        <p className="markets-kicker">{t('markets.kicker')}</p>
        <h2>{t('markets.title')}</h2>
        <p>{t('markets.lead')}</p>
      </div>

      <div className="markets-layout">
        <article className="card markets-main-card">
          <label className="markets-search-field markets-search-row">
            <span>{t('markets.searchLabel')}</span>
            <input
              type="text"
              value={searchTerm}
              onChange={(event) => setSearchTerm(event.target.value)}
              placeholder={t('markets.searchPlaceholder')}
            />
          </label>

          <div className="markets-toolbar">
            <div className="markets-filter-group" role="tablist" aria-label={t('markets.categories.aria')}>
              {(['all', 'crypto', 'stocks', 'forex', 'commodities'] as const).map((category) => (
                <button
                  key={category}
                  type="button"
                  className={`markets-filter${selectedCategory === category ? ' markets-filter-active' : ''}`}
                  onClick={() => setSelectedCategory(category)}
                >
                  {t(`markets.categories.${category}`)}
                </button>
              ))}
            </div>

            <button
              type="button"
              className={`markets-filter${showFavoritesOnly ? ' markets-filter-active' : ''}`}
              onClick={() => setShowFavoritesOnly((prev) => !prev)}
            >
              {t('markets.favoritesOnly')}
            </button>
          </div>

          <div className="markets-table-wrap">
            <table className="markets-table">
              <thead>
                <tr>
                  <th />
                  <th>
                    <button type="button" className="markets-sort-button" onClick={() => handleSort('symbol')}>
                      {t('markets.table.symbol')}
                      {sortIndicator('symbol')}
                    </button>
                  </th>
                  <th>
                    <button type="button" className="markets-sort-button" onClick={() => handleSort('price')}>
                      {t('markets.table.price')}
                      {sortIndicator('price')}
                    </button>
                  </th>
                  <th>
                    <button type="button" className="markets-sort-button" onClick={() => handleSort('previousClose')}>
                      {t('markets.table.previousClose')}
                      {sortIndicator('previousClose')}
                    </button>
                  </th>
                  <th>
                    <button type="button" className="markets-sort-button" onClick={() => handleSort('changePercent')}>
                      {t('markets.table.change24h')}
                      {sortIndicator('changePercent')}
                    </button>
                  </th>
                  <th>
                    <button type="button" className="markets-sort-button" onClick={() => handleSort('high24h')}>
                      {t('markets.table.high24h')}
                      {sortIndicator('high24h')}
                    </button>
                  </th>
                  <th>
                    <button type="button" className="markets-sort-button" onClick={() => handleSort('low24h')}>
                      {t('markets.table.low24h')}
                      {sortIndicator('low24h')}
                    </button>
                  </th>
                  <th>
                    <button type="button" className="markets-sort-button" onClick={() => handleSort('weightedAverage')}>
                      {t('markets.table.weightedAverage')}
                      {sortIndicator('weightedAverage')}
                    </button>
                  </th>
                  <th>
                    <button type="button" className="markets-sort-button" onClick={() => handleSort('lotVolume')}>
                      {t('markets.table.volume24h')}
                      {sortIndicator('lotVolume')}
                    </button>
                  </th>
                  <th>
                    <button type="button" className="markets-sort-button" onClick={() => handleSort('turnover')}>
                      {t('markets.table.turnover')}
                      {sortIndicator('turnover')}
                    </button>
                  </th>
                  <th>{t('markets.table.trend')}</th>
                </tr>
              </thead>
              <tbody>
                {filteredRows.length > 0 ? (
                  filteredRows.map((row) => {
                    const isPositive = row.changePercent >= 0
                    const previousClose = row.price / (1 + row.changePercent / 100)
                    const weightedAverage = (row.high24h + row.low24h + row.price) / 3

                    return (
                      <tr key={row.symbol}>
                        <td>
                          <button
                            type="button"
                            aria-label={favorites.includes(row.symbol) ? t('markets.unfavorite') : t('markets.favorite')}
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
                        <td>{priceFormat.format(previousClose)}</td>
                        <td className={isPositive ? 'markets-positive' : 'markets-negative'}>
                          {percentFormat.format(row.changePercent)}
                        </td>
                        <td>{priceFormat.format(row.high24h)}</td>
                        <td>{priceFormat.format(row.low24h)}</td>
                        <td>{priceFormat.format(weightedAverage)}</td>
                        <td>{row.lotVolumeLabel}</td>
                        <td>{row.turnoverLabel}</td>
                        <td>
                          <Sparkline values={row.sparkline} isPositive={isPositive} compact />
                        </td>
                      </tr>
                    )
                  })
                ) : (
                  <tr>
                    <td colSpan={11} className="markets-empty">
                      {t('markets.noMatches')}
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
          <p className="markets-last-updated">{t('markets.lastUpdated')}</p>
        </article>

        <aside className="card markets-overview-card">
          <div className="markets-overview-tabs" role="tablist" aria-label={t('markets.overview.tabsAria')}>
            {overviewItems.map((item) => (
              <button
                key={item.key}
                type="button"
                className={`markets-overview-tab${selectedOverview === item.key ? ' markets-overview-tab-active' : ''}`}
                onClick={() => setSelectedOverview(item.key)}
              >
                {item.label}
              </button>
            ))}
          </div>

          <div className="markets-overview-body">
            <div className="markets-overview-value-row">
              <strong>{priceFormat.format(selectedOverviewItem.value)}</strong>
              <span className={overviewPositive ? 'markets-positive' : 'markets-negative'}>
                {overviewPositive ? '▲' : '▼'}
              </span>
            </div>

            <dl className="markets-overview-metrics">
              <div>
                <dt>{t('markets.overview.change')}</dt>
                <dd className={overviewPositive ? 'markets-positive' : 'markets-negative'}>
                  {percentFormat.format(selectedOverviewItem.changePercent)} |{' '}
                  {priceFormat.format(selectedOverviewItem.changeValue)}
                </dd>
              </div>
              <div>
                <dt>{t('markets.overview.open')}</dt>
                <dd>{priceFormat.format(selectedOverviewItem.open)}</dd>
              </div>
              <div>
                <dt>{t('markets.overview.previousClose')}</dt>
                <dd>{priceFormat.format(selectedOverviewItem.previousClose)}</dd>
              </div>
            </dl>

            <div className="markets-overview-range-head">
              <span>
                {t('markets.overview.low')}: <strong>{priceFormat.format(selectedOverviewItem.low)}</strong>
              </span>
              <span>
                {t('markets.overview.high')}: <strong>{priceFormat.format(selectedOverviewItem.high)}</strong>
              </span>
            </div>
            <div className="markets-overview-range-bar" aria-hidden />

            <div className="markets-overview-chart">
              <Sparkline values={selectedOverviewItem.sparkline} isPositive={overviewPositive} />
            </div>
          </div>
        </aside>
      </div>
    </section>
  )
}
