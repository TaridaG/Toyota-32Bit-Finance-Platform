import type { IChartApi, ISeriesApi, Time, UTCTimestamp } from 'lightweight-charts'
import type { CandlePoint, ChartAnchor } from '../../types'
import { nearestCandleByTime } from '../utils/nearestCandle'
import { getPricePaneBounds, type PricePaneBounds } from './chartPaneLayout'

export type PixelPoint = { x: number; y: number }

export function timeToCoordinate(chart: IChartApi, time: UTCTimestamp): number | null {
  const x = chart.timeScale().timeToCoordinate(time as Time)
  return x ?? null
}

export function priceToCoordinate(
  series: ISeriesApi<'Candlestick'> | ISeriesApi<'Line'>,
  price: number,
): number | null {
  const y = series.priceToCoordinate(price)
  return y ?? null
}

export function coordinateToTime(chart: IChartApi, x: number): UTCTimestamp | null {
  const raw = chart.timeScale().coordinateToTime(x)
  if (raw == null) return null
  return Number(raw) as UTCTimestamp
}

/** Y is relative to the price pane (not full chart widget). */
export function coordinateToPrice(
  series: ISeriesApi<'Candlestick'> | ISeriesApi<'Line'>,
  y: number,
): number | null {
  const price = series.coordinateToPrice(y)
  if (price == null || !Number.isFinite(price)) return null
  return price
}

function clientToPanePoint(
  mount: HTMLElement,
  pane: PricePaneBounds,
  clientX: number,
  clientY: number,
): { chartX: number; paneY: number } {
  const mountRect = mount.getBoundingClientRect()
  const chartX = clientX - mountRect.left
  const paneY = clientY - mountRect.top - pane.top
  return { chartX, paneY }
}

/** Snap time to nearest bar; keep exact click price so Y stays where the user clicked. */
export function clientToAnchor(
  chart: IChartApi,
  series: ISeriesApi<'Candlestick'> | ISeriesApi<'Line'>,
  mount: HTMLElement,
  clientX: number,
  clientY: number,
  candles: CandlePoint[],
): ChartAnchor | null {
  const pane = getPricePaneBounds(series, mount)
  if (!pane) return null

  const { chartX, paneY } = clientToPanePoint(mount, pane, clientX, clientY)
  if (paneY < 0 || paneY > pane.height) return null

  const time = coordinateToTime(chart, chartX)
  const price = coordinateToPrice(series, paneY)
  if (time == null || price == null) return null

  if (candles.length === 0) {
    return { time, price }
  }

  const candle = nearestCandleByTime(candles, Number(time))
  if (!candle) {
    return { time, price }
  }

  return {
    time: candle.time,
    price,
  }
}

/** Pixel position inside the price-pane overlay (pane-local coordinates). */
export function anchorToPixel(
  chart: IChartApi,
  series: ISeriesApi<'Candlestick'> | ISeriesApi<'Line'>,
  anchor: ChartAnchor,
): PixelPoint | null {
  const x = timeToCoordinate(chart, anchor.time)
  const y = priceToCoordinate(series, anchor.price)
  if (x == null || y == null) return null
  return { x, y }
}

export const projectPendingAnchor = anchorToPixel
