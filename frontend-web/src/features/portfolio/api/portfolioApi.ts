import { apiClient } from '../../../shared/api/client'
import type {
  ApiResponse,
  CreatePortfolioPayload,
  InstrumentPriceCoverage,
  Portfolio,
  PortfolioAllocation,
  PortfolioOverview,
  PortfolioPerformanceSeries,
  PortfolioValueSnapshot,
  PortfolioTradeFlow,
  PortfolioSummary,
  TradeExecution,
  TradePreview,
  TradePreviewPayload,
  TransactionHistoryFilters,
  TransactionHistoryPage,
  TransactionHistoryItem,
} from '../../../shared/types/portfolio'

export async function getPortfolios() {
  const response = await apiClient.get<ApiResponse<Portfolio[]>>('/api/v1/external/portfolios')
  return response.data.data
}

export async function deletePortfolio(id: number) {
  await apiClient.delete(`/api/v1/external/portfolios/${id}`)
}

export async function patchPortfolioAmountsHidden(id: number, amountsHidden: boolean): Promise<Portfolio> {
  const response = await apiClient.patch<ApiResponse<Portfolio>>(`/api/v1/external/portfolios/${id}`, {
    amountsHidden,
  })
  return response.data.data
}

export async function createPortfolio(payload: CreatePortfolioPayload) {
  const response = await apiClient.post<ApiResponse<Portfolio>>('/api/v1/external/portfolios', payload)
  return response.data.data
}

/** Row from `GET /api/v1/instruments` (finance-api catalog) for trade pickers. */
export type InstrumentCatalogPickRow = {
  id: number
  symbol: string
  name: string
  type: string
  exchange: string
}

/** Same envelope rules as `marketService` `toInstrumentMapPayload` (array vs `{ data: [] }`). */
function extractInstrumentListArray(body: unknown): unknown[] {
  if (Array.isArray(body)) {
    return body
  }
  if (body && typeof body === 'object' && Array.isArray((body as { data?: unknown }).data)) {
    return (body as { data: unknown[] }).data
  }
  return []
}

function parseNumericId(raw: unknown): number {
  if (typeof raw === 'number' && Number.isFinite(raw)) {
    return raw
  }
  if (typeof raw === 'string' && raw.trim().length > 0) {
    const n = Number(raw.trim())
    return Number.isFinite(n) ? n : Number.NaN
  }
  return Number.NaN
}

function parseInstrumentListPayload(body: unknown): InstrumentCatalogPickRow[] {
  const arr = extractInstrumentListArray(body)
  const out: InstrumentCatalogPickRow[] = []
  for (const row of arr) {
    if (!row || typeof row !== 'object') continue
    const r = row as Record<string, unknown>
    const rawId = r.id ?? r.instrumentId
    const id = parseNumericId(rawId)
    const symbol = typeof r.symbol === 'string' ? r.symbol.trim().toUpperCase() : ''
    if (!symbol || !Number.isFinite(id)) continue
    const exchangeRaw = r.exchange
    const exchange =
      typeof exchangeRaw === 'string'
        ? exchangeRaw.trim().toUpperCase()
        : exchangeRaw != null && typeof exchangeRaw === 'object' && 'name' in (exchangeRaw as object)
          ? String((exchangeRaw as { name?: unknown }).name ?? '')
            .trim()
            .toUpperCase()
          : ''
    out.push({
      id,
      symbol,
      name: typeof r.name === 'string' && r.name.trim().length > 0 ? r.name.trim() : symbol,
      type: typeof r.type === 'string' ? r.type.trim().toUpperCase() : 'STOCK',
      exchange,
    })
  }
  return out
}

/** Full instrument catalog for UI pickers (ids always present; not tied to live wire symbol merge). */
export async function getInstrumentsCatalogForTradePicker(): Promise<InstrumentCatalogPickRow[]> {
  const response = await apiClient.get<unknown>('/api/v1/instruments')
  return parseInstrumentListPayload(response.data)
}

export async function getPortfolioSummary(id: number) {
  const response = await apiClient.get<ApiResponse<PortfolioSummary>>(
    `/api/v1/external/portfolios/${id}/summary`,
  )
  return response.data.data
}

