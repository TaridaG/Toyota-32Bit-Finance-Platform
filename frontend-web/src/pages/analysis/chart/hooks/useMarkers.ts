import { useEffect, useLayoutEffect, useRef } from 'react'
import type { ISeriesApi, ISeriesMarkersPluginApi, SeriesMarker, Time } from 'lightweight-charts'
import { createSeriesMarkers } from 'lightweight-charts'
import type { ChartTradeEvent } from '../../types'

function buildMarkers(newsMarkers: SeriesMarker<Time>[], trades: ChartTradeEvent[]): SeriesMarker<Time>[] {
  const tradeMarkers: SeriesMarker<Time>[] = trades.map((ev) => {
    const up = ev.side === 'buy'
    return {
      time: ev.time,
      position: up ? 'belowBar' : 'aboveBar',
      color: up ? '#22c55e' : '#ef4444',
      shape: up ? 'arrowUp' : 'arrowDown',
      size: 1,
      id: ev.id,
      text: ev.title,
    }
  })
  return [...newsMarkers, ...tradeMarkers].sort((a, b) => Number(a.time) - Number(b.time))
}

export function useChartMarkers(
  markerSeries: ISeriesApi<'Candlestick'> | ISeriesApi<'Line'> | null,
  input: {
    newsMarkers: SeriesMarker<Time>[]
    trades: ChartTradeEvent[]
  },
) {
  const pluginRef = useRef<ISeriesMarkersPluginApi<Time> | null>(null)

  useLayoutEffect(() => {
    if (!markerSeries) return
    const plugin = createSeriesMarkers(markerSeries, [], { autoScale: true, zOrder: 'top' })
    pluginRef.current = plugin
    return () => {
      try {
        plugin.detach()
      } catch {
        /* series/chart disposed */
      }
      pluginRef.current = null
    }
  }, [markerSeries])

  useEffect(() => {
    const plugin = pluginRef.current
    if (!plugin) return
    plugin.setMarkers(buildMarkers(input.newsMarkers, input.trades))
  }, [markerSeries, input.newsMarkers, input.trades])
}
