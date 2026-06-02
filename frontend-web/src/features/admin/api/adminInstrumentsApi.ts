import { apiClient } from '../../../shared/api/client'
import { isAxiosError } from 'axios'

type ApiResponse<T> = {
  success?: boolean
  data?: T
  error?: { message?: string }
}

export type AdminInstrumentType =
  | 'CRYPTO'
  | 'STOCK'
  | 'FX'
  | 'FUND'
  | 'BOND'
  | 'EUROBOND'
  | 'DEPOSIT'

export type AdminExchange = 'BINANCE' | 'BIST' | 'YAHOO' | 'TEFAS' | 'TCMB' | 'NASDAQ' | 'FINNHUB'
export type AdminIngestSegment = 'CRYPTO' | 'BIST' | 'NASDAQ'

export type CreateAdminInstrumentPayload = {
  symbol: string
  name: string
  type: AdminInstrumentType
  exchange: AdminExchange
  segment?: AdminIngestSegment | ''
}

export async function createAdminInstrument(payload: CreateAdminInstrumentPayload): Promise<number> {
  try {
    const { data } = await apiClient.post<ApiResponse<number>>('/api/v1/admin/instruments', payload)
    if (!data?.success && data?.error?.message) {
      throw new Error(data.error.message)
    }
    return Number(data?.data ?? 0)
  } catch (error) {
    if (isAxiosError(error)) {
      const body = error.response?.data as ApiResponse<number> | undefined
      if (body?.error?.message) {
        throw new Error(body.error.message)
      }
      if (error.response?.status != null) {
        throw new Error(`HTTP ${error.response.status}`)
      }
    }
    throw error
  }
}
