/** EVDS TP.MT210AGS.TRY.* maturity suffixes (same order as backend default list). */
export const TL_DEPOSIT_MATURITY_CODES = ['MT01', 'MT02', 'MT03', 'MT04', 'MT05', 'MT06'] as const

export type TlDepositMaturityCode = (typeof TL_DEPOSIT_MATURITY_CODES)[number]

export function isTlDepositMaturityCode(s: string): s is TlDepositMaturityCode {
  return (TL_DEPOSIT_MATURITY_CODES as readonly string[]).includes(s)
}
