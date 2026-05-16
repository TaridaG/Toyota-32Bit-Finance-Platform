export type NewsCategory = 'all' | 'bist' | 'viop' | 'fx' | 'crypto' | 'macro'
export type SentimentType = 'positive' | 'negative' | 'neutral'

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
  /** UI topic tags: bist, fx, crypto, macro, viop (may be multiple). */
  topicTags: Exclude<NewsCategory, 'all'>[]
  sentiment: SentimentType
  tags: string[]
  relatedAssets: string[]
  reactionPercent1h: number
  correlationNote: string
  sparkline: number[]
}
