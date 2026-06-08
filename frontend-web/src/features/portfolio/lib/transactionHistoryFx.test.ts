import { describe, expect, it } from 'vitest'
import { formatTxFxLegLabel } from './transactionHistoryFx'
import type { TransactionHistoryItem } from '../../../shared/types/portfolio'

function row(overrides: Partial<TransactionHistoryItem>): TransactionHistoryItem {
  return {
    transactionId: 1,
    portfolioId: null,
    portfolioName: null,
    instrumentSymbol: 'AAPL',
    type: 'BUY',
    purchaseMode: 'NOW',
    sourceLabel: null,
    quantity: 1,
    price: 10,
    totalAmount: 10,
    inputCurrency: 'TRY',
    inputAmount: 400,
    fxRateUsed: 0.025,
    acquiredAt: null,
    createdAt: '2026-06-07T12:00:00Z',
    quoteCurrency: 'USD',
    ...overrides,
  }
}

describe('formatTxFxLegLabel', () => {
  it('prefers API-resolved fxDisplay fields', () => {
    const label = formatTxFxLegLabel(
      row({
        fxDisplayFrom: 'USD',
        fxDisplayTo: 'TRY',
        fxDisplayRate: 40.37,
      }),
      'tr-TR',
    )
    expect(label).toContain('USD')
    expect(label).toContain('TRY')
    expect(label).toMatch(/40[,.]37/)
  })

  it('returns dash for domestic TRY stock without display fields', () => {
    const label = formatTxFxLegLabel(
      row({
        instrumentSymbol: 'AKBNK',
        quoteCurrency: 'TRY',
        inputCurrency: 'TRY',
        fxRateUsed: 1,
        fxDisplayFrom: null,
        fxDisplayTo: null,
        fxDisplayRate: null,
      }),
      'tr-TR',
    )
    expect(label).toBe('—')
  })

  it('falls back to legacy fxRateUsed for cross-currency rows', () => {
    const label = formatTxFxLegLabel(
      row({
        fxDisplayFrom: null,
        fxDisplayTo: null,
        fxDisplayRate: null,
        inputCurrency: 'USD',
        quoteCurrency: 'TRY',
        fxRateUsed: 40,
      }),
      'en-US',
    )
    expect(label).toBe('1 USD = 40 TRY')
  })
})
