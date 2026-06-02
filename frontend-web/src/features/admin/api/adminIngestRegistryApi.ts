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

export async function fetchIngestCatalog(): Promise<IngestCatalogItem[]> {
  const { data } = await apiClient.get<IngestCatalogItem[]>('/api/v1/market/ingest/catalog')
  return data ?? []
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

