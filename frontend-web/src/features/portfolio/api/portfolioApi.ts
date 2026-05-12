import { apiClient } from '../../../shared/api/client'
import type {
  ApiResponse,
  CreatePortfolioPayload,
  InstrumentPriceCoverage,
  Portfolio,
  PortfolioAllocation,
  PortfolioOverview,
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
  const response = await apiClient.get<ApiResponse<Portfolio[]>>('/api/external/portfolios')
  return response.data.data
}

export async function deletePortfolio(id: number) {
  await apiClient.delete(`/api/external/portfolios/${id}`)
}

export async function patchPortfolioAmountsHidden(id: number, amountsHidden: boolean): Promise<Portfolio> {
  const response = await apiClient.patch<ApiResponse<Portfolio>>(`/api/external/portfolios/${id}`, {
    amountsHidden,
  })
  return response.data.data
}

export async function createPortfolio(payload: CreatePortfolioPayload) {
  const response = await apiClient.post<ApiResponse<Portfolio>>('/api/external/portfolios', payload)
  return response.data.data
}

export async function getPortfolioSummary(id: number) {
  const response = await apiClient.get<ApiResponse<PortfolioSummary>>(
    `/api/external/portfolios/${id}/summary`,
  )
  return response.data.data
}

export async function getPortfolioAllocation(id: number) {
  const response = await apiClient.get<ApiResponse<PortfolioAllocation[]>>(
    `/api/external/portfolios/${id}/allocation`,
  )
  return response.data.data
}

export async function previewTrade(payload: TradePreviewPayload) {
  const response = await apiClient.post<ApiResponse<TradePreview>>('/api/trades/preview', payload)
  return response.data.data
}

export async function buyTrade(payload: TradePreviewPayload) {
  const response = await apiClient.post<ApiResponse<TradeExecution>>('/api/trades/buy/order', payload)
  return response.data.data
}

export async function getTransactionHistory(portfolioId?: number | null) {
  const query = portfolioId != null ? `?portfolioId=${portfolioId}` : ''
  const response = await apiClient.get<ApiResponse<TransactionHistoryItem[]>>(`/api/history/transactions${query}`)
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
  const response = await apiClient.get<ApiResponse<TransactionHistoryPage>>(`/api/history/transactions/page?${params.toString()}`)
  return response.data.data
}

export async function getMyPortfolioOverview(portfolioId?: number | null, displayCurrency?: string | null) {
  const query = portfolioId != null ? `?portfolioId=${portfolioId}` : ''
  const headers =
    displayCurrency != null && displayCurrency.trim().length > 0
      ? { 'X-Currency': displayCurrency.trim().toUpperCase() }
      : undefined
  const response = await apiClient.get<ApiResponse<PortfolioOverview>>(`/api/portfolio/overview${query}`, { headers })
  return response.data.data
}

export async function getPortfolioSnapshots(portfolioId: number) {
  const response = await apiClient.get<ApiResponse<PortfolioValueSnapshot[]>>(
    `/api/portfolio/snapshots?portfolioId=${portfolioId}`,
  )
  return response.data.data
}

export async function getPortfolioTradeFlow(portfolioId: number, displayCurrency?: string | null) {
  const headers =
    displayCurrency != null && displayCurrency.trim().length > 0
      ? { 'X-Currency': displayCurrency.trim().toUpperCase() }
      : undefined
  const response = await apiClient.get<ApiResponse<PortfolioTradeFlow>>(
    `/api/portfolio/trade-flow?portfolioId=${portfolioId}`,
    { headers },
  )
  return response.data.data
}

export async function getInstrumentPriceCoverage(instrumentId: number) {
  const response = await apiClient.get<ApiResponse<InstrumentPriceCoverage>>(
    `/api/trades/instruments/${instrumentId}/price-coverage`,
  )
  return response.data.data
}

