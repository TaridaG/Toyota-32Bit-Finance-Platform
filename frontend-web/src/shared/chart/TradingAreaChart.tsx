import {
  forwardRef,
  useCallback,
  useImperativeHandle,
  useLayoutEffect,
  useMemo,
  useRef,
} from 'react'
import {
  AreaSeries,
  createChart,
  type IChartApi,
  type ISeriesApi,
  type LineData,
  type TickMarkFormatter,
  type Time,
  type UTCTimestamp,
} from 'lightweight-charts'
import {
  lineDataShallowEqual,
  toLineDataSeries,
  tradingPointsShallowEqual,
  type TradingAreaPoint,
} from './tradingChartData'
import { attachTradingChartInteractions } from './tradingChartInteractions'
import { buildTradingAreaChartOptions } from './tradingChartOptions'
import { axisBandForVisibleSpan, formatTradingAxisTick, formatTradingCrosshairTime } from './tradingChartFormatters'
import { nowUnixSec, setVisibleWindowEndingAt } from './tradingChartViewport'
import { timeToUnixSec } from './timeUtils'

const DEV = import.meta.env.DEV
const DEFAULT_WINDOW_SEC = 7 * 86_400
const DEFAULT_MAX_HISTORY_SEC = 5 * 365 * 86_400
const LOGICAL_BAR_SPACING_MIN = 8
const LOGICAL_BAR_SPACING_MAX = 56

export type TradingAreaChartColors = {
  lineColor: string
  topColor: string
  bottomColor: string
}

export type TradingAreaChartHandle = {
  setWindow: (spanSec: number) => void
  goToLatest: () => void
  fitContent: () => void
}

export type TradingAreaChartProps = {
  chartId: string
  data: TradingAreaPoint[]
  height?: number
  colors: TradingAreaChartColors
  locale?: string
  isDark?: boolean
  maskAmounts?: boolean
  valueFormatter?: (value: number) => string
  emptyLabel?: string
  defaultWindowSec?: number
  maxHistorySec?: number
  rightBoundary?: 'now' | 'lastData'
  className?: string
  mountClassName?: string
  viewportMode?: 'time-window' | 'logical-range'
  /** Dev-only viewport lifecycle log */
  debug?: boolean
}

function chartDebug(
  chartId: string,
  action: string,
  state: Record<string, string | number | boolean | null | undefined>,
): void {
  if (!DEV) return
  console.debug(`[trading-chart][${action}]`, { chartId, ...state })
}

function shortcutEndSec(rightBoundary: 'now' | 'lastData', lastDataSec: number): number {
  const nowSec = nowUnixSec()
  if (rightBoundary === 'lastData') {
    return Math.min(nowSec, Math.max(0, lastDataSec))
  }
  return nowSec
}

function clamp(n: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, n))
}

function computeLogicalBarSpacing(width: number, pointCount: number): number | undefined {
  if (!Number.isFinite(width) || width <= 0 || pointCount <= 1) return undefined
  const visibleBars = Math.max(1, pointCount - 1)
  return clamp((width - 24) / visibleBars, LOGICAL_BAR_SPACING_MIN, LOGICAL_BAR_SPACING_MAX)
}

