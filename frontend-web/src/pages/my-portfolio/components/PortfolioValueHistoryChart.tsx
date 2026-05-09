import { useLayoutEffect, useMemo, useRef, useEffect } from 'react'
import {
  AreaSeries,
  ColorType,
  createChart,
  CrosshairMode,
  type IChartApi,
  type ISeriesApi,
  type LineData,
  type TickMarkFormatter,
  type Time,
  type UTCTimestamp,
} from 'lightweight-charts'
import type { PortfolioValueSnapshot } from '../../../shared/types/portfolio'
import {
  RANGE_TO_MS,
  applyVisibleWindow,
  formatAxisLabel,
  rangeStepSeconds,
  sortDedupeByTime,
  type ValueChartRange,
} from './portfolioChartShared'

export type { ValueChartRange }

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

/**
 * Real snapshots + synthetic flat run from window start → first real point so
 * `setVisibleRange` can actually show 1W vs 1Y width (library cannot extrapolate past data).
 */
function buildLineData(
  snapshots: PortfolioValueSnapshot[],
  liveTotalValue: number | null,
  range: ValueChartRange,
): LineData<Time>[] {
  const nowMs = Date.now()
  const fromSec = Math.floor((nowMs - RANGE_TO_MS[range]) / 1000)
  const nowSec = Math.floor(nowMs / 1000)
  const step = rangeStepSeconds(range)

  const all: { t: number; v: number }[] = []
  for (const s of snapshots) {
    const sec = parseSnapshotTime(s.createdAt)
    const v = parseSnapshotValue(s.totalValue)
    if (!Number.isFinite(sec) || v == null) continue
    if (sec > nowSec) continue
    all.push({ t: sec, v })
  }
  all.sort((a, b) => a.t - b.t)
  const deduped: { t: number; v: number }[] = []
  for (const p of all) {
    const prev = deduped[deduped.length - 1]
    if (prev && prev.t === p.t) prev.v = p.v
    else deduped.push({ t: p.t, v: p.v })
  }
  all.length = 0
  all.push(...deduped)

  let valueBeforeWindow: number | null = null
  const windowReals: LineData<Time>[] = []
  for (const p of all) {
    if (p.t < fromSec) {
      valueBeforeWindow = p.v
      continue
    }
    if (p.t > nowSec) break
    windowReals.push({ time: p.t as UTCTimestamp, value: p.v })
  }

  if (liveTotalValue != null && Number.isFinite(liveTotalValue) && liveTotalValue > 0) {
    const last = windowReals[windowReals.length - 1]
    if (last && last.time === nowSec) {
      windowReals[windowReals.length - 1] = { time: nowSec as UTCTimestamp, value: liveTotalValue }
    } else {
      windowReals.push({ time: nowSec as UTCTimestamp, value: liveTotalValue })
    }
  }

  /** Value for synthetic points left of the first in-window sample. Never use liveTotalValue here — that is “now” (right edge), not the past. */
  const pickLeftAnchor = (firstInWindowVal: number): number => {
    if (valueBeforeWindow != null && Number.isFinite(valueBeforeWindow)) return valueBeforeWindow
    return Number.isFinite(firstInWindowVal) ? firstInWindowVal : 0
  }

  if (windowReals.length === 0) {
    const v = valueBeforeWindow ?? liveTotalValue
    if (v == null || !Number.isFinite(v)) return []
    const flat: LineData<Time>[] = []
    for (let t = fromSec; t < nowSec; t += step) {
      flat.push({ time: t as UTCTimestamp, value: v })
    }
    flat.push({ time: nowSec as UTCTimestamp, value: liveTotalValue ?? v })
    return sortDedupeByTime(flat)
  }

  const firstRealT = windowReals[0].time as number
  const anchor = pickLeftAnchor(windowReals[0].value)

  const head: LineData<Time>[] = []
  head.push({ time: fromSec as UTCTimestamp, value: anchor })
  if (firstRealT - fromSec > 60) {
    for (let t = fromSec + step; t < firstRealT - 1; t += step) {
      head.push({ time: t as UTCTimestamp, value: anchor })
    }
  }

  return sortDedupeByTime([...head, ...windowReals])
}

type Props = {
  range: ValueChartRange
  snapshots: PortfolioValueSnapshot[]
  liveTotalValue: number | null
  height: number
  isDark: boolean
  emptyLabel: string
  locale: string
}

