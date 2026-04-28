import { useMemo } from 'react'
import type { SeriesMarker, Time } from 'lightweight-charts'
import type { AssetNewsItem } from '../../types'

export function useNewsMarkers(news: AssetNewsItem[], visible: boolean): SeriesMarker<Time>[] {
  return useMemo(() => {
    if (!visible) return []
    return news.map((item) => ({
      time: item.createdAt,
      position: 'aboveBar',
      color: '#3b82f6',
      shape: 'circle',
      size: 1,
      id: item.id,
      text: item.title,
    }))
  }, [news, visible])
}
