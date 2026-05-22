import { apiClient } from '../../../shared/api/client'

type ApiEnvelope<T> = {
  success: boolean
  data?: T
  error?: { message?: string }
}

export type WatchlistItem = {
  instrumentId: number
  symbol: string
  name?: string | null
  type?: string | null
  createdAt?: string | null
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

export async function fetchWatchlist(): Promise<WatchlistItem[]> {
  const { data } = await apiClient.get<ApiEnvelope<WatchlistItem[]>>('/api/watchlist')
  return assertSuccessData(data)
}

export async function addWatchlistItem(instrumentId: number): Promise<void> {
  const { data } = await apiClient.post<ApiEnvelope<unknown>>('/api/watchlist', { instrumentId })
  assertSuccessOnly(data)
}

export async function removeWatchlistItem(instrumentId: number): Promise<void> {
  const { data } = await apiClient.delete<ApiEnvelope<unknown>>(`/api/watchlist/${instrumentId}`)
  assertSuccessOnly(data)
}
