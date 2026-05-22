import type { BankRatesRow } from '../api/bankRatesApi'

export type BankRatesSortKey = 'bank' | 'buy' | 'sell' | 'spread' | 'spreadPercent'
export type BankRatesSortDir = 'asc' | 'desc'

export function sortBankRatesRows(
  rows: BankRatesRow[],
  key: BankRatesSortKey,
  dir: BankRatesSortDir,
): BankRatesRow[] {
  const indexed = rows.map((row, index) => ({ row, index }))
  const sign = dir === 'asc' ? 1 : -1

  indexed.sort((a, b) => {
    const cmp = compareRows(a.row, b.row, key)
    if (cmp !== 0) {
      return cmp * sign
    }
    return a.index - b.index
  })

  return indexed.map((x) => x.row)
}

function compareRows(a: BankRatesRow, b: BankRatesRow, key: BankRatesSortKey): number {
  if (key === 'bank') {
    return a.name.localeCompare(b.name, 'tr', { sensitivity: 'base' })
  }
  return compareNum(numFor(a, key), numFor(b, key))
}

function numFor(row: BankRatesRow, key: BankRatesSortKey): number | null {
  switch (key) {
    case 'buy':
      return row.buy ?? null
    case 'sell':
      return row.sell ?? null
    case 'spread':
      return row.spread ?? null
    case 'spreadPercent':
      return row.spreadPercent ?? null
    default:
      return null
  }
}

function compareNum(a: number | null, b: number | null): number {
  if (a == null && b == null) {
    return 0
  }
  if (a == null) {
    return 1
  }
  if (b == null) {
    return -1
  }
  return a - b
}