const TradingAreaChartInner = forwardRef<TradingAreaChartHandle, TradingAreaChartProps>(
  function TradingAreaChart(
    {
      chartId,
      data,
      height = 200,
      colors,
      locale = 'en-US',
      isDark = false,
      maskAmounts = false,
      valueFormatter,
      emptyLabel = '',
      defaultWindowSec = DEFAULT_WINDOW_SEC,
      maxHistorySec = DEFAULT_MAX_HISTORY_SEC,
      rightBoundary = 'now',
      className = 'my-portfolio-value-chart',
      mountClassName = 'my-portfolio-value-chart-mount',
      viewportMode = 'time-window',
      debug = false,
    },
    ref,
  ) {
    const mountRef = useRef<HTMLDivElement | null>(null)
    const chartRef = useRef<IChartApi | null>(null)
    const seriesRef = useRef<ISeriesApi<'Area'> | null>(null)
    const roRef = useRef<ResizeObserver | null>(null)
    const interactionOffRef = useRef<(() => void) | null>(null)

    const heightRef = useRef(height)
    const localeRef = useRef(locale)
    const spanSecRef = useRef(defaultWindowSec)
    const lastBarSecRef = useRef(0)
    const initialViewportAppliedRef = useRef(false)
    const lineDataRef = useRef<LineData<Time>[]>([])
    const stablePointsRef = useRef<TradingAreaPoint[]>([])

    const stablePoints = useMemo(() => {
      if (tradingPointsShallowEqual(stablePointsRef.current, data)) return stablePointsRef.current
      stablePointsRef.current = data
      return data
    }, [data])

    const lineData = useMemo(() => toLineDataSeries(stablePoints), [stablePoints])

    const log = useCallback(
      (action: string, state: Record<string, string | number | boolean | null | undefined>) => {
        if (debug) chartDebug(chartId, action, state)
      },
      [chartId, debug],
    )

    const destroyChart = useCallback(() => {
      interactionOffRef.current?.()
      interactionOffRef.current = null
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
      initialViewportAppliedRef.current = false
      lineDataRef.current = []
      log('destroy_chart', {})
    }, [log])

    const applyPresentation = useCallback(() => {
      const ch = chartRef.current
      if (!ch) return
      const span = spanSecRef.current
      const loc = localeRef.current
      const tickFmt: TickMarkFormatter = (time, tickMarkType, tickLocale) =>
        formatTradingAxisTick(time, tickMarkType, tickLocale ?? loc, span)
      try {
        const options = buildTradingAreaChartOptions({
          width: Math.max(1, ch.chartElement().clientWidth),
          height: Math.max(1, ch.chartElement().clientHeight),
          isDark,
          locale: loc,
          maskAmounts,
          visibleSpanSec: span,
          tickMarkFormatter: tickFmt,
          timeFormatter: (t: Time) => formatTradingCrosshairTime(span, loc, t),
          valueFormatter,
        })
        if (viewportMode === 'logical-range') {
          const logicalBarSpacing = computeLogicalBarSpacing(ch.chartElement().clientWidth, lineDataRef.current.length)
          options.timeScale = {
            ...options.timeScale,
            rightOffset: 0,
            lockVisibleTimeRangeOnResize: false,
            ...(logicalBarSpacing != null ? { barSpacing: logicalBarSpacing } : {}),
          }
        }
        ch.applyOptions(
          options,
        )
      } catch {
        /* */
      }
    }, [isDark, maskAmounts, valueFormatter, viewportMode])

    const applyLogicalViewport = useCallback(() => {
      const chart = chartRef.current
      const points = lineDataRef.current
      if (!chart || points.length === 0) return
      const ts = chart.timeScale()
      const firstSec = timeToUnixSec(points[0].time)
      const lastSec = timeToUnixSec(points[points.length - 1].time)
      const padSec = firstSec === lastSec ? 12 * 3600 : 0
      const fromSec = Math.max(0, firstSec - padSec)
      const toSec = Math.max(fromSec + 60, lastSec + padSec)
      const logicalBarSpacing = computeLogicalBarSpacing(chart.chartElement().clientWidth, points.length)
      try {
        chart.applyOptions({
          timeScale: {
            rightOffset: 0,
            lockVisibleTimeRangeOnResize: false,
            ...(logicalBarSpacing != null ? { barSpacing: logicalBarSpacing } : {}),
          },
        })
      } catch {
        /* */
      }
      if (points.length > 1) {
        try {
          ts.setVisibleLogicalRange({
            from: -0.5,
            to: Math.max(0.5, points.length - 0.5),
          })
          spanSecRef.current = Math.max(60, lastSec - firstSec)
          return
        } catch {
          /* */
        }
      }
      try {
        ts.setVisibleRange({
          from: fromSec as UTCTimestamp,
          to: toSec as UTCTimestamp,
        })
        spanSecRef.current = Math.max(60, toSec - fromSec)
      } catch {
        try {
          ts.fitContent()
        } catch {
          /* */
        }
        spanSecRef.current = Math.max(60, lastSec - firstSec)
      }
    }, [])

    useLayoutEffect(() => {
      heightRef.current = height
    }, [height])

    useLayoutEffect(() => {
      localeRef.current = locale
    }, [locale])

    useLayoutEffect(() => {
      return () => {
        destroyChart()
      }
    }, [destroyChart])

    useLayoutEffect(() => {
      if (lineData.length === 0) {
        destroyChart()
      }
    }, [lineData.length, destroyChart])

    /** Chart yaşam döngüsü: create → series → interaction → setData → tek sefer initial viewport. */
    useLayoutEffect(() => {
      const mount = mountRef.current
      if (!mount || lineData.length === 0) return
      if (chartRef.current) return

      lineDataRef.current = lineData

      const lastBar = timeToUnixSec(lineData[lineData.length - 1].time)
      lastBarSecRef.current = lastBar

      const w0 = Math.max(mount.clientWidth, 120)
      const h0 = Math.max(mount.clientHeight || 0, heightRef.current)
      const span0 = defaultWindowSec
      spanSecRef.current = span0
      const loc0 = localeRef.current

      const tickFmt0: TickMarkFormatter = (time, tickMarkType, tickLocale) =>
        formatTradingAxisTick(time, tickMarkType, tickLocale ?? loc0, span0)

      const chartOptions = buildTradingAreaChartOptions({
        width: w0,
        height: h0,
        isDark,
        locale: loc0,
        maskAmounts,
        visibleSpanSec: span0,
        tickMarkFormatter: tickFmt0,
        timeFormatter: (t: Time) => formatTradingCrosshairTime(span0, loc0, t),
        valueFormatter,
      })
      if (viewportMode === 'logical-range') {
        const logicalBarSpacing = computeLogicalBarSpacing(w0, lineData.length)
        chartOptions.timeScale = {
          ...chartOptions.timeScale,
          rightOffset: 0,
          lockVisibleTimeRangeOnResize: false,
          ...(logicalBarSpacing != null ? { barSpacing: logicalBarSpacing } : {}),
        }
      }

      const chart = createChart(mount, {
        ...chartOptions,
      })

      const series = chart.addSeries(AreaSeries, {
        lineColor: colors.lineColor,
        topColor: colors.topColor,
        bottomColor: colors.bottomColor,
        lineWidth: 2,
        priceLineVisible: true,
        lastValueVisible: false,
        crosshairMarkerVisible: true,
        crosshairMarkerRadius: 4,
      })

      chartRef.current = chart
      seriesRef.current = series

      series.setData(lineData)

      interactionOffRef.current?.()
      interactionOffRef.current =
        viewportMode === 'logical-range'
            ? null
            : attachTradingChartInteractions({
                chart,
                mount,
                spanSecRef,
                getLastDataUnixSec: () => lastBarSecRef.current,
                maxHistorySec,
                rightBoundary,
                onPresentationTick: applyPresentation,
                chartId,
                interactionDebug: debug,
              })

      if (!initialViewportAppliedRef.current) {
        if (viewportMode === 'logical-range') {
          applyLogicalViewport()
        } else {
          setVisibleWindowEndingAt(chart, shortcutEndSec(rightBoundary, lastBar), defaultWindowSec)
        }
        spanSecRef.current = defaultWindowSec
        initialViewportAppliedRef.current = true
        const tr0 = chart.timeScale().getVisibleRange()
        let vf0: number | null = null
        let vt0: number | null = null
        if (tr0) {
          vf0 = timeToUnixSec(tr0.from)
          vt0 = timeToUnixSec(tr0.to)
        }
        const vspan0 = vf0 != null && vt0 != null ? vt0 - vf0 : null
        log('apply_initial_viewport', {
          spanSec: defaultWindowSec,
          lastBarSec: lastBar,
          dataLength: lineData.length,
          firstDataTime: timeToUnixSec(lineData[0].time),
          lastDataTime: lastBar,
          visibleFrom: vf0,
          visibleTo: vt0,
          visibleSpanSec: vspan0,
          axisBand: vspan0 != null ? axisBandForVisibleSpan(vspan0) : null,
          axisBandBySpanRef: axisBandForVisibleSpan(defaultWindowSec),
        })
      }

      applyPresentation()

      const trC = chart.timeScale().getVisibleRange()
      let vfc: number | null = null
      let vtc: number | null = null
      if (trC) {
        vfc = timeToUnixSec(trC.from)
        vtc = timeToUnixSec(trC.to)
      }
      const vspanC = vfc != null && vtc != null ? vtc - vfc : null
      log('create_chart', {
        dataLength: lineData.length,
        firstDataTime: timeToUnixSec(lineData[0].time),
        lastDataTime: lastBar,
        lastBarSec: lastBar,
        visibleFrom: vfc,
        visibleTo: vtc,
        visibleSpanSec: vspanC,
        axisBand: vspanC != null ? axisBandForVisibleSpan(vspanC) : null,
      })

      const syncChartSize = () => {
        if (!chartRef.current || !mount.isConnected) return
        const w = Math.max(mount.clientWidth, 120)
        const h = Math.max(mount.clientHeight || 0, heightRef.current, 1)
        try {
          chartRef.current.resize(w, h)
          if (viewportMode === 'logical-range') {
            requestAnimationFrame(() => applyLogicalViewport())
          }
          log('resize', { w, h })
        } catch {
          /* */
        }
      }
      const ro = new ResizeObserver(() => syncChartSize())
      ro.observe(mount)
      roRef.current = ro
      requestAnimationFrame(syncChartSize)
      // lifecycle: yalnızca chart instance / ilk veri boyutu
    }, [
      lineData.length,
      defaultWindowSec,
      maxHistorySec,
      rightBoundary,
      viewportMode,
      isDark,
      maskAmounts,
      colors.lineColor,
      colors.topColor,
      colors.bottomColor,
      applyPresentation,
      log,
      valueFormatter,
    ])

    /** Yalnızca setData; viewport’a dokunulmaz. */
    useLayoutEffect(() => {
      const series = seriesRef.current
      if (!series || lineData.length === 0) return
      if (lineDataShallowEqual(lineDataRef.current, lineData)) return

      series.setData(lineData)
      lineDataRef.current = lineData
      lastBarSecRef.current = timeToUnixSec(lineData[lineData.length - 1].time)

      const tr = chartRef.current?.timeScale().getVisibleRange()
      let fromT: number | null = null
      let toT: number | null = null
      if (tr) {
        fromT = timeToUnixSec(tr.from)
        toT = timeToUnixSec(tr.to)
      }
      const vspan = fromT != null && toT != null ? toT - fromT : null
      log('set_data', {
        dataLength: lineData.length,
        firstDataTime: timeToUnixSec(lineData[0].time),
        lastDataTime: lastBarSecRef.current,
        lastBarSec: lastBarSecRef.current,
        visibleFrom: fromT,
        visibleTo: toT,
        visibleSpanSec: vspan,
        axisBand: vspan != null ? axisBandForVisibleSpan(vspan) : null,
      })

      applyPresentation()
    }, [lineData, applyPresentation, log])

    useLayoutEffect(() => {
      const series = seriesRef.current
      if (!series) return
      series.applyOptions({
        lineColor: colors.lineColor,
        topColor: colors.topColor,
        bottomColor: colors.bottomColor,
      })
    }, [colors.lineColor, colors.topColor, colors.bottomColor])

    useLayoutEffect(() => {
      if (!chartRef.current) return
      applyPresentation()
    }, [locale, applyPresentation])

    useLayoutEffect(() => {
      const chart = chartRef.current
      const mount = mountRef.current
      if (!chart || !mount || lineData.length === 0) return
      const w = Math.max(mount.clientWidth, 120)
      const h = Math.max(mount.clientHeight || 0, heightRef.current, 1)
      try {
        chart.resize(w, h)
        log('resize_height', { w, h })
      } catch {
        /* */
      }
    }, [height, lineData.length, log])

    const runShortcutViewport = useCallback(
      (spanSec: number) => {
        const chart = chartRef.current
        if (!chart || lineData.length === 0) return
        const span = Math.max(60, Math.floor(spanSec))
        if (viewportMode === 'logical-range') {
          applyLogicalViewport()
        } else {
          setVisibleWindowEndingAt(chart, shortcutEndSec(rightBoundary, lastBarSecRef.current), span)
        }
        spanSecRef.current = span
        applyPresentation()
        const tr = chart.timeScale().getVisibleRange()
        let fromT: number | null = null
        let toT: number | null = null
        if (tr) {
          fromT = timeToUnixSec(tr.from)
          toT = timeToUnixSec(tr.to)
        }
        const vspan = fromT != null && toT != null ? toT - fromT : null
        const spanMismatch =
          vspan != null && Math.abs(vspan - span) > 120 ? Math.round(vspan - span) : 0
        log('apply_shortcut_viewport', {
          shortcutSpanSec: span,
          visibleFrom: fromT,
          visibleTo: toT,
          visibleSpanSec: vspan,
          axisBand: vspan != null ? axisBandForVisibleSpan(vspan) : null,
          axisBandExpected: axisBandForVisibleSpan(span),
          spanMismatchSec: spanMismatch,
        })
      },
      [applyLogicalViewport, applyPresentation, lineData.length, log, rightBoundary, viewportMode],
    )

    useImperativeHandle(
      ref,
      () => ({
        setWindow: (spanSec: number) => {
          runShortcutViewport(spanSec)
        },
        goToLatest: () => {
          const chart = chartRef.current
          if (!chart || lineData.length === 0) return
          const span = Math.max(60, spanSecRef.current)
          if (viewportMode === 'logical-range') {
            applyLogicalViewport()
          } else {
            setVisibleWindowEndingAt(chart, shortcutEndSec(rightBoundary, lastBarSecRef.current), span)
          }
          applyPresentation()
          log('go_to_latest', { spanSec: span })
        },
        fitContent: () => {
          const chart = chartRef.current
          if (!chart) return
          try {
            chart.timeScale().fitContent()
          } catch {
            /* */
          }
          requestAnimationFrame(() => {
            const tr = chart.timeScale().getVisibleRange()
            if (tr) {
              spanSecRef.current = Math.max(60, timeToUnixSec(tr.to) - timeToUnixSec(tr.from))
            }
            applyPresentation()
            log('fit_content', {})
          })
        },
      }),
      [applyLogicalViewport, applyPresentation, lineData.length, log, rightBoundary, runShortcutViewport, viewportMode],
    )

    useLayoutEffect(() => {
      if (!chartRef.current || lineData.length === 0) return
      if (viewportMode === 'logical-range') {
        applyLogicalViewport()
        applyPresentation()
        return
      }
      runShortcutViewport(defaultWindowSec)
    }, [applyLogicalViewport, applyPresentation, defaultWindowSec, lineData.length, runShortcutViewport, viewportMode])

    useLayoutEffect(() => {
      if (viewportMode !== 'logical-range' || !chartRef.current || lineData.length === 0) return
      applyLogicalViewport()
      applyPresentation()
    }, [applyLogicalViewport, applyPresentation, lineData, viewportMode])

    const stableEmpty = useMemo(() => emptyLabel, [emptyLabel])

    return (
      <div className={className}>
        {lineData.length === 0 ? (
          <div className="my-portfolio-value-chart-empty" style={{ minHeight: height }}>
            {stableEmpty}
          </div>
        ) : null}
        <div
          ref={mountRef}
          className={mountClassName}
          style={{
            width: '100%',
            height: lineData.length === 0 ? 0 : height,
            overflow: 'hidden',
            pointerEvents: lineData.length === 0 ? 'none' : 'auto',
          }}
          aria-hidden={lineData.length === 0}
        />
      </div>
    )
  },
)

TradingAreaChartInner.displayName = 'TradingAreaChart'

export const TradingAreaChart = TradingAreaChartInner
