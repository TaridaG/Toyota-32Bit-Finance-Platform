import type { LineData, Time } from 'lightweight-charts'
import { useCallback, useEffect, useState } from 'react'
import { buildIndicators } from '../api/analysisService'
import type { CandlePoint } from '../../../pages/analysis/types'

type IndicatorData = {
  ma20: LineData<Time>[]
  ma50: LineData<Time>[]
  rsi: LineData<Time>[]
}

type UseIndicatorsResult = {
  indicators: IndicatorData
  loading: boolean
  error: string | null
  refetch: () => Promise<void>
}

const EMPTY_INDICATORS: IndicatorData = { ma20: [], ma50: [], rsi: [] }

export function useIndicators(candles: CandlePoint[]): UseIndicatorsResult {
  const [indicators, setIndicators] = useState<IndicatorData>(EMPTY_INDICATORS)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const refetch = useCallback(async () => {
    if (candles.length === 0) {
      setIndicators(EMPTY_INDICATORS)
      setLoading(false)
      return
    }
    setLoading(true)
    setError(null)
    try {
      const next = buildIndicators(candles)
      setIndicators({
        ma20: next.ma20,
        ma50: next.ma50,
        rsi: next.rsi,
      })
    } catch {
      setError('common:noData')
      setIndicators(EMPTY_INDICATORS)
    } finally {
      setLoading(false)
    }
  }, [candles])

  useEffect(() => {
    void refetch()
  }, [refetch])

  return { indicators, loading, error, refetch }
}
