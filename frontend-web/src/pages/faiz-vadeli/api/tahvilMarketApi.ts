import { apiClient } from '../../../shared/api/client'
import { getCachedOrLoad } from './requestCache'

const SUMMARY_TTL_MS = 30_000
const HISTORY_TTL_MS = 5 * 60_000

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
  return getCachedOrLoad(`faiz-vadeli:tahvil:summary:${symbol.toUpperCase()}`, SUMMARY_TTL_MS, async () => {
    const { data } = await apiClient.get<Record<string, MarketPriceSummaryEntry>>('/api/v1/market/prices/summary', {
      params: { symbols: symbol },
    })
    const row = data?.[symbol.toUpperCase()]
    return row ?? null
  })
}

/** Son ~5 yıl (TRBOND için MDS üst sınırı genişletilmiş aralık). */
export async function fetchTahvilHistory(symbol: string): Promise<MarketHistoryPoint[]> {
  const to = new Date()
  const from = new Date(to)
  from.setFullYear(from.getFullYear() - 5)
  return getCachedOrLoad(`faiz-vadeli:tahvil:history:${symbol.toUpperCase()}:5Y`, HISTORY_TTL_MS, async () => {
    const { data } = await apiClient.get<MarketHistoryPoint[]>('/api/v1/market/prices/history', {
      params: {
        symbol: symbol.toUpperCase(),
        from: toDateParam(from),
        to: toDateParam(to),
      },
    })
    return Array.isArray(data) ? data : []
  })
}
