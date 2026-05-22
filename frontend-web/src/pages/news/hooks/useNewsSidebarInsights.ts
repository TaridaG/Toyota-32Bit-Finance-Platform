import { useCallback, useEffect, useMemo, useState } from 'react'
import { fetchNews } from '../../../features/news/api/newsService'
import { buildNewsSidebarStats, type NewsSidebarStats } from '../lib/buildNewsSidebarStats'

const SIDEBAR_SAMPLE_SIZE = 120
const SIDEBAR_MAX_AGE_MINUTES = 7 * 24 * 60

export function useNewsSidebarInsights(language: string | undefined, portfolioSymbols: string[]) {
  const [stats, setStats] = useState<NewsSidebarStats | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)

  const portfolioKey = useMemo(() => portfolioSymbols.join(','), [portfolioSymbols])

  const load = useCallback(async () => {
    setLoading(true)
    setError(false)
    try {
      const response = await fetchNews(
        0,
        SIDEBAR_SAMPLE_SIZE,
        language,
        { category: 'all', range: 'all' },
        undefined,
        { maxAgeMinutes: SIDEBAR_MAX_AGE_MINUTES },
      )
      setStats(buildNewsSidebarStats(response.content ?? [], portfolioSymbols))
    } catch {
      setError(true)
      setStats(null)
    } finally {
      setLoading(false)
    }
  }, [language, portfolioKey, portfolioSymbols])

  useEffect(() => {
    void load()
  }, [load])

  return { stats, loading, error, reload: load }
}
