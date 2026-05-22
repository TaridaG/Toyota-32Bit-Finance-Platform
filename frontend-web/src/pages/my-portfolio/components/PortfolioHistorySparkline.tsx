import { useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import type { PortfolioTradeFlowPoint, PortfolioValueSnapshot } from '../../../shared/types/portfolio'
import {
  VALUE_CHART_RANGES,
  valueChartRangeLabel,
  type ValueChartRange,
} from './portfolioChartShared'
import {
  buildPortfolioTrendSeries,
  buildTradeFlowTrendSeries,
  filterTrendByRange,
  seriesToSparklinePath,
  summarizePortfolioTrend,
} from './portfolioTrendSeries'

const CHART_W = 320
const CHART_H = 64

type BaseProps = {
  range: ValueChartRange
  onRangeChange: (range: ValueChartRange) => void
  isDark: boolean
  locale: string
  maskAmounts?: boolean
  formatValue: (value: number) => string
  emptyLabel: string
  rangeAriaLabel: string
  variant: 'value' | 'tradeFlow'
}

type ValueProps = BaseProps & {
  variant: 'value'
  snapshots: PortfolioValueSnapshot[]
  liveTotalValue: number | null
}

type TradeFlowProps = BaseProps & {
  variant: 'tradeFlow'
  tradeFlowPoints: PortfolioTradeFlowPoint[]
}

export type PortfolioHistorySparklineProps = ValueProps | TradeFlowProps

export function PortfolioHistorySparkline(props: PortfolioHistorySparklineProps) {
  const { t } = useTranslation('portfolio')
  const [hoverIndex, setHoverIndex] = useState<number | null>(null)

  const fullSeries = useMemo(() => {
    if (props.variant === 'value') {
      return buildPortfolioTrendSeries(props.snapshots, props.liveTotalValue)
    }
    return buildTradeFlowTrendSeries(props.tradeFlowPoints)
  }, [
    props.variant,
    props.variant === 'value' ? props.snapshots : props.tradeFlowPoints,
    props.variant === 'value' ? props.liveTotalValue : null,
  ])

  const series = useMemo(
    () => filterTrendByRange(fullSeries, props.range),
    [fullSeries, props.range],
  )

  const summary = useMemo(() => summarizePortfolioTrend(series), [series])
  const values = useMemo(() => series.map((p) => p.v), [series])
  const paths = useMemo(() => seriesToSparklinePath(values, CHART_W, CHART_H), [values])

  const pctFormat = new Intl.NumberFormat(props.locale, {
    maximumFractionDigits: 1,
    signDisplay: 'exceptZero',
  })
  const dateFormat = new Intl.DateTimeFormat(props.locale, { day: 'numeric', month: 'short' })

  const rangeLabel = valueChartRangeLabel(props.range)
  const changeLabel =
    summary?.changePct != null && Number.isFinite(summary.changePct)
      ? t('valueChart.periodChange', {
          pct: props.maskAmounts ? '•••' : pctFormat.format(summary.changePct),
          range: rangeLabel,
        })
      : summary
        ? t('valueChart.flatInRange', { range: rangeLabel })
        : null

  const rangeDates =
    summary && series.length >= 2
      ? t('valueChart.periodRange', {
          from: dateFormat.format(new Date(summary.startMs)),
          to: dateFormat.format(new Date(summary.endMs)),
        })
      : null

  const tone =
    summary?.changePct == null
      ? 'neutral'
      : summary.changePct > 0.05
        ? 'up'
        : summary.changePct < -0.05
          ? 'down'
          : 'neutral'

  const hoverPoint =
    hoverIndex != null && hoverIndex >= 0 && hoverIndex < series.length ? series[hoverIndex] : null

  if (series.length === 0 || !paths.line) {
    return (
      <div className="my-portfolio-history-chart">
        <p className="my-portfolio-mini-trend-empty">{props.emptyLabel}</p>
        <ChartRangeBar range={props.range} onRangeChange={props.onRangeChange} ariaLabel={props.rangeAriaLabel} />
      </div>
    )
  }

  return (
    <div className="my-portfolio-history-chart my-portfolio-mini-trend" data-tone={tone} data-variant={props.variant}>
      <div className="my-portfolio-mini-trend-meta">
        {changeLabel ? <span className="my-portfolio-mini-trend-change">{changeLabel}</span> : null}
        {rangeDates ? <span className="my-portfolio-mini-trend-range">{rangeDates}</span> : null}
        {hoverPoint ? (
          <span className="my-portfolio-mini-trend-hover">
            {props.maskAmounts ? '•••' : props.formatValue(hoverPoint.v)} ·{' '}
            {dateFormat.format(new Date(hoverPoint.t * 1000))}
          </span>
        ) : null}
      </div>
      <div
        className="my-portfolio-history-chart-surface"
        onMouseLeave={() => setHoverIndex(null)}
        onMouseMove={(event) => {
          const rect = event.currentTarget.getBoundingClientRect()
          const ratio = Math.min(Math.max((event.clientX - rect.left) / rect.width, 0), 1)
          const idx = Math.round(ratio * (series.length - 1))
          setHoverIndex(idx)
        }}
      >
        <svg
          className={`my-portfolio-mini-trend-svg${props.isDark ? ' is-dark' : ''}`}
          viewBox={`0 0 ${CHART_W} ${CHART_H}`}
          preserveAspectRatio="none"
          role="img"
          aria-label={changeLabel ?? props.emptyLabel}
        >
          <path className="my-portfolio-mini-trend-area" d={paths.area} />
          <path className="my-portfolio-mini-trend-line" d={paths.line} />
          {hoverIndex != null && series.length > 0 ? (
            <line
              className="my-portfolio-history-chart-crosshair"
              x1={(hoverIndex / Math.max(series.length - 1, 1)) * CHART_W}
              x2={(hoverIndex / Math.max(series.length - 1, 1)) * CHART_W}
              y1={0}
              y2={CHART_H}
            />
          ) : null}
        </svg>
      </div>
      <ChartRangeBar range={props.range} onRangeChange={props.onRangeChange} ariaLabel={props.rangeAriaLabel} />
    </div>
  )
}

function ChartRangeBar({
  range,
  onRangeChange,
  ariaLabel,
}: {
  range: ValueChartRange
  onRangeChange: (range: ValueChartRange) => void
  ariaLabel: string
}) {
  return (
    <div className="my-portfolio-chart-range-below" role="group" aria-label={ariaLabel}>
      {VALUE_CHART_RANGES.map((r) => (
        <button
          key={r}
          type="button"
          className={`my-portfolio-value-range-btn${range === r ? ' is-active' : ''}`}
          onClick={() => onRangeChange(r)}
        >
          {valueChartRangeLabel(r)}
        </button>
      ))}
    </div>
  )
}
