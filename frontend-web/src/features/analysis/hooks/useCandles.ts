import { useCallback, useEffect, useState } from 'react'
import { fetchCandles, normalizeAnalysisInstrumentSymbol, type AnalysisRange } from '../api/analysisService'
import type { CandlePoint } from '../../../pages/analysis/types'

type UseCandlesOpts = {
  currencyKey?: string
  wireCategory?: string | null
  /** When false, skip fetch until enabled (lazy load). */
  enabled?: boolean
}

type UseCandlesResult = {
  candles: CandlePoint[]
  loading: boolean
  error: string | null
  refetch: () => Promise<void>
}

export function useCandles(symbol: string, interval: AnalysisRange, opts?: UseCandlesOpts): UseCandlesResult {
  const [candles, setCandles] = useState<CandlePoint[]>([])
  const [loading, setLoading] = useState(() => normalizeAnalysisInstrumentSymbol(symbol).length > 0)
  const [error, setError] = useState<string | null>(null)
  const wireCategory = opts?.wireCategory ?? null
  const currencyKey = opts?.currencyKey
  const enabled = opts?.enabled ?? true

  const refetch = useCallback(async () => {
    if (!enabled) {
      return
    }
    if (!normalizeAnalysisInstrumentSymbol(symbol)) {
      setCandles([])
      setError(null)
      setLoading(false)
      return
    }
    setLoading(true)
    setError(null)
    try {
      const data = await fetchCandles(symbol, interval, { wireCategory })
      setCandles(data)
    } catch {
      setError('analysis:chartLoadError')
    } finally {
      setLoading(false)
    }
  }, [enabled, interval, symbol, wireCategory])

  useEffect(() => {
    if (!enabled) {
      setLoading(false)
      return
    }
    setCandles([])
    void refetch()
  }, [currencyKey, enabled, refetch])

  return { candles, loading, error, refetch }
}
