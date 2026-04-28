import { apiClient } from '../../../shared/api/client'
import type {
  ApiResponse,
  CreatePortfolioPayload,
  Portfolio,
  PortfolioAllocation,
  PortfolioSummary,
} from '../../../shared/types/portfolio'

export async function getPortfolios() {
  const response = await apiClient.get<ApiResponse<Portfolio[]>>('/api/external/portfolios')
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

