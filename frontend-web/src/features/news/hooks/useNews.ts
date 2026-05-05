import { useCallback, useEffect, useState } from 'react'
import { fetchNews, type NewsApiItem } from '../api/newsService'

type UseNewsResult = {
  data: NewsApiItem[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  loading: boolean
  error: string | null
  refetch: () => Promise<void>
}

export function useNews(page = 0, size = 20, language?: string): UseNewsResult {
  const [data, setData] = useState<NewsApiItem[]>([])
  const [totalElements, setTotalElements] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const refetch = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const response = await fetchNews(page, size, language)
      setData(response.content ?? [])
      setTotalElements(response.totalElements ?? 0)
      setTotalPages(response.totalPages ?? 0)
    } catch {
      setError('loadError')
    } finally {
      setLoading(false)
    }
  }, [language, page, size])

  useEffect(() => {
    void refetch()
  }, [refetch])

  return { data, page, size, totalElements, totalPages, loading, error, refetch }
}
