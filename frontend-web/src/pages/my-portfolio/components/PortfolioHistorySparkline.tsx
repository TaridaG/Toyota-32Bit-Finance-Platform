import { useMemo, useState } from 'react'
import type { PortfolioTradeFlowPoint, PortfolioValueSnapshot } from '../../../shared/types/portfolio'
import {
  buildDayOverDayPctSeries,
  buildPortfolioTrendSeries,
  buildTradeFlowDailySeries,
  fillDailyCalendarSeries,
  seriesToSparklinePath,
  type SparklineScaleMode,
} from './portfolioTrendSeries'

const CHART_W = 400
const DEFAULT_CHART_H = 72
const DASHBOARD_DAYS = 60
const CHART_PAD_X = 2
const CHART_PAD_Y = 4

type BaseProps = {
  isDark: boolean
  locale: string
  maskAmounts?: boolean
  formatValue: (value: number) => string
  emptyLabel: string
  chartHeight?: number
  variant: 'value' | 'valueChangePct' | 'tradeFlow'
}

type ValueProps = BaseProps & {
  variant: 'value' | 'valueChangePct'
  snapshots: PortfolioValueSnapshot[]
  liveTotalValue: number | null
  priorDayValue?: number | null
}

type TradeFlowProps = BaseProps & {
  variant: 'tradeFlow'
  tradeFlowPoints: PortfolioTradeFlowPoint[]
}

export type PortfolioHistorySparklineProps = ValueProps | TradeFlowProps

function zeroScaleMetrics(values: number[], height: number, padY: number) {
  const dataMin = Math.min(...values)
  const dataMax = Math.max(...values)
  const min = Math.min(0, dataMin)
  let max = Math.max(0, dataMax)
  if (max > min) max += (max - min) * 0.1
  else if (max > 0) max *= 1.1
  else if (min < 0) max = Math.abs(min) * 0.05
  const range = Math.max(max - min, Math.max(Math.abs(max), Math.abs(min)) * 0.02, 1e-6)
  const zeroY = height - padY - ((0 - min) / range) * (height - 2 * padY)
  return { min, range, zeroY }
}

function buildSparkBars(values: number[], width: number, height: number, padX = CHART_PAD_X, padY = CHART_PAD_Y) {
  const metrics = zeroScaleMetrics(values, height, padY)
  const slot = (width - padX * 2) / Math.max(values.length, 1)
  const barW = Math.max(Math.min(slot * 0.62, 12), 2)
  const bars = values.map((value, index) => {
    const x = padX + index * slot + (slot - barW) / 2
    const y =
      height - padY - ((value - metrics.min) / metrics.range) * (height - 2 * padY)
    return {
      x,
      y: value >= 0 ? y : metrics.zeroY,
      h: Math.max(Math.abs(metrics.zeroY - y), 1.25),
      positive: value >= 0,
    }
  })
  return { bars, zeroY: metrics.zeroY }
}

