import { useCallback, useEffect, useState } from 'react'
import { fetchCandles, normalizeAnalysisInstrumentSymbol, type AnalysisRange } from '../api/analysisService'
import type { CandlePoint } from '../../../pages/analysis/types'

type UseCandlesResult = {
  candles: CandlePoint[]
  loading: boolean
  error: string | null
  refetch: () => Promise<void>
}

export function useCandles(symbol: string, interval: AnalysisRange, currencyKey?: string): UseCandlesResult {
  const [candles, setCandles] = useState<CandlePoint[]>([])
  const [loading, setLoading] = useState(() => normalizeAnalysisInstrumentSymbol(symbol).length > 0)
  const [error, setError] = useState<string | null>(null)

  const refetch = useCallback(async () => {
    if (!normalizeAnalysisInstrumentSymbol(symbol)) {
      setCandles([])
      setError(null)
      setLoading(false)
      return
    }
    setLoading(true)
    setError(null)
    try {
      const data = await fetchCandles(symbol, interval)
      setCandles(data)
    } catch {
      setError('common:noData')
    } finally {
      setLoading(false)
    }
  }, [interval, symbol])

  useEffect(() => {
    void refetch()
  }, [currencyKey, refetch])

  return { candles, loading, error, refetch }
}
