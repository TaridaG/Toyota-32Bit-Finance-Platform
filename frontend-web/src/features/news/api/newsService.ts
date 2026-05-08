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
  sourceName: string
  category: string
  publishedAt: string
  sentiment: 'positive' | 'negative' | 'neutral'
  relatedSymbols: string[]
  reactionPercent1h: number | null
}

export type NewsOriginalResponse = {
  id: number
  title: string
  summary: string | null
}

export type NewsFetchFilters = {
  category: 'all' | 'bist' | 'viop' | 'fx' | 'crypto' | 'macro'
  range: 'all' | '1h' | '6h' | '24h'
  sentiment: 'all' | 'positive' | 'negative' | 'neutral'
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

export async function fetchNews(
  page: number,
  size: number,
  language?: string,
  filters?: NewsFetchFilters,
): Promise<PageResponse<NewsApiItem>> {
  const maxAgeMinutes = maxAgeMinutesForRange(filters?.range ?? 'all')
  const response = await apiClient.get<ApiResponse<PageResponse<NewsApiItem>>>('/api/news/enriched', {
    params: {
      page,
      size,
      category: filters?.category && filters.category !== 'all' ? filters.category : undefined,
      sentiment: filters?.sentiment && filters.sentiment !== 'all' ? filters.sentiment : undefined,
      maxAgeMinutes,
    },
    headers: language ? { 'X-Language': language } : undefined,
  })
  return response.data.data
}

export async function fetchNewsOriginal(id: number): Promise<NewsOriginalResponse> {
  const response = await apiClient.get<ApiResponse<NewsOriginalResponse>>(`/api/news/enriched/${id}/original`)
  return response.data.data
}
