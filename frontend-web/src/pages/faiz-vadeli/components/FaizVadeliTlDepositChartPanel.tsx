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
import { fetchTlDepositHistory, type TlDepositHistoryPoint } from '../api/tlDepositApi'
import type { TlDepositMaturityCode } from '../lib/tlDepositMaturity'
import { TlDepositSimulator } from './simulators/TlDepositSimulator'

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

function toLineData(points: TlDepositHistoryPoint[]): LineData<Time>[] {
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

export function FaizVadeliTlDepositChartPanel({
  maturity,
  onBack,
}: {
  maturity: TlDepositMaturityCode
  onBack: () => void
}) {
  const { t, i18n } = useTranslation('common')
  const { theme } = useTheme()
  const isDark = theme === 'dark'
  const locale = i18n.resolvedLanguage ?? i18n.language ?? 'en'

  const [loadState, setLoadState] = useState<'loading' | 'error' | 'ready'>('loading')
  const [points, setPoints] = useState<TlDepositHistoryPoint[]>([])
  const [retryNonce, setRetryNonce] = useState(0)
  const [pickDepositDateActive, setPickDepositDateActive] = useState(false)
  const [chartPickIso, setChartPickIso] = useState<string | null>(null)

  const lineData = useMemo(() => toLineData(points), [points])

  useEffect(() => {
    let cancelled = false
    setLoadState('loading')
    setPoints([])
    fetchTlDepositHistory(maturity)
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
  }, [retryNonce, maturity])

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        if (pickDepositDateActive) {
          setPickDepositDateActive(false)
          e.preventDefault()
          return
        }
        onBack()
      }
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onBack, pickDepositDateActive])

  const outerRef = useRef<HTMLDivElement | null>(null)
  const mountRef = useRef<HTMLDivElement | null>(null)
  const pickActiveRef = useRef(false)
  const onPickRef = useRef<(iso: string) => void>(() => {})

  useEffect(() => {
    pickActiveRef.current = pickDepositDateActive
  }, [pickDepositDateActive])

  useEffect(() => {
    onPickRef.current = (iso: string) => {
      setChartPickIso(iso)
      setPickDepositDateActive(false)
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
        priceFormatter: (p: number) =>
          new Intl.NumberFormat(locale, { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(p) + '%',
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
      color: '#ca8a04',
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
  }, [loadState, lineData, isDark, locale])

  const maturityLabel = t(`faizVadeliPage.tlDeposit.maturityLong.${maturity}`)

  return (
    <div className="fi-faiz-panel fi-faiz-panel--chart fi-faiz-panel--policy-chart">
      <div className="fi-faiz-panel-head-row">
        <h3 className="fi-faiz-panel-title">
          {t('faizVadeliPage.tlDeposit.chartTitleWithMaturity', { maturity: maturityLabel })}
        </h3>
        <button type="button" className="fi-faiz-panel-back" onClick={onBack}>
          {t('faizVadeliPage.tlDeposit.chartBackToYield')}
        </button>
      </div>

      {pickDepositDateActive ? (
        <div className="fi-faiz-eurobond-pick-banner" role="status">
          <span>{t('faizVadeliPage.simulator.deposit.pickHint')}</span>
          <button type="button" className="fi-faiz-eurobond-pick-cancel" onClick={() => setPickDepositDateActive(false)}>
            {t('faizVadeliPage.simulator.deposit.pickCancel')}
          </button>
        </div>
      ) : null}

      <div className="fi-faiz-simulator-layout">
        <div className="fi-faiz-simulator-layout-charts">
          <div className="fi-faiz-policy-chart-y-wrap">
            <span className="fi-faiz-policy-chart-y-label">{t('faizVadeliPage.tlDeposit.chartAxisY')}</span>
            <div className="fi-faiz-chart-wrap fi-faiz-policy-panel-chart-wrap">
              {loadState === 'loading' ? (
                <div
                  className="fi-faiz-policy-panel-chart-outer markets-skeleton-row"
                  aria-busy="true"
                  aria-label={t('faizVadeliPage.tlDeposit.chartLoading')}
                />
              ) : null}

              {loadState === 'error' ? (
                <div className="fi-faiz-policy-panel-state">
                  <p>{t('faizVadeliPage.tlDeposit.chartError')}</p>
                  <button
                    type="button"
                    className="profile-settings-btn-secondary"
                    onClick={() => setRetryNonce((n) => n + 1)}
                  >
                    {t('faizVadeliPage.tlDeposit.chartRetry')}
                  </button>
                </div>
              ) : null}

              {loadState === 'ready' && lineData.length === 0 ? (
                <p className="fi-faiz-policy-panel-state">{t('faizVadeliPage.tlDeposit.chartEmpty')}</p>
              ) : null}

              {loadState === 'ready' && lineData.length > 0 ? (
                <div ref={outerRef} className="fi-faiz-policy-panel-chart-outer">
                  <div ref={mountRef} className="fi-faiz-policy-panel-chart-mount" />
                </div>
              ) : null}
            </div>
          </div>
        </div>

        <TlDepositSimulator
          maturity={maturity}
          chartEndDate={chartPickIso}
          pickDepositDateActive={pickDepositDateActive}
          onRequestPickDepositDate={() => setPickDepositDateActive(true)}
        />
      </div>

      <p className="fi-faiz-chart-foot">{t('faizVadeliPage.tlDeposit.chartFoot')}</p>
    </div>
  )
}
