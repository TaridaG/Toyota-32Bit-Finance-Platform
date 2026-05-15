import { apiClient } from '../../../shared/api/client'

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
  const { data } = await apiClient.get<PolicyRateLatestResponse>('/api/rates/policy-rate/latest')
  return data
}

export async function fetchPolicyRateHistory(): Promise<PolicyRateHistoryResponse> {
  const { data } = await apiClient.get<PolicyRateHistoryResponse>('/api/rates/policy-rate/history', {
    params: { range: '5Y', frequency: 'WEEKLY' },
  })
  return data
}
