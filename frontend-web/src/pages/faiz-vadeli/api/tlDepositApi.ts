import { apiClient } from '../../../shared/api/client'

export type TlDepositLatestResponse = {
  value?: number | null
  previousValue?: number | null
  asOfDate?: string | null
  changeVsPrior?: 'UNCHANGED' | 'UP' | 'DOWN' | null
  maturityCode?: string | null
  sourceProvider?: string
  unit?: string
}

export type TlDepositHistoryPoint = {
  date: string
  value: number
  sourceQuality?: string | null
}

export type TlDepositHistoryResponse = {
  symbol?: string
  name?: string
  sourceProvider?: string
  frequency?: string
  unit?: string
  points: TlDepositHistoryPoint[]
}

export async function fetchTlDepositLatest(maturity: string): Promise<TlDepositLatestResponse> {
  const { data } = await apiClient.get<TlDepositLatestResponse>('/api/rates/tl-deposit/latest', {
    params: { maturity },
  })
  return data
}

export async function fetchTlDepositHistory(maturity: string): Promise<TlDepositHistoryResponse> {
  const { data } = await apiClient.get<TlDepositHistoryResponse>('/api/rates/tl-deposit/history', {
    params: { range: '5Y', frequency: 'WEEKLY', maturity },
  })
  return data
}
