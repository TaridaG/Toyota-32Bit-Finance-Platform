import { useMemo } from 'react'
import { TradingAreaChart } from '../../../shared/chart'
import type { PortfolioPerformanceSeries } from '../../../shared/types/portfolio'
import {
  RANGE_TO_SEC,
  type ValueChartRange,
} from './portfolioChartShared'

export type { ValueChartRange } from './portfolioChartShared'
export type PortfolioChartMetric = 'performance' | 'value'

type Props = {
  series: PortfolioPerformanceSeries | null
  metric: PortfolioChartMetric
  range: ValueChartRange
  height: number
  isDark: boolean
  emptyLabel: string
  locale: string
  maskAmounts?: boolean
  moneyFormatter: Intl.NumberFormat
  percentFormatter: Intl.NumberFormat
}

function parseUtcDay(day: string): number {
  const t = Date.parse(`${day}T12:00:00Z`)
  return Number.isFinite(t) ? Math.floor(t / 1000) : Number.NaN
}

function filterRangePoints(
  points: Array<{ time: number; value: number }>,
  range: ValueChartRange,
): Array<{ time: number; value: number }> {
  if (points.length === 0 || range === 'all') {
    return points
  }

  const endSec = points[points.length - 1]!.time
  const startSec = Math.max(0, endSec - RANGE_TO_SEC[range] + 86_400)
  const filtered = points.filter((point) => point.time >= startSec)
  const lastBeforeStart = [...points].reverse().find((point) => point.time < startSec) ?? null

  if (filtered.length === 0) {
    const tail = points[points.length - 1]!
    return lastBeforeStart != null
      ? [
          { time: startSec, value: lastBeforeStart.value },
          { time: tail.time, value: tail.value },
        ]
      : [tail]
  }

  if (filtered[0]!.time > startSec && lastBeforeStart != null) {
    return [{ time: startSec, value: lastBeforeStart.value }, ...filtered]
  }

  return filtered
}

export function PortfolioValueHistoryChart({
  series,
  metric,
  range,
  height,
  isDark,
  emptyLabel,
  locale,
  maskAmounts = false,
  moneyFormatter,
  percentFormatter,
}: Props) {
  const chartData = useMemo(() => {
    const raw = (series?.points ?? [])
      .map((point) => ({
        time: parseUtcDay(point.day),
        value: metric === 'performance' ? point.twrPct : point.marketValue,
      }))
      .filter((point) => Number.isFinite(point.time) && Number.isFinite(point.value))
    return filterRangePoints(raw, range)
  }, [metric, range, series])

  const defaultWindowSec = useMemo(() => {
    if (chartData.length <= 1) {
      return range === 'all' ? RANGE_TO_SEC['1w'] : RANGE_TO_SEC[range]
    }
    if (range !== 'all') {
      return RANGE_TO_SEC[range]
    }
    const dataSpan = chartData[chartData.length - 1]!.time - chartData[0]!.time
    return Math.max(86_400, dataSpan)
  }, [chartData, range])

  return (
    <TradingAreaChart
      chartId={`portfolio-${metric}-${range}`}
      data={chartData}
      height={height}
      isDark={isDark}
      emptyLabel={emptyLabel}
      locale={locale}
      maskAmounts={maskAmounts}
      valueFormatter={(value) => (metric === 'performance' ? `${percentFormatter.format(value)}%` : moneyFormatter.format(value))}
      defaultWindowSec={defaultWindowSec}
      rightBoundary="lastData"
      viewportMode="logical-range"
      debug={false}
      colors={
        metric === 'performance'
          ? {
              lineColor: isDark ? '#34d399' : '#059669',
              topColor: isDark ? 'rgba(52, 211, 153, 0.28)' : 'rgba(5, 150, 105, 0.22)',
              bottomColor: isDark ? 'rgba(52, 211, 153, 0.03)' : 'rgba(5, 150, 105, 0.03)',
            }
          : {
              lineColor: isDark ? '#818cf8' : '#6366f1',
              topColor: isDark ? 'rgba(129, 140, 248, 0.32)' : 'rgba(99, 102, 241, 0.28)',
              bottomColor: isDark ? 'rgba(129, 140, 248, 0.02)' : 'rgba(99, 102, 241, 0.02)',
            }
      }
    />
  )
}
