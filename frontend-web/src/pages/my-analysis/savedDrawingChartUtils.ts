import type { AnalysisRange } from '../../features/analysis/api/analysisService'
import type { ChartDrawingSaveSummary } from '../../features/analysis/api/chartDrawingService'
import type { AssetType } from '../analysis/types'

export function inferWireCategory(assetType: string | null): string | null {
  if (!assetType) return null
  switch (assetType.trim().toLowerCase()) {
    case 'crypto':
      return 'CRYPTO'
    case 'fx':
      return 'FX'
    case 'fund':
      return 'FUND'
    case 'commodity':
      return 'METAL'
    case 'index':
    case 'stock':
      return 'STOCK'
    default:
      return null
  }
}

export function resolveAssetType(assetType: string | null): AssetType {
  const normalized = (assetType ?? 'stock').trim().toLowerCase()
  if (
    normalized === 'stock' ||
    normalized === 'crypto' ||
    normalized === 'fx' ||
    normalized === 'commodity' ||
    normalized === 'index' ||
    normalized === 'fund'
  ) {
    return normalized
  }
  return 'stock'
}

export function resolveChartRangeForSave(summary: ChartDrawingSaveSummary): AnalysisRange {
  const min = summary.minAnchorTime
  const max = summary.maxAnchorTime
  if (min == null || max == null) return '1y'
  const spanDays = Math.max(1, (max - min) / 86_400)
  if (spanDays <= 7) return '7d'
  if (spanDays <= 30) return '30d'
  if (spanDays <= 90) return '90d'
  if (spanDays <= 365) return '1y'
  return '5y'
}

export function formatSavedAt(iso: string, locale: string): string {
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return iso
  return new Intl.DateTimeFormat(locale, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(date)
}
