export function parseAmountLoose(raw: string): number | null {
  const n = Number(String(raw).replace(/\s/g, '').replace(',', '.'))
  return Number.isFinite(n) ? n : null
}

export function daysBetweenIso(a: string, b: string): number {
  const d0 = Date.parse(`${a}T12:00:00Z`)
  const d1 = Date.parse(`${b}T12:00:00Z`)
  if (!Number.isFinite(d0) || !Number.isFinite(d1)) return 0
  return Math.max(0, Math.floor((d1 - d0) / 86400000))
}

export type DepositProjection = {
  days: number
  years: number
  projectedBalance: number
  interestEarned: number
  returnPercent: number
}

/** Basit bileşik faiz: yıllık % oran, ACT/365.25 gün sayımı. */
export function computeDepositProjection(
  principal: number,
  annualRatePercent: number,
  startDate: string,
  endDate: string,
): DepositProjection | null {
  if (principal <= 0 || annualRatePercent < 0 || !startDate || !endDate || startDate > endDate) {
    return null
  }
  const days = daysBetweenIso(startDate, endDate)
  if (days <= 0) return null
  const years = days / 365.25
  const factor = Math.pow(1 + annualRatePercent / 100, years)
  const projectedBalance = principal * factor
  const interestEarned = projectedBalance - principal
  const returnPercent = principal > 0 ? (interestEarned / principal) * 100 : 0
  return { days, years, projectedBalance, interestEarned, returnPercent }
}
