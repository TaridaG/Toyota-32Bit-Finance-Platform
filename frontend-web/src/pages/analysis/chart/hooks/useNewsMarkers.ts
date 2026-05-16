import { useMemo } from 'react'
import type { SeriesMarker, Time } from 'lightweight-charts'
import type { AssetNewsItem } from '../../types'
import { markerColorForTone } from '../../utils/chartNews'

export function useNewsMarkers(news: AssetNewsItem[], visible: boolean): SeriesMarker<Time>[] {
  return useMemo(() => {
    if (!visible) return []
    return news.map((item) => ({
      time: item.createdAt,
      position: 'inBar' as const,
      color: markerColorForTone(item.markerTone ?? 'neutral'),
      shape: 'circle' as const,
      size: 1.2,
      id: item.id,
    }))
  }, [news, visible])
}
