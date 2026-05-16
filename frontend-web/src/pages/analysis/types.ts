import type { UTCTimestamp } from 'lightweight-charts'
import type { MarketCategory } from '../../shared/types/market'

export type AssetType = 'stock' | 'crypto' | 'fx' | 'commodity' | 'index' | 'fund'
export type ChartDisplayType = 'candle' | 'line'
export type TimeRange = '1h' | '6h' | '24h' | '7d' | '30d' | '90d' | '1y' | '5y'
export type DrawTool =
  | 'none'
  | 'trendline'
  | 'ray'
  | 'hline'
  | 'vline'
  | 'rect'
  | 'fib'
  | 'point'

export type AssetDefinition = {
  id: string
  symbol: string
  name: string
  type: AssetType
  /** Raw catalog category from market-data (STOCK, FX, CRYPTO, FUND, METAL, …). */
  wireCategory: string
  /** Piyasa sayfası segmenti (BIST, NASDAQ, Kripto, …). */
  marketSegment: MarketCategory
  marketCap?: number
}

export type NewsMatchReason = {
  kind: 'asset' | 'category'
  categoryUi: string
  symbol?: string
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

export type NewsMarkerTone = 'up' | 'down' | 'neutral'

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
  nextDayChangePercent?: number | null
  markerTone?: NewsMarkerTone
  newsCategoryUi?: string
  matchReasons?: NewsMatchReason[]
}

export type ChartAnchor = {
  time: UTCTimestamp
  price: number
}

export type DrawingMarker = {
  type: DrawTool
  color: string
}

/** Chart-anchored drawing (time + price); moves with pan/zoom. */
export type DrawingItem =
  | { id: string; type: 'point'; color: string; anchor: ChartAnchor }
  | { id: string; type: 'hline'; color: string; price: number }
  | { id: string; type: 'vline'; color: string; time: UTCTimestamp }
  | { id: string; type: 'trendline'; color: string; a: ChartAnchor; b: ChartAnchor }
  | { id: string; type: 'ray'; color: string; a: ChartAnchor; b: ChartAnchor }
  | { id: string; type: 'rect'; color: string; a: ChartAnchor; b: ChartAnchor }
  | { id: string; type: 'fib'; color: string; a: ChartAnchor; b: ChartAnchor }
