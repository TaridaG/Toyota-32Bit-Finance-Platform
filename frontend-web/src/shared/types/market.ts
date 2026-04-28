export type MarketCategory = 'all' | 'crypto' | 'stocks' | 'forex' | 'commodities'

export type MarketOverviewItem = {
  symbol: string
  name: string
  price: number
  change24h: number | null
  high24h: number | null
  low24h: number | null
  category: string | null
  instrumentId: number | null
}

export type MarketOverviewPageResponse = {
  content: MarketOverviewItem[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export type MarketInsightsResponse = {
  topGainers: MarketOverviewItem[]
  topLosers: MarketOverviewItem[]
}

export type ApiResponse<T> = {
  success: boolean
  data: T
}
