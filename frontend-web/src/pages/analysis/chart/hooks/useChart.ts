import { useEffect, useLayoutEffect, useRef, useState } from 'react'
import { ColorType, createChart, CrosshairMode, type IChartApi } from 'lightweight-charts'

const defaultLayoutOptions = {
  layout: {
    background: { type: ColorType.Solid, color: '#0f172a' },
    textColor: '#cbd5e1',
  },
  rightPriceScale: { borderColor: '#334155' },
  timeScale: { borderColor: '#334155', timeVisible: true, secondsVisible: false },
  grid: {
    vertLines: { color: 'rgba(148, 163, 184, 0.12)' },
    horzLines: { color: 'rgba(148, 163, 184, 0.12)' },
  },
  crosshair: {
    mode: CrosshairMode.MagnetOHLC,
    vertLine: { color: '#64748b', labelBackgroundColor: '#334155' },
    horzLine: { color: '#64748b', labelBackgroundColor: '#334155' },
  },
  /** Ensure wheel zoom / drag pan work (defaults are true; set explicitly for clarity). */
  handleScroll: true,
  handleScale: true,
} as const

/**
 * Chart mounts on `chartMountRef` only (empty div) so overlays never block the canvas.
 * `containerRef` is the outer box used for ResizeObserver + layout height.
 */
export function useChart() {
  const containerRef = useRef<HTMLDivElement | null>(null)
  const chartMountRef = useRef<HTMLDivElement | null>(null)
  const chartRef = useRef<IChartApi | null>(null)
  const instanceRef = useRef<IChartApi | null>(null)
  const [chart, setChart] = useState<IChartApi | null>(null)

  useLayoutEffect(() => {
    const outer = containerRef.current
    const mount = chartMountRef.current
    if (!outer || !mount) return

    const width = outer.clientWidth
    const height = Math.max(outer.clientHeight || 460, 320)
    const instance = createChart(mount, {
      width,
      height,
      ...defaultLayoutOptions,
    })

    instanceRef.current = instance
    chartRef.current = instance
    setChart(instance)
    let disposed = false

    const ro = new ResizeObserver(() => {
      if (disposed || !outer.isConnected) return
      const w = outer.clientWidth
      const h = Math.max(outer.clientHeight || 460, 320)
      try {
        instance.applyOptions({ width: w, height: h })
      } catch {
        // Ignore lightweight-charts disposal race during unmount.
      }
    })
    ro.observe(outer)

    return () => {
      disposed = true
      ro.disconnect()
      chartRef.current = null
      setChart(null)
    }
  }, [])

  // Remove the chart in a passive-effect cleanup so layout cleanups (series) and other
  // hooks' subscribeClick / subscribeCrosshairMove unsubscribes still see a live instance.
  useEffect(() => {
    return () => {
      const inst = instanceRef.current
      instanceRef.current = null
      if (!inst) return
      try {
        inst.remove()
      } catch {
        // Race with internal rAF/resize after dispose.
      }
    }
  }, [])

  return { containerRef, chartMountRef, chart, chartRef }
}
