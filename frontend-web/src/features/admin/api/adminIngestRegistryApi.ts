import { apiClient } from '../../../shared/api/client'

export type IngestCatalogItem = {
  instrumentId: number
  symbol: string
  type: string | null
  exchange: string | null
  segment: string
  enabled: boolean
  totalDays: number | null
  recentDays: number | null
  recent30Days: number | null
  lastError: string | null
  lastErrorAt: string | null
}

export type IngestCatalogPage = {
  content: IngestCatalogItem[]
  totalElements: number
  totalPages: number
  page: number
  size: number
}

export async function fetchIngestCatalog(page: number, size = 10): Promise<IngestCatalogPage> {
  const { data } = await apiClient.get<IngestCatalogPage>('/api/v1/market/ingest/catalog', {
    params: { page, size },
  })
  return (
    data ?? {
      content: [],
      totalElements: 0,
      totalPages: 0,
      page: 0,
      size,
    }
  )
}

export async function enableIngest(instrumentId: number, segment: string): Promise<void> {
  await apiClient.post('/api/v1/market/ingest/config/enable', { instrumentId, segment })
}

export async function disableIngest(instrumentId: number, segment: string): Promise<void> {
  await apiClient.post('/api/v1/market/ingest/config/disable', { instrumentId, segment })
}

export async function triggerHistoryPull(instrumentId: number, segment: string): Promise<void> {
  await apiClient.post('/api/v1/market/ingest/actions/history-pull', { instrumentId, segment })
}

export async function triggerLivePull(instrumentId: number, segment: string): Promise<void> {
  await apiClient.post('/api/v1/market/ingest/actions/live-pull', { instrumentId, segment })
}

export async function deleteFromIngest(instrumentId: number, segment: string): Promise<void> {
  await apiClient.post('/api/v1/market/ingest/actions/delete', { instrumentId, segment })
}
