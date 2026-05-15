/** Default Turkey USD Eurobond (ISIN) for Faiz/Vadeli card — must match backend seed order. */
export const TR_USD_EUROBOND_DEFAULT_ISIN = 'US900123AL40'

export const TR_USD_EUROBOND_ISINS = [
  'US900123AL40',
  'US900123AT75',
  'US900123AY60',
  'US900123CM05',
] as const

export type TrUsdEurobondIsin = (typeof TR_USD_EUROBOND_ISINS)[number]

export function isTrUsdEurobondIsin(s: string): s is TrUsdEurobondIsin {
  return (TR_USD_EUROBOND_ISINS as readonly string[]).includes(s)
}
