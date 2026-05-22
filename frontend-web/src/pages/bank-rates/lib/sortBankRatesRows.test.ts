import { describe, expect, it } from 'vitest'
import type { BankRatesRow } from '../api/bankRatesApi'
import { sortBankRatesRows } from './sortBankRatesRows'

const rows: BankRatesRow[] = [
  { slug: 'b', name: 'Beta', buy: 2, sell: 3, spread: 1, spreadPercent: 0.5 },
  { slug: 'a', name: 'Alpha', buy: 1, sell: 4, spread: 3, spreadPercent: 1.2 },
]

describe('sortBankRatesRows', () => {
  it('sorts bank names ascending', () => {
    const out = sortBankRatesRows(rows, 'bank', 'asc')
    expect(out.map((r) => r.slug)).toEqual(['a', 'b'])
  })

  it('sorts buy descending', () => {
    const out = sortBankRatesRows(rows, 'buy', 'desc')
    expect(out.map((r) => r.buy)).toEqual([2, 1])
  })
})
