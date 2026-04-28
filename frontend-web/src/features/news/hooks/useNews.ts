import { useCallback, useEffect, useState } from 'react'
import { fetchNews, type NewsApiItem } from '../api/newsService'

type UseNewsResult = {
  data: NewsApiItem[]
  loading: boolean
  error: string | null
  refetch: () => Promise<void>
}

export function useNews(page = 0, size = 20): UseNewsResult {
  const [data, setData] = useState<NewsApiItem[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const refetch = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const response = await fetchNews(page, size)
      setData(response.content ?? [])
    } catch {
      setError('loadError')
    } finally {
      setLoading(false)
    }
  }, [page, size])

  useEffect(() => {
    void refetch()
  }, [refetch])

  return { data, loading, error, refetch }
}
