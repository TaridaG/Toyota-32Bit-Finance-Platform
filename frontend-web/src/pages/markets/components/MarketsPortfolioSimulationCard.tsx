import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { fetchCandles } from '../../../features/analysis/api/analysisService'
import { normalizeFxCandleSeries } from '../../../features/markets/lib/fxTryHubConversion'
import { useFxTryHubHistory } from '../../analysis/hooks/useFxTryHubHistory'
import type { SupportedCurrency } from '../../../shared/preferences/preferences'
import { useAppPreferences } from '../../../shared/preferences/useAppPreferences'
import type { CandlePoint } from '../../analysis/types'
import {
  MARKETS_PORTFOLIO_SIM_PANEL_ID,
  registerMarketsPortfolioSimAdd,
} from '../lib/marketsPortfolioSimBridge'
import { type MarketsRowDragPayload } from '../lib/marketsRowDrag'
import {
  buildPortfolioReturnSummary,
  computeHoldingPeriodMetrics,
  defaultStartDay,
  equalizeWeights,
  inferHoldingNativeQuote,
  isStartDayValid,
  normalizeWeights,
  portfolioDateBounds,
  type HoldingPeriodMetrics,
  type PortfolioHolding,
} from '../utils/portfolioSimulation'
import { PortfolioAllocationWheel } from './PortfolioAllocationWheel'

const STORAGE_KEY = 'markets.portfolioSim.startDate'
const STORAGE_CURRENCY_KEY = 'markets.portfolioSim.displayCurrency'
const MAX_HOLDINGS = 12

type SimBaseCurrency = 'TRY' | 'USD'

function loadDisplayCurrency(fallback: SupportedCurrency): SimBaseCurrency {
  if (typeof window === 'undefined') {
    return fallback === 'USD' ? 'USD' : 'TRY'
  }
  const stored = window.localStorage.getItem(STORAGE_CURRENCY_KEY)
  if (stored === 'TRY' || stored === 'USD') {
    return stored
  }
  return fallback === 'USD' ? 'USD' : 'TRY'
}

function persistDisplayCurrency(currency: SimBaseCurrency) {
  if (typeof window === 'undefined') {
    return
  }
  window.localStorage.setItem(STORAGE_CURRENCY_KEY, currency)
}

