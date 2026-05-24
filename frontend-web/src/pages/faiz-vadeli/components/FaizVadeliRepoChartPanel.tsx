import { useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import {
  ColorType,
  createChart,
  CrosshairMode,
  HistogramSeries,
  type HistogramData,
  type Time,
} from 'lightweight-charts'
import { useTheme } from '../../../shared/theme/ThemeProvider'
import { fetchRepoRateHistory, type PolicyRateHistoryPoint } from '../api/repoRateApi'

function toHistogramData(points: PolicyRateHistoryPoint[]): HistogramData<Time>[] {
  const rows = (points ?? [])
    .map((p) => {
      const raw = p.value
      const num = typeof raw === 'number' ? raw : Number(raw)
      if (p.date == null || p.date === '' || Number.isNaN(num)) {
        return null
      }
      return { time: p.date as Time, value: num }
    })
    .filter((x): x is HistogramData<Time> => x != null)
  rows.sort((a, b) => String(a.time).localeCompare(String(b.time)))
  return rows
}

export type FaizVadeliRepoChartPanelProps = {
  onBack?: () => void
}

export function FaizVadeliRepoChartPanel({ onBack }: FaizVadeliRepoChartPanelProps) {
  const { t, i18n } = useTranslation('common')
  const { theme } = useTheme()
  const isDark = theme === 'dark'
  const locale = i18n.resolvedLanguage ?? i18n.language ?? 'en'

  const [loadState, setLoadState] = useState<'loading' | 'error' | 'ready'>('loading')
  const [points, setPoints] = useState<PolicyRateHistoryPoint[]>([])
  const [retryNonce, setRetryNonce] = useState(0)

  const barData = useMemo(() => toHistogramData(points), [points])

  useEffect(() => {
    let cancelled = false
    setLoadState('loading')
    setPoints([])
    fetchRepoRateHistory()
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
  }, [retryNonce])

  const outerRef = useRef<HTMLDivElement | null>(null)
  const mountRef = useRef<HTMLDivElement | null>(null)

  useLayoutEffect(() => {
    if (loadState !== 'ready' || barData.length === 0) {
      return
    }
    const outer = outerRef.current
    const mount = mountRef.current
    if (!outer || !mount) return

    const textMuted = isDark ? '#94a3b8' : '#64748b'
    const gridH = isDark ? 'rgba(148, 163, 184, 0.12)' : 'rgba(100, 116, 139, 0.14)'
    const border = isDark ? 'rgba(148,163,184,0.2)' : 'rgba(100,116,139,0.25)'
    const barColor = isDark ? '#3b82f6' : '#2563eb'

    const w = Math.max(outer.clientWidth, 200)
    const h = Math.max(outer.clientHeight, 280)

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
      rightPriceScale: {
        borderColor: border,
        scaleMargins: { top: 0.12, bottom: 0.08 },
      },
      timeScale: {
        borderColor: border,
        fixLeftEdge: true,
        fixRightEdge: true,
      },
      crosshair: { mode: CrosshairMode.Magnet },
    })

    const series = chart.addSeries(HistogramSeries, {
      color: barColor,
      priceFormat: { type: 'custom', formatter: (p: number) => p.toFixed(2) + '%' },
    })
    series.setData(barData)
    chart.timeScale().fitContent()

    const ro = new ResizeObserver(() => {
      const nw = Math.max(outer.clientWidth, 200)
      const nh = Math.max(outer.clientHeight, 280)
      chart.applyOptions({ width: nw, height: nh })
    })
    ro.observe(outer)

    return () => {
      ro.disconnect()
      chart.remove()
    }
  }, [loadState, barData, isDark, locale])

  return (
    <div className="fi-faiz-panel fi-faiz-panel--chart fi-faiz-panel--policy-chart fi-faiz-panel--repo-chart">
      <div className="fi-faiz-panel-head-row">
        <h3 className="fi-faiz-panel-title">{t('faizVadeliPage.repo.chartTitle')}</h3>
        {onBack ? (
          <button type="button" className="fi-faiz-panel-back" onClick={onBack}>
            {t('faizVadeliPage.repo.chartBackToYield')}
          </button>
        ) : null}
      </div>

      <div className="fi-faiz-policy-chart-y-wrap">
        <span className="fi-faiz-policy-chart-y-label">{t('faizVadeliPage.repo.chartAxisY')}</span>
        <div className="fi-faiz-chart-wrap fi-faiz-policy-panel-chart-wrap">
          {loadState === 'loading' ? (
            <div
              className="fi-faiz-policy-panel-chart-outer markets-skeleton-row"
              aria-busy="true"
              aria-label={t('faizVadeliPage.repo.chartLoading')}
            />
          ) : loadState === 'error' ? (
            <div className="fi-faiz-policy-panel-state">
              <p>{t('faizVadeliPage.repo.chartError')}</p>
              <button type="button" className="portal-button portal-button-secondary" onClick={() => setRetryNonce((n) => n + 1)}>
                {t('faizVadeliPage.repo.chartRetry')}
              </button>
            </div>
          ) : barData.length === 0 ? (
            <p className="fi-faiz-policy-panel-state">{t('faizVadeliPage.repo.chartEmpty')}</p>
          ) : (
            <div ref={outerRef} className="fi-faiz-policy-panel-chart-outer">
              <div ref={mountRef} className="fi-faiz-policy-panel-chart-mount" />
            </div>
          )}
        </div>
      </div>
      <p className="fi-faiz-chart-foot">{t('faizVadeliPage.repo.chartFoot')}</p>
    </div>
  )
}
