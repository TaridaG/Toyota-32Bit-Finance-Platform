import { useCallback, useEffect, useState } from 'react'
import { fetchMarketChampions } from '../../../features/markets/api/fetchMarketChampions'
import type { MarketCategory } from '../../../shared/types/market'
import type { SupportedCurrency } from '../../../shared/preferences/preferences'
import type { MarketChampionsSnapshot } from '../lib/marketChampions'

const REFRESH_MS = 60_000

export function useMarketChampions(displayCurrency: SupportedCurrency, category: MarketCategory) {
  const [champions, setChampions] = useState<MarketChampionsSnapshot | null>(null)
  const [loading, setLoading] = useState(true)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const next = await fetchMarketChampions(displayCurrency, category)
      setChampions(next)
    } catch {
      setChampions(null)
    } finally {
      setLoading(false)
    }
  }, [category, displayCurrency])

  useEffect(() => {
    void load()
    const intervalId = window.setInterval(() => {
      void load()
    }, REFRESH_MS)
    return () => window.clearInterval(intervalId)
  }, [load])

  return { champions, loading, reload: load }
}
