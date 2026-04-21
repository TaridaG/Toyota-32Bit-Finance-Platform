import { useEffect, useLayoutEffect, useRef } from 'react'
import type { CandlestickData, IChartApi, ISeriesApi, MouseEventParams, Time } from 'lightweight-charts'

export type OhlcTooltipState = {
  timeLabel: string
  open: number
  high: number
  low: number
  close: number
} | null

export function useCrosshairTooltip(
  chart: IChartApi | null,
  candleSeries: ISeriesApi<'Candlestick'> | null,
  onChange: (state: OhlcTooltipState) => void,
  onPanelSync?: (state: OhlcTooltipState) => void,
) {
  const onChangeRef = useRef(onChange)
  const onPanelRef = useRef(onPanelSync)
  const rafRef = useRef(0)
  const pendingRef = useRef<OhlcTooltipState>(null)

  useLayoutEffect(() => {
    onChangeRef.current = onChange
    onPanelRef.current = onPanelSync
  }, [onChange, onPanelSync])

  useEffect(() => {
    if (!chart || !candleSeries) return

    const flush = () => {
      rafRef.current = 0
      const v = pendingRef.current
      onChangeRef.current(v)
      onPanelRef.current?.(v)
    }

    const handler = (param: MouseEventParams<Time>) => {
      const bar = param.seriesData.get(candleSeries) as CandlestickData<Time> | undefined
      if (!bar || param.time === undefined) {
        pendingRef.current = null
      } else {
        const t = Number(param.time)
        pendingRef.current = {
          timeLabel: Number.isFinite(t) ? new Date(t * 1000).toLocaleString() : '',
          open: bar.open,
          high: bar.high,
          low: bar.low,
          close: bar.close,
        }
      }
      if (!rafRef.current) {
        rafRef.current = requestAnimationFrame(flush)
      }
    }

    chart.subscribeCrosshairMove(handler)
    return () => {
      chart.unsubscribeCrosshairMove(handler)
      if (rafRef.current) cancelAnimationFrame(rafRef.current)
    }
  }, [chart, candleSeries])
}
