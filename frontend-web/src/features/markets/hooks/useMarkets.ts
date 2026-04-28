import { useCallback, useEffect, useState } from 'react'
import { fetchMarketOverview } from '../api/marketService'
import type { MarketCategory, MarketOverviewItem } from '../../../shared/types/market'

type UseMarketsParams = {
  page: number
  size: number
  category: MarketCategory
  searchTerm: string
  sort?: string
}

type UseMarketsResult = {
  rows: MarketOverviewItem[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  loading: boolean
  error: string | null
  refetch: () => Promise<void>
}

function useDebouncedValue<T>(value: T, delayMs: number): T {
  const [debounced, setDebounced] = useState(value)

  useEffect(() => {
    const timeoutId = window.setTimeout(() => setDebounced(value), delayMs)
    return () => window.clearTimeout(timeoutId)
  }, [value, delayMs])

  return debounced
}

export function useMarkets({ page, size, category, searchTerm, sort }: UseMarketsParams): UseMarketsResult {
  const [rows, setRows] = useState<MarketOverviewItem[]>([])
  const [totalElements, setTotalElements] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const debouncedSearch = useDebouncedValue(searchTerm.trim(), 350)

  const refetch = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const response = await fetchMarketOverview({
        page: Math.max(page, 0),
        size,
        category,
        query: debouncedSearch,
        sort,
      })
      setRows(response.content)
      setTotalElements(response.totalElements)
      setTotalPages(response.totalPages)
    } catch {
      setError('Market data could not be loaded. Please try again.')
    } finally {
      setLoading(false)
    }
  }, [category, debouncedSearch, page, size, sort])

  useEffect(() => {
    void refetch()
  }, [refetch])

  return {
    rows,
    page,
    size,
    totalElements,
    totalPages,
    loading,
    error,
    refetch,
  }
}
