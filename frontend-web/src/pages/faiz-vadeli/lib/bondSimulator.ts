import { parseAmountLoose } from './depositSimulator'
import type { BondTenorCode } from './bondTenor'
import { tenorYearsFromCode } from './bondTenor'

export type BondHoldProjection = {
  years: number
  faceValue: number
  purchasePrice: number
  maturityProceeds: number
  totalReturn: number
  returnPercent: number
  pricePer100: number
}

/** Sıfır kupon / iskontolu bono yaklaşımı: temiz fiyat = 100 / (1+y)^T. */
export function computeBondHoldProjection(
  investment: number,
  yieldPercent: number,
  tenor: BondTenorCode,
): BondHoldProjection | null {
  const principal = investment
  const y = yieldPercent
  const years = tenorYearsFromCode(tenor)
  if (principal <= 0 || y < 0 || years <= 0) {
    return null
  }
  const discountFactor = Math.pow(1 + y / 100, years)
  if (!Number.isFinite(discountFactor) || discountFactor <= 0) {
    return null
  }
  const pricePer100 = 100 / discountFactor
  const faceValue = principal * (100 / pricePer100)
  const purchasePrice = principal
  const maturityProceeds = faceValue
  const totalReturn = maturityProceeds - purchasePrice
  const returnPercent = purchasePrice > 0 ? (totalReturn / purchasePrice) * 100 : 0
  return {
    years,
    faceValue,
    purchasePrice,
    maturityProceeds,
    totalReturn,
    returnPercent,
    pricePer100,
  }
}

export { parseAmountLoose }