function newHoldingId(): string {
  return `h-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
}

function loadStartDate(): string {
  if (typeof window === 'undefined') {
    return ''
  }
  return window.localStorage.getItem(STORAGE_KEY) ?? ''
}

function persistStartDate(startDate: string) {
  if (typeof window === 'undefined' || !startDate) {
    return
  }
  window.localStorage.setItem(STORAGE_KEY, startDate)
}

export function MarketsPortfolioSimulationCard() {
  const { t, i18n } = useTranslation('markets')
  const { currency: headerCurrency } = useAppPreferences()
  const locale = i18n.resolvedLanguage ?? i18n.language ?? 'en'

  const [holdings, setHoldings] = useState<PortfolioHolding[]>([])
  const [startDate, setStartDate] = useState(() => loadStartDate())
  const [displayCurrency, setDisplayCurrency] = useState<SimBaseCurrency>(() =>
    loadDisplayCurrency(headerCurrency),
  )
  const [seriesBySymbol, setSeriesBySymbol] = useState<Record<string, CandlePoint[]>>({})
  const [loadingSymbols, setLoadingSymbols] = useState<string[]>([])
  const [failedSymbols, setFailedSymbols] = useState<string[]>([])
  const [activeHoldingId, setActiveHoldingId] = useState<string | null>(null)
  const inFlightRef = useRef<Set<string>>(new Set())
  const seriesRef = useRef(seriesBySymbol)
  seriesRef.current = seriesBySymbol

  useEffect(() => {
    if (startDate) {
      persistStartDate(startDate)
    }
  }, [startDate])

  useEffect(() => {
    persistDisplayCurrency(displayCurrency)
  }, [displayCurrency])

  const loadSeriesForHolding = useCallback(async (holding: PortfolioHolding) => {
    const symbol = holding.symbol
    if (inFlightRef.current.has(symbol) || seriesRef.current[symbol]?.length) {
      return
    }
    inFlightRef.current.add(symbol)
    setLoadingSymbols((prev) => (prev.includes(symbol) ? prev : [...prev, symbol]))
    setFailedSymbols((prev) => prev.filter((s) => s !== symbol))
    try {
      const raw = await fetchCandles(symbol, '5y', { wireCategory: holding.category })
      const series = normalizeFxCandleSeries(symbol, raw)
      setSeriesBySymbol((prev) => ({ ...prev, [symbol]: series }))
    } catch {
      setFailedSymbols((prev) => (prev.includes(symbol) ? prev : [...prev, symbol]))
    } finally {
      inFlightRef.current.delete(symbol)
      setLoadingSymbols((prev) => prev.filter((s) => s !== symbol))
    }
  }, [])

  useEffect(() => {
    for (const holding of holdings) {
      void loadSeriesForHolding(holding)
    }
  }, [holdings, loadSeriesForHolding])

  const dateBounds = useMemo(() => portfolioDateBounds(seriesBySymbol), [seriesBySymbol])

  useEffect(() => {
    if (!dateBounds || holdings.length === 0) {
      return
    }
    if (!startDate || startDate < dateBounds.min || startDate > dateBounds.max) {
      setStartDate(defaultStartDay(dateBounds))
    }
  }, [dateBounds, holdings.length, startDate])

  const exitDay = useMemo(() => {
    if (!dateBounds?.max) {
      return startDate || null
    }
    return dateBounds.max
  }, [dateBounds?.max, startDate])

  const fxEnabled = holdings.length > 0 && Boolean(startDate) && Object.keys(seriesBySymbol).length > 0
  const { loading: fxLoading, rateMapAtDay } = useFxTryHubHistory(fxEnabled, startDate || '', exitDay)

  const weightsSum = holdings.reduce((acc, h) => acc + Math.max(0, h.weightPct), 0)
  const weightsOk = holdings.length === 0 || Math.abs(weightsSum - 100) < 0.05

  const metricsByHoldingId = useMemo(() => {
    const map = new Map<string, HoldingPeriodMetrics>()
    if (!startDate || fxLoading) {
      return map
    }
    const entryFx = rateMapAtDay(startDate)
    const exitFx = exitDay ? rateMapAtDay(exitDay) : entryFx

    for (const holding of holdings) {
      const rawSeries = seriesBySymbol[holding.symbol]
      if (!rawSeries?.length) {
        continue
      }
      const series = normalizeFxCandleSeries(holding.symbol, rawSeries)
      const nativeQuote = inferHoldingNativeQuote(holding)
      const metrics = computeHoldingPeriodMetrics(
        series,
        startDate,
        displayCurrency,
        nativeQuote,
        entryFx,
        exitFx,
      )
      map.set(holding.id, { ...metrics, symbol: holding.symbol })
    }
    return map
  }, [displayCurrency, exitDay, fxLoading, holdings, rateMapAtDay, seriesBySymbol, startDate])

  const portfolioSummary = useMemo(() => {
    if (!startDate || !weightsOk || !isStartDayValid(seriesBySymbol, startDate)) {
      return null
    }
    return buildPortfolioReturnSummary(holdings, metricsByHoldingId, weightsOk)
  }, [holdings, metricsByHoldingId, seriesBySymbol, startDate, weightsOk])

  const addPayload = useCallback((payload: MarketsRowDragPayload) => {
    setHoldings((current) => {
      if (current.length >= MAX_HOLDINGS) {
        return current
      }
      if (current.some((h) => h.symbol === payload.symbol)) {
        return current
      }
      const next = [
        ...current,
        {
          id: newHoldingId(),
          symbol: payload.symbol,
          name: payload.name,
          category: payload.category,
          weightPct: 0,
        },
      ]
      return equalizeWeights(next)
    })
  }, [])

  useEffect(() => registerMarketsPortfolioSimAdd(addPayload), [addPayload])

  const removeHolding = useCallback((id: string) => {
    setActiveHoldingId(null)
    setHoldings((current) => {
      const removed = current.find((h) => h.id === id)
      const next = equalizeWeights(current.filter((h) => h.id !== id))
      if (removed) {
        setSeriesBySymbol((prev) => {
          const stillUsed = next.some((h) => h.symbol === removed.symbol)
          if (stillUsed) {
            return prev
          }
          const { [removed.symbol]: _drop, ...rest } = prev
          return rest
        })
      }
      return next
    })
  }, [])

  const pctFmt = useMemo(
    () =>
      new Intl.NumberFormat(locale, {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
        signDisplay: 'always',
      }),
    [locale],
  )

  const anyLoading = loadingSymbols.length > 0 || fxLoading

  return (
    <article id={MARKETS_PORTFOLIO_SIM_PANEL_ID} className="card markets-portfolio-sim-panel">
      <header className="markets-portfolio-sim-bar-header">
        <div>
          <h3>{t('portfolioSim.title')}</h3>
          <p className="markets-portfolio-sim-lead">{t('portfolioSim.lead')}</p>
        </div>
        <div className="markets-portfolio-sim-toolbar markets-portfolio-sim-toolbar-inline">
          <button
            type="button"
            className="markets-filter"
            disabled={holdings.length === 0}
            onClick={() => setHoldings((current) => equalizeWeights(current))}
          >
            {t('portfolioSim.equalize')}
          </button>
          <button
            type="button"
            className="markets-filter"
            disabled={holdings.length === 0}
            onClick={() => setHoldings((current) => normalizeWeights(current))}
          >
            {t('portfolioSim.normalize')}
          </button>
        </div>
      </header>

      <div className="markets-portfolio-sim-bar-body">
        <PortfolioAllocationWheel
          holdings={holdings}
          activeId={activeHoldingId}
          onActiveChange={setActiveHoldingId}
          onHoldingsChange={setHoldings}
          onRemove={removeHolding}
          onAddPayload={addPayload}
          weightsOk={weightsOk}
          weightsSum={weightsSum}
          locale={locale}
          layout="horizontal"
          metricsByHoldingId={metricsByHoldingId}
          loadingSymbols={loadingSymbols}
          failedSymbols={failedSymbols}
          pctFmt={pctFmt}
        />

        <div className="markets-portfolio-sim-bar-side">
          <div className="markets-portfolio-sim-side-controls">
            <label className="markets-portfolio-sim-date-only">
              <span>{t('portfolioSim.startDate')}</span>
              <input
                type="date"
                value={startDate}
                min={dateBounds?.min}
                max={dateBounds?.max}
                disabled={holdings.length === 0 || !dateBounds}
                onChange={(event) => setStartDate(event.target.value)}
              />
            </label>
            <div className="markets-portfolio-sim-currency">
              <span className="markets-portfolio-sim-currency-label">{t('portfolioSim.baseCurrency')}</span>
              <div
                className="markets-filter-group"
                role="group"
                aria-label={t('portfolioSim.baseCurrencyAria')}
              >
                {(['TRY', 'USD'] as const).map((code) => (
                  <button
                    key={code}
                    type="button"
                    className={`markets-filter${displayCurrency === code ? ' markets-filter-active' : ''}`}
                    disabled={holdings.length === 0}
                    aria-pressed={displayCurrency === code}
                    onClick={() => setDisplayCurrency(code)}
                  >
                    {code === 'TRY' ? t('portfolioSim.currencyTry') : t('portfolioSim.currencyUsd')}
                  </button>
                ))}
              </div>
            </div>
          </div>

          {anyLoading && holdings.length > 0 ? (
            <p className="markets-insights-empty">{t('common:loading')}</p>
          ) : null}

          {portfolioSummary && !anyLoading ? (
            <section className="markets-portfolio-sim-result markets-portfolio-sim-result-compact">
              <div className="markets-portfolio-sim-summary markets-portfolio-sim-summary-slim">
                <div>
                  <span>{t('portfolioSim.totalReturn')}</span>
                  <strong
                    className={
                      portfolioSummary.totalReturnPct >= 0 ? 'markets-positive' : 'markets-negative'
                    }
                  >
                    {pctFmt.format(portfolioSummary.totalReturnPct)}%
                  </strong>
                </div>
                <div className="markets-portfolio-sim-summary-meta">
                  <span>{t('portfolioSim.displayCurrency', { currency: displayCurrency })}</span>
                </div>
              </div>
              <p className="markets-portfolio-sim-period">
                {t('portfolioSim.period', {
                  from: portfolioSummary.entryDay,
                  to: portfolioSummary.exitDay,
                })}
              </p>
            </section>
          ) : null}

          {failedSymbols.length > 0 && !anyLoading ? (
            <p className="markets-insights-empty markets-insights-empty--warn">{t('portfolioSim.loadError')}</p>
          ) : null}

          {!portfolioSummary &&
          !anyLoading &&
          holdings.length > 0 &&
          startDate &&
          weightsOk &&
          failedSymbols.length === 0 ? (
            <p className="markets-insights-empty">{t('portfolioSim.noResult')}</p>
          ) : null}
        </div>
      </div>
    </article>
  )
}