export function PortfolioHistorySparkline(props: PortfolioHistorySparklineProps) {
  const [hoverIndex, setHoverIndex] = useState<number | null>(null)

  const renderBars = props.variant === 'valueChangePct'
  const scaleMode: SparklineScaleMode = props.variant === 'value' ? 'data' : 'zero'

  const series = useMemo(() => {
    if (props.variant !== 'tradeFlow') {
      const filled = fillDailyCalendarSeries(buildPortfolioTrendSeries(props.snapshots, props.liveTotalValue, props.priorDayValue), {
        liveValue: props.liveTotalValue,
        maxDays: DASHBOARD_DAYS,
        fillMode: 'carry',
      })
      return props.variant === 'valueChangePct' ? buildDayOverDayPctSeries(filled) : filled
    }
    const raw = buildTradeFlowDailySeries(props.tradeFlowPoints)
    return fillDailyCalendarSeries(raw, { maxDays: DASHBOARD_DAYS, fillMode: 'zero' })
  }, [props])

  const values = useMemo(() => series.map((p) => p.v), [series])
  const chartH = props.chartHeight ?? (renderBars ? 96 : DEFAULT_CHART_H)
  const paths = useMemo(
    () => (renderBars ? { line: '', area: '' } : seriesToSparklinePath(values, CHART_W, chartH, CHART_PAD_X, CHART_PAD_Y, scaleMode)),
    [values, scaleMode, chartH, renderBars],
  )
  const barChart = useMemo(
    () => (renderBars && values.length > 0 ? buildSparkBars(values, CHART_W, chartH) : null),
    [renderBars, values, chartH],
  )
  const zeroGuideY = useMemo(
    () => (scaleMode === 'zero' && values.length > 0 ? zeroScaleMetrics(values, chartH, CHART_PAD_Y).zeroY : null),
    [scaleMode, values, chartH],
  )

  const dateFormat = useMemo(
    () => new Intl.DateTimeFormat(props.locale, { day: 'numeric', month: 'short' }),
    [props.locale],
  )

  const hoverPoint =
    hoverIndex != null && hoverIndex >= 0 && hoverIndex < series.length ? series[hoverIndex] : null

  if (series.length === 0 || (!renderBars && !paths.line) || (renderBars && !barChart?.bars.length)) {
    return (
      <div className="my-portfolio-sparkline">
        <p className="my-portfolio-sparkline-empty">{props.emptyLabel}</p>
      </div>
    )
  }

  const ariaValue = props.maskAmounts
    ? props.emptyLabel
    : props.formatValue(series[series.length - 1]!.v)
  const hoverRatio =
    hoverIndex != null && series.length > 1 ? hoverIndex / (series.length - 1) : 0.5

  return (
    <div className="my-portfolio-sparkline" data-variant={props.variant}>
      <div
        className="my-portfolio-sparkline-surface"
        onMouseLeave={() => setHoverIndex(null)}
        onMouseMove={(event) => {
          const rect = event.currentTarget.getBoundingClientRect()
          const ratio = Math.min(Math.max((event.clientX - rect.left) / rect.width, 0), 1)
          const idx = Math.round(ratio * (series.length - 1))
          setHoverIndex(idx)
        }}
      >
        <svg
          className={`my-portfolio-sparkline-svg${props.isDark ? ' is-dark' : ''}`}
          viewBox={`0 0 ${CHART_W} ${chartH}`}
          style={{ height: chartH }}
          preserveAspectRatio="none"
          role="img"
          aria-label={ariaValue}
        >
          {zeroGuideY != null ? (
            <line
              className="my-portfolio-sparkline-zero"
              x1={0}
              x2={CHART_W}
              y1={zeroGuideY}
              y2={zeroGuideY}
            />
          ) : null}
          {renderBars
            ? barChart!.bars.map((bar, index) => (
                <rect
                  key={`${series[index]!.t}-${index}`}
                  className={`my-portfolio-sparkline-bar ${bar.positive ? 'is-pos' : 'is-neg'}${hoverIndex === index ? ' is-active' : ''}`}
                  x={bar.x}
                  y={bar.y}
                  width={Math.max(1, Math.min((CHART_W - CHART_PAD_X * 2) / Math.max(values.length, 1) * 0.62, 12))}
                  height={bar.h}
                  rx={1.5}
                />
              ))
            : (
                <>
                  <path className="my-portfolio-sparkline-area" d={paths.area} />
                  <path className="my-portfolio-sparkline-line" d={paths.line} />
                </>
              )}
          {hoverIndex != null && series.length > 1 ? (
            <line
              className="my-portfolio-sparkline-crosshair"
              x1={(hoverIndex / Math.max(series.length - 1, 1)) * CHART_W}
              x2={(hoverIndex / Math.max(series.length - 1, 1)) * CHART_W}
              y1={0}
              y2={chartH}
            />
          ) : null}
        </svg>
        {hoverPoint ? (
          <div className="my-portfolio-sparkline-tip" role="status" style={{ left: `${Math.min(Math.max(hoverRatio * 100, 12), 88)}%` }}>
            <span>{props.maskAmounts ? '•••' : props.formatValue(hoverPoint.v)}</span>
            <span>{dateFormat.format(new Date(hoverPoint.t * 1000))}</span>
          </div>
        ) : null}
      </div>
    </div>
  )
}
