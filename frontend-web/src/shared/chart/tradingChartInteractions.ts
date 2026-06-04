import type { IChartApi, IRange, Time, UTCTimestamp } from 'lightweight-charts'
import { timeToUnixSec } from './timeUtils'
import { normalizeWheelDeltaY } from './zoomController'
import { computeTimeScaleBounds, type TradingRightBoundary } from './tradingChartViewport'

const DEV = import.meta.env.DEV

/** Günlük omurgalı seri ile uyumlu minimum görünür süre. */
const MIN_SPAN_SEC = 86_400

const ZOOM_IN_BASE = 0.9
const ZOOM_OUT_BASE = 1.13
const WHEEL_STEP_SCALE = 55
const WHEEL_STEP_MIN = 0.65
const WHEEL_STEP_MAX = 2.75
const MIN_ZOOM_OUT_MULT = 1.22

function clamp(n: number, lo: number, hi: number): number {
  return Math.min(hi, Math.max(lo, n))
}

/**
 * [from0,to0] penceresini [minUnix,maxUnix] içine taşır; mümkünse span = to0-from0 korunur (pan = salt öteleme).
 */
function clampWindowPreservingSpan(
  from0: number,
  to0: number,
  minUnix: number,
  maxUnix: number,
): { from: number; to: number; clamped: boolean; reasons: string[] } {
  const reasons: string[] = []
  const W = Math.max(1, to0 - from0)
  const corridor = maxUnix - minUnix
  if (W >= corridor - 1) {
    const clamped = Math.abs(from0 - minUnix) > 0.5 || Math.abs(to0 - maxUnix) > 0.5
    return { from: minUnix, to: maxUnix, clamped, reasons: ['corridor_max'] }
  }

  const from = clamp(from0, minUnix, maxUnix - W)
  const to = from + W

  if (to0 > maxUnix + 1) reasons.push('right_overflow')
  if (from0 < minUnix - 1) reasons.push('left_overflow')

  const clamped = Math.abs(from - from0) > 0.5 || Math.abs(to - to0) > 0.5
  if (clamped && reasons.length === 0) reasons.push('translate')

  return { from, to, clamped, reasons }
}

function ixLog(
  enabled: boolean,
  chartId: string | undefined,
  action: string,
  state: Record<string, string | number | boolean | null | undefined>,
): void {
  if (!DEV || !enabled || !chartId) return
   
  console.debug(`[trading-interaction][${action}]`, { chartId, ...state })
}

export type AttachTradingChartInteractionsArgs = {
  chart: IChartApi
  mount: HTMLElement
  spanSecRef: { current: number }
  getLastDataUnixSec: () => number
  maxHistorySec: number
  rightBoundary: TradingRightBoundary
  onPresentationTick: () => void
  chartId?: string
  interactionDebug?: boolean
}

