import type { IChartApi, ISeriesApi, UTCTimestamp } from 'lightweight-charts'
import type { CandlePoint, ChartAnchor } from '../../types'
import { nearestCandleByTime } from '../utils/nearestCandle'
import { chartTimeToCoordinate, coordinateToChartTime } from './chartTimeExtrapolation'
import { getPricePaneBounds, type PricePaneBounds } from './chartPaneLayout'

export type PixelPoint = { x: number; y: number }

export function timeToCoordinate(
  chart: IChartApi,
  time: UTCTimestamp,
  candles: CandlePoint[] = [],
): number | null {
  return chartTimeToCoordinate(chart, time, candles)
}

export function priceToCoordinate(
  series: ISeriesApi<'Candlestick'> | ISeriesApi<'Line'>,
  price: number,
): number | null {
  const y = series.priceToCoordinate(price)
  return y ?? null
}

export function coordinateToTime(
  chart: IChartApi,
  x: number,
  candles: CandlePoint[] = [],
): UTCTimestamp | null {
  return coordinateToChartTime(chart, x, candles)
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

/**
 * Snap to nearest bar only when the click falls inside the loaded candle range.
 * Times after the last bar (or before the first) keep the exact chart time so
 * trendlines / rays can extend into the future like TradingView.
 */
export function resolveDrawingAnchorTime(time: UTCTimestamp, candles: CandlePoint[]): UTCTimestamp {
  if (candles.length === 0) return time

  const t = Number(time)
  const firstT = Number(candles[0].time)
  const lastT = Number(candles[candles.length - 1].time)
  if (t > lastT || t < firstT) return time

  const candle = nearestCandleByTime(candles, t)
  return candle?.time ?? time
}

/** Snap time to nearest bar when inside history; keep exact click price on Y. */
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

  const time = coordinateToTime(chart, chartX, candles)
  const price = coordinateToPrice(series, paneY)
  if (time == null || price == null) return null

  return {
    time: resolveDrawingAnchorTime(time, candles),
    price,
  }
}

/** Pixel position inside the price-pane overlay (pane-local coordinates). */
export function anchorToPixel(
  chart: IChartApi,
  series: ISeriesApi<'Candlestick'> | ISeriesApi<'Line'>,
  anchor: ChartAnchor,
  candles: CandlePoint[] = [],
): PixelPoint | null {
  const x = timeToCoordinate(chart, anchor.time, candles)
  const y = priceToCoordinate(series, anchor.price)
  if (x == null || y == null) return null
  return { x, y }
}

export function projectPendingAnchor(
  chart: IChartApi,
  series: ISeriesApi<'Candlestick'> | ISeriesApi<'Line'>,
  anchor: ChartAnchor,
  candles: CandlePoint[] = [],
): PixelPoint | null {
  return anchorToPixel(chart, series, anchor, candles)
}
