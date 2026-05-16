import { useEffect, useLayoutEffect, useRef } from 'react'
import type {
  CandlestickData,
  IChartApi,
  ISeriesApi,
  LineData,
  MouseEventParams,
  Time,
  UTCTimestamp,
} from 'lightweight-charts'
import type { CandlePoint } from '../../types'

export type OhlcTooltipState = {
  timeLabel: string
  open: number
  high: number
  low: number
  close: number
  volume?: number
  ma20?: number
  ma50?: number
  rsi?: number
} | null

function nearestByTime(data: LineData<Time>[], target: UTCTimestamp): number | undefined {
  if (data.length === 0) return undefined
  let lo = 0
  let hi = data.length - 1
  while (lo <= hi) {
    const mid = Math.floor((lo + hi) / 2)
    const midTime = data[mid].time as number
    if (midTime === target) {
      return Number.isFinite(data[mid].value) ? data[mid].value : undefined
    }
    if (midTime < target) lo = mid + 1
    else hi = mid - 1
  }
  const left = hi >= 0 ? data[hi] : null
  const right = lo < data.length ? data[lo] : null
  const nearest =
    left == null
      ? right
      : right == null
        ? left
        : Math.abs((left.time as number) - target) <= Math.abs((right.time as number) - target)
          ? left
          : right
  if (!nearest || !Number.isFinite(nearest.value)) {
    return undefined
  }
  return nearest.value
}

function nearestCandleByTime(data: CandlePoint[], target: UTCTimestamp): CandlePoint | null {
  if (data.length === 0) return null
  let lo = 0
  let hi = data.length - 1
  while (lo <= hi) {
    const mid = Math.floor((lo + hi) / 2)
    const midTime = data[mid].time
    if (midTime === target) return data[mid]
    if (midTime < target) lo = mid + 1
    else hi = mid - 1
  }
  const left = hi >= 0 ? data[hi] : null
  const right = lo < data.length ? data[lo] : null
  if (!left) return right
  if (!right) return left
  return Math.abs(left.time - target) <= Math.abs(right.time - target) ? left : right
}

type UseCrosshairTooltipArgs = {
  chart: IChartApi | null
  priceSeries: ISeriesApi<'Candlestick'> | ISeriesApi<'Line'> | null
  candles: CandlePoint[]
  ma20Data: LineData<Time>[]
  ma50Data: LineData<Time>[]
  rsiData: LineData<Time>[]
  onChange: (state: OhlcTooltipState) => void
  onPanelSync?: (state: OhlcTooltipState) => void
}

export function useCrosshairTooltip({
  chart,
  priceSeries,
  candles,
  ma20Data,
  ma50Data,
  rsiData,
  onChange,
  onPanelSync,
}: UseCrosshairTooltipArgs) {
  const refs = useRef({ candles, ma20Data, ma50Data, rsiData })
  const onChangeRef = useRef<(state: OhlcTooltipState) => void>(onChange)
  const onPanelRef = useRef<typeof onPanelSync>(onPanelSync)
  const rafRef = useRef(0)
  const pendingRef = useRef<OhlcTooltipState>(null)

  useLayoutEffect(() => {
    refs.current = { candles, ma20Data, ma50Data, rsiData }
    onChangeRef.current = onChange
    onPanelRef.current = onPanelSync
  }, [candles, ma20Data, ma50Data, onChange, onPanelSync, rsiData])

  useEffect(() => {
    if (!chart || !priceSeries) return

    const flush = () => {
      rafRef.current = 0
      const v = pendingRef.current
      onChangeRef.current(v)
      onPanelRef.current?.(v)
    }

    const handler = (param: MouseEventParams<Time>) => {
      const raw = param.seriesData.get(priceSeries)
      const bar =
        raw != null && 'open' in raw ? (raw as CandlestickData<Time>) : undefined
      const lineClose =
        raw != null && 'value' in raw && Number.isFinite((raw as LineData<Time>).value)
          ? (raw as LineData<Time>).value
          : undefined
      if (param.time === undefined) {
        pendingRef.current = null
      } else {
        const t = Number(param.time)
        const asTime = t as UTCTimestamp
        const snapped = nearestByTime(
          refs.current.candles.map((item) => ({ time: item.time, value: item.close })),
          asTime,
        )
        const snappedBar = nearestCandleByTime(refs.current.candles, asTime)
        if (!bar && !snappedBar && lineClose == null) {
          pendingRef.current = null
          return
        }
        const ma20 = nearestByTime(refs.current.ma20Data, asTime)
        const ma50 = nearestByTime(refs.current.ma50Data, asTime)
        const rsi = nearestByTime(refs.current.rsiData, asTime)
        pendingRef.current = {
          timeLabel: Number.isFinite(t) ? new Date(t * 1000).toLocaleString() : '',
          open: bar?.open ?? snappedBar?.open ?? lineClose ?? NaN,
          high: bar?.high ?? snappedBar?.high ?? lineClose ?? NaN,
          low: bar?.low ?? snappedBar?.low ?? lineClose ?? NaN,
          close: bar?.close ?? snapped ?? snappedBar?.close ?? lineClose ?? NaN,
          volume: snappedBar?.volume,
          ma20,
          ma50,
          rsi,
        }
      }
      if (!rafRef.current) {
        rafRef.current = requestAnimationFrame(flush)
      }
    }

    chart.subscribeCrosshairMove(handler)
    return () => {
      try {
        chart.unsubscribeCrosshairMove(handler)
      } catch {
        /* chart may already be removed */
      }
      if (rafRef.current) cancelAnimationFrame(rafRef.current)
    }
  }, [chart, priceSeries])
}
