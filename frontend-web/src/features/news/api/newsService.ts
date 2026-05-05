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

export async function fetchNews(page: number, size: number, language?: string): Promise<PageResponse<NewsApiItem>> {
  const response = await apiClient.get<ApiResponse<PageResponse<NewsApiItem>>>('/api/news/enriched', {
    params: { page, size },
    headers: language ? { 'X-Language': language } : undefined,
  })
  return response.data.data
}

export async function fetchNewsOriginal(id: number): Promise<NewsOriginalResponse> {
  const response = await apiClient.get<ApiResponse<NewsOriginalResponse>>(`/api/news/enriched/${id}/original`)
  return response.data.data
}
