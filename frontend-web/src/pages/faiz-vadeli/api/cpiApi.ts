import { apiClient } from '../../../shared/api/client'
import { getCachedOrLoad } from './requestCache'

const LATEST_TTL_MS = 30_000
const HISTORY_TTL_MS = 5 * 60_000

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
  return getCachedOrLoad(`faiz-vadeli:cpi:latest:${metric}`, LATEST_TTL_MS, async () => {
    const { data } = await apiClient.get<CpiLatestResponse>('/api/v1/rates/cpi/latest', { params: { metric } })
    return data
  })
}

export async function fetchCpiHistory(
  metric: CpiMetricCode,
  range: '5Y' = '5Y',
): Promise<CpiHistoryResponse> {
  return getCachedOrLoad(`faiz-vadeli:cpi:history:${metric}:${range}`, HISTORY_TTL_MS, async () => {
    const { data } = await apiClient.get<CpiHistoryResponse>('/api/v1/rates/cpi/history', {
      params: { metric, range },
    })
    return data
  })
}