export function PortfolioValueHistoryChart({
  range,
  snapshots,
  liveTotalValue,
  height,
  isDark,
  emptyLabel,
  locale,
}: Props) {
  const outerRef = useRef<HTMLDivElement | null>(null)
  const mountRef = useRef<HTMLDivElement | null>(null)
  const chartRef = useRef<IChartApi | null>(null)
  const seriesRef = useRef<ISeriesApi<'Area'> | null>(null)
  const roRef = useRef<ResizeObserver | null>(null)
  const heightRef = useRef(height)
  useEffect(() => {
    heightRef.current = height
  }, [height])

  const data = useMemo(
    () => buildLineData(snapshots, liveTotalValue, range),
    [snapshots, liveTotalValue, range],
  )

  const destroyChart = () => {
    roRef.current?.disconnect()
    roRef.current = null
    const c = chartRef.current
    chartRef.current = null
    seriesRef.current = null
    if (c) {
      try {
        c.remove()
      } catch {
        /* */
      }
    }
  }

  useLayoutEffect(() => {
    return () => {
      destroyChart()
    }
  }, [])

  useLayoutEffect(() => {
    const outer = outerRef.current
    const mount = mountRef.current
    if (!outer || !mount) return

    if (data.length === 0) {
      destroyChart()
      return
    }

    const line = isDark ? '#818cf8' : '#6366f1'
    const top = isDark ? 'rgba(129, 140, 248, 0.32)' : 'rgba(99, 102, 241, 0.28)'
    const bottom = isDark ? 'rgba(129, 140, 248, 0.02)' : 'rgba(99, 102, 241, 0.02)'

    const timeFormatter = (t: Time) => formatAxisLabel(t, range, locale)
    const tickMarkFormatter: TickMarkFormatter = (t) => formatAxisLabel(t, range, locale)

    if (!chartRef.current) {
      const w0 = Math.max(mount.clientWidth, 120)
      const h0 = Math.max(mount.clientHeight || 0, height)
      const chart = createChart(mount, {
        width: w0,
        height: h0,
        layout: {
          background: { type: ColorType.Solid, color: 'transparent' },
          textColor: isDark ? '#94a3b8' : '#64748b',
          fontSize: 11,
          attributionLogo: false,
        },
        localization: {
          locale,
          timeFormatter,
          dateFormat: "dd MMM ''yy",
        },
        grid: {
          vertLines: { color: 'rgba(148, 163, 184, 0.07)' },
          horzLines: { color: 'rgba(148, 163, 184, 0.07)' },
        },
        rightPriceScale: {
          borderVisible: false,
          scaleMargins: { top: 0.12, bottom: 0.08 },
        },
        timeScale: {
          borderVisible: false,
          fixRightEdge: false,
          fixLeftEdge: false,
          shiftVisibleRangeOnNewBar: false,
          secondsVisible: false,
          timeVisible: range === '1w',
          visible: true,
          ticksVisible: true,
          tickMarkFormatter,
        },
        crosshair: {
          mode: CrosshairMode.Magnet,
          vertLine: { color: 'rgba(148, 163, 184, 0.35)', labelBackgroundColor: isDark ? '#334155' : '#e2e8f0' },
          horzLine: { color: 'rgba(148, 163, 184, 0.35)', labelBackgroundColor: isDark ? '#334155' : '#e2e8f0' },
        },
        handleScroll: {
          mouseWheel: true,
          pressedMouseMove: true,
          horzTouchDrag: true,
          vertTouchDrag: false,
        },
        handleScale: {
          axisPressedMouseMove: true,
          mouseWheel: true,
          pinch: true,
        },
      })

      const series = chart.addSeries(AreaSeries, {
        lineColor: line,
        topColor: top,
        bottomColor: bottom,
        lineWidth: 2,
        priceLineVisible: true,
        lastValueVisible: false,
        crosshairMarkerVisible: true,
        crosshairMarkerRadius: 4,
      })

      chartRef.current = chart
      seriesRef.current = series

      const syncChartSize = () => {
        if (!chartRef.current || !mount.isConnected) return
        const w = Math.max(mount.clientWidth, 120)
        const h = Math.max(mount.clientHeight || 0, heightRef.current, 1)
        try {
          chartRef.current.resize(w, h, true)
        } catch {
          /* */
        }
      }
      const ro = new ResizeObserver(() => syncChartSize())
      ro.observe(mount)
      roRef.current = ro
      requestAnimationFrame(syncChartSize)
    }

    const chart = chartRef.current
    const series = seriesRef.current
    if (!chart || !series) return

    series.applyOptions({
      lineColor: line,
      topColor: top,
      bottomColor: bottom,
    })

    chart.applyOptions({
      layout: {
        background: { type: ColorType.Solid, color: 'transparent' },
        textColor: isDark ? '#94a3b8' : '#64748b',
        fontSize: 11,
        attributionLogo: false,
      },
      localization: {
        locale,
        timeFormatter,
        dateFormat: "dd MMM ''yy",
      },
      timeScale: {
        shiftVisibleRangeOnNewBar: false,
        timeVisible: range === '1w',
        tickMarkFormatter,
      },
    })

    series.setData(data)

    const w = Math.max(mount.clientWidth, 120)
    const h = Math.max(mount.clientHeight || 0, heightRef.current, 1)
    try {
      chart.resize(w, h, true)
    } catch {
      /* */
    }

    const raf = requestAnimationFrame(() => {
      if (!chartRef.current) return
      applyVisibleWindow(chartRef.current, range)
    })
    return () => cancelAnimationFrame(raf)
  }, [data, range, locale, height, isDark])

  if (data.length === 0) {
    return (
      <div className="my-portfolio-value-chart-empty" style={{ minHeight: height }}>
        {emptyLabel}
      </div>
    )
  }

  return (
    <div ref={outerRef} className="my-portfolio-value-chart">
      <div ref={mountRef} className="my-portfolio-value-chart-mount" style={{ width: '100%', height }} />
    </div>
  )
}

export const VALUE_CHART_RANGES: ValueChartRange[] = ['1w', '1m', '3m', '6m', '1y']

export function valueChartRangeLabel(r: ValueChartRange): string {
  switch (r) {
    case '1w':
      return '1W'
    case '1m':
      return '1M'
    case '3m':
      return '3M'
    case '6m':
      return '6M'
    case '1y':
      return '1Y'
    default:
      return r
  }
}
