import { apiClient } from '../../../shared/api/client'
import { getCachedOrLoad } from './requestCache'
import type { ViopSegment } from '../lib/viopSegment'

export type { ViopSegment } from '../lib/viopSegment'

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

export type ViopActiveContract = {
  contractCode: string
  underlying?: string | null
  marketGroup?: string | null
  expiryDate?: string | null
  tradeDate?: string | null
  settlementPrice?: number | null
  changePercent?: number | null
  volumeTl?: number | null
  volumeQty?: number | null
  openInterest?: number | null
  ingestedAt?: string | null
}

export type ViopContractHistoryPoint = {
  tradeDate?: string | null
  settlementPrice?: number | null
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

/** Son ~5 yıl (VIOP alias sembolleri için günlük MDS fiyat geçmişi). */
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

export async function fetchViopActiveContracts(segment: ViopSegment = 'rates_bonds'): Promise<ViopActiveContract[]> {
  return getCachedOrLoad(`faiz-vadeli:viop:contracts:${segment}`, SUMMARY_TTL_MS, async () => {
    const { data } = await apiClient.get<ViopActiveContract[] | { data?: ViopActiveContract[] }>(
      '/api/v1/market/viop/contracts/active',
      { params: { segment } },
    )
    if (Array.isArray(data)) {
      return data
    }
    return Array.isArray(data?.data) ? data.data : []
  })
}

export async function fetchViopContractHistory(contractCode: string): Promise<MarketHistoryPoint[]> {
  const to = new Date()
  const from = new Date(to)
  from.setFullYear(from.getFullYear() - 5)
  const key = `faiz-vadeli:viop:history:${contractCode.toUpperCase()}:5Y`
  return getCachedOrLoad(key, HISTORY_TTL_MS, async () => {
    const { data } = await apiClient.get<ViopContractHistoryPoint[] | { data?: ViopContractHistoryPoint[] }>(
      `/api/v1/market/viop/contracts/${encodeURIComponent(contractCode)}/history`,
      {
        params: { from: toDateParam(from), to: toDateParam(to) },
      },
    )
    const rows = Array.isArray(data) ? data : Array.isArray(data?.data) ? data.data : []
    return rows.map((row) => ({ time: row.tradeDate ?? null, value: row.settlementPrice ?? null }))
  })
}
