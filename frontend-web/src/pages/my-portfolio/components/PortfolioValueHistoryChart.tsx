import { useMemo, useRef } from 'react'
import type { LineData, Time } from 'lightweight-charts'
import type { PortfolioValueSnapshot } from '../../../shared/types/portfolio'
import { TradingAreaChart } from '../../../shared/chart'
import { lineSeriesDataShallowEqual, MAX_CHART_HISTORY_SEC, normalizePortfolioValueLineData } from './portfolioChartShared'

export type { ValueChartRange } from './portfolioChartShared'

function parseSnapshotTime(iso: string): number {
  const t = Date.parse(iso)
  return Number.isFinite(t) ? Math.floor(t / 1000) : NaN
}

function parseSnapshotValue(raw: unknown): number | null {
  if (raw == null) return null
  if (typeof raw === 'number') return Number.isFinite(raw) ? raw : null
  if (typeof raw === 'string') {
    const n = Number(raw.trim().replace(/\s/g, '').replace(',', '.'))
    return Number.isFinite(n) ? n : null
  }
  return null
}

function buildLineData(snapshots: PortfolioValueSnapshot[], liveTotalValue: number | null): LineData<Time>[] {
  const nowSec = Math.floor(Date.now() / 1000)
  const raw: { t: number; v: number }[] = []
  for (const s of snapshots) {
    const sec = parseSnapshotTime(s.createdAt)
    const v = parseSnapshotValue(s.totalValue)
    if (!Number.isFinite(sec) || v == null) continue
    if (sec > nowSec) continue
    raw.push({ t: sec, v })
  }
  const tailOverride =
    liveTotalValue != null && Number.isFinite(liveTotalValue) ? liveTotalValue : null
  return normalizePortfolioValueLineData(raw, nowSec, tailOverride)
}

type Props = {
  snapshots: PortfolioValueSnapshot[]
  liveTotalValue: number | null
  height: number
  isDark: boolean
  emptyLabel: string
  locale: string
  maskAmounts?: boolean
}

export function PortfolioValueHistoryChart({
  snapshots,
  liveTotalValue,
  height,
  isDark,
  emptyLabel,
  locale,
  maskAmounts = false,
}: Props) {
  const dataStableRef = useRef<LineData<Time>[]>([])
  const lineData = useMemo(() => {
    const next = buildLineData(snapshots, liveTotalValue)
    const prev = dataStableRef.current
    if (lineSeriesDataShallowEqual(prev, next)) return prev
    dataStableRef.current = next
    return next
  }, [snapshots, liveTotalValue])

  const chartData = useMemo(
    () => lineData.map((d) => ({ time: d.time as number, value: d.value })),
    [lineData],
  )

  return (
    <TradingAreaChart
      chartId="portfolio-value"
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
        lineColor: isDark ? '#818cf8' : '#6366f1',
        topColor: isDark ? 'rgba(129, 140, 248, 0.32)' : 'rgba(99, 102, 241, 0.28)',
        bottomColor: isDark ? 'rgba(129, 140, 248, 0.02)' : 'rgba(99, 102, 241, 0.02)',
      }}
    />
  )
}
