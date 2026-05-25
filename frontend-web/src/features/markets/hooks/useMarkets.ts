import axios from 'axios'
import { useCallback, useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { fetchLiveFxMidRows, fetchMarketOverviewPage, repriceMarketOverviewRows } from '../api/marketService'
import { parseMarketSortQuery } from '../lib/marketSort'
import type { MarketCategory, MarketOverviewItem } from '../../../shared/types/market'
import type { SupportedCurrency } from '../../../shared/preferences/preferences'

type UseMarketsParams = {
  page: number
  size: number
  category: MarketCategory
  searchTerm: string
  sort?: string
  displayCurrency: SupportedCurrency
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
  signal?: AbortSignal
}

const POLL_INTERVAL_VISIBLE_MS = 10_000
const POLL_INTERVAL_HIDDEN_MS = 30_000

type FetchContext = {
  queryKey: string
  currency: SupportedCurrency
}

function isAbortError(err: unknown): boolean {
  if (axios.isCancel(err)) {
    return true
  }
  if (axios.isAxiosError(err) && err.code === 'ERR_CANCELED') {
    return true
  }
  return err instanceof DOMException && err.name === 'AbortError'
}

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
      (a.nativeQuote ?? null) !== (b.nativeQuote ?? null) ||
      (a.displayAmount ?? null) !== (b.displayAmount ?? null) ||
      (a.trendScore ?? null) !== (b.trendScore ?? null) ||
      (a.trendLabel ?? null) !== (b.trendLabel ?? null) ||
      (a.timestamp ?? null) !== (b.timestamp ?? null) ||
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

export function useMarkets({ page, size, category, searchTerm, sort, displayCurrency }: UseMarketsParams): UseMarketsResult {
  const { t } = useTranslation('markets')
  const [rows, setRows] = useState<MarketOverviewItem[]>([])
  const [totalElements, setTotalElements] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const debouncedSearch = useDebouncedValue(searchTerm.trim(), 350)
  const abortRef = useRef<AbortController | null>(null)
  const rowsRef = useRef<MarketOverviewItem[]>([])
  const lastFetchContextRef = useRef<FetchContext | null>(null)
  const sortField = parseMarketSortQuery(sort).field
  const backendDisplayCurrency: SupportedCurrency = displayCurrency

  useEffect(() => {
    rowsRef.current = rows
  }, [rows])

  const refetchInternal = useCallback(
    async (options?: RefetchOptions) => {
      const silent = options?.silent === true
      const externalSignal = options?.signal
      const resolvedPage = Math.max(page, 0)
      const queryKey = [resolvedPage, size, category, debouncedSearch, sort ?? ''].join('|')
      const lastFetchContext = lastFetchContextRef.current
      const canClientReprice =
        !silent &&
        sortField !== 'displayAmount' &&
        rowsRef.current.length > 0 &&
        lastFetchContext?.queryKey === queryKey &&
        lastFetchContext.currency !== displayCurrency

      if (canClientReprice) {
        try {
          const fxRows = await fetchLiveFxMidRows()
          if (externalSignal?.aborted) {
            return
          }
          const nextRows = repriceMarketOverviewRows(rowsRef.current, displayCurrency, fxRows)
          setRows((prev) => (rowsEqual(prev, nextRows) ? prev : nextRows))
          setError(null)
          lastFetchContextRef.current = { queryKey, currency: displayCurrency }
          return
        } catch (err) {
          if (isAbortError(err) || externalSignal?.aborted) {
            return
          }
          console.warn('market repricing fallback failed, refetching overview', err)
        }
      }

      if (!silent) {
        abortRef.current?.abort()
        abortRef.current = new AbortController()
      }

      const signal = externalSignal ?? abortRef.current?.signal
      if (!silent) {
        setLoading(true)
      }
      try {
        const response = await fetchMarketOverviewPage({
          page: resolvedPage,
          size,
          category,
          query: debouncedSearch,
          sort,
          displayCurrency: backendDisplayCurrency,
          signal,
        })
        if (signal?.aborted) {
          return
        }
        let nextRows = Array.isArray(response.content) ? response.content : []
        let resolvedCurrency = backendDisplayCurrency
        setRows((prev) => (rowsEqual(prev, nextRows) ? prev : nextRows))
        const nextTotalElements = response.totalElements ?? 0
        const nextTotalPages = response.totalPages ?? 0
        setTotalElements((prev) => (prev === nextTotalElements ? prev : nextTotalElements))
        setTotalPages((prev) => (prev === nextTotalPages ? prev : nextTotalPages))
        setError(null)
        lastFetchContextRef.current = { queryKey, currency: resolvedCurrency }
      } catch (err) {
        if (isAbortError(err) || signal?.aborted) {
          return
        }
        console.error('market overview request failed', err)
        if (!silent) {
          const timedOut = axios.isAxiosError(err) && err.code === 'ECONNABORTED'
          setError(timedOut ? t('loadErrorTimeout') : t('loadError'))
        }
      } finally {
        if (!silent && !signal?.aborted) {
          setLoading(false)
        }
      }
    },
    [backendDisplayCurrency, category, debouncedSearch, displayCurrency, page, size, sort, sortField, t],
  )

  const refetch = useCallback(async () => refetchInternal(), [refetchInternal])

  useEffect(() => {
    const controller = new AbortController()
    void refetchInternal({ signal: controller.signal })
    return () => {
      controller.abort()
    }
  }, [refetchInternal])

  useEffect(() => {
    let intervalId: number | null = null
    let pollInFlight = false

    const tick = async () => {
      if (pollInFlight || document.visibilityState !== 'visible') {
        return
      }
      pollInFlight = true
      try {
        await refetchInternal({ silent: true })
      } finally {
        pollInFlight = false
      }
    }

    const restartPolling = () => {
      if (intervalId != null) {
        window.clearInterval(intervalId)
      }
      const intervalMs = document.visibilityState === 'visible' ? POLL_INTERVAL_VISIBLE_MS : POLL_INTERVAL_HIDDEN_MS
      intervalId = window.setInterval(() => {
        void tick()
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

  useEffect(() => {
    return () => {
      abortRef.current?.abort()
    }
  }, [])

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
