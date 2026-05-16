import { useTranslation } from 'react-i18next'
import type { OhlcTooltipState } from '../chart/hooks/useCrosshairTooltip'
import type { AssetType } from '../types'
import { formatNumber, formatPrice } from '../../../shared/format/number'

type ChartHoverInsightCardProps = {
  symbol: string | null
  readout: OhlcTooltipState
  mode: 'crosshair' | 'latest' | 'empty'
  showMA20: boolean
  showMA50: boolean
  showRsi: boolean
  locale: string
  currency: string
  assetType: AssetType
}

function formatOptionalPrice(
  value: number | undefined,
  locale: string,
  currency: string,
  assetType: AssetType,
): string {
  if (value == null || !Number.isFinite(value)) return '—'
  return formatPrice(value, locale, currency, assetType)
}

function InsightMetricRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="fi-chart-insight-row">
      <span className="fi-chart-insight-row-label">{label}</span>
      <span className="fi-chart-insight-row-value">{value}</span>
    </div>
  )
}

export function ChartHoverInsightCard({
  symbol,
  readout,
  mode,
  showMA20,
  showMA50,
  showRsi,
  locale,
  currency,
  assetType,
}: ChartHoverInsightCardProps) {
  const { t } = useTranslation('analysis')

  const modeLabel =
    mode === 'crosshair'
      ? t('readout.crosshair')
      : mode === 'latest'
        ? t('readout.latestBar')
        : null

  const isUp =
    readout != null && Number.isFinite(readout.close) && Number.isFinite(readout.open)
      ? readout.close >= readout.open
      : true

  const barChangePct =
    readout != null &&
    Number.isFinite(readout.close) &&
    Number.isFinite(readout.open) &&
    readout.open !== 0
      ? ((readout.close - readout.open) / readout.open) * 100
      : null

  return (
    <aside className="fi-chart-hover-insight" aria-label={t('readout.panelAria')}>
      <header className="fi-chart-hover-insight-head">
        <div className="fi-chart-hover-insight-head-top">
          {symbol ? <span className="fi-chart-hover-insight-symbol">{symbol}</span> : null}
          {modeLabel ? <span className="fi-chart-hover-insight-badge">{modeLabel}</span> : null}
        </div>
        {readout?.timeLabel ? (
          <time className="fi-chart-hover-insight-time" dateTime={readout.timeLabel}>
            {readout.timeLabel}
          </time>
        ) : null}
      </header>

      {mode === 'empty' || !readout ? (
        <p className="fi-chart-hover-insight-hint">{t('readout.hoverHint')}</p>
      ) : (
        <div className="fi-chart-hover-insight-body" role="status">
          <div
            className={`fi-chart-insight-close-block${isUp ? ' fi-chart-insight-close-block--up' : ' fi-chart-insight-close-block--down'}`}
          >
            <span className="fi-chart-insight-close-label">{t('readout.closeLabel')}</span>
            <span className="fi-chart-insight-close-value">
              {formatOptionalPrice(readout.close, locale, currency, assetType)}
            </span>
            {barChangePct != null && Number.isFinite(barChangePct) ? (
              <span className="fi-chart-insight-change-chip">
                {isUp ? '▲' : '▼'} {formatNumber(Math.abs(barChangePct), locale, 2)}%
              </span>
            ) : null}
          </div>

          <section className="fi-chart-insight-section">
            <h3 className="fi-chart-insight-section-title">{t('readout.sectionPrice')}</h3>
            <div className="fi-chart-insight-rows">
              <InsightMetricRow
                label={t('readout.openLabel')}
                value={formatOptionalPrice(readout.open, locale, currency, assetType)}
              />
              <InsightMetricRow
                label={t('readout.highLabel')}
                value={formatOptionalPrice(readout.high, locale, currency, assetType)}
              />
              <InsightMetricRow
                label={t('readout.lowLabel')}
                value={formatOptionalPrice(readout.low, locale, currency, assetType)}
              />
              {readout.volume != null && Number.isFinite(readout.volume) ? (
                <InsightMetricRow
                  label={t('volumeLabel')}
                  value={formatNumber(readout.volume, locale, 0)}
                />
              ) : null}
            </div>
          </section>

          {showMA20 || showMA50 || showRsi ? (
            <section className="fi-chart-insight-section">
              <h3 className="fi-chart-insight-section-title">{t('readout.sectionIndicators')}</h3>
              <div className="fi-chart-insight-rows">
                {showMA20 ? (
                  <InsightMetricRow
                    label="MA 20"
                    value={formatOptionalPrice(readout.ma20, locale, currency, assetType)}
                  />
                ) : null}
                {showMA50 ? (
                  <InsightMetricRow
                    label="MA 50"
                    value={formatOptionalPrice(readout.ma50, locale, currency, assetType)}
                  />
                ) : null}
                {showRsi ? (
                  <InsightMetricRow
                    label="RSI (14)"
                    value={
                      readout.rsi != null && Number.isFinite(readout.rsi)
                        ? formatNumber(readout.rsi, locale, 2)
                        : '—'
                    }
                  />
                ) : null}
              </div>
            </section>
          ) : null}
        </div>
      )}
    </aside>
  )
}
