import { useEffect, useMemo, useState } from 'react'
import { fetchAnalysisInstrumentCatalog, type CatalogRow } from '../../../features/markets/api/marketService'
import type { MarketCategory } from '../../../shared/types/market'
import { catalogRowToAsset } from '../utils/analysisCatalog'
import type { AssetDefinition } from '../types'

type UseAnalysisInstrumentCatalogResult = {
  assets: AssetDefinition[]
  rows: CatalogRow[]
  loading: boolean
  error: string | null
}

export function useAnalysisInstrumentCatalog(
  category: MarketCategory,
  enabled = true,
): UseAnalysisInstrumentCatalogResult {
  const [rows, setRows] = useState<CatalogRow[]>([])
  const [loading, setLoading] = useState(enabled)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!enabled) {
      setLoading(false)
      return
    }
    let cancelled = false
    setLoading(true)
    setError(null)
    void fetchAnalysisInstrumentCatalog({
      category: category === 'all' ? undefined : category,
    })
      .then((snapshot) => {
        if (!cancelled) {
          setRows(snapshot.filteredRows)
        }
      })
      .catch(() => {
        if (!cancelled) {
          setRows([])
          setError('loadError')
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
  }, [category, enabled])

  const assets = useMemo(
    () => rows.map((row) => catalogRowToAsset(row, category === 'all' ? 'all' : category)),
    [category, rows],
  )

  return { assets, rows, loading, error }
}
