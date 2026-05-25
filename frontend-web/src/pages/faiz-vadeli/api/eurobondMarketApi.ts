import { apiClient } from '../../../shared/api/client'
import { getCachedOrLoad } from './requestCache'

const INSTRUMENTS_TTL_MS = 30_000
const HISTORY_TTL_MS = 5 * 60_000
const CASHFLOW_TTL_MS = 60_000

export type EurobondInstrumentWire = {
  displayName?: string | null
  isin?: string | null
  symbol?: string | null
  name?: string | null
  issuer?: string | null
  currency?: string | null
  maturityDate?: string | null
  remainingYears?: number | string | null
  couponPercent?: number | string | null
  couponFrequency?: string | null
  cleanPrice?: number | string | null
  bidPrice?: number | string | null
  askPrice?: number | string | null
  yieldToMaturityPercent?: number | string | null
  dailyChangePercent?: number | string | null
  sourceProvider?: string | null
  lastUpdatedAt?: string | null
}

export type EurobondHistoryPointWire = {
  date?: string | null
  closePrice?: number | string | null
  openPrice?: number | string | null
  highPrice?: number | string | null
  lowPrice?: number | string | null
  closeYieldPercent?: number | string | null
  openYieldPercent?: number | string | null
  highYieldPercent?: number | string | null
  lowYieldPercent?: number | string | null
  changePercent?: number | string | null
  sourceProvider?: string | null
}

export type EurobondHistoryWire = {
  isin?: string | null
  range?: string | null
  frequency?: string | null
  points?: EurobondHistoryPointWire[] | null
}

export type EurobondCashflowWire = {
  isin?: string | null
  nominalAmount?: number | string | null
  couponPercent?: number | string | null
  annualCouponUsd?: number | string | null
  semiAnnualCouponUsd?: number | string | null
  approximatePurchaseAmountUsd?: number | string | null
  maturityPrincipalUsd?: number | string | null
  disclaimer?: string | null
}

type ApiEnvelope<T> = { success?: boolean; data?: T | null; error?: { message?: string } }

function unwrapData<T>(raw: unknown): T | null {
  if (raw == null || typeof raw !== 'object') return null
  const o = raw as ApiEnvelope<T>
  if (o.success === false) return null
  return (o.data ?? null) as T | null
}

export async function fetchTrEurobondInstruments(): Promise<EurobondInstrumentWire[]> {
  return getCachedOrLoad('faiz-vadeli:eurobond:instruments', INSTRUMENTS_TTL_MS, async () => {
    const { data } = await apiClient.get<ApiEnvelope<EurobondInstrumentWire[]>>('/api/market/eurobonds/tr/instruments')
    const list = unwrapData<EurobondInstrumentWire[]>(data)
    return Array.isArray(list) ? list : []
  })
}

export async function fetchTrEurobondHistory(
  isin: string,
  range: '1Y' | '5Y' | 'ALL',
  frequency: 'DAILY' | 'WEEKLY' | 'MONTHLY',
): Promise<EurobondHistoryWire | null> {
  return getCachedOrLoad(`faiz-vadeli:eurobond:history:${isin}:${range}:${frequency}`, HISTORY_TTL_MS, async () => {
    const { data } = await apiClient.get<ApiEnvelope<EurobondHistoryWire>>(
      `/api/market/eurobonds/tr/instruments/${encodeURIComponent(isin)}/history`,
      { params: { range, frequency } },
    )
    return unwrapData<EurobondHistoryWire>(data)
  })
}

export async function fetchTrEurobondCashflow(isin: string, nominal: number): Promise<EurobondCashflowWire | null> {
  return getCachedOrLoad(`faiz-vadeli:eurobond:cashflow:${isin}:${nominal}`, CASHFLOW_TTL_MS, async () => {
    const { data } = await apiClient.get<ApiEnvelope<EurobondCashflowWire>>('/api/market/eurobonds/tr/cashflow', {
      params: { isin, nominal },
    })
    return unwrapData<EurobondCashflowWire>(data)
  })
}

export function toNum(v: number | string | null | undefined): number | null {
  if (v == null) return null
  if (typeof v === 'number') return Number.isFinite(v) ? v : null
  const n = Number(String(v).replace(',', '.'))
  return Number.isFinite(n) ? n : null
}
