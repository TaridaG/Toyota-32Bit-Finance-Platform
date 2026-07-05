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
import { VIOP_PANEL_SEGMENTS, type ViopPanelSegment } from '../lib/viopSegment'

type ChipRow =
  | { kind: 'alias'; symbol: string; price?: number | null; change1D?: number | null }
  | { kind: 'contract'; contractCode: string; price?: number | null; change1D?: number | null }

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

function pickContractChips(contracts: ViopActiveContract[], limit = 3): ChipRow[] {
  const sorted = [...contracts].sort((a, b) => {
    const volA = a.volumeTl ?? 0
    const volB = b.volumeTl ?? 0
    if (volB !== volA) return volB - volA
    return (a.expiryDate ?? '').localeCompare(b.expiryDate ?? '')
  })
  return sorted.slice(0, limit).map((row) => ({
    kind: 'contract',
    contractCode: row.contractCode,
    price: row.settlementPrice ?? null,
    change1D: row.changePercent ?? null,
  }))
}

async function loadAliasChips(segment: ViopPanelSegment): Promise<ChipRow[]> {
  if (segment === 'rates') {
    const symbols = ['VIOP_TLREF_NEAR', 'VIOP_DIBS_NEAR', 'VIOP_FAIZ_NEAR'] as const
    const rows = await Promise.all(symbols.map(async (symbol) => ({ symbol, ...(await fetchTahvilSummary(symbol)) })))
    return rows.map((row) => ({
      kind: 'alias',
      symbol: row.symbol,
      price: row.price ?? null,
      change1D: row.change1D ?? null,
    }))
  }
  if (segment === 'bonds') {
    const row = await fetchTahvilSummary('VIOP_DIBS_NEAR')
    return [{ kind: 'alias', symbol: 'VIOP_DIBS_NEAR', price: row?.price ?? null, change1D: row?.change1D ?? null }]
  }
  return []
}

export function FaizVadeliViopDetailPanel({ onBack }: { onBack: () => void }) {
  const { t, i18n } = useTranslation('common')
  const { theme } = useTheme()
  const locale = i18n.resolvedLanguage ?? i18n.language ?? 'en'
  const isDark = theme === 'dark'

  const [activeSegment, setActiveSegment] = useState<ViopPanelSegment>('rates')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [contracts, setContracts] = useState<ViopActiveContract[]>([])
  const [chipRows, setChipRows] = useState<ChipRow[]>([])
  const [selectedCode, setSelectedCode] = useState<string | null>(null)
  const [history, setHistory] = useState<Array<{ time?: string | null; value?: number | null }>>([])
  const [historyState, setHistoryState] = useState<'loading' | 'ready' | 'error'>('loading')
  const [query, setQuery] = useState('')

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError(null)
    setQuery('')
    Promise.all([fetchViopActiveContracts(activeSegment), loadAliasChips(activeSegment)])
      .then(([rows, aliases]) => {
        if (cancelled) return
        setContracts(rows)
        setChipRows(aliases.length > 0 ? aliases : pickContractChips(rows))
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
  }, [activeSegment, t])

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

      <div className="fi-viop-segment-tabs" role="tablist" aria-label={t('faizVadeliPage.tahvil.segmentTabsAria')}>
        {VIOP_PANEL_SEGMENTS.map((segment) => (
          <button
            key={segment}
            type="button"
            role="tab"
            aria-selected={activeSegment === segment}
            className={`fi-viop-segment-tab${activeSegment === segment ? ' is-active' : ''}`}
            onClick={() => setActiveSegment(segment)}
          >
            {t(`faizVadeliPage.tahvil.segment.${segment}`)}
          </button>
        ))}
      </div>

      {chipRows.length > 0 ? (
        <div className="fi-viop-alias-strip">
          {chipRows.map((row) => (
            <div
              key={row.kind === 'alias' ? row.symbol : row.contractCode}
              className="fi-viop-alias-chip"
              onClick={() => {
                if (row.kind === 'contract') setSelectedCode(row.contractCode)
              }}
              onKeyDown={(e) => {
                if (row.kind === 'contract' && (e.key === 'Enter' || e.key === ' ')) {
                  e.preventDefault()
                  setSelectedCode(row.contractCode)
                }
              }}
              role={row.kind === 'contract' ? 'button' : undefined}
              tabIndex={row.kind === 'contract' ? 0 : undefined}
            >
              <strong>
                {row.kind === 'alias'
                  ? t(`faizVadeliPage.tahvil.symbolLabel.${row.symbol}`)
                  : row.contractCode}
              </strong>
              <span>{fmtNum(row.price ?? null, locale, 2)}</span>
              <small>{toPct(row.change1D ?? null, locale)}</small>
            </div>
          ))}
        </div>
      ) : null}

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
            {activeSegment === 'all' ? (
              <p className="fi-faiz-policy-panel-state fi-viop-all-hint">{t('faizVadeliPage.tahvil.allSegmentLimitHint')}</p>
            ) : null}
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
