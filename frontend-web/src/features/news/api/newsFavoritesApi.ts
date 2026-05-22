import { apiClient } from '../../../shared/api/client'
import type { NewsApiItem, NewsFetchFilters } from './newsService'

type PageResponse<T> = {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

type ApiEnvelope<T> = {
  success: boolean
  data?: T
  error?: { message?: string }
}

export type NewsFavoriteItem = {
  newsId: number
}

function assertSuccessData<T>(body: ApiEnvelope<T>): T {
  if (!body.success) {
    throw new Error(body.error?.message ?? 'Request failed')
  }
  return body.data as T
}

function assertSuccessOnly(body: ApiEnvelope<unknown>): void {
  if (!body.success) {
    throw new Error(body.error?.message ?? 'Request failed')
  }
}

export async function fetchNewsFavorites(): Promise<NewsFavoriteItem[]> {
  const { data } = await apiClient.get<ApiEnvelope<NewsFavoriteItem[]>>('/api/news/favorites')
  return assertSuccessData(data)
}

export async function addNewsFavorite(newsId: number): Promise<void> {
  const { data } = await apiClient.post<ApiEnvelope<unknown>>('/api/news/favorites', { newsId })
  assertSuccessOnly(data)
}

export async function removeNewsFavorite(newsId: number): Promise<void> {
  const { data } = await apiClient.delete<ApiEnvelope<unknown>>(`/api/news/favorites/${newsId}`)
  assertSuccessOnly(data)
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

export async function fetchFavoriteNewsEnriched(
  page: number,
  size: number,
  language?: string,
  filters?: NewsFetchFilters,
  search?: string,
): Promise<PageResponse<NewsApiItem>> {
  const maxAgeMinutes = maxAgeMinutesForRange(filters?.range ?? 'all')
  const normalizedSearch = search?.trim()
  const { data } = await apiClient.get<ApiEnvelope<PageResponse<NewsApiItem>>>('/api/news/favorites/enriched', {
    params: {
      page,
      size,
      category: filters?.category && filters.category !== 'all' ? filters.category : undefined,
      maxAgeMinutes,
      q: normalizedSearch || undefined,
    },
    headers: language ? { 'X-Language': language } : undefined,
  })
  return assertSuccessData(data)
}
