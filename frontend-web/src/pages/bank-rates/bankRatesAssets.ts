import type { BankRatesAssetCode } from './api/bankRatesApi'

export type BankRatesTabId = 'usd' | 'eur' | 'gold' | 'gbp'

export const BANK_RATES_TAB_ASSET: Record<BankRatesTabId, BankRatesAssetCode> = {
  usd: 'USD',
  eur: 'EUR',
  gold: 'GOLD',
  gbp: 'GBP',
}

export const BANK_RATES_TABS: BankRatesTabId[] = ['usd', 'eur', 'gbp', 'gold']
