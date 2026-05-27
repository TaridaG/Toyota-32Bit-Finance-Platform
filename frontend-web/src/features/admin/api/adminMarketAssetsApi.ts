import { isAxiosError } from 'axios'
import { apiClient } from '../../../shared/api/client'

type AdminEnvelope<T> = { success: boolean; data?: T | null; error?: { message?: string } }

export type MarketAssetStatRow = {
  instrumentId: number
  symbol: string
  instrumentName: string
  portfolioCount: number
  userCount: number
  avgWeightPercent: number
  rank: number
}

export type MarketAssetStatsPage = {
  content: MarketAssetStatRow[]
  totalElements: number
  totalPages: number
  page: number
  size: number
}

export type MarketAssetDashboard = {
  status: 'IDLE' | 'RUNNING' | 'READY' | 'FAILED' | string
  startedAt: string | null
  computedAt: string | null
  avgWatchlistInstrumentsPerUser: number | null
  avgInstrumentsPerPortfolio: number | null
  avgPortfolioWeightPercent: number | null
  instrumentRowCount: number
  errorMessage: string | null
  table: MarketAssetStatsPage
}

export type MarketAssetRecomputeResponse = {
  status: string
  started: boolean
}

function readMessage(err: unknown): string {
  if (isAxiosError(err)) {
    const msg = (err.response?.data as AdminEnvelope<unknown> | undefined)?.error?.message
    if (msg) return msg
  }
  return err instanceof Error ? err.message : 'Request failed'
}

export async function fetchMarketAssetDashboard(page = 0, size = 10): Promise<MarketAssetDashboard> {
  const { data } = await apiClient.get<AdminEnvelope<MarketAssetDashboard>>('/api/v1/admin/metrics/market-assets', {
    params: { page, size },
  })
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Failed to load market assets')
  }
  return normalizeDashboard(data.data)
}

export async function recomputeMarketAssets(): Promise<MarketAssetRecomputeResponse> {
  const { data } = await apiClient.post<AdminEnvelope<MarketAssetRecomputeResponse>>(
    '/api/v1/admin/metrics/market-assets/recompute',
  )
  if (!data.success || !data.data) {
    throw new Error(data.error?.message ?? 'Failed to start recompute')
  }
  return data.data
}

function normalizeDashboard(raw: MarketAssetDashboard): MarketAssetDashboard {
  return {
    ...raw,
    avgWatchlistInstrumentsPerUser: num(raw.avgWatchlistInstrumentsPerUser),
    avgInstrumentsPerPortfolio: num(raw.avgInstrumentsPerPortfolio),
    avgPortfolioWeightPercent: num(raw.avgPortfolioWeightPercent),
    table: {
      ...raw.table,
      content: (raw.table?.content ?? []).map((r) => ({
        ...r,
        avgWeightPercent: num(r.avgWeightPercent) ?? 0,
      })),
    },
  }
}

function num(v: unknown): number | null {
  if (v == null) return null
  const n = typeof v === 'number' ? v : Number(v)
  return Number.isFinite(n) ? n : null
}

export { readMessage as readMarketAssetApiError }
