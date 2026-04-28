type NumberLike = number | null | undefined

export type PricePrecisionType = 'crypto' | 'fx' | 'stock' | 'commodity' | 'index'

function precisionByType(type: PricePrecisionType): { min: number; max: number } {
  if (type === 'crypto') return { min: 2, max: 6 }
  if (type === 'fx') return { min: 4, max: 5 }
  if (type === 'stock') return { min: 2, max: 2 }
  return { min: 2, max: 4 }
}

export function formatPrice(value: NumberLike, locale: string, currency: string, type: PricePrecisionType = 'stock'): string {
  if (value == null || !Number.isFinite(value)) {
    return '-'
  }
  const precision = precisionByType(type)
  return new Intl.NumberFormat(locale, {
    style: 'currency',
    currency,
    minimumFractionDigits: precision.min,
    maximumFractionDigits: precision.max,
  }).format(value)
}

export function formatNumber(value: NumberLike, locale: string, digits = 2): string {
  if (value == null || !Number.isFinite(value)) {
    return '-'
  }
  return new Intl.NumberFormat(locale, {
    minimumFractionDigits: digits,
    maximumFractionDigits: digits,
  }).format(value)
}
