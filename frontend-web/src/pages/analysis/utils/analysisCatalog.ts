import type { CatalogRow } from '../../../features/markets/api/marketService'
import { inferNativeQuote } from '../../../features/markets/lib/marketDisplayConversion'
import type { MarketCategory, MarketNativeQuote, MarketOverviewItem } from '../../../shared/types/market'
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
  const nativeQuote = inferNativeQuote(row.symbol, row.category ?? null, row.source, row.listedExchange)
  return {
    symbol: row.symbol.trim().toUpperCase(),
    name: row.name?.trim() || row.symbol,
    price: row.price ?? 0,
    nativeQuote,
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

/** Quote currency for chart/ticker labels — native feed currency, not header preference. */
export function resolveAnalysisQuoteCurrency(
  asset: AssetDefinition | null | undefined,
  overview: MarketOverviewItem | null | undefined,
  catalogRow?: CatalogRow | null,
): MarketNativeQuote {
  if (overview?.nativeQuote) {
    return overview.nativeQuote
  }
  if (catalogRow) {
    return inferNativeQuote(catalogRow.symbol, catalogRow.category ?? null, catalogRow.source, catalogRow.listedExchange)
  }
  if (asset) {
    const exchange =
      asset.marketSegment === 'bist'
        ? 'BIST'
        : asset.marketSegment === 'nasdaq'
          ? 'NASDAQ'
          : null
    return inferNativeQuote(asset.symbol, asset.wireCategory, null, exchange)
  }
  return 'USD'
}
