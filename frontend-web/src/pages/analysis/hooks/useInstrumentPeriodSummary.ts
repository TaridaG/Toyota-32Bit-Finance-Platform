import { useEffect, useState } from 'react'
import { fetchMarketPricesSummary } from '../../../features/markets/api/marketService'

export type InstrumentPeriodSummary = {
  price: number
  change1D: number
  weekly: number
  monthly: number
  threeMonth: number
  sixMonth: number
  yearly: number
}

type UseInstrumentPeriodSummaryResult = {
  summary: InstrumentPeriodSummary | null
  loading: boolean
}

export function useInstrumentPeriodSummary(
  symbol: string | null | undefined,
): UseInstrumentPeriodSummaryResult {
  const [summary, setSummary] = useState<InstrumentPeriodSummary | null>(null)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    const normalized = symbol?.trim()
    if (!normalized) {
      setSummary(null)
      setLoading(false)
      return
    }

    let cancelled = false
    setLoading(true)
    void fetchMarketPricesSummary([normalized])
      .then((bySymbol) => {
        if (cancelled) {
          return
        }
        const key = Object.keys(bySymbol).find(
          (k) => k.trim().toUpperCase() === normalized.toUpperCase(),
        )
        const item = key != null ? bySymbol[key] : undefined
        if (!item) {
          setSummary(null)
          return
        }
        setSummary({
          price: item.price,
          change1D: item.change1D,
          weekly: item.change1W ?? 0,
          monthly: item.change1M,
          threeMonth: item.change3M,
          sixMonth: item.change6M,
          yearly: item.change1Y,
        })
      })
      .catch(() => {
        if (!cancelled) {
          setSummary(null)
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
  }, [symbol])

  return { summary, loading }
}
