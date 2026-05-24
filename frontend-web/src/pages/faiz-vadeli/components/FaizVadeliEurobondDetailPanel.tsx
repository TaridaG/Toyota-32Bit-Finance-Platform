import { useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import {
  ColorType,
  createChart,
  CrosshairMode,
  LineSeries,
  type IChartApi,
  type LineData,
  type MouseEventParams,
  type Time,
} from 'lightweight-charts'
import { useTheme } from '../../../shared/theme/ThemeProvider'
import {
  fetchTrEurobondHistory,
  toNum,
  type EurobondHistoryPointWire,
  type EurobondInstrumentWire,
} from '../api/eurobondMarketApi'
import {
  EurobondSimulator,
  type EurobondChartPickMode,
  type EurobondSimulatorHandle,
} from './simulators/EurobondSimulator'

const EM = '—'

function timeToIsoDay(t: Time): string | null {
  if (t == null) return null
  if (typeof t === 'string') {
    return t.length >= 10 ? t.slice(0, 10) : t
  }
  if (typeof t === 'number') {
    return new Date(t * 1000).toISOString().slice(0, 10)
  }
  if (typeof t === 'object' && 'year' in t && 'month' in t && 'day' in t) {
    const o = t as { year: number; month: number; day: number }
    const mm = String(o.month).padStart(2, '0')
    const dd = String(o.day).padStart(2, '0')
    return `${o.year}-${mm}-${dd}`
  }
  return null
}

function dedupeByDay(points: LineData<Time>[]): LineData<Time>[] {
  const m = new Map<string, number>()
  for (const p of points) {
    const day = String(p.time)
    m.set(day, p.value)
  }
  return [...m.entries()]
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([time, value]) => ({ time: time as Time, value }))
}

function toPriceSeries(points: EurobondHistoryPointWire[]): LineData<Time>[] {
  const raw: LineData<Time>[] = []
  for (const p of points) {
    const d = p.date
    const v = toNum(p.closePrice)
    if (!d || v == null) continue
    raw.push({ time: d as Time, value: v })
  }
  return dedupeByDay(raw)
}

function toYieldSeries(points: EurobondHistoryPointWire[]): LineData<Time>[] {
  const raw: LineData<Time>[] = []
  for (const p of points) {
    const d = p.date
    const v = toNum(p.closeYieldPercent)
    if (!d || v == null) continue
    raw.push({ time: d as Time, value: v })
  }
  return dedupeByDay(raw)
}

