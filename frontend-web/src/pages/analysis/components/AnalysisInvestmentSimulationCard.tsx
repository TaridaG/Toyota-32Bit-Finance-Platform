import { useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { inferNativeQuote } from '../../../features/markets/lib/marketDisplayConversion'
import { useCandles } from '../../../features/analysis/hooks/useCandles'
import { SUPPORTED_CURRENCIES, type SupportedCurrency } from '../../../shared/preferences/preferences'
import type { AssetDefinition, CandlePoint } from '../types'
import { useFxTryHubHistory } from '../hooks/useFxTryHubHistory'
import {
  computeInvestmentSimulation,
  findCandleOnOrBeforeDay,
  isoDayBounds,
  utcTimestampToIsoDay,
} from '../utils/investmentSimulation'

type AnalysisInvestmentSimulationCardProps = {
  asset: AssetDefinition | null
  fallbackSeries: CandlePoint[]
  displayCurrency: SupportedCurrency
  purchaseDate: string
  onPurchaseDateChange: (isoDay: string) => void
  chartPickActive: boolean
  onChartPickActiveChange: (active: boolean) => void
}

function parseAmount(raw: string): number | null {
  const n = Number(String(raw).replace(/\s/g, '').replace(',', '.'))
  return Number.isFinite(n) && n > 0 ? n : null
}

function formatFxRate(rate: number, locale: string): string {
  const abs = Math.abs(rate)
  const digits = abs >= 100 ? 2 : abs >= 1 ? 4 : 6
  return new Intl.NumberFormat(locale, { maximumFractionDigits: digits }).format(rate)
}

export function AnalysisInvestmentSimulationCard({
  asset,
  fallbackSeries,
  displayCurrency,
  purchaseDate,
  onPurchaseDateChange,
  chartPickActive,
  onChartPickActiveChange,
}: AnalysisInvestmentSimulationCardProps) {
  const { t, i18n } = useTranslation('analysis')
  const locale = i18n.resolvedLanguage ?? i18n.language ?? 'en'

  const [amount, setAmount] = useState('10000')
  const [paymentCurrency, setPaymentCurrency] = useState<SupportedCurrency>(displayCurrency)

  useEffect(() => {
    setPaymentCurrency(displayCurrency)
  }, [displayCurrency])

  const { candles: historySeries, loading: historyLoading } = useCandles(
    asset?.symbol ?? '',
    '5y',
    { wireCategory: asset?.wireCategory ?? null },
  )

  const series = historySeries.length > 0 ? historySeries : fallbackSeries
  const dayBounds = useMemo(() => isoDayBounds(series), [series])
  const exitDayForFx = useMemo(
    () => (series.length > 0 ? utcTimestampToIsoDay(series[series.length - 1].time) : null),
    [series],
  )

  const { loading: fxLoading, error: fxError, rateMapAtDay } = useFxTryHubHistory(
    asset != null,
    purchaseDate,
    exitDayForFx,
  )

  const nativeQuote = useMemo(() => {
    if (!asset) return 'USD' as const
    return inferNativeQuote(asset.symbol, asset.wireCategory)
  }, [asset])

  useEffect(() => {
    if (!asset || !dayBounds) return
    if (!purchaseDate || purchaseDate < dayBounds.min || purchaseDate > dayBounds.max) {
      onPurchaseDateChange(dayBounds.max)
    }
  }, [asset?.id, dayBounds, onPurchaseDateChange, purchaseDate])

  const simAttempt = useMemo(() => {
    const amt = parseAmount(amount)
    if (amt == null || !purchaseDate) return null
    const entryFx = rateMapAtDay(purchaseDate)
    const exitDay = series.length > 0 ? utcTimestampToIsoDay(series[series.length - 1].time) : purchaseDate
    const exitFx = rateMapAtDay(exitDay)
    return {
      ...computeInvestmentSimulation(
        series,
        purchaseDate,
        amt,
        paymentCurrency,
        nativeQuote,
        entryFx,
        exitFx,
      ),
      exitDay,
    }
  }, [amount, nativeQuote, paymentCurrency, purchaseDate, rateMapAtDay, series])

  const sim = simAttempt?.result ?? null
  const breakdown = sim?.breakdown ?? simAttempt?.partial ?? null
  const exitDay = simAttempt?.exitDay ?? null

  const moneyFmt = useMemo(
    () =>
      new Intl.NumberFormat(locale, {
        style: 'currency',
        currency: paymentCurrency,
        maximumFractionDigits: 2,
      }),
    [locale, paymentCurrency],
  )

  const nativeMoneyFmt = useMemo(
    () =>
      new Intl.NumberFormat(locale, {
        style: 'currency',
        currency: nativeQuote,
        maximumFractionDigits: nativeQuote === 'TRY' ? 2 : 4,
      }),
    [locale, nativeQuote],
  )

  const priceFmt = useMemo(
    () =>
      new Intl.NumberFormat(locale, {
        minimumFractionDigits: 2,
        maximumFractionDigits: nativeQuote === 'TRY' ? 2 : 4,
      }),
    [locale, nativeQuote],
  )

  const lotsFmt = useMemo(
    () =>
      new Intl.NumberFormat(locale, {
        minimumFractionDigits: 0,
        maximumFractionDigits: 4,
      }),
    [locale],
  )

  const pctFmt = useMemo(
    () =>
      new Intl.NumberFormat(locale, {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
        signDisplay: 'exceptZero',
      }),
    [locale],
  )

  const entryPreview = purchaseDate ? findCandleOnOrBeforeDay(series, purchaseDate) : null
  const fxBusy = fxLoading && asset != null

  const failureMessage =
    simAttempt?.failure === 'no_entry_fx'
      ? t('simulation.noEntryFx', { from: paymentCurrency, to: nativeQuote })
      : simAttempt?.failure === 'no_exit_fx'
        ? t('simulation.noExitFx', { from: nativeQuote, to: paymentCurrency })
        : null

  if (!asset) {
    return (
      <article className="card fi-analysis-simulation-card">
        <div className="fi-panel-head">
          <h3>{t('simulation.title')}</h3>
        </div>
        <p className="fi-empty">{t('simulation.noAsset')}</p>
      </article>
    )
  }

  return (
    <article className="card fi-analysis-simulation-card">
      <div className="fi-panel-head">
        <h3>{t('simulation.title')}</h3>
        <small>{t('simulation.subtitle', { symbol: asset.symbol })}</small>
      </div>

      {chartPickActive ? (
        <div className="fi-analysis-sim-pick-banner" role="status">
          <span>{t('simulation.chartPickHint')}</span>
          <button type="button" className="fi-analysis-sim-pick-cancel" onClick={() => onChartPickActiveChange(false)}>
            {t('simulation.chartPickCancel')}
          </button>
        </div>
      ) : null}

      <div className="fi-analysis-sim-columns">
        <section className="fi-analysis-sim-col fi-analysis-sim-col--inputs" aria-label={t('simulation.inputsSectionAria')}>
          <label className="fi-analysis-sim-label">
            {t('simulation.amountLabel')}
            <div className="fi-analysis-sim-amount-row">
              <input
                className="fi-analysis-sim-input"
                inputMode="decimal"
                value={amount}
                onChange={(e) => setAmount(e.target.value)}
              />
              <div className="fi-analysis-sim-currency-rail" role="listbox" aria-label={t('simulation.currencyAria')}>
                {SUPPORTED_CURRENCIES.map((code) => (
                  <button
                    key={code}
                    type="button"
                    role="option"
                    aria-selected={paymentCurrency === code}
                    className={`fi-analysis-sim-currency-chip${paymentCurrency === code ? ' fi-analysis-sim-currency-chip--active' : ''}`}
                    onClick={() => setPaymentCurrency(code)}
                  >
                    {code}
                  </button>
                ))}
              </div>
            </div>
          </label>

          <label className="fi-analysis-sim-label">
            {t('simulation.dateLabel')}
            <div className="fi-analysis-sim-date-row">
              <input
                className="fi-analysis-sim-input"
                type="date"
                value={purchaseDate}
                min={dayBounds?.min}
                max={dayBounds?.max}
                disabled={!dayBounds || historyLoading}
                onChange={(e) => onPurchaseDateChange(e.target.value)}
              />
              <button
                type="button"
                className={`fi-analysis-sim-pick-btn${chartPickActive ? ' fi-analysis-sim-pick-btn--active' : ''}`}
                title={t('simulation.chartPickFromChart')}
                aria-label={t('simulation.chartPickFromChart')}
                onClick={() => onChartPickActiveChange(!chartPickActive)}
              >
                ◎
              </button>
            </div>
          </label>

          {entryPreview ? (
            <p className="fi-analysis-sim-meta">
              {t('simulation.entryPrice', {
                date: new Intl.DateTimeFormat(locale, { dateStyle: 'medium' }).format(
                  new Date(`${utcTimestampToIsoDay(entryPreview.time)}T12:00:00`),
                ),
                price: `${priceFmt.format(entryPreview.close)} ${nativeQuote}`,
              })}
            </p>
          ) : purchaseDate && !historyLoading ? (
            <p className="fi-analysis-sim-meta fi-analysis-sim-meta--warn">{t('simulation.noPriceOnDate')}</p>
          ) : null}

          {fxError ? (
            <p className="fi-analysis-sim-meta fi-analysis-sim-meta--warn">{t('simulation.fxUnavailable')}</p>
          ) : failureMessage ? (
            <p className="fi-analysis-sim-meta fi-analysis-sim-meta--warn">{failureMessage}</p>
          ) : null}

          {(historyLoading && series.length === 0) || fxBusy ? (
            <div className="markets-skeleton-row fi-analysis-sim-skel" aria-busy="true" />
          ) : null}
        </section>

        <section className="fi-analysis-sim-col fi-analysis-sim-col--breakdown" aria-label={t('simulation.breakdownSectionAria')}>
          {breakdown && (breakdown.entryFxRate != null || breakdown.amountInNative != null) ? (
            <dl className="fi-analysis-sim-dl fi-analysis-sim-dl--breakdown">
              {breakdown.entryFxRate != null ? (
                <div>
                  <dt>{t('simulation.entryFx', { date: purchaseDate })}</dt>
                  <dd>
                    {t('simulation.fxRateLine', {
                      from: paymentCurrency,
                      to: nativeQuote,
                      rate: formatFxRate(breakdown.entryFxRate, locale),
                    })}
                  </dd>
                </div>
              ) : null}
              {breakdown.amountInNative != null ? (
                <div>
                  <dt>{t('simulation.costNative', { currency: nativeQuote })}</dt>
                  <dd>{nativeMoneyFmt.format(breakdown.amountInNative)}</dd>
                </div>
              ) : null}
              {sim ? (
                <div>
                  <dt>{t('simulation.lots')}</dt>
                  <dd>{lotsFmt.format(sim.units)}</dd>
                </div>
              ) : null}
              {breakdown.exitFxRate != null && exitDay ? (
                <div>
                  <dt>{t('simulation.exitFx', { date: exitDay })}</dt>
                  <dd>
                    {t('simulation.fxRateLine', {
                      from: nativeQuote,
                      to: paymentCurrency,
                      rate: formatFxRate(breakdown.exitFxRate, locale),
                    })}
                  </dd>
                </div>
              ) : null}
              {breakdown.valueInNative != null ? (
                <div>
                  <dt>{t('simulation.valueNative', { currency: nativeQuote })}</dt>
                  <dd>{nativeMoneyFmt.format(breakdown.valueInNative)}</dd>
                </div>
              ) : null}
            </dl>
          ) : (
            <p className="fi-analysis-sim-col-empty">{t('simulation.breakdownEmpty')}</p>
          )}
        </section>

        <section className="fi-analysis-sim-col fi-analysis-sim-col--result" aria-label={t('simulation.resultSectionAria')}>
          <dl className="fi-analysis-sim-dl fi-analysis-sim-dl--result">
            <div>
              <dt>{t('simulation.cost')}</dt>
              <dd>{sim ? moneyFmt.format(sim.cost) : '—'}</dd>
            </div>
            <div>
              <dt>{t('simulation.currentValue')}</dt>
              <dd>{sim ? moneyFmt.format(sim.currentValue) : '—'}</dd>
            </div>
            <div>
              <dt>{t('simulation.pnl')}</dt>
              <dd className={sim ? (sim.pnl >= 0 ? 'fi-up' : 'fi-down') : undefined}>
                {sim ? moneyFmt.format(sim.pnl) : '—'}
              </dd>
            </div>
            <div>
              <dt>{t('simulation.pnlPct')}</dt>
              <dd className={sim ? (sim.pnl >= 0 ? 'fi-up' : 'fi-down') : undefined}>
                {sim ? `${pctFmt.format(sim.pnlPct)}%` : '—'}
              </dd>
            </div>
          </dl>
          <p className="fi-analysis-sim-foot">{t('simulation.disclaimer')}</p>
        </section>
      </div>
    </article>
  )
}
