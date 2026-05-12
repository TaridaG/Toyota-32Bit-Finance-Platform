import type { IChartApi, Time, UTCTimestamp } from 'lightweight-charts'
import { timeToUnixSec } from './timeUtils'

function clamp(n: number, lo: number, hi: number): number {
  return Math.min(hi, Math.max(lo, n))
}

/** Mouse line vs trackpad — deltaMode + tavan ile tutarlı zoom hızı. */
export function normalizeWheelDeltaY(e: WheelEvent): number {
  let y = e.deltaY
  if (e.deltaMode === 1) y *= 16
  else if (e.deltaMode === 2) y *= 120
  const cap = 160
  return Math.sign(y) * Math.min(Math.abs(y), cap)
}

export type TimeZoomBounds = () => { minUnix: number; maxUnix: number }

export type AnchoredTimeZoomOptions = {
  minSpanSec?: number
  maxSpanSec?: number
  zoomIntensity?: number
}

const DEFAULT_MIN_SPAN = 60
const DEFAULT_ZOOM_INTENSITY = 0.00165

/**
 * Tekerlek = zaman zoom. **Aşağı kaydır** (çoğu cihazda) → uzaklaş; **yukarı** → yakınlaş.
 * `capture: true` ile LWC canvas’tan önce alınır; hafif çapraz trackpad hareketi zoom’u öldürmez.
 */
export function installAnchoredTimeWheelZoom(
  chart: IChartApi,
  container: HTMLElement,
  getBounds: TimeZoomBounds,
  opts?: AnchoredTimeZoomOptions,
): () => void {
  const minSpanUser = Math.max(45, opts?.minSpanSec ?? DEFAULT_MIN_SPAN)
  const zoomK = opts?.zoomIntensity ?? DEFAULT_ZOOM_INTENSITY

  const onWheel = (e: WheelEvent) => {
    if (e.shiftKey) return

    const absX = Math.abs(e.deltaX)
    const absY = Math.abs(e.deltaY)
    if (absY < 1 && absX > 8) return

    const dy = normalizeWheelDeltaY(e)
    if (Math.abs(dy) < 0.25) return

    const ts = chart.timeScale()
    const tr = ts.getVisibleRange() as { from: Time; to: Time } | null
    if (!tr) return

    const { minUnix, maxUnix } = getBounds()
    if (!Number.isFinite(minUnix) || !Number.isFinite(maxUnix) || maxUnix <= minUnix) return

    e.preventDefault()
    e.stopPropagation()

    const rect = container.getBoundingClientRect()
    const w = Math.max(1, rect.width)
    const x = clamp(e.clientX - rect.left, 0, w)
    const fromSec = timeToUnixSec(tr.from)
    const toSec = timeToUnixSec(tr.to)
    let span = Math.max(minSpanUser, toSec - fromSec)
    let maxSpanAllowed = Math.max(minSpanUser, maxUnix - minUnix)
    if (opts?.maxSpanSec != null) {
      maxSpanAllowed = Math.min(maxSpanAllowed, opts.maxSpanSec)
    }
    span = Math.min(span, maxSpanAllowed)

    let anchorSec: number
    const tAtX = ts.coordinateToTime(x)
    if (tAtX != null) {
      anchorSec = timeToUnixSec(tAtX)
    } else {
      const ratio = x / w
      anchorSec = fromSec + span * ratio
    }
    anchorSec = clamp(anchorSec, minUnix, maxUnix)

    /** dy > 0 → span büyür (uzaklaş). */
    const scale = Math.exp(dy * zoomK)
    const newSpan = clamp(span * scale, minSpanUser, maxSpanAllowed)

    const ratioInWindow = span > 0 ? (anchorSec - fromSec) / span : 0.5
    let nf = anchorSec - newSpan * ratioInWindow
    let nt = nf + newSpan

    if (nt > maxUnix) {
      nt = maxUnix
      nf = nt - newSpan
    }
    if (nf < minUnix) {
      nf = minUnix
      nt = Math.min(maxUnix, nf + newSpan)
    }

    try {
      ts.setVisibleRange({ from: nf as UTCTimestamp, to: nt as UTCTimestamp })
    } catch {
      /* */
    }
  }

  container.addEventListener('wheel', onWheel, { passive: false, capture: true })
  return () => container.removeEventListener('wheel', onWheel, true)
}
