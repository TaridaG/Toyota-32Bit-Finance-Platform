import { memo, useMemo } from 'react'
import { motion } from 'framer-motion'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import type { MarketOverviewItem } from '../../../../shared/types/market'
import { formatNumber, formatPrice } from '../../../../shared/format/number'
import type { SupportedCurrency } from '../../../../shared/preferences/preferences'
import {
  SPARKLINE_HEIGHT,
  SPARKLINE_WIDTH,
  toSparklinePath,
  toSparklinePoints,
  trendBadgeClass,
} from './sparklineUtils'

type CompactMarketTableProps = {
  rows: MarketOverviewItem[]
  loading: boolean
  error: string | null
  onRetry: () => void
  currency: SupportedCurrency
  locale: string
}

function trendLabelText(label: MarketOverviewItem['trendLabel'], t: (key: string) => string): string {
  if (!label) {
    return '—'
  }
  const map: Record<NonNullable<MarketOverviewItem['trendLabel']>, string> = {
    WEAK: t('globalMarkets.trend.weak'),
    NEUTRAL: t('globalMarkets.trend.neutral'),
    STRONG: t('globalMarkets.trend.strong'),
    VERY_STRONG: t('globalMarkets.trend.veryStrong'),
  }
  return map[label]
}

export const CompactMarketTable = memo(function CompactMarketTable({
  rows,
  loading,
  error,
  onRetry,
  currency,
  locale,
}: CompactMarketTableProps) {
  const { t } = useTranslation('landing')
  const percentDisplay = useMemo(
    () =>
      new Intl.NumberFormat(locale, {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
        signDisplay: 'exceptZero',
      }),
    [locale],
  )

  return (
    <motion.div
      className="gm-market-table-shell"
      initial={{ opacity: 0, y: 16 }}
      whileInView={{ opacity: 1, y: 0 }}
      viewport={{ once: true, amount: 0.35 }}
      transition={{ duration: 0.55, delay: 0.12 }}
    >
      <div className="gm-market-table-head">
        <h3>{t('globalMarkets.table.title')}</h3>
        <span className="gm-market-table-live">{t('globalMarkets.table.live')}</span>
      </div>

      {loading ? (
        <p className="gm-market-table-state">{t('states.loading')}</p>
      ) : error ? (
        <div className="gm-market-table-state-row">
          <p className="gm-market-table-state">{t('states.error')}</p>
          <button type="button" onClick={onRetry}>
            {t('states.retry')}
          </button>
        </div>
      ) : (
        <div className="gm-market-table-scroll">
          <table className="gm-market-table">
            <thead>
              <tr>
                <th>{t('globalMarkets.table.symbol')}</th>
                <th>{t('globalMarkets.table.price')}</th>
                <th>1D</th>
                <th>1M</th>
                <th>3M</th>
                <th>6M</th>
                <th>1Y</th>
                <th>{t('globalMarkets.table.trendScore')}</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row, index) => {
                const points = toSparklinePoints(row)
                const path = toSparklinePath(points)
                const isTrendUp = points[points.length - 1] >= points[0]
                const score = row.trendScore
                return (
                  <tr key={row.symbol} className="gm-market-row" style={{ animationDelay: `${index * 40}ms` }}>
                    <td>
                      <div className="gm-market-symbol-cell">
                        <span className="gm-market-symbol-dot" aria-hidden="true" />
                        <div>
                          <strong>{row.symbol}</strong>
                          <small>{row.name}</small>
                        </div>
                      </div>
                    </td>
                    <td className="gm-market-num">{formatPrice(row.price, locale, currency)}</td>
                    <td className={`gm-market-num ${(row.change1D ?? 0) >= 0 ? 'is-up' : 'is-down'}`}>
                      {percentDisplay.format(row.change1D ?? 0)}
                    </td>
                    <td className={`gm-market-num ${(row.change1M ?? 0) >= 0 ? 'is-up' : 'is-down'}`}>
                      {percentDisplay.format(row.change1M ?? 0)}
                    </td>
                    <td className={`gm-market-num ${(row.change3M ?? 0) >= 0 ? 'is-up' : 'is-down'}`}>
                      {percentDisplay.format(row.change3M ?? 0)}
                    </td>
                    <td className={`gm-market-num ${(row.change6M ?? 0) >= 0 ? 'is-up' : 'is-down'}`}>
                      {percentDisplay.format(row.change6M ?? 0)}
                    </td>
                    <td className={`gm-market-num ${(row.change1Y ?? 0) >= 0 ? 'is-up' : 'is-down'}`}>
                      {percentDisplay.format(row.change1Y ?? 0)}
                    </td>
                    <td>
                      <div className="gm-market-trend-cell">
                        <span className={`markets-trend-badge ${trendBadgeClass(score)}`}>
                          {score != null
                            ? `${formatNumber(score, locale, 0)} · ${trendLabelText(row.trendLabel, t)}`
                            : '—'}
                        </span>
                        <svg
                          className="sparkline sparkline-compact gm-market-sparkline"
                          viewBox={`0 0 ${SPARKLINE_WIDTH} ${SPARKLINE_HEIGHT}`}
                          aria-hidden="true"
                        >
                          <path
                            d={path}
                            className={isTrendUp ? 'sparkline-line-positive' : 'sparkline-line-negative'}
                          />
                        </svg>
                      </div>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}

      <Link to="/markets" className="gm-market-table-link">
        {t('globalMarkets.table.viewAll')}
        <span aria-hidden="true">→</span>
      </Link>
    </motion.div>
  )
})
