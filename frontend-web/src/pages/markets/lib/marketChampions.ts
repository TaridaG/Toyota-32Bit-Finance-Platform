import type { MarketOverviewItem } from '../../../shared/types/market'

export type MarketChampionPeriod = 'day' | 'week' | 'month' | 'year'

export type MarketChampion = {
  symbol: string
  name: string
  changePct: number
}

export function championFromRow(
  row: MarketOverviewItem | undefined,
  changePct: number | null | undefined,
): MarketChampion | null {
  if (!row || changePct == null || !Number.isFinite(changePct)) {
    return null
  }
  return {
    symbol: row.symbol,
    name: row.name,
    changePct,
  }
}

export type MarketChampionsSnapshot = Record<MarketChampionPeriod, MarketChampion | null>
