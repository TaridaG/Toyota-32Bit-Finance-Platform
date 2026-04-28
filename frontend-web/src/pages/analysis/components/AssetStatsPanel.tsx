import type { ChartReadout } from '../chart/readout'
import { useTranslation } from 'react-i18next'

type AssetStatsPanelProps = {
  currentPrice: number
  daily: number
  weekly: number
  monthly: number
  yearly: number
  volume: number
  marketCap?: number
  chartReadout?: ChartReadout | null
}

function fmtOhlc(n: number) {
  return n.toLocaleString(undefined, { maximumFractionDigits: 4 })
}

export function AssetStatsPanel({
  currentPrice,
  daily,
  weekly,
  monthly,
  yearly,
  volume,
  marketCap,
  chartReadout,
}: AssetStatsPanelProps) {
  const { t } = useTranslation('analysis')
  return (
    <article className="card fi-asset-stats">
      <h3>{t('assetDetailsTitle')}</h3>
      <p className="fi-asset-price">{currentPrice.toLocaleString(undefined, { maximumFractionDigits: 2 })}</p>
      {chartReadout ? (
        <div className="fi-chart-readout">
          <div className="fi-chart-readout-label">
            {chartReadout.source === 'hover' ? t('readout.crosshair') : t('readout.pinnedBar')}
          </div>
          <div className="fi-chart-readout-time">{chartReadout.timeLabel}</div>
          <dl className="fi-chart-readout-ohlc">
            <div>
              <dt>O</dt>
              <dd>{fmtOhlc(chartReadout.open)}</dd>
            </div>
            <div>
              <dt>H</dt>
              <dd>{fmtOhlc(chartReadout.high)}</dd>
            </div>
            <div>
              <dt>L</dt>
              <dd>{fmtOhlc(chartReadout.low)}</dd>
            </div>
            <div>
              <dt>C</dt>
              <dd>{fmtOhlc(chartReadout.close)}</dd>
            </div>
          </dl>
        </div>
      ) : null}
      <ul>
        <li>
          <span>{t('periods.daily')}</span>
          <strong className={daily >= 0 ? 'fi-up' : 'fi-down'}>{daily.toFixed(2)}%</strong>
        </li>
        <li>
          <span>{t('periods.weekly')}</span>
          <strong className={weekly >= 0 ? 'fi-up' : 'fi-down'}>{weekly.toFixed(2)}%</strong>
        </li>
        <li>
          <span>{t('periods.monthly')}</span>
          <strong className={monthly >= 0 ? 'fi-up' : 'fi-down'}>{monthly.toFixed(2)}%</strong>
        </li>
        <li>
          <span>{t('periods.yearly')}</span>
          <strong className={yearly >= 0 ? 'fi-up' : 'fi-down'}>{yearly.toFixed(2)}%</strong>
        </li>
      </ul>
      <div className="fi-asset-meta">
        <small>{t('volumeLabel')}: {volume.toLocaleString()}</small>
        {marketCap ? <small>{t('marketCapLabel')}: {marketCap.toLocaleString()}</small> : null}
      </div>
    </article>
  )
}
