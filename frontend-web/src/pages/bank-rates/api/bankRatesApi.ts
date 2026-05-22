import { apiClient } from '../../../shared/api/client'
import { enrichBankRatesRows } from '../lib/enrichBankRatesRows'

export type BankRatesAssetCode = 'USD' | 'EUR' | 'GBP' | 'GOLD'

export type BankRatesRow = {
  slug: string
  name: string
  logoUrl?: string | null
  detailUrl?: string | null
  buy?: number | null
  sell?: number | null
  spread?: number | null
  spreadPercent?: number | null
  bestBuy?: boolean
  bestSell?: boolean
  bestSpreadMin?: boolean
  bestSpreadMax?: boolean
  bestSpreadPctMin?: boolean
  bestSpreadPctMax?: boolean
}

export type BankRatesResponse = {
  asset: BankRatesAssetCode
  title?: string | null
  sourceUrl?: string | null
  sourceProvider?: string
  fetchedAt?: string | null
  rows: BankRatesRow[]
}

export async function fetchBankRates(asset: BankRatesAssetCode): Promise<BankRatesResponse> {
  const { data } = await apiClient.get<BankRatesResponse>('/api/rates/bank-rates', {
    params: { asset },
  })
  return {
    ...data,
    rows: enrichBankRatesRows(data.rows ?? []),
  }
}
