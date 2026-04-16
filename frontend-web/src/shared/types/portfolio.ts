export type Portfolio = {
  id: number
  name: string
  baseCurrency: string
}

export type PositionSummary = {
  instrumentId: number
  symbol: string
  quantity: number
  avgCost: number
  currentPrice: number
  marketValue: number
  pnl: number
  pnlPercentage: number
}

export type PortfolioSummary = {
  totalCost: number
  totalMarketValue: number
  totalPnL: number
  totalPnLPercentage: number
  positions: PositionSummary[]
}

export type PortfolioAllocation = {
  symbol: string
  marketValue: number
  percentage: number
}

export type CreatePortfolioPayload = {
  name: string
  baseCurrency?: string
}

export type ApiResponse<T> = {
  success: boolean
  data: T
}

