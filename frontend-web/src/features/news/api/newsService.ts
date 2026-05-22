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
  range: 'all' | '1h' | '6h' | '24h'
}

function maxAgeMinutesForRange(range: NewsFetchFilters['range']): number | undefined {
  switch (range) {
    case '1h':
      return 60
    case '6h':
      return 360
    case '24h':
      return 1440
    default:
      return undefined
  }
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
  options?: { maxAgeMinutes?: number },
): Promise<PageResponse<NewsApiItem>> {
  const maxAgeMinutes = options?.maxAgeMinutes ?? maxAgeMinutesForRange(filters?.range ?? 'all')
  const normalizedSearch = search?.trim()
  const response = await apiClient.get<ApiResponse<PageResponse<NewsApiItem>>>('/api/news/enriched', {
    params: {
      page,
      size,
      category: filters?.category && filters.category !== 'all' ? filters.category : undefined,
      maxAgeMinutes,
      q: normalizedSearch || undefined,
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
