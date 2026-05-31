import { apiClient } from '../../../shared/api/client'
import { getCachedOrLoad } from './requestCache'

const LATEST_TTL_MS = 30_000
const HISTORY_TTL_MS = 5 * 60_000

export type PolicyRateLatestResponse = {
  value?: number | null
  decisionDate?: string | null
  changeVsPrior?: 'UNCHANGED' | 'UP' | 'DOWN' | null
  sourceProvider?: string
  unit?: string
}

export type PolicyRateHistoryPoint = {
  date: string
  value: number
  sourceQuality?: string | null
}

export type PolicyRateHistoryResponse = {
  symbol?: string
  name?: string
  sourceProvider?: string
  frequency?: string
  unit?: string
  points: PolicyRateHistoryPoint[]
}

export async function fetchPolicyRateLatest(): Promise<PolicyRateLatestResponse> {
  return getCachedOrLoad('faiz-vadeli:policy-rate:latest', LATEST_TTL_MS, async () => {
    const { data } = await apiClient.get<PolicyRateLatestResponse>('/api/v1/rates/policy-rate/latest')
    return data
  })
}

export async function fetchPolicyRateHistory(): Promise<PolicyRateHistoryResponse> {
  return getCachedOrLoad('faiz-vadeli:policy-rate:history:5Y:WEEKLY', HISTORY_TTL_MS, async () => {
    const { data } = await apiClient.get<PolicyRateHistoryResponse>('/api/v1/rates/policy-rate/history', {
      params: { range: '5Y', frequency: 'WEEKLY' },
    })
    return data
  })
}
