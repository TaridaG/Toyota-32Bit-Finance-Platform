import { useCallback, useLayoutEffect, useMemo, useRef, useState, type MouseEvent } from 'react'
import { useTranslation } from 'react-i18next'
import type { PortfolioOverviewItem } from '../../../shared/types/portfolio'
import { groupPnlByPortfolio, type SymbolPortfolioLine } from '../lib/portfolioAggregateBreakdown'
import { PortfolioBreakdownList } from './PortfolioBreakdownList'

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

const LEGEND_ASSET_PREVIEW = 2

type Segment = {
  key: 'win' | 'lose'
  sharePct: number
  fill: string
  /** Σpnl / Σ(alım maliyeti) × 100; zarar tarafı negatif */
  returnOnCostPct: number | null
  label: string
  previewSymbols: string[]
  hasMoreAssets: boolean
}

type Props = {
  items: PortfolioOverviewItem[]
  totalPnl: number
  totalPnlPercent: number
  currencyFormat: Intl.NumberFormat
  pctFormat: Intl.NumberFormat
  sharePctDisplay: Intl.NumberFormat
  hideAmounts?: boolean
  portfolioPnlBreakdown?: { win: SymbolPortfolioLine[]; lose: SymbolPortfolioLine[] }
}

export function PnlSplitDonut({
  items,
  totalPnl,
  totalPnlPercent,
  currencyFormat,
  pctFormat,
  sharePctDisplay,
  hideAmounts = false,
  portfolioPnlBreakdown,
}: Props) {
  const { t } = useTranslation('portfolio')
  const visualRef = useRef<HTMLDivElement>(null)
  const centerRef = useRef<HTMLDivElement>(null)
  const valueRef = useRef<HTMLElement>(null)
  const [hovered, setHovered] = useState<number | null>(null)
  const [tooltipAnchor, setTooltipAnchor] = useState<{ x: number; y: number } | null>(null)
  const [legendSymbolHover, setLegendSymbolHover] = useState<{
    symbol: string
    pos: { x: number; y: number }
  } | null>(null)

  const { segments, hasData } = useMemo(() => {
    const eps = 1e-9
    let winPnl = 0
    let losePnl = 0
    let winCost = 0
    let loseCost = 0
    const winRows: { symbol: string; pnl: number }[] = []
    const loseRows: { symbol: string; pnl: number }[] = []
    for (const it of items) {
      const p = parseItemValue(it.pnl)
      if (Math.abs(p) <= eps) continue
      const qty = parseItemValue(it.quantity)
      const avg = parseItemValue(it.avgBuyPrice)
      const cost = Math.max(avg * qty, 0)
      const symbol = (it.symbol || it.name || '').trim()
      if (p > eps) {
        winPnl += p
        winCost += cost
        if (symbol) winRows.push({ symbol, pnl: p })
      } else {
        losePnl += p
        loseCost += cost
        if (symbol) loseRows.push({ symbol, pnl: p })
      }
    }
    winRows.sort((a, b) => b.pnl - a.pnl)
    loseRows.sort((a, b) => a.pnl - b.pnl)
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
        previewSymbols: winRows.slice(0, LEGEND_ASSET_PREVIEW).map((r) => r.symbol),
        hasMoreAssets: winRows.length > LEGEND_ASSET_PREVIEW,
      })
    }
    if (losePct > 0 && loseMag > eps) {
      segs.push({
        key: 'lose',
        sharePct: losePct,
        fill: LOSE_COLOR,
        returnOnCostPct: loseRate,
        label: t('pnlDonut.losers'),
        previewSymbols: loseRows.slice(0, LEGEND_ASSET_PREVIEW).map((r) => r.symbol),
        hasMoreAssets: loseRows.length > LEGEND_ASSET_PREVIEW,
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

  /** One logical slice at 100%: SVG arc path degenerates; draw a stroke ring instead (no 12/6 seam). */
  const isSingleFullRing = segments.length === 1

  const updateTooltipAnchor = useCallback(
    (index: number) => {
      const wrap = visualRef.current
      if (!wrap || index < 0 || index >= angles.length) {
        setTooltipAnchor(null)
        return
      }
      const { start, end } = angles[index]
      const mid = (start + end) / 2
      const scale = wrap.offsetWidth / 100
      const cx = wrap.offsetWidth / 2
      const cy = wrap.offsetHeight / 2
      const r = ((R_IN + R_OUT) / 2) * scale
      setTooltipAnchor({
        x: cx + r * Math.cos(mid),
        y: cy + r * Math.sin(mid),
      })
    },
    [angles],
  )

  const focusSegment = useCallback(
    (index: number) => {
      setHovered(index)
      updateTooltipAnchor(index)
    },
    [updateTooltipAnchor],
  )

  const clearHover = useCallback(() => {
    setHovered(null)
    setTooltipAnchor(null)
    setLegendSymbolHover(null)
  }, [])

  const active = hovered != null ? segments[hovered] : null
  const totalPnlText = currencyFormat.format(totalPnl)

  const activeSegmentBreakdown = useMemo(() => {
    if (!portfolioPnlBreakdown || active == null) return []
    const lines = active.key === 'win' ? portfolioPnlBreakdown.win : portfolioPnlBreakdown.lose
    return groupPnlByPortfolio(lines).map((group) => ({
      portfolioId: group.portfolioId,
      portfolioName: group.portfolioName,
      amount: group.totalPnl,
      subRows: group.symbols.map((s) => ({ label: s.symbol, amount: s.pnl })),
    }))
  }, [active, portfolioPnlBreakdown])

  const legendSymbolBreakdown = useMemo(() => {
    if (!portfolioPnlBreakdown || !legendSymbolHover) return []
    const symbol = legendSymbolHover.symbol
    const lines = [...portfolioPnlBreakdown.win, ...portfolioPnlBreakdown.lose].filter(
      (line) => line.symbol === symbol,
    )
    return lines.map((line) => ({
      portfolioId: line.portfolioId,
      portfolioName: line.portfolioName,
      amount: line.pnl,
    }))
  }, [legendSymbolHover, portfolioPnlBreakdown])

  const fitCenterValue = useCallback(() => {
    const wrap = visualRef.current
    const center = centerRef.current
    const valueEl = valueRef.current
    if (!wrap || !center || !valueEl) return
    if (hideAmounts) {
      valueEl.style.fontSize = ''
      return
    }

    const availableWidth = Math.max(center.clientWidth - 4, 0)
    const wrapWidth = wrap.clientWidth
    if (!availableWidth || !wrapWidth) return

    const maxPx = Math.min(27.5, wrapWidth * 0.138)
    const minPx = Math.max(12, wrapWidth * 0.068)

    valueEl.style.fontSize = `${maxPx}px`
    const naturalWidth = valueEl.scrollWidth
    if (!naturalWidth) return

    const nextPx = Math.max(minPx, Math.min(maxPx, ((availableWidth * 0.98) / naturalWidth) * maxPx))
    valueEl.style.fontSize = `${nextPx.toFixed(2)}px`
  }, [hideAmounts, totalPnlText])

  useLayoutEffect(() => {
    fitCenterValue()
    const wrap = visualRef.current
    if (!wrap || typeof ResizeObserver === 'undefined') return
    const ro = new ResizeObserver(() => fitCenterValue())
    ro.observe(wrap)
    return () => ro.disconnect()
  }, [fitCenterValue])

  if (!hasData) {
    const trackR = (R_OUT + R_IN) / 2
    const trackW = R_OUT - R_IN
    return (
      <div className="my-portfolio-pnl-donut-shell my-portfolio-pnl-donut-shell--empty">
        <div className="my-portfolio-pnl-donut-visual-wrap" ref={visualRef}>
          <svg className="my-portfolio-pnl-donut-svg" viewBox="0 0 100 100" aria-hidden>
            <circle
              cx={CX}
              cy={CY}
              r={trackR}
              fill="none"
              stroke="rgba(148, 163, 184, 0.22)"
              strokeWidth={trackW}
            />
          </svg>
          <div className="my-portfolio-pnl-donut-center my-portfolio-pnl-donut-center--muted">
            <span className="my-portfolio-pnl-donut-empty-caption">{t('pnlDonut.empty')}</span>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="my-portfolio-pnl-donut-shell" onMouseLeave={clearHover}>
      <div className="my-portfolio-pnl-donut-visual-wrap" ref={visualRef}>
        <svg
          className="my-portfolio-pnl-donut-svg"
          viewBox="0 0 100 100"
          role="img"
          aria-label={t('pnlDonut.aria')}
        >
          <title>{t('pnlDonut.aria')}</title>
          {isSingleFullRing ? (
            <g
              transform={`translate(${CX},${CY}) scale(${hovered === 0 ? 1.075 : 1}) translate(${-CX},${-CY})`}
              className="my-portfolio-allocation-donut-seg"
            >
              <circle
                cx={CX}
                cy={CY}
                r={(R_OUT + R_IN) / 2}
                fill="none"
                stroke={segments[0].fill}
                strokeWidth={R_OUT - R_IN}
                className="my-portfolio-allocation-donut-path"
                onMouseEnter={() => focusSegment(0)}
                onFocus={() => focusSegment(0)}
              />
            </g>
          ) : (
            angles.map(({ start, end, seg, i }) => {
              const { ox, oy } = midAnchor(CX, CY, R_OUT, R_IN, start, end)
              const scale = hovered === i ? 1.075 : 1
              return (
                <g
                  key={`${seg.key}-${i}`}
                  className="my-portfolio-allocation-donut-seg"
                  transform={`translate(${ox},${oy}) scale(${scale}) translate(${-ox},${-oy})`}
                >
                  <path
                    d={slicePath(CX, CY, R_OUT, R_IN, start, end)}
                    fill={seg.fill}
                    stroke="rgba(15, 23, 42, 0.35)"
                    strokeWidth={0.28}
                    className="my-portfolio-allocation-donut-path"
                    onMouseEnter={() => focusSegment(i)}
                    onFocus={() => focusSegment(i)}
                  />
                </g>
              )
            })
          )}
        </svg>
        <div className="my-portfolio-pnl-donut-center" ref={centerRef}>
          {hideAmounts ? (
            <>
              <strong className="my-portfolio-pnl-donut-center-value">••••</strong>
              <span>•••</span>
            </>
          ) : (
            <>
              <strong ref={valueRef} className="my-portfolio-pnl-donut-center-value">
                {totalPnlText}
              </strong>
              <span>
                {pctFormat.format(totalPnlPercent)}
                %
              </span>
            </>
          )}
        </div>

        {active != null && hovered != null && tooltipAnchor != null && !hideAmounts ? (
          <div
            className="my-portfolio-allocation-tooltip my-portfolio-pnl-donut-tooltip my-portfolio-pnl-donut-tooltip--anchored"
            style={{ position: 'absolute', left: tooltipAnchor.x, top: tooltipAnchor.y }}
            role="tooltip"
          >
            <p className="my-portfolio-pnl-donut-tooltip-title">{active.label}</p>
            <p
              className="my-portfolio-pnl-donut-tooltip-pct"
              style={{ color: active.key === 'win' ? WIN_COLOR : LOSE_COLOR }}
            >
              {active.returnOnCostPct != null && Number.isFinite(active.returnOnCostPct)
                ? `${pctFormat.format(active.returnOnCostPct)}%`
                : '—'}
            </p>
            <p className="my-portfolio-pnl-donut-tooltip-share">
              {t('pnlDonut.ringShare', { pct: sharePctDisplay.format(active.sharePct) })}
            </p>
            {activeSegmentBreakdown.length > 0 ? (
              <PortfolioBreakdownList
                title={active.key === 'win' ? t('breakdown.segmentWinDetail') : t('breakdown.segmentLoseDetail')}
                rows={activeSegmentBreakdown}
                currencyFormat={currencyFormat}
                hideAmounts={hideAmounts}
              />
            ) : null}
          </div>
        ) : null}

        {legendSymbolHover != null && legendSymbolBreakdown.length > 0 ? (
          <div
            className="my-portfolio-allocation-tooltip my-portfolio-pnl-donut-tooltip my-portfolio-breakdown-tooltip"
            style={{
              position: 'fixed',
              left: legendSymbolHover.pos.x + 16,
              top: legendSymbolHover.pos.y + 16,
            }}
            role="tooltip"
          >
            <p className="my-portfolio-pnl-donut-tooltip-title">{legendSymbolHover.symbol}</p>
            <PortfolioBreakdownList
              rows={legendSymbolBreakdown}
              currencyFormat={currencyFormat}
              hideAmounts={hideAmounts}
            />
          </div>
        ) : null}
      </div>

      <div
        className={`my-portfolio-pnl-donut-legend${segments.length === 1 ? ' my-portfolio-pnl-donut-legend--single' : ''}`}
        role="group"
        aria-label={t('pnlDonut.legendAria')}
      >
        {segments.map((seg, i) => (
          <div
            key={seg.key}
            className={`my-portfolio-pnl-donut-legend-col${hovered === i ? ' is-active' : ''}`}
            onMouseEnter={() => focusSegment(i)}
            onFocus={() => focusSegment(i)}
            tabIndex={0}
          >
            <div className="my-portfolio-pnl-donut-legend-head">
              <span className="my-portfolio-pnl-donut-legend-left">
                <span
                  className="my-portfolio-pnl-donut-legend-dot"
                  style={{ background: seg.fill }}
                  aria-hidden
                />
                <span className="my-portfolio-pnl-donut-legend-label">{seg.label}</span>
              </span>
              <span className="my-portfolio-pnl-donut-legend-pct">{sharePctDisplay.format(seg.sharePct)}%</span>
            </div>
            {seg.previewSymbols.length > 0 || seg.hasMoreAssets ? (
              <ul className="my-portfolio-pnl-donut-legend-assets" aria-label={seg.label}>
                {hideAmounts
                  ? seg.previewSymbols.map((_, idx) => (
                      <li key={`mask-${seg.key}-${idx}`} className="my-portfolio-pnl-donut-legend-asset">
                        •••
                      </li>
                    ))
                  : seg.previewSymbols.map((symbol) => (
                      <li
                        key={`${seg.key}-${symbol}`}
                        className="my-portfolio-pnl-donut-legend-asset"
                        onMouseEnter={(e: MouseEvent) => {
                          if (!portfolioPnlBreakdown) return
                          setLegendSymbolHover({ symbol, pos: { x: e.clientX, y: e.clientY } })
                        }}
                        onMouseMove={(e: MouseEvent) => {
                          if (!portfolioPnlBreakdown) return
                          setLegendSymbolHover({ symbol, pos: { x: e.clientX, y: e.clientY } })
                        }}
                        onMouseLeave={() => setLegendSymbolHover(null)}
                      >
                        {symbol}
                      </li>
                    ))}
                {seg.hasMoreAssets ? (
                  <li className="my-portfolio-pnl-donut-legend-asset my-portfolio-pnl-donut-legend-more">
                    {t('pnlDonut.moreAssetsShort')}
                  </li>
                ) : null}
              </ul>
            ) : null}
          </div>
        ))}
      </div>
    </div>
  )
}
