/** TR government bond (TCMB EVDS) tenor codes mapped to TRBOND* symbols. */

export const BOND_TENOR_CODES = ['1Y', '2Y', '3Y', '5Y', '10Y'] as const

export type BondTenorCode = (typeof BOND_TENOR_CODES)[number]

export const DEFAULT_BOND_TENOR: BondTenorCode = '5Y'

export function bondSymbolFromTenor(tenor: BondTenorCode): string {
  return `TRBOND${tenor.replace('Y', '')}Y`
}

export function tenorYearsFromCode(tenor: BondTenorCode): number {
  return Number.parseInt(tenor.replace('Y', ''), 10)
}

export function isBondTenorCode(raw: string): raw is BondTenorCode {
  return (BOND_TENOR_CODES as readonly string[]).includes(raw)
}
