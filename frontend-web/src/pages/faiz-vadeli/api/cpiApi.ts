import { apiClient } from '../../../shared/api/client'

export type CpiMetricCode = 'INDEX' | 'MONTHLY_PCT' | 'YEARLY_PCT'

export type CpiLatestResponse = {
  metric?: CpiMetricCode | null
  value?: number | null
  observationMonth?: string | null
  deltaVsPriorMonth?: number | null
  sourceProvider?: string
  unit?: 'PERCENT' | 'INDEX' | string
}

export type CpiHistoryPoint = {
  date: string
  value: number
  sourceQuality?: string | null
}

export type CpiHistoryResponse = {
  frequency?: string
  unit?: string
  sourceProvider?: string
  points: CpiHistoryPoint[]
}

export async function fetchCpiLatest(metric: CpiMetricCode = 'YEARLY_PCT'): Promise<CpiLatestResponse> {
  const { data } = await apiClient.get<CpiLatestResponse>('/api/rates/cpi/latest', { params: { metric } })
  return data
}

export async function fetchCpiHistory(
  metric: CpiMetricCode,
  range: '5Y' = '5Y',
): Promise<CpiHistoryResponse> {
  const { data } = await apiClient.get<CpiHistoryResponse>('/api/rates/cpi/history', {
    params: { metric, range },
  })
  return data
}
