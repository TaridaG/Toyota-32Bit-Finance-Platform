import type { ChartReadout } from '../chart/readout'

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
  return (
    <article className="card fi-asset-stats">
      <h3>Asset Details</h3>
      <p className="fi-asset-price">{currentPrice.toLocaleString(undefined, { maximumFractionDigits: 2 })}</p>
      {chartReadout ? (
        <div className="fi-chart-readout">
          <div className="fi-chart-readout-label">
            {chartReadout.source === 'hover' ? 'Crosshair (live)' : 'Pinned bar'}
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
          <span>Daily</span>
          <strong className={daily >= 0 ? 'fi-up' : 'fi-down'}>{daily.toFixed(2)}%</strong>
        </li>
        <li>
          <span>Weekly</span>
          <strong className={weekly >= 0 ? 'fi-up' : 'fi-down'}>{weekly.toFixed(2)}%</strong>
        </li>
        <li>
          <span>Monthly</span>
          <strong className={monthly >= 0 ? 'fi-up' : 'fi-down'}>{monthly.toFixed(2)}%</strong>
        </li>
        <li>
          <span>Yearly</span>
          <strong className={yearly >= 0 ? 'fi-up' : 'fi-down'}>{yearly.toFixed(2)}%</strong>
        </li>
      </ul>
      <div className="fi-asset-meta">
        <small>Volume: {volume.toLocaleString()}</small>
        {marketCap ? <small>Market Cap: {marketCap.toLocaleString()}</small> : null}
      </div>
    </article>
  )
}
