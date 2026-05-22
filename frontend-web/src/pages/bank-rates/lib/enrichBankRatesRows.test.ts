import { describe, expect, it } from 'vitest'
import type { BankRatesRow } from '../api/bankRatesApi'
import { enrichBankRatesRows } from './enrichBankRatesRows'

const sample: BankRatesRow[] = [
  { slug: 'a', name: 'A', buy: 10, sell: 12, spread: 0.01, spreadPercent: 0.1 },
  { slug: 'b', name: 'B', buy: 11, sell: 10, spread: 3, spreadPercent: 5 },
]

describe('enrichBankRatesRows', () => {
  it('marks spread and spread percent extrema', () => {
    const out = enrichBankRatesRows(sample)
    expect(out[0].bestSpreadMin).toBe(true)
    expect(out[0].bestSpreadPctMin).toBe(true)
    expect(out[1].bestSpreadMax).toBe(true)
    expect(out[1].bestSpreadPctMax).toBe(true)
  })
})
