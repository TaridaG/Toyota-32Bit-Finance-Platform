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

type RefetchOptions = {
  silent?: boolean
}

const POLL_INTERVAL_VISIBLE_MS = 10_000
const POLL_INTERVAL_HIDDEN_MS = 30_000

function rowsEqual(prev: MarketOverviewItem[], next: MarketOverviewItem[]): boolean {
  if (prev.length !== next.length) {
    return false
  }
  for (let i = 0; i < prev.length; i += 1) {
    const a = prev[i]
    const b = next[i]
    if (
      a.symbol !== b.symbol ||
      a.name !== b.name ||
      a.price !== b.price ||
      a.change24h !== b.change24h ||
      a.change1D !== b.change1D ||
      a.change1M !== b.change1M ||
      a.change3M !== b.change3M ||
      a.change6M !== b.change6M ||
      a.change1Y !== b.change1Y ||
      a.high24h !== b.high24h ||
      a.low24h !== b.low24h ||
      a.category !== b.category ||
      a.instrumentId !== b.instrumentId ||
      a.timestamp !== b.timestamp ||
      a.freshness !== b.freshness
    ) {
      return false
    }
  }
  return true
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

  const refetchInternal = useCallback(async (options?: RefetchOptions) => {
    const silent = options?.silent === true
    if (!silent) {
      setLoading(true)
    }
    try {
      const response = await fetchMarketOverview({
        page: Math.max(page, 0),
        size,
        category,
        query: debouncedSearch,
        sort,
      })
      const nextRows = Array.isArray(response.content) ? response.content : []
      setRows((prev) => (rowsEqual(prev, nextRows) ? prev : nextRows))
      const nextTotalElements = response.totalElements ?? 0
      const nextTotalPages = response.totalPages ?? 0
      setTotalElements((prev) => (prev === nextTotalElements ? prev : nextTotalElements))
      setTotalPages((prev) => (prev === nextTotalPages ? prev : nextTotalPages))
      setError(null)
    } catch (err) {
      console.error('market overview request failed', err)
      if (!silent) {
        setError('Market data could not be loaded. Please try again.')
      }
    } finally {
      if (!silent) {
        setLoading(false)
      }
    }
  }, [category, debouncedSearch, page, size, sort])

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
