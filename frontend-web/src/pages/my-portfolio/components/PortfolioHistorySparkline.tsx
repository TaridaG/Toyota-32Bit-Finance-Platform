import { useMemo, useState } from 'react'
import type { PortfolioTradeFlowPoint, PortfolioValueSnapshot } from '../../../shared/types/portfolio'
import {
  buildPortfolioTrendSeries,
  buildTradeFlowDailySeries,
  fillDailyCalendarSeries,
  seriesToSparklinePath,
  type SparklineScaleMode,
} from './portfolioTrendSeries'

const CHART_W = 400
const DEFAULT_CHART_H = 72
const DASHBOARD_DAYS = 60

type BaseProps = {
  isDark: boolean
  locale: string
  maskAmounts?: boolean
  formatValue: (value: number) => string
  emptyLabel: string
  chartHeight?: number
  variant: 'value' | 'tradeFlow'
}

type ValueProps = BaseProps & {
  variant: 'value'
  snapshots: PortfolioValueSnapshot[]
  liveTotalValue: number | null
  priorDayValue?: number | null
}

type TradeFlowProps = BaseProps & {
  variant: 'tradeFlow'
  tradeFlowPoints: PortfolioTradeFlowPoint[]
}

export type PortfolioHistorySparklineProps = ValueProps | TradeFlowProps

export function PortfolioHistorySparkline(props: PortfolioHistorySparklineProps) {
  const [hoverIndex, setHoverIndex] = useState<number | null>(null)

  const scaleMode: SparklineScaleMode = props.variant === 'value' ? 'data' : 'zero'

  const series = useMemo(() => {
    if (props.variant === 'value') {
      const raw = buildPortfolioTrendSeries(
        props.snapshots,
        props.liveTotalValue,
        props.priorDayValue,
      )
      return fillDailyCalendarSeries(raw, {
        liveValue: props.liveTotalValue,
        maxDays: DASHBOARD_DAYS,
        fillMode: 'carry',
      })
    }
    const raw = buildTradeFlowDailySeries(props.tradeFlowPoints)
    return fillDailyCalendarSeries(raw, { maxDays: DASHBOARD_DAYS, fillMode: 'zero' })
  }, [props])

  const values = useMemo(() => series.map((p) => p.v), [series])
  const chartH = props.chartHeight ?? DEFAULT_CHART_H
  const paths = useMemo(
    () => seriesToSparklinePath(values, CHART_W, chartH, 2, 4, scaleMode),
    [values, scaleMode, chartH],
  )

  const dateFormat = useMemo(
    () => new Intl.DateTimeFormat(props.locale, { day: 'numeric', month: 'short' }),
    [props.locale],
  )

  const hoverPoint =
    hoverIndex != null && hoverIndex >= 0 && hoverIndex < series.length ? series[hoverIndex] : null

  if (series.length === 0 || !paths.line) {
    return (
      <div className="my-portfolio-sparkline">
        <p className="my-portfolio-sparkline-empty">{props.emptyLabel}</p>
      </div>
    )
  }

  const ariaValue = props.maskAmounts
    ? props.emptyLabel
    : props.formatValue(series[series.length - 1]!.v)

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
          {scaleMode === 'zero' ? (
            <line
              className="my-portfolio-sparkline-zero"
              x1={0}
              x2={CHART_W}
            y1={chartH - 4}
            y2={chartH - 4}
            />
          ) : null}
          <path className="my-portfolio-sparkline-area" d={paths.area} />
          <path className="my-portfolio-sparkline-line" d={paths.line} />
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
          <div className="my-portfolio-sparkline-tip" role="status">
            <span>{props.maskAmounts ? '•••' : props.formatValue(hoverPoint.v)}</span>
            <span>{dateFormat.format(new Date(hoverPoint.t * 1000))}</span>
          </div>
        ) : null}
      </div>
    </div>
  )
}
