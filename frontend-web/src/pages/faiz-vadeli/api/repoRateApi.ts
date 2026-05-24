import { apiClient } from '../../../shared/api/client'
import type { PolicyRateHistoryPoint, PolicyRateHistoryResponse } from './policyRateApi'

export type RepoRateLatestResponse = {
  value?: number | null
  observationDate?: string | null
  change1dBasisPoints?: number | null
  sourceProvider?: string
  unit?: string
  evdsSeries?: string
}

export async function fetchRepoRateLatest(): Promise<RepoRateLatestResponse> {
  const { data } = await apiClient.get<RepoRateLatestResponse>('/api/rates/repo/latest')
  return data
}

export async function fetchRepoRateHistory(): Promise<PolicyRateHistoryResponse> {
  const { data } = await apiClient.get<PolicyRateHistoryResponse>('/api/rates/repo/history', {
    params: { range: '5Y' },
  })
  return data
}

export type { PolicyRateHistoryPoint }
