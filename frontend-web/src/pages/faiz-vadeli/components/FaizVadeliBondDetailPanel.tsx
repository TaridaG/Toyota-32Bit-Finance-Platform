import { useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import {
  ColorType,
  createChart,
  CrosshairMode,
  LineSeries,
  type LineData,
  type Time,
} from 'lightweight-charts'
import { useTheme } from '../../../shared/theme/ThemeProvider'
import {
  fetchBondYieldCurveLatest,
  fetchBondYieldHistory,
  type BondYieldCurvePoint,
  type PolicyRateHistoryPoint,
} from '../api/bondMarketApi'
import type { BondTenorCode } from '../lib/bondTenor'
import { BondSimulator } from './simulators/BondSimulator'

function toLineData(points: PolicyRateHistoryPoint[]): LineData<Time>[] {
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

function curveMaxYield(points: BondYieldCurvePoint[]): number {
  let max = 0
  for (const p of points) {
    const v = p.value == null ? NaN : Number(p.value)
    if (Number.isFinite(v) && v > max) max = v
  }
  return max > 0 ? max : 1
}

export function FaizVadeliBondDetailPanel({
  tenor,
  onBack,
}: {
  tenor: BondTenorCode
  onBack: () => void
}) {
  const { t, i18n } = useTranslation('common')
  const { theme } = useTheme()
  const isDark = theme === 'dark'
  const locale = i18n.resolvedLanguage ?? i18n.language ?? 'en'

  const [loadState, setLoadState] = useState<'loading' | 'error' | 'ready'>('loading')
  const [points, setPoints] = useState<PolicyRateHistoryPoint[]>([])
  const [curvePoints, setCurvePoints] = useState<BondYieldCurvePoint[]>([])
  const [retryNonce, setRetryNonce] = useState(0)
  const [chartYield, setChartYield] = useState<number | null>(null)

  const lineData = useMemo(() => toLineData(points), [points])
  const curveMax = useMemo(() => curveMaxYield(curvePoints), [curvePoints])

  useEffect(() => {
    let cancelled = false
    setLoadState('loading')
    setPoints([])
    Promise.all([fetchBondYieldHistory(tenor), fetchBondYieldCurveLatest()])
      .then(([hist, curve]) => {
        if (cancelled) return
        setPoints(hist.points ?? [])
        setCurvePoints(curve.points ?? [])
        const last = (hist.points ?? []).at(-1)
        const v = last?.value
        setChartYield(v == null ? null : Number(v))
        setLoadState('ready')
      })
      .catch(() => {
        if (!cancelled) setLoadState('error')
      })
    return () => {
      cancelled = true
    }
  }, [retryNonce, tenor])

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') onBack()
    }
    window.addEventListener('keydown', onKey)
    return () => window.removeEventListener('keydown', onKey)
  }, [onBack])

  const outerRef = useRef<HTMLDivElement | null>(null)
  const mountRef = useRef<HTMLDivElement | null>(null)

  useLayoutEffect(() => {
    if (loadState !== 'ready' || lineData.length === 0) {
      return
    }
    const outer = outerRef.current
    const mount = mountRef.current
    if (!outer || !mount) return

    const textMuted = isDark ? '#94a3b8' : '#64748b'
    const gridH = isDark ? 'rgba(148, 163, 184, 0.12)' : 'rgba(100, 116, 139, 0.14)'
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
      },
      handleScroll: { mouseWheel: true, pressedMouseMove: true, horzTouchDrag: true, vertTouchDrag: false },
      handleScale: { axisPressedMouseMove: true, mouseWheel: true, pinch: true },
    })

    const series = chart.addSeries(LineSeries, {
      color: isDark ? '#f59e0b' : '#d97706',
      lineWidth: 2,
      priceScaleId: 'left',
      priceLineVisible: false,
      lastValueVisible: true,
    })
    series.setData(lineData)
    chart.timeScale().fitContent()

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
        chart.remove()
      } catch {
        /* race */
      }
    }
  }, [loadState, lineData, isDark, locale])

  const fmtPct = (v: number | null) =>
    v == null ? '—' : `${new Intl.NumberFormat(locale, { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(v)}%`

  const tenorLabel = t(`faizVadeliPage.bond.tenorShort.${tenor}`)

  return (
    <div className="fi-faiz-panel fi-faiz-panel--chart fi-faiz-panel--policy-chart fi-faiz-bond-detail">
      <div className="fi-faiz-panel-head-row">
        <h3 className="fi-faiz-panel-title">
          {t('faizVadeliPage.bond.detailTitle', { tenor: tenorLabel })}
        </h3>
        <button type="button" className="fi-faiz-panel-back" onClick={onBack}>
          {t('faizVadeliPage.bond.chartBackToYield')}
        </button>
      </div>

      <div className="fi-faiz-bond-edu" role="note">
        <ul>
          <li>{t('faizVadeliPage.bond.edu1')}</li>
          <li>{t('faizVadeliPage.bond.edu2')}</li>
          <li>{t('faizVadeliPage.bond.edu3')}</li>
        </ul>
      </div>

      <div className="fi-faiz-simulator-layout">
        <div className="fi-faiz-simulator-layout-charts">
          {loadState === 'ready' && curvePoints.length > 0 ? (
            <div className="fi-faiz-bond-curve-block">
              <h4 className="fi-faiz-bond-subtitle">{t('faizVadeliPage.bond.curveTitle')}</h4>
              <p className="fi-faiz-bond-hint">{t('faizVadeliPage.bond.curveHint')}</p>
              <div className="fi-faiz-bond-curve" role="img" aria-label={t('faizVadeliPage.bond.curveTitle')}>
                {curvePoints.map((row) => {
                  const y = row.value == null ? null : Number(row.value)
                  const widthPct = y == null || !Number.isFinite(y) ? 0 : Math.max(4, (y / curveMax) * 100)
                  const active = (row.tenor ?? '').toUpperCase() === tenor
                  const bp = row.change1dBasisPoints
                  return (
                    <div
                      key={row.tenor ?? row.symbol}
                      className={`fi-faiz-bond-curve-row${active ? ' fi-faiz-bond-curve-row--active' : ''}`}
                    >
                      <span className="fi-faiz-bond-curve-tenor">{row.tenor ?? '—'}</span>
                      <div className="fi-faiz-bond-curve-bar-track">
                        <div className="fi-faiz-bond-curve-bar-fill" style={{ width: `${widthPct}%` }} />
                      </div>
                      <span className="fi-faiz-bond-curve-yield">{fmtPct(y)}</span>
                      <span className="fi-faiz-bond-curve-d1">
                        {bp == null || !Number.isFinite(bp)
                          ? ''
                          : t('faizVadeliPage.bond.curveChange1d', {
                              bp: `${bp > 0 ? '+' : ''}${bp}`,
                            })}
                      </span>
                    </div>
                  )
                })}
              </div>
            </div>
          ) : null}

          <div className="fi-faiz-policy-chart-y-wrap">
            <span className="fi-faiz-policy-chart-y-label">{t('faizVadeliPage.bond.chartAxisY')}</span>
            <div className="fi-faiz-chart-wrap fi-faiz-policy-panel-chart-wrap">
              {loadState === 'loading' ? (
                <div
                  className="fi-faiz-policy-panel-chart-outer markets-skeleton-row"
                  aria-busy="true"
                  aria-label={t('faizVadeliPage.bond.chartLoading')}
                />
              ) : null}

              {loadState === 'error' ? (
                <div className="fi-faiz-policy-panel-state">
                  <p>{t('faizVadeliPage.bond.chartError')}</p>
                  <button
                    type="button"
                    className="profile-settings-btn-secondary"
                    onClick={() => setRetryNonce((n) => n + 1)}
                  >
                    {t('faizVadeliPage.bond.chartRetry')}
                  </button>
                </div>
              ) : null}

              {loadState === 'ready' && lineData.length === 0 ? (
                <p className="fi-faiz-policy-panel-state">{t('faizVadeliPage.bond.chartEmpty')}</p>
              ) : null}

              {loadState === 'ready' && lineData.length > 0 ? (
                <div ref={outerRef} className="fi-faiz-policy-panel-chart-outer">
                  <div ref={mountRef} className="fi-faiz-policy-panel-chart-mount" />
                </div>
              ) : null}
            </div>
          </div>
        </div>

        <BondSimulator tenor={tenor} chartYieldPercent={chartYield} />
      </div>

      <p className="fi-faiz-chart-foot">{t('faizVadeliPage.bond.chartFoot')}</p>
    </div>
  )
}
