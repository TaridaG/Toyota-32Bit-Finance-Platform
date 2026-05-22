import type { UTCTimestamp } from 'lightweight-charts'
import type { NewsApiItem } from '../../../features/news/api/newsService'
import type { AssetDefinition, AssetNewsItem, CandlePoint, NewsMatchReason } from '../types'
import { marketCategoryToNewsUi } from './analysisCatalog'

export function normalizeChartSymbol(symbol: string): string {
  return symbol.replace(/[^A-Za-z0-9]/g, '').toUpperCase()
}

export function mapNewsCategoryUi(category: string | null | undefined): string {
  const c = (category ?? '').trim().toUpperCase()
  switch (c) {
    case 'CRYPTO':
      return 'crypto'
    case 'FX':
      return 'fx'
    case 'VIOP':
      return 'viop'
    case 'STOCK':
      return 'bist'
    case 'FUND':
    case 'BOND':
    case 'GENERAL_ECONOMY':
      return 'macro'
    default:
      return 'macro'
  }
}

export function resolveChartNewsCategoryUi(asset: AssetDefinition): string {
  if (asset.marketSegment && asset.marketSegment !== 'all') {
    return marketCategoryToNewsUi(asset.marketSegment)
  }
  const wire = asset.wireCategory.toUpperCase()
  if (wire === 'CRYPTO') return 'crypto'
  if (wire === 'FX') return 'fx'
  if (wire === 'FUND' || wire === 'BOND') return 'macro'
  if (asset.type === 'stock') return 'bist'
  return 'macro'
}

function nearestCandleIndexAtOrBefore(candles: CandlePoint[], timeSec: number): number {
  if (candles.length === 0) return -1
  let lo = 0
  let hi = candles.length - 1
  let best = -1
  while (lo <= hi) {
    const mid = Math.floor((lo + hi) / 2)
    const mt = Number(candles[mid].time)
    if (mt <= timeSec) {
      best = mid
      lo = mid + 1
    } else {
      hi = mid - 1
    }
  }
  return best
}

function nearestCandleIndexAtOrAfter(candles: CandlePoint[], timeSec: number): number {
  if (candles.length === 0) return -1
  let lo = 0
  let hi = candles.length - 1
  let best = -1
  while (lo <= hi) {
    const mid = Math.floor((lo + hi) / 2)
    const mt = Number(candles[mid].time)
    if (mt >= timeSec) {
      best = mid
      hi = mid - 1
    } else {
      lo = mid + 1
    }
  }
  return best
}

function utcDayStartSec(timeSec: number): number {
  const d = new Date(timeSec * 1000)
  d.setUTCHours(0, 0, 0, 0)
  return Math.floor(d.getTime() / 1000)
}

/** Snap marker to the candle on the news publish day (Haberler ile aynı gün). */
export function snapNewsToChartTime(candles: CandlePoint[], publishedSec: number): UTCTimestamp | null {
  if (candles.length === 0) return null
  const minT = Number(candles[0].time)
  const maxT = Number(candles[candles.length - 1].time)
  const maxPublished = maxT + 86_400
  if (publishedSec < minT || publishedSec > maxPublished) {
    return null
  }

  const publishDay = utcDayStartSec(publishedSec)
  const sameDay = candles.filter((c) => utcDayStartSec(Number(c.time)) === publishDay)
  if (sameDay.length > 0) {
    let best = sameDay[0]
    for (const candle of sameDay) {
      if (Number(candle.time) <= publishedSec) {
        best = candle
      }
    }
    return best.time
  }

  const beforeIdx = nearestCandleIndexAtOrBefore(candles, publishedSec)
  const afterIdx = nearestCandleIndexAtOrAfter(candles, publishedSec)
  if (beforeIdx < 0 && afterIdx < 0) {
    return null
  }
  if (beforeIdx < 0) {
    return candles[afterIdx].time
  }
  if (afterIdx < 0) {
    return candles[beforeIdx].time
  }
  const before = candles[beforeIdx]
  const after = candles[afterIdx]
  const distBefore = Math.abs(Number(before.time) - publishedSec)
  const distAfter = Math.abs(Number(after.time) - publishedSec)
  return (distAfter < distBefore ? after : before).time
}

export function computeNextDayChangePercent(candles: CandlePoint[], newsTimeSec: number): number | null {
  if (candles.length < 2) return null
  const baseIdx = nearestCandleIndexAtOrBefore(candles, newsTimeSec)
  if (baseIdx < 0) return null
  const base = candles[baseIdx]
  const nextIdx = nearestCandleIndexAtOrAfter(candles, Number(base.time) + 86_400)
  if (nextIdx < 0 || nextIdx === baseIdx) return null
  const next = candles[nextIdx]
  if (!base.close || base.close === 0) return null
  return ((next.close - base.close) / base.close) * 100
}

export type NewsMarkerTone = 'up' | 'down' | 'neutral'

