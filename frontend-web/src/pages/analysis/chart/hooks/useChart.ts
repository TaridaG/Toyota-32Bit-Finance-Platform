import { useEffect, useLayoutEffect, useRef, useState } from 'react'
import { createChart, type IChartApi } from 'lightweight-charts'
import { useTheme } from '../../../../shared/theme/ThemeProvider'
import { getAnalysisChartLayoutOptions } from '../analysisChartLayoutOptions'

/**
 * Chart mounts on `chartMountRef` only (empty div) so overlays never block the canvas.
 * `containerRef` is the outer box used for ResizeObserver + layout height.
 */
export function useChart() {
  const { theme } = useTheme()
  const isDark = theme === 'dark'
  const containerRef = useRef<HTMLDivElement | null>(null)
  const chartMountRef = useRef<HTMLDivElement | null>(null)
  const chartRef = useRef<IChartApi | null>(null)
  const instanceRef = useRef<IChartApi | null>(null)
  const [chart, setChart] = useState<IChartApi | null>(null)

  useEffect(() => {
    const instance = instanceRef.current
    if (!instance) return
    try {
      instance.applyOptions(getAnalysisChartLayoutOptions(isDark))
    } catch {
      /* chart disposed */
    }
  }, [isDark])

  useLayoutEffect(() => {
    const outer = containerRef.current
    const mount = chartMountRef.current
    if (!outer || !mount) return

    const width = outer.clientWidth
    const height = Math.max(outer.clientHeight || 460, 320)
    const instance = createChart(mount, {
      width,
      height,
      ...getAnalysisChartLayoutOptions(isDark),
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
