import { useEffect, useLayoutEffect, useRef } from 'react'
import type { UTCTimestamp } from 'lightweight-charts'
import { createSeriesMarkers, type ISeriesApi, type ISeriesMarkersPluginApi, type SeriesMarker, type Time } from 'lightweight-charts'
import type { ChartTradeEvent } from '../../types'

const SELECTION_MARKER_ID = '__bar_selection__'

function buildMarkers(
  newsMarkers: SeriesMarker<Time>[],
  trades: ChartTradeEvent[],
  selectedBarTime: UTCTimestamp | null,
): SeriesMarker<Time>[] {
  const eventMarkers: SeriesMarker<Time>[] = []

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
  eventMarkers.push(...newsMarkers, ...tradeMarkers)

  if (selectedBarTime != null) {
    eventMarkers.push({
      time: selectedBarTime,
      position: 'inBar',
      color: '#fbbf24',
      shape: 'square',
      size: 2,
      id: SELECTION_MARKER_ID,
      text: 'Selected bar',
    })
  }

  return eventMarkers.sort((a, b) => Number(a.time) - Number(b.time))
}

export function useChartMarkers(
  markerSeries: ISeriesApi<'Candlestick'> | ISeriesApi<'Line'> | null,
  input: {
    newsMarkers: SeriesMarker<Time>[]
    trades: ChartTradeEvent[]
    selectedBarTime: UTCTimestamp | null
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
    plugin.setMarkers(
      buildMarkers(input.newsMarkers, input.trades, input.selectedBarTime),
    )
  }, [markerSeries, input.newsMarkers, input.trades, input.selectedBarTime])
}
