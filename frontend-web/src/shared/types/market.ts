export type MarketCategory = 'all' | 'crypto' | 'stocks' | 'forex' | 'commodities'

export type MarketNativeQuote = 'TRY' | 'USD'

export type MarketOverviewItem = {
  symbol: string
  name: string
  price: number
  /** Quote currency of the raw `price` from the feed (TRY=BIST-style, USD=US equity/crypto). */
  nativeQuote?: MarketNativeQuote
  /** Spot converted into the header-selected currency using TRY crosses from `/api/market/fx`. */
  displayAmount?: number | null
  timestamp?: string | null
  freshness?: 'LIVE' | 'STALE'
  change24h: number | null
  change1D?: number | null
  change1M?: number | null
  change3M?: number | null
  change6M?: number | null
  change1Y?: number | null
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
