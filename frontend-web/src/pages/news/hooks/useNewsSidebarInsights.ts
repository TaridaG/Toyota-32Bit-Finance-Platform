import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { fetchNewsWeeklySummary } from '../../../features/news/api/newsService'
import type { NewsSidebarStats } from '../lib/buildNewsSidebarStats'

export function useNewsSidebarInsights(
  language: string | undefined,
  portfolioSymbols: string[],
  enabled = true,
) {
  const [stats, setStats] = useState<NewsSidebarStats | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)
  const hasLoadedOnceRef = useRef(false)

  const portfolioKey = useMemo(() => portfolioSymbols.join(','), [portfolioSymbols])

  const load = useCallback(async () => {
    if (!enabled) {
      return
    }
    if (!hasLoadedOnceRef.current) {
      setLoading(true)
    }
    setError(false)
    try {
      const response = await fetchNewsWeeklySummary(language, portfolioSymbols)
      setStats({
        totalSampled: response.totalCount,
        topics: response.topics,
        topAssets: response.topAssets,
        sources: response.sources,
        portfolioRelatedCount: response.portfolioRelatedCount,
      })
    } catch {
      setError(true)
      setStats(null)
    } finally {
      hasLoadedOnceRef.current = true
      setLoading(false)
    }
  }, [enabled, language, portfolioKey, portfolioSymbols])

  useEffect(() => {
    if (!enabled) {
      return
    }
    void load()
  }, [enabled, load])

  return { stats, loading, error, reload: load }
}
