import { useMemo, useRef } from 'react'
import type { LineData, Time } from 'lightweight-charts'
import type { PortfolioTradeFlowPoint } from '../../../shared/types/portfolio'
import { TradingAreaChart } from '../../../shared/chart'
import {
  lineSeriesDataShallowEqual,
  MAX_CHART_HISTORY_SEC,
  normalizeTradeFlowLineData,
  RANGE_TO_MS,
  type ValueChartRange,
} from './portfolioChartShared'

export type { ValueChartRange }

function buildTradeFlowLineData(points: PortfolioTradeFlowPoint[]): LineData<Time>[] {
  const sorted = [...points].sort((a, b) => {
    const ta = Date.parse(a.createdAt)
    const tb = Date.parse(b.createdAt)
    if (ta !== tb) return ta - tb
    return a.transactionId - b.transactionId
  })

  let cum = 0
  const steps: { t: number; cum: number }[] = []
  for (const p of sorted) {
    const sec = Math.floor(Date.parse(p.createdAt) / 1000)
    if (!Number.isFinite(sec)) continue
    cum += p.signedAmount
    const prev = steps[steps.length - 1]
    if (prev && prev.t === sec) prev.cum = cum
    else steps.push({ t: sec, cum })
  }

  const nowSec = Math.floor(Date.now() / 1000)
  const rawPoints = steps.filter((s) => s.t <= nowSec).map((s) => ({ t: s.t, v: s.cum }))
  return normalizeTradeFlowLineData(rawPoints, nowSec)
}

type Props = {
  points: PortfolioTradeFlowPoint[]
  height: number
  isDark: boolean
  emptyLabel: string
  locale: string
  maskAmounts?: boolean
}

export function TradeFlowHistoryChart({
  points,
  height,
  isDark,
  emptyLabel,
  locale,
  maskAmounts = false,
}: Props) {
  const dataStableRef = useRef<LineData<Time>[]>([])
  const lineData = useMemo(() => {
    const next = buildTradeFlowLineData(points)
    const prev = dataStableRef.current
    if (lineSeriesDataShallowEqual(prev, next)) return prev
    dataStableRef.current = next
    return next
  }, [points])

  const chartData = useMemo(
    () => lineData.map((d) => ({ time: d.time as number, value: d.value })),
    [lineData],
  )

  return (
    <TradingAreaChart
      chartId="portfolio-trade-flow"
      data={chartData}
      height={height}
      isDark={isDark}
      emptyLabel={emptyLabel}
      locale={locale}
      maskAmounts={maskAmounts}
      maxHistorySec={MAX_CHART_HISTORY_SEC}
      rightBoundary="now"
      debug={import.meta.env.DEV}
      colors={{
        lineColor: isDark ? '#34d399' : '#059669',
        topColor: isDark ? 'rgba(52, 211, 153, 0.28)' : 'rgba(5, 150, 105, 0.22)',
        bottomColor: isDark ? 'rgba(52, 211, 153, 0.02)' : 'rgba(5, 150, 105, 0.02)',
      }}
    />
  )
}

/** Kart üstündeki alım / satım / net — tüm işlem geçmişi. */
export function tradeFlowTotals(points: PortfolioTradeFlowPoint[]): { buy: number; sell: number; net: number } {
  let buy = 0
  let sell = 0
  for (const p of points) {
    if (p.signedAmount > 0) buy += p.signedAmount
    else sell += -p.signedAmount
  }
  return { buy, sell, net: buy - sell }
}

export function tradeFlowPeriodTotals(
  points: PortfolioTradeFlowPoint[],
  range: ValueChartRange,
): { buy: number; sell: number; net: number } {
  const nowMs = Date.now()
  const fromSec = Math.floor((nowMs - RANGE_TO_MS[range]) / 1000)
  const nowSec = Math.floor(nowMs / 1000)
  let buy = 0
  let sell = 0
  for (const p of points) {
    const sec = Math.floor(Date.parse(p.createdAt) / 1000)
    if (!Number.isFinite(sec) || sec < fromSec || sec > nowSec) continue
    if (p.signedAmount > 0) buy += p.signedAmount
    else sell += -p.signedAmount
  }
  return { buy, sell, net: buy - sell }
}
