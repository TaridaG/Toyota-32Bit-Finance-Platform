import { useCallback, useMemo, useState, type MouseEvent } from 'react'
import { useTranslation } from 'react-i18next'
import type { PortfolioOverviewItem } from '../../../shared/types/portfolio'

const CX = 50
const CY = 50
const R_OUT = 44
const R_IN = 25
const WIN_COLOR = '#22c55e'
const LOSE_COLOR = '#ef4444'
const SLIVER = 0.04

function parseItemValue(value: unknown): number {
  if (value == null) return 0
  if (typeof value === 'number') return Number.isFinite(value) ? value : 0
  if (typeof value === 'string') {
    const n = Number(value.trim().replace(/\s/g, '').replace(',', '.'))
    return Number.isFinite(n) ? n : 0
  }
  return 0
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

type Segment = {
  key: 'win' | 'lose'
  sharePct: number
  fill: string
  /** Σpnl / Σ(alım maliyeti) × 100; zarar tarafı negatif */
  returnOnCostPct: number | null
  label: string
}

type Props = {
  items: PortfolioOverviewItem[]
  totalPnl: number
  totalPnlPercent: number
  currencyFormat: Intl.NumberFormat
  pctFormat: Intl.NumberFormat
  sharePctDisplay: Intl.NumberFormat
}

export function PnlSplitDonut({
  items,
  totalPnl,
  totalPnlPercent,
  currencyFormat,
  pctFormat,
  sharePctDisplay,
}: Props) {
  const { t } = useTranslation('portfolio')
  const [hovered, setHovered] = useState<number | null>(null)
  const [tooltipPos, setTooltipPos] = useState({ x: 0, y: 0 })

  const { segments, hasData } = useMemo(() => {
    const eps = 1e-9
    let winPnl = 0
    let losePnl = 0
    let winCost = 0
    let loseCost = 0
    for (const it of items) {
      const p = parseItemValue(it.pnl)
      if (Math.abs(p) <= eps) continue
      const qty = parseItemValue(it.quantity)
      const avg = parseItemValue(it.avgBuyPrice)
      const cost = Math.max(avg * qty, 0)
      if (p > eps) {
        winPnl += p
        winCost += cost
      } else {
        losePnl += p
        loseCost += cost
      }
    }
    const winMag = Math.max(winPnl, 0)
    const loseMag = Math.max(-losePnl, 0)
    /** Kar/zarar oranı büyüklükleri (maliyete göre); halka bunlara göre bölünür — varlık değeri ağırlığı değil */
    const winRateW = winCost > eps ? winPnl / winCost : 0
    const loseRateW = loseCost > eps ? (-losePnl) / loseCost : 0
    const denomRate = Math.max(winRateW, 0) + Math.max(loseRateW, 0)
    const denomMag = winMag + loseMag
    if (denomMag <= eps) {
      return { segments: [] as Segment[], hasData: false }
    }
    const missingCostWin = winMag > eps && winCost <= eps
    const missingCostLose = loseMag > eps && loseCost <= eps
    const useRateSplit = denomRate > eps && !missingCostWin && !missingCostLose
    const denomSplit = useRateSplit ? denomRate : denomMag
    let winPct = useRateSplit ? (Math.max(winRateW, 0) / denomSplit) * 100 : (winMag / denomSplit) * 100
    let losePct = useRateSplit ? (Math.max(loseRateW, 0) / denomSplit) * 100 : (loseMag / denomSplit) * 100
    if (winPct > 0 && winPct < SLIVER) winPct = SLIVER
    if (losePct > 0 && losePct < SLIVER) losePct = SLIVER
    const norm = winPct + losePct
    if (norm > eps) {
      winPct = (winPct / norm) * 100
      losePct = (losePct / norm) * 100
    }

    const winRate = winCost > eps ? (winPnl / winCost) * 100 : null
    const loseRate = loseCost > eps ? (losePnl / loseCost) * 100 : null

    const segs: Segment[] = []
    if (winPct > 0 && winMag > eps) {
      segs.push({
        key: 'win',
        sharePct: winPct,
        fill: WIN_COLOR,
        returnOnCostPct: winRate,
        label: t('pnlDonut.winners'),
      })
    }
    if (losePct > 0 && loseMag > eps) {
      segs.push({
        key: 'lose',
        sharePct: losePct,
        fill: LOSE_COLOR,
        returnOnCostPct: loseRate,
        label: t('pnlDonut.losers'),
      })
    }
    return { segments: segs, hasData: segs.length > 0 }
  }, [items, t])

  const angles = useMemo(() => {
    let acc = 0
    return segments.map((seg, i) => {
      const startPct = acc
      acc += seg.sharePct
      const endPct = i === segments.length - 1 ? 100 : acc
      const start = (-Math.PI / 2) + (2 * Math.PI * startPct) / 100
      const end = (-Math.PI / 2) + (2 * Math.PI * endPct) / 100
      return { start, end, seg, i }
    })
  }, [segments])

  const handleMove = useCallback((e: MouseEvent) => {
    setTooltipPos({ x: e.clientX, y: e.clientY })
  }, [])

  const clearHover = useCallback(() => setHovered(null), [])

  const active = hovered != null ? segments[hovered] : null

  if (!hasData) {
    return (
      <p className="my-portfolio-pnl-donut-empty" style={{ textAlign: 'center', color: 'var(--color-text-soft)' }}>
        {t('pnlDonut.empty')}
      </p>
    )
  }

  return (
    <div className="my-portfolio-pnl-donut-shell" onMouseLeave={clearHover}>
      <div className="my-portfolio-pnl-donut-visual-wrap">
        <svg
          className="my-portfolio-pnl-donut-svg"
          viewBox="0 0 100 100"
          role="img"
          aria-label={t('pnlDonut.aria')}
        >
          <title>{t('pnlDonut.aria')}</title>
          {angles.map(({ start, end, seg, i }) => {
            const { ox, oy } = midAnchor(CX, CY, R_OUT, R_IN, start, end)
            const scale = hovered === i ? 1.075 : 1
            return (
              <g
                key={seg.key}
                className="my-portfolio-allocation-donut-seg"
                transform={`translate(${ox},${oy}) scale(${scale}) translate(${-ox},${-oy})`}
              >
                <path
                  d={slicePath(CX, CY, R_OUT, R_IN, start, end)}
                  fill={seg.fill}
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
        <div className="my-portfolio-pnl-donut-center">
          <strong>{currencyFormat.format(totalPnl)}</strong>
          <span>
            {pctFormat.format(totalPnlPercent)}
            %
          </span>
        </div>
      </div>

      <ul className="my-portfolio-pnl-donut-legend" aria-label={t('pnlDonut.legendAria')}>
        {segments.map((seg, i) => (
          <li
            key={seg.key}
            className={hovered === i ? 'is-active' : undefined}
            onMouseEnter={(e) => {
              setHovered(i)
              handleMove(e)
            }}
            onMouseMove={handleMove}
          >
            <span
              className="my-portfolio-pnl-donut-legend-dot"
              style={{ background: seg.fill }}
              aria-hidden
            />
            <span className="my-portfolio-pnl-donut-legend-label">{seg.label}</span>
            <span className="my-portfolio-pnl-donut-legend-pct">{sharePctDisplay.format(seg.sharePct)}%</span>
          </li>
        ))}
      </ul>

      {active != null && hovered != null ? (
        <div
          className="my-portfolio-allocation-tooltip my-portfolio-pnl-donut-tooltip"
          style={{ left: tooltipPos.x + 16, top: tooltipPos.y + 16 }}
          role="tooltip"
        >
          <p
            className="my-portfolio-pnl-donut-tooltip-pct"
            style={{ color: active.key === 'win' ? WIN_COLOR : LOSE_COLOR }}
          >
            {active.returnOnCostPct != null && Number.isFinite(active.returnOnCostPct)
              ? `${pctFormat.format(active.returnOnCostPct)}%`
              : '—'}
          </p>
        </div>
      ) : null}
    </div>
  )
}
