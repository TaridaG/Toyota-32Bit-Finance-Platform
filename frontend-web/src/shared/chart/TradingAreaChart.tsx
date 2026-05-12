import {
  forwardRef,
  useCallback,
  useImperativeHandle,
  useLayoutEffect,
  useMemo,
  useRef,
} from 'react'
import { AreaSeries, createChart, type IChartApi, type ISeriesApi, type LineData, type TickMarkFormatter, type Time } from 'lightweight-charts'
import {
  lineDataShallowEqual,
  toLineDataSeries,
  tradingPointsShallowEqual,
  type TradingAreaPoint,
} from './tradingChartData'
import { attachTradingChartInteractions } from './tradingChartInteractions'
import { buildTradingAreaChartOptions } from './tradingChartOptions'
import { axisBandForVisibleSpan, formatTradingAxisTick, formatTradingCrosshairTime } from './tradingChartFormatters'
import { setVisibleWindowAlignedToNow } from './tradingChartViewport'
import { timeToUnixSec } from './timeUtils'

const DEV = import.meta.env.DEV
const DEFAULT_WINDOW_SEC = 7 * 86_400
const DEFAULT_MAX_HISTORY_SEC = 5 * 365 * 86_400

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
  /** Dev-only viewport lifecycle log */
  debug?: boolean
}

function chartDebug(
  chartId: string,
  action: string,
  state: Record<string, string | number | boolean | null | undefined>,
): void {
  if (!DEV) return
  // eslint-disable-next-line no-console
  console.debug(`[trading-chart][${action}]`, { chartId, ...state })
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
        ch.applyOptions(
          buildTradingAreaChartOptions({
            width: Math.max(1, ch.chartElement().clientWidth),
            height: Math.max(1, ch.chartElement().clientHeight),
            isDark,
            locale: loc,
            maskAmounts,
            visibleSpanSec: span,
            tickMarkFormatter: tickFmt,
            timeFormatter: (t: Time) => formatTradingCrosshairTime(span, loc, t),
            valueFormatter,
          }),
        )
      } catch {
        /* */
      }
    }, [isDark, maskAmounts, valueFormatter])

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

      const chart = createChart(mount, {
        ...buildTradingAreaChartOptions({
          width: w0,
          height: h0,
          isDark,
          locale: loc0,
          maskAmounts,
          visibleSpanSec: span0,
          tickMarkFormatter: tickFmt0,
          timeFormatter: (t: Time) => formatTradingCrosshairTime(span0, loc0, t),
          valueFormatter,
        }),
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
      interactionOffRef.current = attachTradingChartInteractions({
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
        setVisibleWindowAlignedToNow(chart, defaultWindowSec)
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
          log('resize', { w, h })
        } catch {
          /* */
        }
      }
      const ro = new ResizeObserver(() => syncChartSize())
      ro.observe(mount)
      roRef.current = ro
      requestAnimationFrame(syncChartSize)
      // eslint-disable-next-line react-hooks/exhaustive-deps -- lifecycle: yalnızca chart instance / ilk veri boyutu
    }, [
      lineData.length,
      defaultWindowSec,
      maxHistorySec,
      rightBoundary,
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
        setVisibleWindowAlignedToNow(chart, span)
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
      [applyPresentation, lineData.length, log],
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
          setVisibleWindowAlignedToNow(chart, span)
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
      [applyPresentation, lineData.length, log, runShortcutViewport],
    )

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
