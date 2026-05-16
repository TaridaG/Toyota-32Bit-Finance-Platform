import { useEffect, useRef } from 'react'
import type { IChartApi, ISeriesApi } from 'lightweight-charts'
import { getPricePaneBounds } from './chartPaneLayout'

function scaleFingerprint(
  chart: IChartApi,
  series: ISeriesApi<'Candlestick'> | ISeriesApi<'Line'>,
  mount: HTMLElement,
): string {
  const pane = getPricePaneBounds(series, mount)
  const range = series.priceScale().getVisibleRange()
  const logical = chart.timeScale().getVisibleLogicalRange()
  const yFrom = range ? series.priceToCoordinate(range.from) : null
  const yTo = range ? series.priceToCoordinate(range.to) : null

  return [
    range?.from,
    range?.to,
    yFrom,
    yTo,
    logical?.from,
    logical?.to,
    pane?.left,
    pane?.top,
    pane?.width,
    pane?.height,
    series.priceScale().width(),
  ].join('|')
}

type UseDrawingLayoutSyncOptions = {
  chart: IChartApi | null
  priceSeries: ISeriesApi<'Candlestick'> | ISeriesApi<'Line'> | null
  mountRef: React.RefObject<HTMLDivElement | null>
  /** Re-project overlays when true (drawings, pending anchor, or active draw tool). */
  active: boolean
  onLayoutChange: () => void
}

/**
 * Lightweight Charts has no price-scale range subscription; poll while overlays are shown
 * so Y-axis drag/zoom keeps drawings aligned with price/time coordinates.
 */
export function useDrawingLayoutSync({
  chart,
  priceSeries,
  mountRef,
  active,
  onLayoutChange,
}: UseDrawingLayoutSyncOptions) {
  const fingerprintRef = useRef<string>('')

  useEffect(() => {
    const mount = mountRef.current
    if (!active || !chart || !priceSeries || !mount) return

    const sync = () => {
      const next = scaleFingerprint(chart, priceSeries, mount)
      if (next !== fingerprintRef.current) {
        fingerprintRef.current = next
        onLayoutChange()
      }
    }

    sync()
    let raf = 0
    const tick = () => {
      sync()
      raf = requestAnimationFrame(tick)
    }
    raf = requestAnimationFrame(tick)

    const onPointer = () => sync()
    mount.addEventListener('pointermove', onPointer, { passive: true })
    mount.addEventListener('pointerup', onPointer, { passive: true })

    const onCrosshair = () => sync()
    chart.subscribeCrosshairMove(onCrosshair)

    return () => {
      cancelAnimationFrame(raf)
      mount.removeEventListener('pointermove', onPointer)
      mount.removeEventListener('pointerup', onPointer)
      try {
        chart.unsubscribeCrosshairMove(onCrosshair)
      } catch {
        /* disposed */
      }
    }
  }, [active, chart, mountRef, onLayoutChange, priceSeries])
}
