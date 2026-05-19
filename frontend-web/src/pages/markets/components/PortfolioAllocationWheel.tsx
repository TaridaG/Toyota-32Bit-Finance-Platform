import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import {
  MARKETS_ROW_DRAG_MIME,
  parseMarketsRowDrag,
  type MarketsRowDragPayload,
} from '../lib/marketsRowDrag'
import type { HoldingPeriodMetrics, PortfolioHolding } from '../utils/portfolioSimulation'
import {
  buildAllocationSegments,
  cumulativePctToAngle,
  MIN_SEGMENT_PCT,
  pointerToCumulativePct,
  polar,
  roundWeight,
  slicePath,
} from './portfolioAllocationGeometry'

const CX = 100
const CY = 100
const R_OUT = 88
const R_IN = 54
const HANDLE_R = 9
const TAU = Math.PI * 2
const START = -Math.PI / 2

type PortfolioAllocationWheelProps = {
  holdings: PortfolioHolding[]
  activeId: string | null
  onActiveChange: (id: string | null) => void
  onHoldingsChange: (holdings: PortfolioHolding[]) => void
  onRemove: (id: string) => void
  onAddPayload: (payload: MarketsRowDragPayload) => void
  weightsOk: boolean
  weightsSum: number
  locale: string
  layout?: 'stacked' | 'horizontal'
  metricsByHoldingId?: Map<string, HoldingPeriodMetrics>
  loadingSymbols?: string[]
  failedSymbols?: string[]
  pctFmt?: Intl.NumberFormat
}

function applyBoundaryDrag(
  holdings: PortfolioHolding[],
  boundaryIndex: number,
  targetCumulativePct: number,
): PortfolioHolding[] {
  if (boundaryIndex < 1 || boundaryIndex >= holdings.length) {
    return holdings
  }
  const before = holdings
    .slice(0, boundaryIndex - 1)
    .reduce((sum, h) => sum + h.weightPct, 0)
  const left = holdings[boundaryIndex - 1]
  const right = holdings[boundaryIndex]
  const pairSum = left.weightPct + right.weightPct
  const minCum = before + MIN_SEGMENT_PCT
  const maxCum = before + pairSum - MIN_SEGMENT_PCT
  const cum = Math.min(maxCum, Math.max(minCum, targetCumulativePct))
  const newLeft = roundWeight(cum - before)
  const newRight = roundWeight(pairSum - newLeft)
  return holdings.map((h, index) => {
    if (index === boundaryIndex - 1) {
      return { ...h, weightPct: newLeft }
    }
    if (index === boundaryIndex) {
      return { ...h, weightPct: newRight }
    }
    return h
  })
}

