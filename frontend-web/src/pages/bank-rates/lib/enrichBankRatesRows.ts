import type { BankRatesRow } from '../api/bankRatesApi'

/** API bayrakları eksik olsa bile satır yıldızlarını istemci tarafında hesaplar. */
export function enrichBankRatesRows(rows: BankRatesRow[]): BankRatesRow[] {
  if (rows.length === 0) {
    return rows
  }

  const out = rows.map((r) => ({ ...r }))

  markMax(out, (r) => r.buy, (r, on) => {
    r.bestBuy = on
  })
  markMin(out, (r) => r.sell, (r, on) => {
    r.bestSell = on
  })
  markMinMax(out, (r) => r.spread, (r, min, max) => {
    r.bestSpreadMin = min
    r.bestSpreadMax = max
  })
  markMinMax(out, (r) => r.spreadPercent, (r, min, max) => {
    r.bestSpreadPctMin = min
    r.bestSpreadPctMax = max
  })

  return out
}

function markMax(
  rows: BankRatesRow[],
  value: (row: BankRatesRow) => number | null | undefined,
  apply: (row: BankRatesRow, on: boolean) => void,
) {
  const max = extremum(rows, value, 'max')
  if (max == null) {
    return
  }
  for (const row of rows) {
    const v = value(row)
    apply(row, v != null && !Number.isNaN(v) && v === max)
  }
}

function markMin(
  rows: BankRatesRow[],
  value: (row: BankRatesRow) => number | null | undefined,
  apply: (row: BankRatesRow, on: boolean) => void,
) {
  const min = extremum(rows, value, 'min')
  if (min == null) {
    return
  }
  for (const row of rows) {
    const v = value(row)
    apply(row, v != null && !Number.isNaN(v) && v === min)
  }
}

function markMinMax(
  rows: BankRatesRow[],
  value: (row: BankRatesRow) => number | null | undefined,
  apply: (row: BankRatesRow, min: boolean, max: boolean) => void,
) {
  const min = extremum(rows, value, 'min')
  const max = extremum(rows, value, 'max')
  if (min == null || max == null) {
    return
  }
  for (const row of rows) {
    const v = value(row)
    if (v == null || Number.isNaN(v)) {
      apply(row, false, false)
      continue
    }
    apply(row, v === min, v === max)
  }
}

function extremum(
  rows: BankRatesRow[],
  value: (row: BankRatesRow) => number | null | undefined,
  kind: 'min' | 'max',
): number | null {
  let result: number | null = null
  for (const row of rows) {
    const v = value(row)
    if (v == null || Number.isNaN(v)) {
      continue
    }
    if (result == null) {
      result = v
      continue
    }
    result = kind === 'min' ? Math.min(result, v) : Math.max(result, v)
  }
  return result
}
