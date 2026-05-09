import { useEffect, useLayoutEffect, useMemo, useRef } from 'react'
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
import type { PortfolioTradeFlowPoint } from '../../../shared/types/portfolio'
import {
  RANGE_TO_MS,
  applyVisibleWindow,
  formatAxisLabel,
  rangeStepSeconds,
  sortDedupeByTime,
  type ValueChartRange,
} from './portfolioChartShared'

export type { ValueChartRange }

function buildTradeFlowLineData(points: PortfolioTradeFlowPoint[], range: ValueChartRange): LineData<Time>[] {
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

  if (steps.length === 0) return []

  const nowMs = Date.now()
  const fromSec = Math.floor((nowMs - RANGE_TO_MS[range]) / 1000)
  const nowSec = Math.floor(nowMs / 1000)
  const step = rangeStepSeconds(range)
  const liveCum = steps[steps.length - 1].cum

  let valueBeforeWindow = 0
  for (const s of steps) {
    if (s.t < fromSec) valueBeforeWindow = s.cum
  }

  const inWindow = steps.filter((s) => s.t >= fromSec && s.t <= nowSec)
  const windowReals: LineData<Time>[] = inWindow.map((s) => ({
    time: s.t as UTCTimestamp,
    value: s.cum,
  }))

  const lastW = windowReals[windowReals.length - 1]
  if (!lastW) {
    const flat: LineData<Time>[] = []
    for (let t = fromSec; t < nowSec; t += step) {
      flat.push({ time: t as UTCTimestamp, value: valueBeforeWindow })
    }
    flat.push({ time: nowSec as UTCTimestamp, value: liveCum })
    return sortDedupeByTime(flat)
  }

  if ((lastW.time as number) < nowSec) {
    windowReals.push({ time: nowSec as UTCTimestamp, value: liveCum })
  } else if ((lastW.time as number) === nowSec) {
    windowReals[windowReals.length - 1] = { time: nowSec as UTCTimestamp, value: liveCum }
  }

  const firstRealT = windowReals[0].time as number
  const anchor = valueBeforeWindow

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
  points: PortfolioTradeFlowPoint[]
  height: number
  isDark: boolean
  emptyLabel: string
  locale: string
}

export function TradeFlowHistoryChart({ range, points, height, isDark, emptyLabel, locale }: Props) {
  const outerRef = useRef<HTMLDivElement | null>(null)
  const mountRef = useRef<HTMLDivElement | null>(null)
  const chartRef = useRef<IChartApi | null>(null)
  const seriesRef = useRef<ISeriesApi<'Area'> | null>(null)
  const roRef = useRef<ResizeObserver | null>(null)
  const heightRef = useRef(height)
  useEffect(() => {
    heightRef.current = height
  }, [height])

  const data = useMemo(() => buildTradeFlowLineData(points, range), [points, range])

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

    const line = isDark ? '#34d399' : '#059669'
    const top = isDark ? 'rgba(52, 211, 153, 0.28)' : 'rgba(5, 150, 105, 0.22)'
    const bottom = isDark ? 'rgba(52, 211, 153, 0.02)' : 'rgba(5, 150, 105, 0.02)'

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
