import { useCallback, useEffect, useState } from 'react'
import {
  fetchMarketsCategoryPulse,
  MARKETS_PULSE_CATEGORIES,
  type MarketCategoryPulseItem,
  type MarketCategoryPulseOverall,
} from '../api/marketService'

const POLL_MS_VISIBLE = 45_000
const POLL_MS_HIDDEN = 90_000

export function useMarketsCategoryPulse() {
  const [items, setItems] = useState<MarketCategoryPulseItem[] | null>(null)
  const [overall, setOverall] = useState<MarketCategoryPulseOverall | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(async (silent: boolean) => {
    if (!silent) {
      setLoading(true)
    }
    try {
      const next = await fetchMarketsCategoryPulse()
      setItems(next.segments)
      setOverall(next.overall)
      setError(null)
    } catch {
      if (!silent) {
        setError('pulse')
      }
    } finally {
      if (!silent) {
        setLoading(false)
      }
    }
  }, [])

  useEffect(() => {
    void load(false)
    let intervalId: number | null = null
    const tick = () => {
      void load(true)
    }
    const restart = () => {
      if (intervalId != null) {
        window.clearInterval(intervalId)
      }
      const ms = document.visibilityState === 'visible' ? POLL_MS_VISIBLE : POLL_MS_HIDDEN
      intervalId = window.setInterval(tick, ms)
    }
    const onVis = () => restart()
    restart()
    document.addEventListener('visibilitychange', onVis)
    return () => {
      document.removeEventListener('visibilitychange', onVis)
      if (intervalId != null) {
        window.clearInterval(intervalId)
      }
    }
  }, [load])

  const refetch = useCallback(() => load(false), [load])

  return {
    items,
    overall,
    loading,
    error,
    refetch,
    categories: MARKETS_PULSE_CATEGORIES,
  }
}
