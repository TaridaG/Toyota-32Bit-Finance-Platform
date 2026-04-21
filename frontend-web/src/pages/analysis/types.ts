import type { UTCTimestamp } from 'lightweight-charts'

export type AssetType = 'stock' | 'crypto' | 'fx' | 'commodity' | 'index'
export type TimeRange = '1D' | '1W' | '1M' | '3M' | '1Y' | 'ALL'
export type DrawTool = 'none' | 'trendline' | 'point' | 'hline'

export type AssetDefinition = {
  id: string
  symbol: string
  name: string
  type: AssetType
  marketCap?: number
}

export type CandlePoint = {
  time: UTCTimestamp
  open: number
  high: number
  low: number
  close: number
  volume: number
}

/** Simulated execution / blotter event for chart markers (distinct from news). */
export type ChartTradeEvent = {
  id: string
  time: UTCTimestamp
  title: string
  side: 'buy' | 'sell'
}

export type AssetNewsItem = {
  id: string
  assetId: string
  title: string
  summary: string
  source: string
  impact: 'positive' | 'negative' | 'neutral'
  createdAt: UTCTimestamp
  reactionPercent1h: number
  relatedAssets: string[]
}

export type DrawingItem =
  | { id: string; type: 'point'; x: number; y: number }
  | { id: string; type: 'hline'; y: number }
  | { id: string; type: 'trendline'; x1: number; y1: number; x2: number; y2: number }
