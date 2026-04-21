import { useEffect, useLayoutEffect, useRef } from 'react'
import type { UTCTimestamp } from 'lightweight-charts'
import { createSeriesMarkers, type ISeriesApi, type ISeriesMarkersPluginApi, type SeriesMarker, type Time } from 'lightweight-charts'
import type { AssetNewsItem, ChartTradeEvent } from '../../types'

const SELECTION_MARKER_ID = '__bar_selection__'

function buildMarkers(
  news: AssetNewsItem[],
  trades: ChartTradeEvent[],
  show: boolean,
  selectedBarTime: UTCTimestamp | null,
): SeriesMarker<Time>[] {
  const eventMarkers: SeriesMarker<Time>[] = []

  if (show) {
    const newsMarkers: SeriesMarker<Time>[] = news.map((item) => ({
      time: item.createdAt,
      position: 'aboveBar',
      color: '#3b82f6',
      shape: 'circle',
      size: 1,
      id: item.id,
      text: item.title,
    }))

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
  }

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
  candleSeries: ISeriesApi<'Candlestick'> | null,
  input: {
    news: AssetNewsItem[]
    trades: ChartTradeEvent[]
    visible: boolean
    selectedBarTime: UTCTimestamp | null
  },
) {
  const pluginRef = useRef<ISeriesMarkersPluginApi<Time> | null>(null)

  useLayoutEffect(() => {
    if (!candleSeries) return
    const plugin = createSeriesMarkers(candleSeries, [], { autoScale: true, zOrder: 'top' })
    pluginRef.current = plugin
    return () => {
      plugin.detach()
      pluginRef.current = null
    }
  }, [candleSeries])

  useEffect(() => {
    const plugin = pluginRef.current
    if (!plugin) return
    plugin.setMarkers(
      buildMarkers(input.news, input.trades, input.visible, input.selectedBarTime),
    )
  }, [candleSeries, input.news, input.trades, input.visible, input.selectedBarTime])
}
