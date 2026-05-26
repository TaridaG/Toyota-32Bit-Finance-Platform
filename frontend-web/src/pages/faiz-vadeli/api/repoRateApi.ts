import { apiClient } from '../../../shared/api/client'
import type { PolicyRateHistoryPoint, PolicyRateHistoryResponse } from './policyRateApi'
import { getCachedOrLoad } from './requestCache'

const LATEST_TTL_MS = 30_000
const HISTORY_TTL_MS = 5 * 60_000

export type RepoRateLatestResponse = {
  value?: number | null
  observationDate?: string | null
  change1dBasisPoints?: number | null
  sourceProvider?: string
  unit?: string
  evdsSeries?: string
}

export async function fetchRepoRateLatest(): Promise<RepoRateLatestResponse> {
  return getCachedOrLoad('faiz-vadeli:repo:latest', LATEST_TTL_MS, async () => {
    const { data } = await apiClient.get<RepoRateLatestResponse>('/api/rates/repo/latest')
    return data
  })
}

export async function fetchRepoRateHistory(): Promise<PolicyRateHistoryResponse> {
  return getCachedOrLoad('faiz-vadeli:repo:history:5Y', HISTORY_TTL_MS, async () => {
    const { data } = await apiClient.get<PolicyRateHistoryResponse>('/api/rates/repo/history', {
      params: { range: '5Y' },
    })
    return data
  })
}

export type { PolicyRateHistoryPoint }
