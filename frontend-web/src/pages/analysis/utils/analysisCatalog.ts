import type { CatalogRow } from '../../../features/markets/api/marketService'
import type { MarketCategory, MarketOverviewItem } from '../../../shared/types/market'
import type { AssetDefinition, AssetType } from '../types'

export const ANALYSIS_MARKET_CATEGORIES: MarketCategory[] = [
  'all',
  'crypto',
  'bist',
  'nasdaq',
  'forex',
  'metals',
  'globalFutures',
  'funds',
  'bonds',
]

export function mapCategoryToAssetType(category: string | null | undefined): AssetType {
  const c = (category ?? 'STOCK').toUpperCase()
  switch (c) {
    case 'CRYPTO':
      return 'crypto'
    case 'FX':
      return 'fx'
    case 'FUND':
      return 'fund'
    case 'METAL':
      return 'commodity'
    case 'STOCK':
    default:
      return 'stock'
  }
}

/** Maps Piyasa segment → haber `categoryUi` filtresi. */
export function marketCategoryToNewsUi(category: MarketCategory): string {
  switch (category) {
    case 'crypto':
      return 'crypto'
    case 'forex':
    case 'metals':
      return 'fx'
    case 'bist':
    case 'nasdaq':
      return 'bist'
    case 'funds':
    case 'bonds':
    case 'globalFutures':
    case 'eurobond':
    default:
      return 'macro'
  }
}

export function catalogRowToAsset(row: CatalogRow, marketSegment: MarketCategory): AssetDefinition {
  const symbol = row.symbol.trim().toUpperCase()
  const wire = (row.category ?? 'STOCK').toUpperCase()
  return {
    id: symbol.toLowerCase(),
    symbol,
    name: row.name?.trim() || symbol,
    type: mapCategoryToAssetType(row.category),
    wireCategory: wire,
    marketSegment,
  }
}

function inferMarketSegmentFromOverview(wire: string, exchange: string | null | undefined): MarketCategory {
  const w = wire.toUpperCase()
  if (w === 'CRYPTO') return 'crypto'
  if (w === 'FX') return 'forex'
  if (w === 'METAL') return 'metals'
  if (w === 'FUND') return 'funds'
  const ex = (exchange ?? '').toUpperCase()
  if (ex.includes('BIST') || ex.includes('ISTANBUL')) return 'bist'
  if (ex.includes('NASDAQ') || ex.includes('NYSE') || ex.includes('US')) return 'nasdaq'
  if (w === 'STOCK') return 'bist'
  return 'all'
}

export function overviewRowToAsset(row: MarketOverviewItem): AssetDefinition {
  const symbol = row.symbol.trim().toUpperCase()
  const wire = (row.category ?? 'STOCK').toUpperCase()
  return {
    id: symbol.toLowerCase(),
    symbol,
    name: row.name?.trim() || symbol,
    type: mapCategoryToAssetType(row.category),
    wireCategory: wire,
    marketSegment: inferMarketSegmentFromOverview(wire, row.exchange),
  }
}

export function catalogRowToOverview(row: CatalogRow): MarketOverviewItem {
  return {
    symbol: row.symbol.trim().toUpperCase(),
    name: row.name?.trim() || row.symbol,
    price: row.price ?? 0,
    timestamp: row.timestamp,
    freshness: row.freshness,
    change24h: row.change24h ?? null,
    change1D: row.change24h ?? null,
    high24h: row.high24h ?? null,
    low24h: row.low24h ?? null,
    category: row.category ?? null,
    exchange: row.listedExchange ?? null,
    instrumentId: row.instrumentId,
  }
}
