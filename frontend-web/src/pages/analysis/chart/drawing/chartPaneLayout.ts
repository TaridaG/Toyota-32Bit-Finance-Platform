import type { ISeriesApi } from 'lightweight-charts'

export type PricePaneBounds = {
  left: number
  top: number
  width: number
  height: number
}

/**
 * Plot area of the main price pane (excludes the right price scale) relative to chart mount.
 * LWC time/x coordinates are relative to this plot strip, not the full pane including the scale.
 */
export function getPricePaneBounds(
  series: ISeriesApi<'Candlestick'> | ISeriesApi<'Line'>,
  mount: HTMLElement,
): PricePaneBounds | null {
  const paneEl = series.getPane().getHTMLElement()
  if (!paneEl) return null

  const mountRect = mount.getBoundingClientRect()
  const paneRect = paneEl.getBoundingClientRect()
  if (paneRect.width <= 0 || paneRect.height <= 0) return null

  const priceScaleWidth = Math.max(0, series.priceScale().width())
  const plotWidth = Math.max(0, paneRect.width - priceScaleWidth)

  return {
    left: paneRect.left - mountRect.left,
    top: paneRect.top - mountRect.top,
    width: plotWidth,
    height: paneRect.height,
  }
}
