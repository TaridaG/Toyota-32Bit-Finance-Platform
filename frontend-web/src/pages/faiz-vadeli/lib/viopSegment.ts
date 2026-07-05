/** VIOP detay paneli segment sekmeleri (MDS `?segment=` query değerleri). */
export const VIOP_PANEL_SEGMENTS = [
  'rates',
  'bonds',
  'equity',
  'commodity',
  'all',
] as const

export type ViopPanelSegment = (typeof VIOP_PANEL_SEGMENTS)[number]

export type ViopSegment = ViopPanelSegment | 'rates_bonds' | 'index'

export const VIOP_ALIAS_SYMBOLS = ['VIOP_TLREF_NEAR', 'VIOP_DIBS_NEAR', 'VIOP_FAIZ_NEAR'] as const

export type ViopAliasSymbol = (typeof VIOP_ALIAS_SYMBOLS)[number]

export function isViopPanelSegment(value: string): value is ViopPanelSegment {
  return (VIOP_PANEL_SEGMENTS as readonly string[]).includes(value)
}