export function PortfolioAllocationWheel({
  holdings,
  activeId,
  onActiveChange,
  onHoldingsChange,
  onRemove,
  onAddPayload,
  weightsOk,
  weightsSum,
  locale,
  layout = 'horizontal',
  metricsByHoldingId,
  loadingSymbols = [],
  failedSymbols = [],
  pctFmt,
}: PortfolioAllocationWheelProps) {
  const { t } = useTranslation('markets')
  const svgRef = useRef<SVGSVGElement>(null)
  const [dragBoundary, setDragBoundary] = useState<number | null>(null)
  const [dropActive, setDropActive] = useState(false)

  const segments = useMemo(() => buildAllocationSegments(holdings), [holdings])
  const isEmpty = segments.length === 0

  const pctLabelFmt = useMemo(
    () => new Intl.NumberFormat(locale, { maximumFractionDigits: 1 }),
    [locale],
  )
  const returnFmt = pctFmt ?? pctLabelFmt

  const boundaries = useMemo(() => {
    let cum = 0
    const points: { index: number; pct: number; angle: number }[] = []
    for (let i = 0; i < holdings.length - 1; i += 1) {
      cum += holdings[i].weightPct
      points.push({
        index: i + 1,
        pct: cum,
        angle: cumulativePctToAngle(cum),
      })
    }
    return points
  }, [holdings])

  const onPointerMove = useCallback(
    (event: PointerEvent) => {
      if (dragBoundary == null || !svgRef.current) {
        return
      }
      const rect = svgRef.current.getBoundingClientRect()
      const targetPct = pointerToCumulativePct(event.clientX, event.clientY, rect)
      onHoldingsChange(applyBoundaryDrag(holdings, dragBoundary, targetPct))
    },
    [dragBoundary, holdings, onHoldingsChange],
  )

  const endDrag = useCallback(() => {
    setDragBoundary(null)
  }, [])

  useEffect(() => {
    if (dragBoundary == null) {
      return
    }
    window.addEventListener('pointermove', onPointerMove)
    window.addEventListener('pointerup', endDrag)
    window.addEventListener('pointercancel', endDrag)
    return () => {
      window.removeEventListener('pointermove', onPointerMove)
      window.removeEventListener('pointerup', endDrag)
      window.removeEventListener('pointercancel', endDrag)
    }
  }, [dragBoundary, endDrag, onPointerMove])

  const startBoundaryDrag = (boundaryIndex: number, event: React.PointerEvent) => {
    event.preventDefault()
    event.stopPropagation()
    setDragBoundary(boundaryIndex)
    ;(event.target as Element).setPointerCapture?.(event.pointerId)
  }

  const handleDrop = (event: React.DragEvent) => {
    event.preventDefault()
    setDropActive(false)
    const raw =
      event.dataTransfer.getData(MARKETS_ROW_DRAG_MIME) ||
      event.dataTransfer.getData('text/plain')
    const payload = parseMarketsRowDrag(raw)
    if (payload) {
      onAddPayload(payload)
    }
  }

  return (
    <div
      className={`markets-allocation-panel markets-allocation-panel--${layout}${dropActive ? ' is-drop-active' : ''}${isEmpty ? ' is-empty' : ''}`}
      onDragOver={(event) => {
        event.preventDefault()
        event.dataTransfer.dropEffect = 'copy'
        setDropActive(true)
      }}
      onDragLeave={(event) => {
        if (event.currentTarget.contains(event.relatedTarget as Node)) {
          return
        }
        setDropActive(false)
      }}
      onDrop={handleDrop}
    >
      <div className="markets-allocation-chart-wrap">
        <svg
          ref={svgRef}
          className={`markets-allocation-chart${dragBoundary != null ? ' markets-allocation-chart-dragging' : ''}`}
          viewBox="0 0 200 200"
          role="img"
          aria-label={t('portfolioSim.allocationChartAria')}
        >
          {isEmpty ? (
            <>
              <path
                className="markets-allocation-empty-ring"
                d={slicePath(CX, CY, R_OUT, R_IN, START, START + TAU - 0.001)}
              />
              <circle className="markets-allocation-hole" cx={CX} cy={CY} r={R_IN - 2} />
              <text x={CX} y={CY - 4} className="markets-allocation-center-label" textAnchor="middle">
                {t('portfolioSim.allocationCenter')}
              </text>
              <text x={CX} y={CY + 14} className="markets-allocation-center-value" textAnchor="middle">
                0%
              </text>
            </>
          ) : (
            <>
              <circle className="markets-allocation-chart-bg" cx={CX} cy={CY} r={(R_OUT + R_IN) / 2} />

              {segments.map((segment) => {
                const isActive = activeId === segment.id
                const isDimmed = activeId != null && !isActive
                const midAngle = (segment.startAngle + segment.endAngle) / 2
                const labelR = (R_OUT + R_IN) / 2
                const [lx, ly] = polar(CX, CY, labelR, midAngle)
                const showInnerLabel = segment.endAngle - segment.startAngle > 0.35

                return (
                  <g
                    key={segment.id}
                    className={`markets-allocation-segment${isActive ? ' is-active' : ''}${isDimmed ? ' is-dimmed' : ''}`}
                    style={{ color: segment.color }}
                    onMouseEnter={() => onActiveChange(segment.id)}
                    onMouseLeave={() => onActiveChange(null)}
                  >
                    <path
                      className="markets-allocation-slice-hit"
                      d={slicePath(CX, CY, R_OUT + 6, R_IN - 4, segment.startAngle, segment.endAngle)}
                      fill="transparent"
                    />
                    <path
                      className="markets-allocation-slice"
                      d={slicePath(CX, CY, R_OUT, R_IN, segment.startAngle, segment.endAngle)}
                      fill={segment.color}
                    />
                    {showInnerLabel ? (
                      <text
                        x={lx}
                        y={ly}
                        className="markets-allocation-slice-label"
                        textAnchor="middle"
                        dominantBaseline="middle"
                      >
                        {pctLabelFmt.format(segment.weightPct)}%
                      </text>
                    ) : null}
                  </g>
                )
              })}

              {boundaries.map((boundary) => {
                const [hx, hy] = polar(CX, CY, R_OUT + 2, boundary.angle)
                return (
                  <g key={`boundary-${boundary.index}`}>
                    <circle
                      className="markets-allocation-handle-hit"
                      cx={hx}
                      cy={hy}
                      r={HANDLE_R + 6}
                      onPointerDown={(event) => startBoundaryDrag(boundary.index, event)}
                    />
                    <circle
                      className="markets-allocation-handle"
                      cx={hx}
                      cy={hy}
                      r={HANDLE_R}
                      onPointerDown={(event) => startBoundaryDrag(boundary.index, event)}
                    />
                  </g>
                )
              })}

              <circle className="markets-allocation-hole" cx={CX} cy={CY} r={R_IN - 2} />
              <text x={CX} y={CY - 6} className="markets-allocation-center-label" textAnchor="middle">
                {t('portfolioSim.allocationCenter')}
              </text>
              <text x={CX} y={CY + 12} className="markets-allocation-center-value" textAnchor="middle">
                {pctLabelFmt.format(weightsSum)}%
              </text>
            </>
          )}
        </svg>
        {!isEmpty ? (
          <p className="markets-allocation-drag-hint">{t('portfolioSim.dragAllocation')}</p>
        ) : null}
      </div>

      <div className="markets-allocation-legend-wrap">
        {isEmpty ? (
          <p className="markets-allocation-legend-empty">{t('portfolioSim.empty')}</p>
        ) : (
          <ul className="markets-allocation-legend" aria-label={t('portfolioSim.legendAria')}>
            {segments.map((segment) => {
              const isActive = activeId === segment.id
              const metrics = metricsByHoldingId?.get(segment.id)
              const isLoading = loadingSymbols.includes(segment.symbol)
              const isFailed = failedSymbols.includes(segment.symbol)
              const fmtReturn = (value: number | null | undefined) =>
                value == null ? '—' : `${returnFmt.format(value)}%`
              const returnClass = (value: number | null | undefined) =>
                value == null ? '' : value >= 0 ? 'markets-positive' : 'markets-negative'

              return (
                <li
                  key={segment.id}
                  className={`markets-allocation-legend-item${isActive ? ' is-active' : ''}`}
                  style={
                    isActive
                      ? {
                          borderColor: `color-mix(in srgb, ${segment.color} 55%, var(--color-border))`,
                          background: `color-mix(in srgb, ${segment.color} 12%, var(--color-surface))`,
                        }
                      : undefined
                  }
                  onMouseEnter={() => onActiveChange(segment.id)}
                  onMouseLeave={() => onActiveChange(null)}
                >
                  <span className="markets-allocation-legend-swatch" style={{ background: segment.color }} />
                  <div className="markets-allocation-legend-text">
                    <strong>{segment.symbol}</strong>
                    <span>{segment.name}</span>
                    {isLoading ? (
                      <span className="markets-allocation-legend-status">{t('portfolioSim.holdingLoading')}</span>
                    ) : null}
                    {!isLoading && isFailed ? (
                      <span className="markets-allocation-legend-status markets-allocation-legend-status--error">
                        {t('portfolioSim.holdingError')}
                      </span>
                    ) : null}
                    {!isLoading && !isFailed && metrics && !metrics.error ? (
                      <dl className="markets-allocation-legend-metrics">
                        <div>
                          <dt>{t('portfolioSim.metricAsset')}</dt>
                          <dd className={returnClass(metrics.assetReturnPct)}>
                            {fmtReturn(metrics.assetReturnPct)}
                          </dd>
                        </div>
                        {metrics.fxReturnPct != null ? (
                          <div>
                            <dt>{t('portfolioSim.metricFx')}</dt>
                            <dd className={returnClass(metrics.fxReturnPct)}>
                              {fmtReturn(metrics.fxReturnPct)}
                            </dd>
                          </div>
                        ) : null}
                        <div>
                          <dt>{t('portfolioSim.metricTotal')}</dt>
                          <dd className={returnClass(metrics.totalReturnPct)}>
                            {fmtReturn(metrics.totalReturnPct)}
                          </dd>
                        </div>
                      </dl>
                    ) : null}
                    {!isLoading && metrics?.error ? (
                      <span className="markets-allocation-legend-status markets-allocation-legend-status--error">
                        {t('portfolioSim.holdingError')}
                      </span>
                    ) : null}
                  </div>
                  <span className="markets-allocation-legend-pct">{pctLabelFmt.format(segment.weightPct)}%</span>
                  <button
                    type="button"
                    className="markets-portfolio-sim-remove"
                    onClick={() => onRemove(segment.id)}
                    aria-label={t('portfolioSim.removeAria', { symbol: segment.symbol })}
                  >
                    ×
                  </button>
                </li>
              )
            })}
          </ul>
        )}

        {!isEmpty ? (
          <p
            className={
              weightsOk
                ? 'markets-portfolio-sim-weight-sum'
                : 'markets-portfolio-sim-weight-sum markets-portfolio-sim-weight-sum-warn'
            }
          >
            {t('portfolioSim.weightTotal', {
              value: pctLabelFmt.format(weightsSum),
            })}
          </p>
        ) : null}
      </div>
    </div>
  )
}