export function attachTradingChartInteractions(args: AttachTradingChartInteractionsArgs): () => void {
  const dbg = args.interactionDebug === true
  const cid = args.chartId

  let presentationRaf = 0

  const schedulePresentation = () => {
    if (presentationRaf !== 0) return
    presentationRaf = requestAnimationFrame(() => {
      presentationRaf = 0
      args.onPresentationTick()
    })
  }

  const syncSpanRef = () => {
    const tr = args.chart.timeScale().getVisibleRange()
    if (tr) {
      args.spanSecRef.current = Math.max(1, timeToUnixSec(tr.to) - timeToUnixSec(tr.from))
    }
  }

  const onVisibleTimeRangeChange = (tr: IRange<Time> | null) => {
    if (!tr) {
      schedulePresentation()
      return
    }

    const nowSec = Math.floor(Date.now() / 1000)
    const lastData = args.getLastDataUnixSec()
    const { minUnix, maxUnix } = computeTimeScaleBounds(nowSec, lastData, args.maxHistorySec, args.rightBoundary)
    if (!Number.isFinite(minUnix) || !Number.isFinite(maxUnix) || maxUnix <= minUnix) {
      schedulePresentation()
      return
    }

    const fromS0 = timeToUnixSec(tr.from)
    const toS0 = timeToUnixSec(tr.to)
    const spanBefore = toS0 - fromS0

    const { from, to, clamped, reasons } = clampWindowPreservingSpan(fromS0, toS0, minUnix, maxUnix)
    const spanAfter = to - from

    if (dbg) {
      if (clamped) {
        ixLog(dbg, cid, 'visible_range_clamp', {
          clampReason: reasons.join(','),
          visibleFrom: fromS0,
          visibleTo: toS0,
          visibleSpan: spanBefore,
          newFrom: from,
          newTo: to,
          newSpan: spanAfter,
          spanDriftSec: spanAfter - spanBefore,
        })
      } else if (Math.abs(spanAfter - spanBefore) > Math.max(45, spanBefore * 0.02)) {
        ixLog(dbg, cid, 'pan_span_drift', {
          visibleFrom: fromS0,
          visibleTo: toS0,
          visibleSpan: spanBefore,
          newSpan: spanAfter,
          spanDriftSec: spanAfter - spanBefore,
        })
      }
    }

    if (clamped) {
      try {
        args.chart.timeScale().setVisibleRange({ from: from as UTCTimestamp, to: to as UTCTimestamp })
      } catch {
        /* */
      }
    }

    syncSpanRef()
    schedulePresentation()
  }

  args.chart.timeScale().subscribeVisibleTimeRangeChange(onVisibleTimeRangeChange)

  let accDy = 0
  let wheelRaf = 0
  let lastClientX = 0

  const flushWheel = () => {
    wheelRaf = 0
    const dy = accDy
    accDy = 0
    if (Math.abs(dy) < 0.25) {
      if (Math.abs(accDy) >= 0.25) {
        wheelRaf = requestAnimationFrame(flushWheel)
      }
      return
    }

    const zoomingOut = dy > 0
    const step = clamp(Math.abs(dy) / WHEEL_STEP_SCALE, WHEEL_STEP_MIN, WHEEL_STEP_MAX)
    const zoomInMult = clamp(ZOOM_IN_BASE - 0.02 * (step - WHEEL_STEP_MIN), 0.87, 0.93)
    const zoomOutMult = clamp(ZOOM_OUT_BASE + 0.05 * (step - WHEEL_STEP_MIN), 1.08, 1.24)
    const mult = zoomingOut ? zoomOutMult : zoomInMult

    const ts = args.chart.timeScale()
    const tr = ts.getVisibleRange() as { from: Time; to: Time } | null
    if (!tr) {
      if (Math.abs(accDy) >= 0.25) wheelRaf = requestAnimationFrame(flushWheel)
      return
    }

    const nowSec = Math.floor(Date.now() / 1000)
    const lastData = args.getLastDataUnixSec()
    const { minUnix, maxUnix } = computeTimeScaleBounds(nowSec, lastData, args.maxHistorySec, args.rightBoundary)
    if (!Number.isFinite(minUnix) || !Number.isFinite(maxUnix) || maxUnix <= minUnix) {
      if (Math.abs(accDy) >= 0.25) wheelRaf = requestAnimationFrame(flushWheel)
      return
    }

    const fromSec = timeToUnixSec(tr.from)
    const toSec = timeToUnixSec(tr.to)
    let span = Math.max(MIN_SPAN_SEC, toSec - fromSec)
    const maxSpanAllowed = Math.max(MIN_SPAN_SEC, maxUnix - minUnix)
    span = Math.min(span, maxSpanAllowed)

    let newSpan = clamp(span * mult, MIN_SPAN_SEC, maxSpanAllowed)
    if (zoomingOut && span <= MIN_SPAN_SEC * 1.002) {
      newSpan = Math.min(maxSpanAllowed, Math.max(newSpan, span * MIN_ZOOM_OUT_MULT, MIN_SPAN_SEC * 1.15))
    }

    const rect = args.mount.getBoundingClientRect()
    const w = Math.max(1, rect.width)
    const x = clamp(lastClientX - rect.left, 0, w)

    const tAtX = ts.coordinateToTime(x)
    const anchorSec = clamp(
      tAtX != null ? timeToUnixSec(tAtX) : fromSec + span * (x / w),
      minUnix,
      maxUnix,
    )

    const ratioInWindow = span > 0 ? (anchorSec - fromSec) / span : 0.5
    let nf = anchorSec - newSpan * ratioInWindow
    let nt = nf + newSpan

    const fitted = clampWindowPreservingSpan(nf, nt, minUnix, maxUnix)
    nf = fitted.from
    nt = fitted.to

    ixLog(dbg, cid, 'wheel_zoom', {
      dy,
      zoomFactor: mult,
      zoomingOut,
      visibleFrom: fromSec,
      visibleTo: toSec,
      visibleSpan: span,
      newLogicalFrom: nf,
      newLogicalTo: nt,
      newLogicalSpan: nt - nf,
      spanDeltaVsBefore: nt - nf - span,
      atMinZoom: span <= MIN_SPAN_SEC * 1.002,
      wheelClampReason: fitted.clamped ? fitted.reasons.join(',') : null,
    })

    try {
      ts.setVisibleRange({ from: nf as UTCTimestamp, to: nt as UTCTimestamp })
    } catch {
      /* */
    }

    if (Math.abs(accDy) >= 0.25) {
      wheelRaf = requestAnimationFrame(flushWheel)
    }
  }

  const onWheel = (e: WheelEvent) => {
    if (e.shiftKey) return
    const absX = Math.abs(e.deltaX)
    const absY = Math.abs(e.deltaY)
    if (absY < 1 && absX > 8) return

    const dy = normalizeWheelDeltaY(e)
    if (Math.abs(dy) < 0.25) return

    e.preventDefault()
    e.stopPropagation()
    lastClientX = e.clientX
    accDy += dy
    if (wheelRaf !== 0) return
    wheelRaf = requestAnimationFrame(flushWheel)
  }

  args.mount.addEventListener('wheel', onWheel, { passive: false, capture: true })

  const onDown = () => {
    args.mount.style.cursor = 'grabbing'
  }
  const onUp = () => {
    args.mount.style.cursor = 'grab'
  }
  args.mount.addEventListener('mousedown', onDown)
  window.addEventListener('mouseup', onUp)
  args.mount.style.cursor = 'grab'

  return () => {
    args.chart.timeScale().unsubscribeVisibleTimeRangeChange(onVisibleTimeRangeChange)
    args.mount.removeEventListener('wheel', onWheel, true)
    args.mount.removeEventListener('mousedown', onDown)
    window.removeEventListener('mouseup', onUp)
    if (presentationRaf !== 0) {
      cancelAnimationFrame(presentationRaf)
      presentationRaf = 0
    }
    if (wheelRaf !== 0) {
      cancelAnimationFrame(wheelRaf)
      wheelRaf = 0
    }
    args.mount.style.cursor = ''
  }
}
