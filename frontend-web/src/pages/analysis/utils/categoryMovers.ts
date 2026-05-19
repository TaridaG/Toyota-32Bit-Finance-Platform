import type { CategoryPerformanceRow } from '../../../features/markets/api/marketService'

export const CATEGORY_MOVERS_TOP_N = 5

export type CategoryMoversSnapshot = {
  winners: CategoryPerformanceRow[]
  losers: CategoryPerformanceRow[]
}

function rankKey(row: CategoryPerformanceRow): number {
  if (Number.isFinite(row.yearlyPct) && row.yearlyPct !== 0) {
    return row.yearlyPct
  }
  if (Number.isFinite(row.monthlyPct)) {
    return row.monthlyPct
  }
  return row.weeklyPct
}

export function buildCategoryMoversSnapshot(rows: CategoryPerformanceRow[]): CategoryMoversSnapshot {
  const eligible = rows.filter((row) => Number.isFinite(rankKey(row)))
  const winners = [...eligible].sort((a, b) => rankKey(b) - rankKey(a)).slice(0, CATEGORY_MOVERS_TOP_N)
  const losers = [...eligible].sort((a, b) => rankKey(a) - rankKey(b)).slice(0, CATEGORY_MOVERS_TOP_N)
  return { winners, losers }
}
