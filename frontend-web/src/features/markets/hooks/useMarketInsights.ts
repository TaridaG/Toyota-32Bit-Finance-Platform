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

export function useMarketInsights(): UseMarketInsightsResult {
  const [topGainers, setTopGainers] = useState<MarketOverviewItem[]>([])
  const [topLosers, setTopLosers] = useState<MarketOverviewItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const refetch = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await fetchMarketInsights()
      setTopGainers(data.topGainers ?? [])
      setTopLosers(data.topLosers ?? [])
    } catch {
      setError('Market insights could not be loaded.')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void refetch()
  }, [refetch])

  return { topGainers, topLosers, loading, error, refetch }
}