export async function getPortfolioAllocation(id: number) {
  const response = await apiClient.get<ApiResponse<PortfolioAllocation[]>>(
    `/api/v1/external/portfolios/${id}/allocation`,
  )
  return response.data.data
}

export async function previewTrade(payload: TradePreviewPayload) {
  const response = await apiClient.post<ApiResponse<TradePreview>>('/api/v1/trades/preview', payload)
  return response.data.data
}

export async function buyTrade(payload: TradePreviewPayload) {
  const response = await apiClient.post<ApiResponse<TradeExecution>>('/api/v1/trades/buy/order', payload)
  return response.data.data
}

export async function getTransactionHistory(portfolioId?: number | null) {
  const query = portfolioId != null ? `?portfolioId=${portfolioId}` : ''
  const response = await apiClient.get<ApiResponse<TransactionHistoryItem[]>>(`/api/v1/history/transactions${query}`)
  return response.data.data
}

export async function getTransactionHistoryPage(page: number, size: number, filters: TransactionHistoryFilters, portfolioId?: number | null) {
  const params = new URLSearchParams()
  params.set('page', String(page))
  params.set('size', String(size))
  if (portfolioId != null) params.set('portfolioId', String(portfolioId))
  if (filters.symbol) params.set('symbol', filters.symbol)
  if (filters.type) params.set('type', filters.type)
  if (filters.purchaseMode) params.set('purchaseMode', filters.purchaseMode)
  if (filters.inputCurrency) params.set('inputCurrency', filters.inputCurrency)
  if (filters.fromDate) params.set('fromDate', filters.fromDate)
  if (filters.toDate) params.set('toDate', filters.toDate)
  const response = await apiClient.get<ApiResponse<TransactionHistoryPage>>(`/api/v1/history/transactions/page?${params.toString()}`)
  return response.data.data
}

export async function getMyPortfolioOverview(portfolioId?: number | null, displayCurrency?: string | null) {
  const query = portfolioId != null ? `?portfolioId=${portfolioId}` : ''
  const headers =
    displayCurrency != null && displayCurrency.trim().length > 0
      ? { 'X-Currency': displayCurrency.trim().toUpperCase() }
      : undefined
  const response = await apiClient.get<ApiResponse<PortfolioOverview>>(`/api/v1/portfolio/overview${query}`, { headers })
  return response.data.data
}

export async function getPortfolioSnapshots(portfolioId: number) {
  const response = await apiClient.get<ApiResponse<PortfolioValueSnapshot[]>>(
    `/api/v1/portfolio/snapshots?portfolioId=${portfolioId}`,
  )
  return response.data.data
}

export async function getPortfolioTradeFlow(portfolioId: number | null, displayCurrency?: string | null) {
  const headers =
    displayCurrency != null && displayCurrency.trim().length > 0
      ? { 'X-Currency': displayCurrency.trim().toUpperCase() }
      : undefined
  const qs = portfolioId != null ? `?portfolioId=${portfolioId}` : ''
  const response = await apiClient.get<ApiResponse<PortfolioTradeFlow>>(`/api/v1/portfolio/trade-flow${qs}`, { headers })
  return response.data.data
}

export async function getPortfolioPerformanceSeries(
  portfolioId: number | null,
  displayCurrency?: string | null,
  range: '1w' | '1m' | '3m' | '6m' | '1y' | 'all' = 'all',
) {
  const headers =
    displayCurrency != null && displayCurrency.trim().length > 0
      ? { 'X-Currency': displayCurrency.trim().toUpperCase() }
      : undefined
  const params = new URLSearchParams()
  params.set('range', range)
  if (portfolioId != null) params.set('portfolioId', String(portfolioId))
  const qs = `?${params.toString()}`
  const response = await apiClient.get<ApiResponse<PortfolioPerformanceSeries>>(`/api/v1/portfolio/performance-series${qs}`, { headers })
  return response.data.data
}

export async function getInstrumentPriceCoverage(instrumentId: number) {
  const response = await apiClient.get<ApiResponse<InstrumentPriceCoverage>>(
    `/api/v1/trades/instruments/${instrumentId}/price-coverage`,
  )
  return response.data.data
}

