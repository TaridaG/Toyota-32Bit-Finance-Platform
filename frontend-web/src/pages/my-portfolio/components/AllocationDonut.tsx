import { useCallback, useEffect, useMemo, useRef, useState, type MouseEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { fetchMarketOverview } from '../../../features/markets/api/marketService'
import type { MarketOverviewItem } from '../../../shared/types/market'
import type { PortfolioOverviewItem } from '../../../shared/types/portfolio'

export type AllocationDonutRow = {
  /** Stable id for React/SVG keys (e.g. category bucket); falls back to symbol. */
  rowKey?: string
  symbol: string
  value: number
  sharePct: number
  colorClass: string
  /** When set, shown in legend as portfolio-wide weight (e.g. in category panels). */
  portfolioSharePct?: number
  /** Category donut: 1D % change in total category market value (holdings). */
  categoryValueChange1dPct?: number | null
  /** Category donut: 1D change in portfolio weight for this category (percentage points). */
  categoryWeightChangePp1d?: number | null
}

export type AllocationCategoryGroup = {
  key: string
  label: string
  shareOfPortfolio: number
  rows: AllocationDonutRow[]
  holdings: PortfolioOverviewItem[]
}

const CX = 50
const CY = 50
const R_OUT = 44
const R_IN = 25

function colorClassToCssColor(colorClass: string): string {
  switch (colorClass) {
    case 'my-portfolio-dot-blue':
      return '#3b82f6'
    case 'my-portfolio-dot-purple':
      return '#a855f7'
    case 'my-portfolio-dot-pink':
      return '#fb7185'
    case 'my-portfolio-dot-green':
      return '#22c55e'
    case 'my-portfolio-dot-other':
      return '#64748b'
    default:
      return '#64748b'
  }
}

function polar(cx: number, cy: number, r: number, a: number) {
  return [cx + r * Math.cos(a), cy + r * Math.sin(a)] as const
}

function slicePath(cx: number, cy: number, rOut: number, rIn: number, a0: number, a1: number): string {
  const largeArc = a1 - a0 > Math.PI ? 1 : 0
  const [x0o, y0o] = polar(cx, cy, rOut, a0)
  const [x1o, y1o] = polar(cx, cy, rOut, a1)
  const [x1i, y1i] = polar(cx, cy, rIn, a1)
  const [x0i, y0i] = polar(cx, cy, rIn, a0)
  return `M ${x0o} ${y0o} A ${rOut} ${rOut} 0 ${largeArc} 1 ${x1o} ${y1o} L ${x1i} ${y1i} A ${rIn} ${rIn} 0 ${largeArc} 0 ${x0i} ${y0i} Z`
}

function midAnchor(cx: number, cy: number, rOut: number, rIn: number, a0: number, a1: number) {
  const mid = (a0 + a1) / 2
  const r = (rIn + rOut) / 2
  return { ox: cx + r * Math.cos(mid), oy: cy + r * Math.sin(mid) }
}

type Props = {
  rows: AllocationDonutRow[]
  sharePctDisplay: Intl.NumberFormat
  currencyFormat: Intl.NumberFormat
  ariaLabel: string
  /** Instrument: live market % in tooltip; category: bucket value/weight 1D metrics from overview. */
  donutVariant?: 'instrument' | 'category'
}

function formatPricePct(fmt: Intl.NumberFormat, v: number | null | undefined): string {
  if (v == null || Number.isNaN(v)) return '—'
  return `${fmt.format(v)}%`
}

export function AllocationDonut({
  rows,
  sharePctDisplay,
  currencyFormat,
  ariaLabel,
  donutVariant = 'instrument',
}: Props) {
  const { t } = useTranslation('portfolio')
  const [hovered, setHovered] = useState<number | null>(null)
  const [tooltipPos, setTooltipPos] = useState({ x: 0, y: 0 })
  const [marketRow, setMarketRow] = useState<MarketOverviewItem | null>(null)
  const reqId = useRef(0)
  const cacheRef = useRef<Map<string, MarketOverviewItem>>(new Map())

  const pricePctFmt = useMemo(
    () =>
      new Intl.NumberFormat(undefined, {
        maximumFractionDigits: 2,
        minimumFractionDigits: 0,
        signDisplay: 'exceptZero',
      }),
    [],
  )

  const ppFmt = useMemo(
    () =>
      new Intl.NumberFormat(undefined, {
        maximumFractionDigits: 2,
        minimumFractionDigits: 0,
        signDisplay: 'exceptZero',
      }),
    [],
  )

  const angles = useMemo(() => {
    let acc = 0
    return rows.map((row, i) => {
      const startPct = acc
      acc += row.sharePct
      const endPct = i === rows.length - 1 ? 100 : acc
      const start = (-Math.PI / 2) + (2 * Math.PI * startPct) / 100
      const end = (-Math.PI / 2) + (2 * Math.PI * endPct) / 100
      return { start, end, row }
    })
  }, [rows])

  useEffect(() => {
    if (donutVariant === 'category') {
      setMarketRow(null)
      return
    }
    if (hovered == null) {
      setMarketRow(null)
      return
    }
    const symbol = rows[hovered]?.symbol
    if (!symbol) return
    const cached = cacheRef.current.get(symbol)
    if (cached) {
      setMarketRow(cached)
      return
    }
    setMarketRow(null)
    const id = ++reqId.current
    void fetchMarketOverview({
      page: 0,
      size: 160,
      category: 'all',
      query: symbol,
      displayCurrency: 'USD',
    })
      .then((page) => {
        if (id !== reqId.current) return
        const sym = symbol.trim()
        const row =
          page.content.find((r) => r.symbol === sym) ??
          page.content.find((r) => r.symbol.toUpperCase() === sym.toUpperCase()) ??
          null
        if (row) cacheRef.current.set(symbol, row)
        setMarketRow(row)
      })
      .catch(() => {
        if (id !== reqId.current) return
        setMarketRow(null)
      })
  }, [donutVariant, hovered, rows])

  const handleMove = useCallback((e: MouseEvent) => {
    setTooltipPos({ x: e.clientX, y: e.clientY })
  }, [])

  const clearHover = useCallback(() => {
    setHovered(null)
    setMarketRow(null)
  }, [])

  const activeRow = hovered != null ? rows[hovered] : null

  const weekPriceRef =
    marketRow?.change1M != null && Number.isFinite(marketRow.change1M)
      ? marketRow.change1M / 4
      : marketRow?.change1D ?? marketRow?.change24h ?? null

  const categoryAlloc1dCell =
    activeRow != null &&
    activeRow.categoryWeightChangePp1d != null &&
    Number.isFinite(activeRow.categoryWeightChangePp1d)
      ? `${ppFmt.format(activeRow.categoryWeightChangePp1d)} pp`
      : '—'

  return (
    <div className="my-portfolio-allocation-donut-shell" onMouseLeave={clearHover}>
      <div className="my-portfolio-allocation-donut-visual">
        <svg
          className="my-portfolio-allocation-donut-svg"
          viewBox="0 0 100 100"
          role="img"
          aria-label={ariaLabel}
        >
          <title>{ariaLabel}</title>
          {angles.map(({ start, end, row }, i) => {
            const { ox, oy } = midAnchor(CX, CY, R_OUT, R_IN, start, end)
            const scale = hovered === i ? 1.075 : 1
            const fill = colorClassToCssColor(row.colorClass)
            return (
              <g
                key={row.rowKey ?? row.symbol}
                className="my-portfolio-allocation-donut-seg"
                transform={`translate(${ox},${oy}) scale(${scale}) translate(${-ox},${-oy})`}
              >
                <path
                  d={slicePath(CX, CY, R_OUT, R_IN, start, end)}
                  fill={fill}
                  stroke="rgba(15, 23, 42, 0.35)"
                  strokeWidth={0.28}
                  className="my-portfolio-allocation-donut-path"
                  onMouseEnter={(e) => {
                    setHovered(i)
                    handleMove(e)
                  }}
                  onMouseMove={handleMove}
                />
              </g>
            )
          })}
        </svg>
      </div>

      <ul className="my-portfolio-allocation-legend" aria-label={t('allocation.legendAria')}>
        {rows.map((row, i) => (
          <li
            key={row.rowKey ?? row.symbol}
            className={hovered === i ? 'is-active' : undefined}
            onMouseEnter={(e) => {
              setHovered(i)
              handleMove(e)
            }}
            onMouseMove={handleMove}
          >
            <span className="my-portfolio-allocation-legend-left">
              <span className={`my-portfolio-dot ${row.colorClass}`} aria-hidden />
              <span className="my-portfolio-allocation-legend-symbol">{row.symbol}</span>
            </span>
            <span className="my-portfolio-allocation-legend-pct">
              {sharePctDisplay.format(row.sharePct)}%
              {row.portfolioSharePct != null ? (
                <span className="my-portfolio-allocation-legend-portfolio">
                  {' '}
                  ({sharePctDisplay.format(row.portfolioSharePct)}% {t('allocation.inPortfolio')})
                </span>
              ) : null}
            </span>
          </li>
        ))}
      </ul>

      {activeRow != null && hovered != null ? (
        <div
          className="my-portfolio-allocation-tooltip"
          style={{ left: tooltipPos.x + 16, top: tooltipPos.y + 16 }}
          role="tooltip"
        >
          <div className="my-portfolio-allocation-tooltip-head">
            <strong>{activeRow.symbol}</strong>
            <p className="my-portfolio-allocation-tooltip-meta">
              {t('allocation.tooltipWeight')}{' '}
              <span className="my-portfolio-allocation-tooltip-w">
                {sharePctDisplay.format(activeRow.portfolioSharePct ?? activeRow.sharePct)}%
              </span>
              <span className="my-portfolio-allocation-tooltip-sep">·</span>
              {currencyFormat.format(activeRow.value)}
            </p>
          </div>
          <div className="my-portfolio-allocation-tooltip-grid">
            <span />
            <span className="my-portfolio-allocation-tooltip-col">{t('allocation.period1d')}</span>
            <span className="my-portfolio-allocation-tooltip-col">{t('allocation.period1w')}</span>
            <span className="my-portfolio-allocation-tooltip-col">{t('allocation.period1y')}</span>
            <span className="my-portfolio-allocation-tooltip-rowlabel">{t('allocation.tooltipAllocRow')}</span>
            {donutVariant === 'category' ? (
              <>
                <span className="my-portfolio-allocation-tooltip-cell">{categoryAlloc1dCell}</span>
                <span className="my-portfolio-allocation-tooltip-cell is-muted">{t('allocation.tooltipPending')}</span>
                <span className="my-portfolio-allocation-tooltip-cell is-muted">{t('allocation.tooltipPending')}</span>
              </>
            ) : (
              <>
                <span className="my-portfolio-allocation-tooltip-cell is-muted">{t('allocation.tooltipPending')}</span>
                <span className="my-portfolio-allocation-tooltip-cell is-muted">{t('allocation.tooltipPending')}</span>
                <span className="my-portfolio-allocation-tooltip-cell is-muted">{t('allocation.tooltipPending')}</span>
              </>
            )}
            <span className="my-portfolio-allocation-tooltip-rowlabel">{t('allocation.tooltipPriceRow')}</span>
            {donutVariant === 'category' ? (
              <>
                <span className="my-portfolio-allocation-tooltip-cell">
                  {formatPricePct(pricePctFmt, activeRow?.categoryValueChange1dPct ?? null)}
                </span>
                <span className="my-portfolio-allocation-tooltip-cell is-muted">{t('allocation.tooltipPending')}</span>
                <span className="my-portfolio-allocation-tooltip-cell is-muted">{t('allocation.tooltipPending')}</span>
              </>
            ) : (
              <>
                <span className="my-portfolio-allocation-tooltip-cell">
                  {formatPricePct(pricePctFmt, marketRow?.change1D)}
                </span>
                <span className="my-portfolio-allocation-tooltip-cell">
                  {formatPricePct(pricePctFmt, weekPriceRef)}
                </span>
                <span className="my-portfolio-allocation-tooltip-cell">
                  {formatPricePct(pricePctFmt, marketRow?.change1Y)}
                </span>
              </>
            )}
          </div>
          <p className="my-portfolio-allocation-tooltip-foot">
            {donutVariant === 'category' ? t('allocation.tooltipFootnoteCategory') : t('allocation.tooltipFootnote')}
          </p>
        </div>
      ) : null}
    </div>
  )
}
