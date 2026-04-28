import { apiClient } from '../../../shared/api/client'
import type { ApiResponse, MarketCategory, MarketInsightsResponse, MarketOverviewPageResponse } from '../../../shared/types/market'

type FetchMarketsParams = {
  page: number
  size: number
  category?: MarketCategory
  query?: string
  sort?: string
}

const categoryToQueryParam: Record<Exclude<MarketCategory, 'all'>, string> = {
  crypto: 'CRYPTO',
  stocks: 'STOCK',
  forex: 'FX',
  commodities: 'FUND',
}

export async function fetchMarketOverview(params: FetchMarketsParams): Promise<MarketOverviewPageResponse> {
  const category = params.category
  const query = params.query?.trim()
  const sort = params.sort?.trim()

  const response = await apiClient.get<ApiResponse<MarketOverviewPageResponse>>('/api/market/overview', {
    params: {
      page: params.page,
      size: params.size,
      ...(category && category !== 'all'
        ? { category: categoryToQueryParam[category] }
        : {}),
      ...(query ? { q: query } : {}),
      ...(sort ? { sort } : {}),
    },
  })

  return response.data.data
}

export async function fetchMarketInsights(): Promise<MarketInsightsResponse> {
  const response = await apiClient.get<ApiResponse<MarketInsightsResponse>>('/api/market/insights')
  return response.data.data
}
