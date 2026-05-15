/** TCMB Hazine gösterge getirisi sembolleri (MDS fiyat geçmişi). */
export const TAHVIL_SYMBOLS = ['TRBOND1Y', 'TRBOND2Y', 'TRBOND3Y'] as const

export type TahvilSymbol = (typeof TAHVIL_SYMBOLS)[number]

export function isTahvilSymbol(s: string): s is TahvilSymbol {
  return (TAHVIL_SYMBOLS as readonly string[]).includes(s)
}
