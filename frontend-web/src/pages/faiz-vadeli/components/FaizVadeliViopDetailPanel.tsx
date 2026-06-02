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
  fetchTahvilSummary,
  fetchViopActiveContracts,
  fetchViopContractHistory,
  type ViopActiveContract,
} from '../api/tahvilMarketApi'

type AliasSummary = { symbol: string; price?: number | null; change1D?: number | null }

function fmtNum(value: number | null | undefined, locale: string, digits = 2): string {
  if (value == null || Number.isNaN(value)) return '—'
  return new Intl.NumberFormat(locale, { minimumFractionDigits: digits, maximumFractionDigits: digits }).format(value)
}

function toPct(value: number | null | undefined, locale: string): string {
  if (value == null || Number.isNaN(value)) return '—'
  return `${new Intl.NumberFormat(locale, { minimumFractionDigits: 2, maximumFractionDigits: 2, signDisplay: 'exceptZero' }).format(value)}%`
}

function toLine(points: Array<{ time?: string | null; value?: number | null }>): LineData<Time>[] {
  const m = new Map<string, number>()
  for (const p of points) {
    if (!p?.time || p.value == null || Number.isNaN(Number(p.value))) continue
    const day = String(p.time).slice(0, 10)
    m.set(day, Number(p.value))
  }
  return [...m.entries()].sort(([a], [b]) => a.localeCompare(b)).map(([time, value]) => ({ time: time as Time, value }))
}

