import type { BankRatesRow } from '../api/bankRatesApi'

export function pickBestBuyRows(rows: BankRatesRow[]): BankRatesRow[] {
  return rows.filter((r) => r.bestBuy)
}

export function pickBestSellRows(rows: BankRatesRow[]): BankRatesRow[] {
  return rows.filter((r) => r.bestSell)
}

export function pickBestBuyPrice(rows: BankRatesRow[]): number | null {
  const row = pickBestBuyRow(rows)
  return row?.buy ?? null
}

export function pickBestSellPrice(rows: BankRatesRow[]): number | null {
  const row = pickBestSellRow(rows)
  return row?.sell ?? null
}

/** İlk eşit-en-iyi alış satırı (kart önizlemesi için logo + fiyat). */
export function pickBestBuyRow(rows: BankRatesRow[]): BankRatesRow | null {
  const best = pickBestBuyRows(rows)
  if (best.length === 0) {
    return null
  }
  return best.reduce((a, b) => {
    const av = a.buy ?? -Infinity
    const bv = b.buy ?? -Infinity
    return bv > av ? b : a
  })
}

/** İlk eşit-en-iyi satış satırı (kart önizlemesi için logo + fiyat). */
export function pickBestSellRow(rows: BankRatesRow[]): BankRatesRow | null {
  const best = pickBestSellRows(rows)
  if (best.length === 0) {
    return null
  }
  return best.reduce((a, b) => {
    const av = a.sell ?? Infinity
    const bv = b.sell ?? Infinity
    return bv < av ? b : a
  })
}