export function toneFromNextDayChange(change: number | null): NewsMarkerTone {
  if (change == null || !Number.isFinite(change)) return 'neutral'
  if (change > 0.05) return 'up'
  if (change < -0.05) return 'down'
  return 'neutral'
}

export function markerColorForTone(tone: NewsMarkerTone): string {
  switch (tone) {
    case 'up':
      return '#22c55e'
    case 'down':
      return '#ef4444'
    default:
      return '#94a3b8'
  }
}

function allocateMarkerTime(baseTime: number, usedCounts: Map<number, number>): UTCTimestamp {
  const slot = usedCounts.get(baseTime) ?? 0
  usedCounts.set(baseTime, slot + 1)
  return (baseTime + slot * 60) as UTCTimestamp
}

function buildMatchReasons(
  item: NewsApiItem,
  asset: AssetDefinition,
  assetCategoryUi: string,
): NewsMatchReason[] {
  const target = normalizeChartSymbol(asset.symbol)
  const related = (item.relatedSymbols ?? []).map(normalizeChartSymbol)
  const topicTags =
    item.topicTags && item.topicTags.length > 0
      ? item.topicTags.map((t) => t.toLowerCase())
      : [mapNewsCategoryUi(item.category)]
  const reasons: NewsMatchReason[] = []
  if (related.includes(target)) {
    reasons.push({ kind: 'asset', symbol: asset.symbol, categoryUi: assetCategoryUi })
  }
  for (const tag of topicTags) {
    if (tag === assetCategoryUi) {
      reasons.push({ kind: 'category', categoryUi: tag })
    }
  }
  return reasons
}

export function mapNewsToChartItems(
  feed: NewsApiItem[],
  asset: AssetDefinition,
  assetCategoryUi: string,
  candles: CandlePoint[],
): AssetNewsItem[] {
  const items: AssetNewsItem[] = []
  const seenIds = new Set<string>()
  const usedTimes = new Map<number, number>()

  for (const item of feed) {
    const id = String(item.id)
    if (seenIds.has(id)) {
      continue
    }
    const matchReasons = buildMatchReasons(item, asset, assetCategoryUi)
    if (matchReasons.length === 0) {
      continue
    }

    const publishedSec = Math.floor(Date.parse(item.publishedAt) / 1000)
    if (!Number.isFinite(publishedSec)) {
      continue
    }
    const snapped = snapNewsToChartTime(candles, publishedSec)
    if (snapped == null) {
      continue
    }

    seenIds.add(id)
    const markerTime = allocateMarkerTime(Number(snapped), usedTimes)
    const nextDayChange = computeNextDayChangePercent(candles, publishedSec)
    const tone = toneFromNextDayChange(nextDayChange)

    items.push({
      id,
      assetId: asset.id,
      title: item.title ?? item.titleOriginal ?? '',
      summary: item.summary ?? item.summaryOriginal ?? '',
      source: item.sourceName,
      impact: item.sentiment,
      createdAt: markerTime,
      reactionPercent1h: item.reactionPercent1h ?? 0,
      relatedAssets: item.relatedSymbols ?? [],
      nextDayChangePercent: nextDayChange,
      markerTone: tone,
      newsCategoryUi: mapNewsCategoryUi(item.category),
      matchReasons,
    })
  }
  return items.sort((a, b) => b.createdAt - a.createdAt)
}

/** All favorites that fall on the visible chart window (no asset/category filter). */
export function mapFavoriteNewsToChartItems(
  feed: NewsApiItem[],
  candles: CandlePoint[],
  fallbackAssetId: string,
): AssetNewsItem[] {
  const items: AssetNewsItem[] = []
  const seenIds = new Set<string>()
  const usedTimes = new Map<number, number>()

  for (const item of feed) {
    const id = String(item.id)
    if (seenIds.has(id)) {
      continue
    }
    const publishedSec = Math.floor(Date.parse(item.publishedAt) / 1000)
    if (!Number.isFinite(publishedSec)) {
      continue
    }
    const snapped = snapNewsToChartTime(candles, publishedSec)
    if (snapped == null) {
      continue
    }

    seenIds.add(id)
    const markerTime = allocateMarkerTime(Number(snapped), usedTimes)
    const nextDayChange = computeNextDayChangePercent(candles, publishedSec)
    const tone = toneFromNextDayChange(nextDayChange)

    items.push({
      id,
      assetId: fallbackAssetId,
      title: item.title ?? item.titleOriginal ?? '',
      summary: item.summary ?? item.summaryOriginal ?? '',
      source: item.sourceName,
      impact: item.sentiment,
      createdAt: markerTime,
      reactionPercent1h: item.reactionPercent1h ?? 0,
      relatedAssets: item.relatedSymbols ?? [],
      nextDayChangePercent: nextDayChange,
      markerTone: tone,
      newsCategoryUi: mapNewsCategoryUi(item.category),
      matchReasons: [{ kind: 'favorite', categoryUi: 'favorite' }],
    })
  }
  return items.sort((a, b) => b.createdAt - a.createdAt)
}
