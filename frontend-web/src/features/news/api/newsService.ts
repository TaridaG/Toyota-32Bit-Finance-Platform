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
  sourceName: string
  category: string
  publishedAt: string
  sentiment: 'positive' | 'negative' | 'neutral'
  relatedSymbols: string[]
  reactionPercent1h: number | null
}

export async function fetchNews(page: number, size: number): Promise<PageResponse<NewsApiItem>> {
  const response = await apiClient.get<ApiResponse<PageResponse<NewsApiItem>>>('/api/news/enriched', {
    params: { page, size },
  })
  return response.data.data
}
