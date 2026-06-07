import { apiClient } from '../../../shared/api/client'
import { getCachedOrLoad } from './requestCache'
import type { PolicyRateHistoryPoint, PolicyRateHistoryResponse } from './policyRateApi'

const LATEST_TTL_MS = 30_000
const HISTORY_TTL_MS = 5 * 60_000

export type BondYieldLatestResponse = {
  tenor?: string | null
  symbol?: string | null
  value?: number | null
  observationDate?: string | null
  change1dBasisPoints?: number | null
  sourceProvider?: string
  unit?: string
  evdsSeries?: string | null
}

export type BondYieldCurvePoint = {
  tenor?: string | null
  symbol?: string | null
  value?: number | null
  change1dBasisPoints?: number | null
}

export type BondYieldCurveResponse = {
  observationDate?: string | null
  sourceProvider?: string
  unit?: string
  points?: BondYieldCurvePoint[] | null
}

export async function fetchBondYieldLatest(tenor: string): Promise<BondYieldLatestResponse> {
  return getCachedOrLoad(`faiz-vadeli:bond:latest:${tenor}`, LATEST_TTL_MS, async () => {
    const { data } = await apiClient.get<BondYieldLatestResponse>('/api/v1/rates/bond/latest', {
      params: { tenor },
    })
    return data
  })
}

export async function fetchBondYieldHistory(tenor: string): Promise<PolicyRateHistoryResponse> {
  return getCachedOrLoad(`faiz-vadeli:bond:history:${tenor}:5Y:DAILY`, HISTORY_TTL_MS, async () => {
    const { data } = await apiClient.get<PolicyRateHistoryResponse>('/api/v1/rates/bond/history', {
      params: { range: '5Y', tenor },
    })
    return data
  })
}

export async function fetchBondYieldCurveLatest(): Promise<BondYieldCurveResponse> {
  return getCachedOrLoad('faiz-vadeli:bond:curve:latest', LATEST_TTL_MS, async () => {
    const { data } = await apiClient.get<BondYieldCurveResponse>('/api/v1/rates/bond/curve/latest')
    return data
  })
}

export type { PolicyRateHistoryPoint }