function EurobondLwChart({
  lineData,
  loadState,
  color,
  locale,
  isDark,
  chartPickActive,
  onTimePicked,
}: {
  lineData: LineData<Time>[]
  loadState: 'loading' | 'error' | 'ready'
  color: string
  locale: string
  isDark: boolean
  chartPickActive?: boolean
  onTimePicked?: (isoDay: string) => void
}) {
  const outerRef = useRef<HTMLDivElement | null>(null)
  const mountRef = useRef<HTMLDivElement | null>(null)
  const chartRef = useRef<IChartApi | null>(null)
  const pickActiveRef = useRef(false)
  const onPickRef = useRef<((iso: string) => void) | undefined>(undefined)

  useEffect(() => {
    pickActiveRef.current = !!chartPickActive
    onPickRef.current = onTimePicked
  }, [chartPickActive, onTimePicked])

  useLayoutEffect(() => {
    if (loadState !== 'ready' || lineData.length === 0) {
      return
    }
    const outer = outerRef.current
    const mount = mountRef.current
    if (!outer || !mount) return

    const textMuted = isDark ? '#94a3b8' : '#64748b'
    const gridH = isDark ? 'rgba(148, 163, 184, 0.12)' : 'rgba(100, 116, 139, 0.14)'
    const labelBg = isDark ? '#334155' : '#e2e8f0'
    const border = isDark ? 'rgba(148,163,184,0.2)' : 'rgba(100,116,139,0.25)'

    const w = Math.max(outer.clientWidth, 200)
    const h = Math.max(outer.clientHeight, 200)

    const chart = createChart(mount, {
      width: w,
      height: h,
      layout: {
        background: { type: ColorType.Solid, color: 'transparent' },
        textColor: textMuted,
        fontSize: 11,
        attributionLogo: false,
      },
      localization: {
        locale,
        dateFormat: "dd MMM ''yy" as const,
        priceFormatter: (p: number) =>
          new Intl.NumberFormat(locale, {
            minimumFractionDigits: 2,
            maximumFractionDigits: 3,
            signDisplay: 'exceptZero',
          }).format(p),
      },
      grid: {
        vertLines: { visible: false },
        horzLines: { color: gridH },
      },
      rightPriceScale: { visible: false },
      leftPriceScale: {
        visible: true,
        borderVisible: true,
        borderColor: border,
        scaleMargins: { top: 0.1, bottom: 0.08 },
      },
      timeScale: {
        borderVisible: true,
        borderColor: border,
        fixLeftEdge: false,
        fixRightEdge: false,
      },
      crosshair: {
        mode: CrosshairMode.Magnet,
        vertLine: { color: 'rgba(148, 163, 184, 0.35)', labelBackgroundColor: labelBg },
        horzLine: { color: 'rgba(148, 163, 184, 0.35)', labelBackgroundColor: labelBg },
      },
      handleScroll: { mouseWheel: true, pressedMouseMove: true, horzTouchDrag: true, vertTouchDrag: false },
      handleScale: { axisPressedMouseMove: true, mouseWheel: true, pinch: true },
    })

    const series = chart.addSeries(LineSeries, {
      color,
      lineWidth: 2,
      priceScaleId: 'left',
      priceLineVisible: false,
      lastValueVisible: true,
    })
    series.setData(lineData)
    chart.timeScale().fitContent()
    chartRef.current = chart

    const clickHandler = (param: MouseEventParams<Time>) => {
      if (!pickActiveRef.current || !onPickRef.current || !param.point) return
      const day = timeToIsoDay(param.time as Time)
      if (day) onPickRef.current(day)
    }
    chart.subscribeClick(clickHandler)

    let disposed = false
    const ro = new ResizeObserver(() => {
      if (disposed || !outer.isConnected) return
      const nw = Math.max(outer.clientWidth, 200)
      const nh = Math.max(outer.clientHeight, 200)
      try {
        chart.applyOptions({ width: nw, height: nh })
      } catch {
        /* removed */
      }
    })
    ro.observe(outer)

    return () => {
      disposed = true
      ro.disconnect()
      try {
        chart.unsubscribeClick(clickHandler)
      } catch {
        /* noop */
      }
      chartRef.current = null
      try {
        chart.remove()
      } catch {
        /* race */
      }
    }
  }, [loadState, lineData, isDark, locale, color])

  return (
    <div className="fi-faiz-chart-wrap fi-faiz-policy-panel-chart-wrap">
      {loadState === 'loading' ? (
        <div className="fi-faiz-policy-panel-chart-outer markets-skeleton-row" aria-busy="true" style={{ minHeight: 200 }} />
      ) : null}
      {loadState === 'error' ? <p className="fi-faiz-policy-panel-state">{EM}</p> : null}
      {loadState === 'ready' && lineData.length === 0 ? <p className="fi-faiz-policy-panel-state">{EM}</p> : null}
      {loadState === 'ready' && lineData.length > 0 ? (
        <div ref={outerRef} className="fi-faiz-policy-panel-chart-outer fi-faiz-eurobond-chart-h">
          <div ref={mountRef} className="fi-faiz-policy-panel-chart-mount" />
        </div>
      ) : null}
    </div>
  )
}

