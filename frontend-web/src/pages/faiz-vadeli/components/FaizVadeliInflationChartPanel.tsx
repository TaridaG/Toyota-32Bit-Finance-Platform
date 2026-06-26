import { useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import {
  ColorType,
  createChart,
  CrosshairMode,
  LineSeries,
  type LineData,
  type MouseEventParams,
  type Time,
} from 'lightweight-charts'
import { useTheme } from '../../../shared/theme/ThemeProvider'
import { fetchCpiHistory, type CpiHistoryPoint, type CpiMetricCode } from '../api/cpiApi'
import { InflationSimulator } from './simulators/InflationSimulator'

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

function toLineData(points: CpiHistoryPoint[]): LineData<Time>[] {
  const rows = (points ?? [])
    .map((p) => {
      const raw = p.value
      const num = typeof raw === 'number' ? raw : Number(raw)
      if (p.date == null || p.date === '' || Number.isNaN(num)) {
        return null
      }
      return { time: p.date as Time, value: num }
    })
    .filter((x): x is LineData<Time> => x != null)
  rows.sort((a, b) => String(a.time).localeCompare(String(b.time)))
  return rows
}

const METRICS: CpiMetricCode[] = ['YEARLY_PCT', 'MONTHLY_PCT', 'INDEX']

export function FaizVadeliInflationChartPanel({
  metric,
  onMetricChange,
}: {
  metric: CpiMetricCode
  onMetricChange: (m: CpiMetricCode) => void
}) {
  const { t, i18n } = useTranslation('common')
  const { theme } = useTheme()
  const isDark = theme === 'dark'
  const locale = i18n.resolvedLanguage ?? i18n.language ?? 'en'

  const [loadState, setLoadState] = useState<'loading' | 'error' | 'ready'>('loading')
  const [points, setPoints] = useState<CpiHistoryPoint[]>([])
  const [retryNonce, setRetryNonce] = useState(0)
  const [pickStartDateActive, setPickStartDateActive] = useState(false)
  const [chartPickIso, setChartPickIso] = useState<string | null>(null)

  const isIndex = metric === 'INDEX'
  const lineData = useMemo(() => toLineData(points), [points])

  useEffect(() => {
    let cancelled = false
    setLoadState('loading')
    setPoints([])
    fetchCpiHistory(metric)
      .then((r) => {
        if (cancelled) return
        setPoints(r.points ?? [])
        setLoadState('ready')
      })
      .catch(() => {
        if (cancelled) return
        setLoadState('error')
      })
    return () => {
      cancelled = true
    }
  }, [metric, retryNonce])

  const outerRef = useRef<HTMLDivElement | null>(null)
  const mountRef = useRef<HTMLDivElement | null>(null)
  const pickActiveRef = useRef(false)
  const onPickRef = useRef<(iso: string) => void>(() => {})

  useEffect(() => {
    pickActiveRef.current = pickStartDateActive
  }, [pickStartDateActive])

  useEffect(() => {
    onPickRef.current = (iso: string) => {
      setChartPickIso(iso)
      setPickStartDateActive(false)
    }
  }, [])

  useLayoutEffect(() => {
    if (loadState !== 'ready' || lineData.length === 0) {
      return
    }
    const outer = outerRef.current
    const mount = mountRef.current
    if (!outer || !mount) return

    const textMuted = isDark ? '#94a3b8' : '#64748b'
    const gridH = isDark ? 'rgba(148, 163, 184, 0.12)' : 'rgba(100, 116, 139, 0.14)'
    const labelBg = isDark ? '#3a3a3a' : '#e2e8f0'
    const border = isDark ? 'rgba(148,163,184,0.2)' : 'rgba(100,116,139,0.25)'

    const w = Math.max(outer.clientWidth, 200)
    const h = Math.max(outer.clientHeight, 240)

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
        priceFormatter: (p: number) => {
          const nf = new Intl.NumberFormat(locale, {
            minimumFractionDigits: isIndex ? 0 : 2,
            maximumFractionDigits: isIndex ? 2 : 2,
          })
          return isIndex ? nf.format(p) : `${nf.format(p)}%`
        },
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
      color: '#f59e0b',
      lineWidth: 2,
      priceScaleId: 'left',
      priceLineVisible: false,
      lastValueVisible: true,
    })
    series.setData(lineData)
    chart.timeScale().fitContent()

    const clickHandler = (param: MouseEventParams<Time>) => {
      if (!pickActiveRef.current || !param.point) return
      const day = timeToIsoDay(param.time as Time)
      if (day) onPickRef.current(day)
    }
    chart.subscribeClick(clickHandler)

    let disposed = false
    const ro = new ResizeObserver(() => {
      if (disposed || !outer.isConnected) return
      const nw = Math.max(outer.clientWidth, 200)
      const nh = Math.max(outer.clientHeight, 240)
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
      try {
        chart.remove()
      } catch {
        /* race */
      }
    }
  }, [loadState, lineData, isDark, locale, isIndex])

  const axisLabel = isIndex
    ? t('faizVadeliPage.inflation.chartAxisIndex')
    : t('faizVadeliPage.inflation.chartAxisPct')

  return (
    <div className="fi-faiz-panel fi-faiz-panel--chart fi-faiz-panel--policy-chart fi-faiz-panel--inflation-chart">
      <div className="fi-faiz-inflation-chart-head">
        <h3 className="fi-faiz-panel-title">{t('faizVadeliPage.inflation.chartTitle')}</h3>
        <div
          className="fi-faiz-inflation-metric-tabs"
          role="tablist"
          aria-label={t('faizVadeliPage.inflation.metricTabsAria')}
        >
          {METRICS.map((m) => (
            <button
              key={m}
              type="button"
              role="tab"
              aria-selected={metric === m}
              className={`fi-faiz-inflation-metric-tab${metric === m ? ' fi-faiz-inflation-metric-tab--active' : ''}`}
              onClick={() => onMetricChange(m)}
            >
              {t(`faizVadeliPage.inflation.metric.${m}`)}
            </button>
          ))}
        </div>
      </div>

      {pickStartDateActive ? (
        <div className="fi-faiz-eurobond-pick-banner" role="status">
          <span>{t('faizVadeliPage.simulator.inflation.pickHint')}</span>
          <button
            type="button"
            className="fi-faiz-eurobond-pick-cancel"
            onClick={() => setPickStartDateActive(false)}
          >
            {t('faizVadeliPage.simulator.inflation.pickCancel')}
          </button>
        </div>
      ) : null}

      <div className="fi-faiz-simulator-layout">
        <div className="fi-faiz-simulator-layout-charts">
          <div className="fi-faiz-policy-chart-y-wrap">
            <span className="fi-faiz-policy-chart-y-label">{axisLabel}</span>
            <div className="fi-faiz-chart-wrap fi-faiz-policy-panel-chart-wrap">
              {loadState === 'loading' ? (
                <div
                  className="fi-faiz-policy-panel-chart-outer markets-skeleton-row"
                  aria-busy="true"
                  aria-label={t('faizVadeliPage.inflation.chartLoading')}
                />
              ) : null}

              {loadState === 'error' ? (
                <div className="fi-faiz-policy-panel-state">
                  <p>{t('faizVadeliPage.inflation.chartError')}</p>
                  <button
                    type="button"
                    className="profile-settings-btn-secondary"
                    onClick={() => setRetryNonce((n) => n + 1)}
                  >
                    {t('faizVadeliPage.inflation.chartRetry')}
                  </button>
                </div>
              ) : null}

              {loadState === 'ready' && lineData.length === 0 ? (
                <p className="fi-faiz-policy-panel-state">{t('faizVadeliPage.inflation.chartEmpty')}</p>
              ) : null}

              {loadState === 'ready' && lineData.length > 0 ? (
                <div ref={outerRef} className="fi-faiz-policy-panel-chart-outer">
                  <div ref={mountRef} className="fi-faiz-policy-panel-chart-mount" />
                </div>
              ) : null}
            </div>
          </div>
        </div>

        <InflationSimulator
          chartStartDate={chartPickIso}
          pickStartDateActive={pickStartDateActive}
          onRequestPickStartDate={() => setPickStartDateActive(true)}
        />
      </div>

      <p className="fi-faiz-chart-foot">{t('faizVadeliPage.inflation.chartFoot')}</p>
    </div>
  )
}
