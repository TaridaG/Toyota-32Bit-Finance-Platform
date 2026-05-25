import { useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import { formatNumber, formatPrice } from '../../../shared/format/number'
import type { AssetDefinition, AssetType } from '../types'
import { resolveInstrumentDisplayLabel } from '../../../features/markets/lib/tefasFundDisplay'

export type TickerHorizonReturns = {
  weekly: number | null
  monthly: number | null
  threeMonth: number | null
  sixMonth: number | null
  yearly: number | null
}

export type AnalysisTickerBarProps = {
  asset: AssetDefinition
  price: number
  dailyPct: number
  dailyHigh: number | null
  dailyLow: number | null
  weeklyPct: number
  yearlyPct: number
  categoryTag: string
  currencyCode: string
  scopeTag: string
  locale: string
  currency: string
  assetType: AssetType
  instrumentPickerOpen: boolean
  onInstrumentTriggerClick: () => void
  /** Wide grid (legacy). */
  layout?: 'banner' | 'terminal'
  /** Terminal strip: performance % by horizon (API + candle fallback from page). */
  horizonReturns?: TickerHorizonReturns | null
}

function pctDeltaFromDaily(current: number, dailyPct: number) {
  if (!Number.isFinite(current) || !Number.isFinite(dailyPct) || dailyPct === 0) return 0
  const denom = 1 + dailyPct / 100
  if (!Number.isFinite(denom) || denom === 0) return 0
  return current - current / denom
}

function HorizonStat({ label, value, locale }: { label: string; value: number | null; locale: string }) {
  if (value == null || !Number.isFinite(value)) {
    return (
      <>
        <span className="fi-analysis-ticker-terminal-lbl">{label}</span>
        <span className="fi-analysis-ticker-terminal-horizon-val fi-analysis-ticker-terminal-horizon-val--na">—</span>
      </>
    )
  }
  const up = value >= 0
  return (
    <>
      <span className="fi-analysis-ticker-terminal-lbl">{label}</span>
      <span
        className={`fi-analysis-ticker-terminal-horizon-val${up ? ' fi-analysis-ticker-terminal-horizon-val--up' : ' fi-analysis-ticker-terminal-horizon-val--down'}`}
      >
        {up ? '+' : ''}
        {formatNumber(value, locale, 2)}%
      </span>
    </>
  )
}

export function AnalysisTickerBar({
  asset,
  price,
  dailyPct,
  dailyHigh,
  dailyLow,
  weeklyPct,
  yearlyPct,
  categoryTag,
  currencyCode,
  scopeTag,
  locale,
  currency,
  assetType,
  instrumentPickerOpen,
  onInstrumentTriggerClick,
  layout = 'banner',
  horizonReturns = null,
}: AnalysisTickerBarProps) {
  const { t } = useTranslation('analysis')
  const displayLabel = useMemo(
    () => resolveInstrumentDisplayLabel(asset.symbol, asset.name),
    [asset.symbol, asset.name],
  )
  const absMove = pctDeltaFromDaily(price, dailyPct)
  const up = dailyPct >= 0

  const hr = horizonReturns ?? {
    weekly: null,
    monthly: null,
    threeMonth: null,
    sixMonth: null,
    yearly: null,
  }

  const identityBlock = (
    <div className="fi-analysis-ticker-left">
      <div className="fi-analysis-ticker-icon" aria-hidden="true">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
          <path d="M4 19V5M4 19h16M8 17V9m4 8V6m4 11v-5" />
        </svg>
      </div>
      <div className="fi-analysis-ticker-symbol-block">
        <div className="fi-analysis-ticker-symbol-row">
          <button
            type="button"
            className={`fi-analysis-ticker-symbol-trigger${instrumentPickerOpen ? ' fi-analysis-ticker-symbol-trigger--open' : ''}`}
            onClick={onInstrumentTriggerClick}
            aria-expanded={instrumentPickerOpen}
            aria-haspopup="dialog"
            aria-controls="fi-analysis-instrument-popover"
            aria-label={t('ticker.openInstrumentPicker')}
          >
            <span className="fi-analysis-ticker-symbol-trigger-main">
              <span className="fi-analysis-ticker-symbol">{displayLabel.symbol}</span>
              <span className="fi-analysis-ticker-chevron" aria-hidden="true">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.2">
                  <path d="M6 9l6 6 6-6" />
                </svg>
              </span>
            </span>
            <span className="fi-analysis-ticker-name">{displayLabel.name}</span>
            <span className="fi-analysis-ticker-tags">
              <span className="fi-analysis-ticker-tag">{categoryTag}</span>
              <span className="fi-analysis-ticker-tag">{currencyCode}</span>
              <span className="fi-analysis-ticker-tag">{scopeTag}</span>
            </span>
          </button>
          <button type="button" className="fi-analysis-ticker-star" disabled title={t('ticker.favoriteSoon')}>
            ★
          </button>
        </div>
      </div>
    </div>
  )

  const priceBlock = (
    <div className="fi-analysis-ticker-price-block">
      <div className="fi-analysis-ticker-price">{formatPrice(price, locale, currency, assetType)}</div>
      <div className={`fi-analysis-ticker-change${up ? ' fi-analysis-ticker-change--up' : ' fi-analysis-ticker-change--down'}`}>
        <span>{up ? '▲' : '▼'}</span>
        <span>
          {formatNumber(dailyPct, locale, 2)}% ({up ? '+' : ''}
          {formatNumber(absMove, locale, 2)})
        </span>
      </div>
    </div>
  )

  if (layout === 'terminal') {
    return (
      <header className="fi-analysis-ticker-bar fi-analysis-ticker-bar--terminal" aria-label={t('ticker.aria')}>
        <div className="fi-analysis-ticker-terminal">
          <div className="fi-analysis-ticker-terminal-seg fi-analysis-ticker-terminal-seg--id">{identityBlock}</div>
          <div className="fi-analysis-ticker-terminal-seg fi-analysis-ticker-terminal-seg--price">{priceBlock}</div>
          <div className="fi-analysis-ticker-terminal-seg fi-analysis-ticker-terminal-seg--ohlc-group">
            <div className="fi-analysis-ticker-terminal-ohlc-cell">
              <span className="fi-analysis-ticker-terminal-lbl">{t('ticker.dailyHigh')}</span>
              <span className="fi-analysis-ticker-terminal-val">
                {dailyHigh != null ? formatPrice(dailyHigh, locale, currency, assetType) : '—'}
              </span>
            </div>
            <div className="fi-analysis-ticker-terminal-ohlc-cell">
              <span className="fi-analysis-ticker-terminal-lbl">{t('ticker.dailyLow')}</span>
              <span
                className={`fi-analysis-ticker-terminal-val${dailyLow != null ? ' fi-analysis-ticker-terminal-val--low' : ''}`}
              >
                {dailyLow != null ? formatPrice(dailyLow, locale, currency, assetType) : '—'}
              </span>
            </div>
          </div>
          <div className="fi-analysis-ticker-terminal-seg fi-analysis-ticker-terminal-seg--horizons">
            <div className="fi-analysis-ticker-terminal-horizon-row" role="group" aria-label={t('ticker.horizonReturnsAria')}>
              <div className="fi-analysis-ticker-terminal-horizon-card">
                <HorizonStat label={t('periods.weekly')} value={hr.weekly} locale={locale} />
              </div>
              <div className="fi-analysis-ticker-terminal-horizon-card">
                <HorizonStat label={t('periods.monthly')} value={hr.monthly} locale={locale} />
              </div>
              <div className="fi-analysis-ticker-terminal-horizon-card">
                <HorizonStat label={t('periods.threeMonth')} value={hr.threeMonth} locale={locale} />
              </div>
              <div className="fi-analysis-ticker-terminal-horizon-card">
                <HorizonStat label={t('periods.sixMonth')} value={hr.sixMonth} locale={locale} />
              </div>
              <div className="fi-analysis-ticker-terminal-horizon-card">
                <HorizonStat label={t('periods.yearly')} value={hr.yearly} locale={locale} />
              </div>
            </div>
          </div>
        </div>
      </header>
    )
  }

  return (
    <header className="fi-analysis-ticker-bar" aria-label={t('ticker.aria')}>
      {identityBlock}
      {priceBlock}

      <dl className="fi-analysis-ticker-metrics">
        <div className="fi-analysis-ticker-metric">
          <dt>{t('ticker.dailyHigh')}</dt>
          <dd>{dailyHigh != null ? formatPrice(dailyHigh, locale, currency, assetType) : '—'}</dd>
        </div>
        <div className="fi-analysis-ticker-metric">
          <dt>{t('ticker.dailyLow')}</dt>
          <dd className={dailyLow != null ? 'fi-analysis-ticker-metric--low' : undefined}>
            {dailyLow != null ? formatPrice(dailyLow, locale, currency, assetType) : '—'}
          </dd>
        </div>
        <div className="fi-analysis-ticker-metric">
          <dt>{t('ticker.weeklyChange')}</dt>
          <dd className={weeklyPct >= 0 ? 'fi-up' : 'fi-down'}>{formatNumber(weeklyPct, locale, 2)}%</dd>
        </div>
        <div className="fi-analysis-ticker-metric">
          <dt>{t('ticker.yearlyChange')}</dt>
          <dd className={yearlyPct >= 0 ? 'fi-up' : 'fi-down'}>{formatNumber(yearlyPct, locale, 2)}%</dd>
        </div>
      </dl>
    </header>
  )
}