export function FaizVadeliEurobondDetailPanel({
  instruments,
  selectedIsin,
  onSelectIsin,
  onBack,
}: {
  instruments: EurobondInstrumentWire[]
  selectedIsin: string
  onSelectIsin: (isin: string) => void
  onBack: () => void
}) {
  const { t, i18n } = useTranslation('common')
  const { theme } = useTheme()
  const isDark = theme === 'dark'
  const locale = i18n.resolvedLanguage ?? i18n.language ?? 'en'

  const [range, setRange] = useState<'1Y' | '5Y' | 'ALL'>('5Y')
  const [frequency, setFrequency] = useState<'DAILY' | 'WEEKLY' | 'MONTHLY'>('DAILY')
  const [histState, setHistState] = useState<'loading' | 'error' | 'ready'>('loading')
  const [histPoints, setHistPoints] = useState<EurobondHistoryPointWire[]>([])
  const [retryNonce, setRetryNonce] = useState(0)

  const [chartPickMode, setChartPickMode] = useState<EurobondChartPickMode>(null)
  const simRef = useRef<EurobondSimulatorHandle>(null)

  const priceLine = useMemo(() => toPriceSeries(histPoints), [histPoints])
  const yieldLine = useMemo(() => toYieldSeries(histPoints), [histPoints])

  useEffect(() => {
    let cancelled = false
    setHistState('loading')
    setHistPoints([])
    fetchTrEurobondHistory(selectedIsin, range, frequency)
      .then((h) => {
        if (cancelled) return
        setHistPoints(h?.points ?? [])
        setHistState('ready')
      })
      .catch(() => {
        if (!cancelled) setHistState('error')
      })
    return () => {
      cancelled = true
    }
  }, [selectedIsin, range, frequency, retryNonce])

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        if (chartPickMode) {
          setChartPickMode(null)
          e.preventDefault()
          return
        }
        onBack()
      }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onBack, chartPickMode])

  const fmtPrice = (v: number | null) =>
    v == null
      ? EM
      : new Intl.NumberFormat(locale, { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(v)
  const fmtPct = (v: number | null) =>
    v == null ? EM : `${new Intl.NumberFormat(locale, { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(v)}%`
  const handleChartTimePick = (iso: string) => {
    simRef.current?.handleChartPick(iso)
  }

  return (
    <div className="fi-faiz-panel fi-faiz-panel--chart fi-faiz-panel--policy-chart fi-faiz-eurobond-detail">
      <div className="fi-faiz-eurobond-head">
        <h3 className="fi-faiz-panel-title">{t('faizVadeliPage.eurobond.detailTitle')}</h3>
        <button type="button" className="fi-faiz-panel-back" onClick={onBack}>
          {t('faizVadeliPage.eurobond.chartBackToYield')}
        </button>
      </div>

      <div className="fi-faiz-eurobond-layout">
        <div className="fi-faiz-eurobond-main">
          <div className="fi-faiz-eurobond-edu" role="note">
            <ul>
              <li>{t('faizVadeliPage.eurobond.edu1')}</li>
              <li>{t('faizVadeliPage.eurobond.edu2')}</li>
              <li>{t('faizVadeliPage.eurobond.edu3')}</li>
              <li>{t('faizVadeliPage.eurobond.edu4')}</li>
            </ul>
          </div>

          <div className="fi-faiz-eurobond-table-wrap">
            <table className="fi-faiz-eurobond-table">
          <thead>
            <tr>
              <th>{t('faizVadeliPage.eurobond.colBond')}</th>
              <th>{t('faizVadeliPage.eurobond.colIsin')}</th>
              <th>{t('faizVadeliPage.eurobond.colMat')}</th>
              <th>{t('faizVadeliPage.eurobond.colRem')}</th>
              <th>{t('faizVadeliPage.eurobond.colCpn')}</th>
              <th>{t('faizVadeliPage.eurobond.colPx')}</th>
              <th>{t('faizVadeliPage.eurobond.colYtm')}</th>
              <th>{t('faizVadeliPage.eurobond.colChg')}</th>
              <th>{t('faizVadeliPage.eurobond.colSrc')}</th>
            </tr>
          </thead>
          <tbody>
            {instruments.map((row) => {
              const isin = (row.isin ?? '').toUpperCase()
              const active = isin === selectedIsin.toUpperCase()
              const mat = row.maturityDate ?? ''
              const rem = toNum(row.remainingYears)
              const cpn = toNum(row.couponPercent)
              const px = toNum(row.cleanPrice)
              const ytm = toNum(row.yieldToMaturityPercent)
              const d1 = toNum(row.dailyChangePercent)
              return (
                <tr
                  key={isin}
                  className={active ? 'fi-faiz-eurobond-row--active' : undefined}
                  onClick={() => onSelectIsin(isin)}
                  onKeyDown={(e) => {
                    if (e.key === 'Enter' || e.key === ' ') {
                      e.preventDefault()
                      onSelectIsin(isin)
                    }
                  }}
                  tabIndex={0}
                  role="button"
                  aria-selected={active}
                >
                  <td>{row.displayName ?? EM}</td>
                  <td className="fi-faiz-eurobond-mono">{isin || EM}</td>
                  <td>{mat || EM}</td>
                  <td>{rem == null ? EM : `${new Intl.NumberFormat(locale, { minimumFractionDigits: 1, maximumFractionDigits: 1 }).format(rem)} ${t('faizVadeliPage.eurobond.yearsSuffix')}`}</td>
                  <td>{cpn == null ? EM : fmtPct(cpn)}</td>
                  <td>{px == null ? EM : fmtPrice(px)}</td>
                  <td>{ytm == null ? EM : fmtPct(ytm)}</td>
                  <td>{d1 == null ? EM : fmtPct(d1)}</td>
                  <td>{row.sourceProvider ?? EM}</td>
                </tr>
              )
            })}
          </tbody>
            </table>
          </div>

          {chartPickMode ? (
            <div className="fi-faiz-eurobond-pick-banner" role="status">
              <span>
                {chartPickMode === 'purchasePrice' || chartPickMode === 'salePrice'
                  ? t('faizVadeliPage.eurobond.pastPickHintPrice')
                  : t('faizVadeliPage.eurobond.pastPickHintDate')}
              </span>
              <button type="button" className="fi-faiz-eurobond-pick-cancel" onClick={() => setChartPickMode(null)}>
                {t('faizVadeliPage.eurobond.pastPickCancel')}
              </button>
            </div>
          ) : null}

          <div className="fi-faiz-eurobond-controls">
            <span className="fi-faiz-eurobond-controls-label">{t('faizVadeliPage.eurobond.range')}</span>
            {(['1Y', '5Y', 'ALL'] as const).map((r) => (
              <button
                key={r}
                type="button"
                className={`fi-faiz-eurobond-pill${range === r ? ' fi-faiz-eurobond-pill--on' : ''}`}
                onClick={() => setRange(r)}
              >
                {r === '1Y'
                  ? t('faizVadeliPage.eurobond.range1Y')
                  : r === '5Y'
                    ? t('faizVadeliPage.eurobond.range5Y')
                    : t('faizVadeliPage.eurobond.rangeAll')}
              </button>
            ))}
            <span className="fi-faiz-eurobond-controls-label">{t('faizVadeliPage.eurobond.frequency')}</span>
            {(['DAILY', 'WEEKLY', 'MONTHLY'] as const).map((f) => (
              <button
                key={f}
                type="button"
                className={`fi-faiz-eurobond-pill${frequency === f ? ' fi-faiz-eurobond-pill--on' : ''}`}
                onClick={() => setFrequency(f)}
              >
                {f === 'DAILY'
                  ? t('faizVadeliPage.eurobond.freqDaily')
                  : f === 'WEEKLY'
                    ? t('faizVadeliPage.eurobond.freqWeekly')
                    : t('faizVadeliPage.eurobond.freqMonthly')}
              </button>
            ))}
          </div>

          {histState === 'error' ? (
            <div className="fi-faiz-policy-panel-state">
              <p>{t('faizVadeliPage.eurobond.chartError')}</p>
              <button type="button" className="profile-settings-btn-secondary" onClick={() => setRetryNonce((n) => n + 1)}>
                {t('faizVadeliPage.eurobond.chartRetry')}
              </button>
            </div>
          ) : null}

          <div className="fi-faiz-eurobond-charts">
            <div className="fi-faiz-eurobond-chart-block">
              <h4 className="fi-faiz-eurobond-subtitle">{t('faizVadeliPage.eurobond.priceChartTitle')}</h4>
              <p className="fi-faiz-eurobond-hint">{t('faizVadeliPage.eurobond.priceChartHint')}</p>
              <div className="fi-faiz-policy-chart-y-wrap">
                <span className="fi-faiz-policy-chart-y-label">{t('faizVadeliPage.eurobond.axisPrice')}</span>
                <EurobondLwChart
                  lineData={priceLine}
                  loadState={histState}
                  color="#22c55e"
                  locale={locale}
                  isDark={isDark}
                  chartPickActive={chartPickMode !== null}
                  onTimePicked={handleChartTimePick}
                />
              </div>
              {histState === 'ready' && priceLine.length === 0 ? (
                <p className="fi-faiz-policy-panel-state">{t('faizVadeliPage.eurobond.priceChartEmpty')}</p>
              ) : null}
            </div>

            <div className="fi-faiz-eurobond-chart-block">
              <h4 className="fi-faiz-eurobond-subtitle">{t('faizVadeliPage.eurobond.yieldChartTitle')}</h4>
              <p className="fi-faiz-eurobond-hint">{t('faizVadeliPage.eurobond.yieldChartHint')}</p>
              <div className="fi-faiz-policy-chart-y-wrap">
                <span className="fi-faiz-policy-chart-y-label">{t('faizVadeliPage.eurobond.axisYield')}</span>
                <EurobondLwChart
                  lineData={yieldLine}
                  loadState={histState}
                  color="#6366f1"
                  locale={locale}
                  isDark={isDark}
                  chartPickActive={chartPickMode !== null}
                  onTimePicked={handleChartTimePick}
                />
              </div>
              {histState === 'ready' && yieldLine.length === 0 ? (
                <p className="fi-faiz-policy-panel-state">{t('faizVadeliPage.eurobond.yieldChartEmpty')}</p>
              ) : null}
            </div>
          </div>
        </div>

        <div className="fi-faiz-eurobond-side">
          <EurobondSimulator
            ref={simRef}
            selectedIsin={selectedIsin}
            instruments={instruments}
            histPoints={histPoints}
            pickMode={chartPickMode}
            onPickModeChange={setChartPickMode}
          />
        </div>
      </div>

      <p className="fi-faiz-chart-foot">{t('faizVadeliPage.eurobond.detailFoot')}</p>
    </div>
  )
}