export function FaizVadeliViopDetailPanel({ onBack }: { onBack: () => void }) {
  const { t, i18n } = useTranslation('common')
  const { theme } = useTheme()
  const locale = i18n.resolvedLanguage ?? i18n.language ?? 'en'
  const isDark = theme === 'dark'

  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [contracts, setContracts] = useState<ViopActiveContract[]>([])
  const [selectedCode, setSelectedCode] = useState<string | null>(null)
  const [aliasRows, setAliasRows] = useState<AliasSummary[]>([])
  const [history, setHistory] = useState<Array<{ time?: string | null; value?: number | null }>>([])
  const [historyState, setHistoryState] = useState<'loading' | 'ready' | 'error'>('loading')
  const [query, setQuery] = useState('')

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError(null)
    Promise.all([
      fetchViopActiveContracts(),
      Promise.all(['VIOP_TLREF_NEAR', 'VIOP_DIBS_NEAR', 'VIOP_FAIZ_NEAR'].map(async (symbol) => ({ symbol, ...(await fetchTahvilSummary(symbol)) }))),
    ])
      .then(([rows, aliases]) => {
        if (cancelled) return
        setContracts(rows)
        setAliasRows(aliases)
        setSelectedCode(rows[0]?.contractCode ?? null)
      })
      .catch(() => {
        if (!cancelled) setError(t('faizVadeliPage.tahvil.detailLoadError'))
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [t])

  useEffect(() => {
    if (!selectedCode) {
      setHistory([])
      setHistoryState('ready')
      return
    }
    let cancelled = false
    setHistoryState('loading')
    fetchViopContractHistory(selectedCode)
      .then((rows) => {
        if (cancelled) return
        setHistory(rows)
        setHistoryState('ready')
      })
      .catch(() => {
        if (!cancelled) {
          setHistory([])
          setHistoryState('error')
        }
      })
    return () => {
      cancelled = true
    }
  }, [selectedCode])

  const filtered = useMemo(() => {
    const q = query.trim().toUpperCase()
    if (!q) return contracts
    return contracts.filter((row) => `${row.contractCode} ${row.underlying ?? ''}`.toUpperCase().includes(q))
  }, [contracts, query])

  useEffect(() => {
    if (!filtered.some((r) => r.contractCode === selectedCode)) {
      setSelectedCode(filtered[0]?.contractCode ?? null)
    }
  }, [filtered, selectedCode])

  const selected = filtered.find((r) => r.contractCode === selectedCode) ?? filtered[0] ?? null
  const lineData = useMemo(() => toLine(history), [history])
  const outerRef = useRef<HTMLDivElement | null>(null)
  const mountRef = useRef<HTMLDivElement | null>(null)

  useLayoutEffect(() => {
    if (historyState !== 'ready' || lineData.length === 0) return
    const outer = outerRef.current
    const mount = mountRef.current
    if (!outer || !mount) return
    const textMuted = isDark ? '#94a3b8' : '#64748b'
    const gridH = isDark ? 'rgba(148, 163, 184, 0.12)' : 'rgba(100, 116, 139, 0.14)'
    const border = isDark ? 'rgba(148,163,184,0.2)' : 'rgba(100,116,139,0.25)'
    const chart = createChart(mount, {
      width: Math.max(outer.clientWidth, 200),
      height: Math.max(outer.clientHeight, 240),
      layout: { background: { type: ColorType.Solid, color: 'transparent' }, textColor: textMuted, fontSize: 11, attributionLogo: false },
      localization: { locale, dateFormat: "dd MMM ''yy" as const, priceFormatter: (p: number) => fmtNum(p, locale, 2) },
      grid: { vertLines: { visible: false }, horzLines: { color: gridH } },
      rightPriceScale: { visible: false },
      leftPriceScale: { visible: true, borderVisible: true, borderColor: border, scaleMargins: { top: 0.1, bottom: 0.08 } },
      timeScale: { borderVisible: true, borderColor: border },
      crosshair: { mode: CrosshairMode.Magnet },
    })
    const series = chart.addSeries(LineSeries, { color: '#0284c7', lineWidth: 2, priceScaleId: 'left', priceLineVisible: false, lastValueVisible: true })
    series.setData(lineData)
    chart.timeScale().fitContent()
    return () => {
      chart.remove()
    }
  }, [historyState, lineData, isDark, locale])

  return (
    <div className="fi-faiz-panel fi-faiz-panel--chart fi-faiz-panel--viop-detail">
      <div className="fi-faiz-panel-head-row">
        <h3 className="fi-faiz-panel-title">{t('faizVadeliPage.tahvil.detailTitle')}</h3>
        <button type="button" className="fi-faiz-panel-back" onClick={onBack}>
          {t('faizVadeliPage.tahvil.chartBackToYield')}
        </button>
      </div>

      <div className="fi-viop-alias-strip">
        {aliasRows.map((row) => (
          <div key={row.symbol} className="fi-viop-alias-chip">
            <strong>{t(`faizVadeliPage.tahvil.symbolLabel.${row.symbol}`)}</strong>
            <span>{fmtNum(row.price ?? null, locale, 2)}</span>
            <small>{toPct(row.change1D ?? null, locale)}</small>
          </div>
        ))}
      </div>

      {loading ? <p className="fi-faiz-policy-panel-state">{t('faizVadeliPage.tahvil.detailLoading')}</p> : null}
      {error ? <p className="fi-faiz-policy-panel-state">{error}</p> : null}

      {!loading && !error ? (
        <div className="fi-viop-detail-grid">
          <div className="fi-viop-table-panel">
            <div className="fi-viop-table-toolbar">
              <input
                className="fi-viop-search-input"
                placeholder={t('faizVadeliPage.tahvil.detailSearchPlaceholder')}
                value={query}
                onChange={(e) => setQuery(e.target.value)}
              />
            </div>
            <div className="fi-viop-table-wrap">
              <table className="fi-viop-table">
                <thead>
                  <tr>
                    <th>{t('faizVadeliPage.tahvil.colContract')}</th>
                    <th>{t('faizVadeliPage.tahvil.colUnderlying')}</th>
                    <th>{t('faizVadeliPage.tahvil.colExpiry')}</th>
                    <th>{t('faizVadeliPage.tahvil.colSettle')}</th>
                    <th>{t('faizVadeliPage.tahvil.colChange1D')}</th>
                    <th>{t('faizVadeliPage.tahvil.colVolume')}</th>
                    <th>{t('faizVadeliPage.tahvil.colOpenInterest')}</th>
                  </tr>
                </thead>
                <tbody>
                  {filtered.map((row) => (
                    <tr key={row.contractCode} className={selectedCode === row.contractCode ? 'is-selected' : ''} onClick={() => setSelectedCode(row.contractCode)}>
                      <td>{row.contractCode}</td>
                      <td>{row.underlying ?? '—'}</td>
                      <td>{row.expiryDate ?? '—'}</td>
                      <td>{fmtNum(row.settlementPrice ?? null, locale, 2)}</td>
                      <td>{toPct(row.changePercent ?? null, locale)}</td>
                      <td>{fmtNum(row.volumeTl ?? null, locale, 0)}</td>
                      <td>{fmtNum(row.openInterest ?? null, locale, 0)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>

          <div className="fi-viop-chart-panel">
            <h4 className="fi-faiz-panel-title">{selected?.contractCode ?? t('faizVadeliPage.tahvil.detailNoSelection')}</h4>
            {historyState === 'loading' ? <p className="fi-faiz-policy-panel-state">{t('faizVadeliPage.tahvil.chartLoading')}</p> : null}
            {historyState === 'error' ? <p className="fi-faiz-policy-panel-state">{t('faizVadeliPage.tahvil.chartError')}</p> : null}
            {historyState === 'ready' && lineData.length === 0 ? <p className="fi-faiz-policy-panel-state">{t('faizVadeliPage.tahvil.chartEmpty')}</p> : null}
            {historyState === 'ready' && lineData.length > 0 ? (
              <div ref={outerRef} className="fi-faiz-policy-panel-chart-outer">
                <div ref={mountRef} className="fi-faiz-policy-panel-chart-mount" />
              </div>
            ) : null}
          </div>
        </div>
      ) : null}
    </div>
  )
}

