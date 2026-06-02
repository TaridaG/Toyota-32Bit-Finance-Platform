/** VIOP faiz/tahvil near-contract alias sembolleri (MDS fiyat geçmişi). */
export const TAHVIL_SYMBOLS = ['VIOP_TLREF_NEAR', 'VIOP_DIBS_NEAR', 'VIOP_FAIZ_NEAR'] as const

export type TahvilSymbol = (typeof TAHVIL_SYMBOLS)[number]

export function isTahvilSymbol(s: string): s is TahvilSymbol {
  return (TAHVIL_SYMBOLS as readonly string[]).includes(s)
}
