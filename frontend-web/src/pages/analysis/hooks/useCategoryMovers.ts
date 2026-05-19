import { useEffect, useMemo, useState } from 'react'
import {
  fetchCategoryPerformanceRows,
  type CategoryPerformanceRow,
} from '../../../features/markets/api/marketService'
import type { MarketCategory } from '../../../shared/types/market'
import { buildCategoryMoversSnapshot } from '../utils/categoryMovers'

type UseCategoryMoversResult = {
  loading: boolean
  error: boolean
  rows: CategoryPerformanceRow[]
  winners: CategoryPerformanceRow[]
  losers: CategoryPerformanceRow[]
}

export function useCategoryMovers(segment: MarketCategory): UseCategoryMoversResult {
  const [rows, setRows] = useState<CategoryPerformanceRow[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(false)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError(false)
    void fetchCategoryPerformanceRows(segment)
      .then((data) => {
        if (!cancelled) {
          setRows(data)
        }
      })
      .catch(() => {
        if (!cancelled) {
          setRows([])
          setError(true)
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false)
        }
      })
    return () => {
      cancelled = true
    }
  }, [segment])

  const { winners, losers } = useMemo(() => buildCategoryMoversSnapshot(rows), [rows])

  return { loading, error, rows, winners, losers }
}
