import { useCallback, useEffect, useState } from 'react'
import { fetchMarketInsights } from '../api/marketService'
import type { MarketOverviewItem } from '../../../shared/types/market'

type UseMarketInsightsResult = {
  topGainers: MarketOverviewItem[]
  topLosers: MarketOverviewItem[]
  loading: boolean
  error: string | null
  refetch: () => Promise<void>
}

type RefetchOptions = {
  silent?: boolean
}

const POLL_INTERVAL_VISIBLE_MS = 10_000
const POLL_INTERVAL_HIDDEN_MS = 30_000

export function useMarketInsights(): UseMarketInsightsResult {
  const [topGainers, setTopGainers] = useState<MarketOverviewItem[]>([])
  const [topLosers, setTopLosers] = useState<MarketOverviewItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const refetchInternal = useCallback(async (options?: RefetchOptions) => {
    const silent = options?.silent === true
    if (!silent) {
      setLoading(true)
    }
    try {
      const data = await fetchMarketInsights()
      const nextTopGainers = Array.isArray(data.topGainers) ? data.topGainers : []
      const nextTopLosers = Array.isArray(data.topLosers) ? data.topLosers : []
      setTopGainers(nextTopGainers)
      setTopLosers(nextTopLosers)
      setError(null)
    } catch (err) {
      console.error('market insights request failed', err)
      if (!silent) {
        setError('Market insights could not be loaded.')
      }
    } finally {
      if (!silent) {
        setLoading(false)
      }
    }
  }, [])

  const refetch = useCallback(async () => refetchInternal(), [refetchInternal])

  useEffect(() => {
    void refetchInternal()
  }, [refetchInternal])

  useEffect(() => {
    let intervalId: number | null = null
    const restartPolling = () => {
      if (intervalId != null) {
        window.clearInterval(intervalId)
      }
      const intervalMs = document.visibilityState === 'visible' ? POLL_INTERVAL_VISIBLE_MS : POLL_INTERVAL_HIDDEN_MS
      intervalId = window.setInterval(() => {
        void refetchInternal({ silent: true })
      }, intervalMs)
    }
    const onVisibilityChange = () => restartPolling()
    restartPolling()
    document.addEventListener('visibilitychange', onVisibilityChange)
    return () => {
      document.removeEventListener('visibilitychange', onVisibilityChange)
      if (intervalId != null) {
        window.clearInterval(intervalId)
      }
    }
  }, [refetchInternal])

  return { topGainers, topLosers, loading, error, refetch }
}
