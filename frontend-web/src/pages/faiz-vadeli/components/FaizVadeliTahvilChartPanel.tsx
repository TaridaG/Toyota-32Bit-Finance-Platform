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
import { fetchTahvilHistory, type MarketHistoryPoint } from '../api/tahvilMarketApi'
import type { TahvilSymbol } from '../lib/tahvilSymbol'

function historyToLineData(points: MarketHistoryPoint[]): LineData<Time>[] {
  /** One bar per calendar day; last observation wins (API may return duplicate days). */
  const byDay = new Map<string, number>()
  for (const p of points ?? []) {
    const raw = p.value
    const num = typeof raw === 'number' ? raw : Number(raw)
    const t = p.time
    if (t == null || t === '' || Number.isNaN(num)) {
      continue
    }
    const ms = Date.parse(String(t))
    if (Number.isNaN(ms)) {
      continue
    }
    const day = new Date(ms).toISOString().slice(0, 10)
    byDay.set(day, num)
  }
  return [...byDay.entries()]
    .sort(([a], [b]) => a.localeCompare(b))
    .map(([day, value]) => ({ time: day as Time, value }))
}

export function FaizVadeliTahvilChartPanel({
  symbol,
  onBack,
}: {
  symbol: TahvilSymbol
  onBack: () => void
}) {
  const { t, i18n } = useTranslation('common')
  const { theme } = useTheme()
  const isDark = theme === 'dark'
  const locale = i18n.resolvedLanguage ?? i18n.language ?? 'en'

  const [loadState, setLoadState] = useState<'loading' | 'error' | 'ready'>('loading')
  const [points, setPoints] = useState<MarketHistoryPoint[]>([])
  const [retryNonce, setRetryNonce] = useState(0)

  const lineData = useMemo(() => historyToLineData(points), [points])

  useEffect(() => {
    let cancelled = false
    setLoadState('loading')
    setPoints([])
    fetchTahvilHistory(symbol)
      .then((r) => {
        if (cancelled) return
        setPoints(r ?? [])
        setLoadState('ready')
      })
      .catch(() => {
        if (cancelled) return
        setLoadState('error')
      })
    return () => {
      cancelled = true
    }
  }, [retryNonce, symbol])

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        onBack()
      }
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
    const labelBg = isDark ? '#334155' : '#e2e8f0'
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
      color: '#0284c7',
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
        chart.remove()
      } catch {
        /* race */
      }
    }
  }, [loadState, lineData, isDark, locale])

  const tenorLabel = t(`faizVadeliPage.tahvil.symbolLabel.${symbol}`)

  return (
    <div className="fi-faiz-panel fi-faiz-panel--chart fi-faiz-panel--policy-chart">
      <div className="fi-faiz-panel-head-row">
        <h3 className="fi-faiz-panel-title">{t('faizVadeliPage.tahvil.chartTitleWithTenor', { tenor: tenorLabel })}</h3>
        <button type="button" className="fi-faiz-panel-back" onClick={onBack}>
          {t('faizVadeliPage.tahvil.chartBackToYield')}
        </button>
      </div>

      <div className="fi-faiz-policy-chart-y-wrap">
        <span className="fi-faiz-policy-chart-y-label">{t('faizVadeliPage.tahvil.chartAxisY')}</span>
        <div className="fi-faiz-chart-wrap fi-faiz-policy-panel-chart-wrap">
          {loadState === 'loading' ? (
            <div
              className="fi-faiz-policy-panel-chart-outer markets-skeleton-row"
              aria-busy="true"
              aria-label={t('faizVadeliPage.tahvil.chartLoading')}
            />
          ) : null}

          {loadState === 'error' ? (
            <div className="fi-faiz-policy-panel-state">
              <p>{t('faizVadeliPage.tahvil.chartError')}</p>
              <button type="button" className="profile-settings-btn-secondary" onClick={() => setRetryNonce((n) => n + 1)}>
                {t('faizVadeliPage.tahvil.chartRetry')}
              </button>
            </div>
          ) : null}

          {loadState === 'ready' && lineData.length === 0 ? (
            <p className="fi-faiz-policy-panel-state">{t('faizVadeliPage.tahvil.chartEmpty')}</p>
          ) : null}

          {loadState === 'ready' && lineData.length > 0 ? (
            <div ref={outerRef} className="fi-faiz-policy-panel-chart-outer">
              <div ref={mountRef} className="fi-faiz-policy-panel-chart-mount" />
            </div>
          ) : null}
        </div>
      </div>

      <p className="fi-faiz-chart-foot">{t('faizVadeliPage.tahvil.chartFoot')}</p>
    </div>
  )
}
