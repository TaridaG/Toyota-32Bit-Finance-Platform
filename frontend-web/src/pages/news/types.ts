export type NewsCategory = 'all' | 'bist' | 'viop' | 'fx' | 'crypto' | 'macro'
export type SentimentType = 'positive' | 'negative' | 'neutral'

export type MarketTickerItem = {
  symbol: string
  price: number
  changePercent: number
  sparkline: number[]
}

export type NewsDataPoint = {
  id: string
  title: string
  summary: string
  titleOriginal?: string
  summaryOriginal?: string
  titleTranslated?: string
  summaryTranslated?: string
  translatedLanguage?: string | null
  translated?: boolean
  details: string
  source: string
  timeAgoMinutes: number
  timeAgoLabel?: string
  category: Exclude<NewsCategory, 'all'>
  sentiment: SentimentType
  tags: string[]
  relatedAssets: string[]
  reactionPercent1h: number
  correlationNote: string
  sparkline: number[]
}

export type TrendItem = {
  asset: string
  changePercent: number
}
