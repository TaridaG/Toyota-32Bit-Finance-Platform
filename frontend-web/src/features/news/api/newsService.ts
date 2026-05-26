import { apiClient } from '../../../shared/api/client'

type ApiResponse<T> = {
  success: boolean
  data: T
}

type PageResponse<T> = {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export type NewsApiItem = {
  id: number
  title: string
  summary: string | null
  titleOriginal?: string
  summaryOriginal?: string | null
  translatedLanguage?: string | null
  translated?: boolean
  imageUrl?: string | null
  sourceName: string
  category: string
  publishedAt: string
  /** Present in API; not shown on news page UI. */
  sentiment?: 'positive' | 'negative' | 'neutral'
  relatedSymbols: string[]
  topicTags?: string[]
  reactionPercent1h?: number | null
}

export type NewsOriginalResponse = {
  id: number
  title: string
  summary: string | null
}

export type NewsFetchFilters = {
  category: 'all' | 'bist' | 'viop' | 'fx' | 'crypto' | 'macro'
  range: 'all' | '1h' | '6h' | '24h' | '7d'
}

function maxAgeMinutesForRange(range: NewsFetchFilters['range']): number | undefined {
  switch (range) {
    case '1h':
      return 60
    case '6h':
      return 360
    case '24h':
      return 1440
    case '7d':
      return 7 * 24 * 60
    default:
      return undefined
  }
}

export type NewsFetchOptions = {
  maxAgeMinutes?: number
  relatedSymbols?: string[]
  sourceName?: string
  assetKey?: string
  primaryTopic?: 'bist' | 'viop' | 'fx' | 'crypto' | 'macro'
}

export type NewsWeeklyTopicRow = {
  key: 'bist' | 'viop' | 'fx' | 'crypto' | 'macro'
  count: number
  percent: number
}

export type NewsWeeklyAssetRow = {
  symbol: string
  count: number
}

export type NewsWeeklySourceRow = {
  name: string
  count: number
}

export type NewsWeeklySummaryResponse = {
  totalCount: number
  topics: NewsWeeklyTopicRow[]
  topAssets: NewsWeeklyAssetRow[]
  sources: NewsWeeklySourceRow[]
  portfolioRelatedCount: number
}

export type FetchChartNewsParams = {
  symbol: string
  categoryUi: string
  fromSec: number
  toSec: number
  language?: string
}

/** Loads enriched news for the visible chart window (symbol/category + published date range). */
export async function fetchNewsForChart(params: FetchChartNewsParams): Promise<NewsApiItem[]> {
  const from = new Date(params.fromSec * 1000).toISOString()
  const to = new Date(params.toSec * 1000).toISOString()
  const response = await apiClient.get<ApiResponse<NewsApiItem[]>>('/api/news/enriched/chart', {
    params: {
      symbol: params.symbol.replace('/', '').toUpperCase(),
      category: params.categoryUi,
      from,
      to,
    },
    headers: params.language ? { 'X-Language': params.language } : undefined,
  })
  return response.data.data ?? []
}

export async function fetchNews(
  page: number,
  size: number,
  language?: string,
  filters?: NewsFetchFilters,
  search?: string,
  options?: NewsFetchOptions,
): Promise<PageResponse<NewsApiItem>> {
  const maxAgeMinutes = options?.maxAgeMinutes ?? maxAgeMinutesForRange(filters?.range ?? 'all')
  const normalizedSearch = search?.trim()
  const relatedSymbols = (options?.relatedSymbols ?? [])
    .map((symbol) => symbol.trim())
    .filter(Boolean)
    .join(',')
  const response = await apiClient.get<ApiResponse<PageResponse<NewsApiItem>>>('/api/news/enriched', {
    params: {
      page,
      size,
      category: filters?.category && filters.category !== 'all' ? filters.category : undefined,
      maxAgeMinutes,
      q: normalizedSearch || undefined,
      relatedSymbols: relatedSymbols || undefined,
      sourceName: options?.sourceName?.trim() || undefined,
      assetKey: options?.assetKey?.trim() || undefined,
      primaryTopic: options?.primaryTopic || undefined,
    },
    headers: language ? { 'X-Language': language } : undefined,
  })
  return response.data.data
}

export async function fetchNewsWeeklySummary(
  language?: string,
  portfolioSymbols?: string[],
): Promise<NewsWeeklySummaryResponse> {
  const normalizedPortfolioSymbols = (portfolioSymbols ?? [])
    .map((symbol) => symbol.trim())
    .filter(Boolean)
    .join(',')
  const response = await apiClient.get<ApiResponse<NewsWeeklySummaryResponse>>('/api/news/enriched/weekly-summary', {
    params: {
      portfolioSymbols: normalizedPortfolioSymbols || undefined,
    },
    headers: language ? { 'X-Language': language } : undefined,
  })
  return response.data.data
}

export async function fetchNewsOriginal(id: number): Promise<NewsOriginalResponse> {
  const response = await apiClient.get<ApiResponse<NewsOriginalResponse>>(`/api/news/enriched/${id}/original`)
  return response.data.data
}

export type NewsRelatedAssetPerformance = {
  symbol: string
  name: string
  price: number | null
  change1d: number | null
  change1w: number | null
  change1m: number | null
  change3m: number | null
  change6m: number | null
  change1y: number | null
}

export type NewsDetailApi = {
  id: number
  title: string
  summary: string | null
  titleOriginal?: string | null
  summaryOriginal?: string | null
  translatedLanguage?: string | null
  translated?: boolean
  articleUrl: string
  imageUrl?: string | null
  sourceName: string
  category: string
  categoryUi: string
  publishedAt: string
  sentiment?: 'positive' | 'negative' | 'neutral'
  relatedSymbols: string[]
  topicTags?: string[]
  relatedAssets: NewsRelatedAssetPerformance[]
}

export async function fetchNewsDetail(id: number, language?: string): Promise<NewsDetailApi> {
  const response = await apiClient.get<ApiResponse<NewsDetailApi>>(`/api/news/enriched/${id}`, {
    headers: language ? { 'X-Language': language } : undefined,
  })
  return response.data.data
}
