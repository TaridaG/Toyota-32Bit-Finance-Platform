import { useEffect, useLayoutEffect, useRef, useState } from 'react'
import type { IChartApi, MouseEventParams, Time } from 'lightweight-charts'
import type { AssetNewsItem } from '../../types'

export type NewsMarkerTooltipState = {
  item: AssetNewsItem
  x: number
  y: number
} | null

type UseNewsMarkerTooltipArgs = {
  chart: IChartApi | null
  newsItems: AssetNewsItem[]
  enabled: boolean
}

export function useNewsMarkerTooltip({ chart, newsItems, enabled }: UseNewsMarkerTooltipArgs) {
  const [tooltip, setTooltip] = useState<NewsMarkerTooltipState>(null)
  const newsRef = useRef(newsItems)

  useLayoutEffect(() => {
    newsRef.current = newsItems
  }, [newsItems])

  useEffect(() => {
    if (!chart || !enabled) {
      setTooltip(null)
      return
    }

    const handler = (param: MouseEventParams<Time>) => {
      if (!param.point || param.time === undefined) {
        setTooltip(null)
        return
      }

      const oid = param.hoveredObjectId
      if (typeof oid === 'string' && oid.length > 0) {
        const hit = newsRef.current.find((n) => n.id === oid)
        if (hit) {
          setTooltip({ item: hit, x: param.point.x, y: param.point.y })
          return
        }
      }

      setTooltip(null)
    }

    chart.subscribeCrosshairMove(handler)
    return () => {
      try {
        chart.unsubscribeCrosshairMove(handler)
      } catch {
        /* chart disposed */
      }
    }
  }, [chart, enabled])

  return tooltip
}
