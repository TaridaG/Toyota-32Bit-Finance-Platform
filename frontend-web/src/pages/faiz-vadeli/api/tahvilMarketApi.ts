import { apiClient } from '../../../shared/api/client'

export type MarketPriceSummaryEntry = {
  price?: number | null
  change1D?: number | null
  change1M?: number | null
  change3M?: number | null
  change6M?: number | null
  change1Y?: number | null
}

export type MarketHistoryPoint = {
  time?: string | null
  value?: number | null
}

function toDateParam(d: Date): string {
  return d.toISOString().slice(0, 10)
}

export async function fetchTahvilSummary(symbol: string): Promise<MarketPriceSummaryEntry | null> {
  const { data } = await apiClient.get<Record<string, MarketPriceSummaryEntry>>('/api/market/prices/summary', {
    params: { symbols: symbol },
  })
  const row = data?.[symbol.toUpperCase()]
  return row ?? null
}

/** Son ~5 yıl (TRBOND için MDS üst sınırı genişletilmiş aralık). */
export async function fetchTahvilHistory(symbol: string): Promise<MarketHistoryPoint[]> {
  const to = new Date()
  const from = new Date(to)
  from.setFullYear(from.getFullYear() - 5)
  const { data } = await apiClient.get<MarketHistoryPoint[]>('/api/market/prices/history', {
    params: {
      symbol: symbol.toUpperCase(),
      from: toDateParam(from),
      to: toDateParam(to),
    },
  })
  return Array.isArray(data) ? data : []
}
