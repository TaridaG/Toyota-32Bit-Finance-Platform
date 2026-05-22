import type { NewsApiItem } from '../../../features/news/api/newsService'
import type { NewsCategory, NewsDataPoint } from '../types'

export function stripHtml(value: string): string {
  return value.replace(/<[^>]*>/g, ' ').replace(/\s+/g, ' ').trim()
}

export function mapNewsItem(
  item: NewsApiItem,
  t: (key: string, options?: Record<string, unknown>) => string,
): NewsDataPoint {
  const titleTranslated = stripHtml(item.title?.trim() || '-')
  const summaryTranslated = stripHtml(item.summary?.trim() || '-')
  const relatedAssets = item.relatedSymbols?.filter(Boolean) ?? []
  return {
    id: String(item.id),
    title: titleTranslated,
    summary: summaryTranslated,
    titleOriginal: item.titleOriginal ? stripHtml(item.titleOriginal) : undefined,
    summaryOriginal: item.summaryOriginal ? stripHtml(item.summaryOriginal) : undefined,
    titleTranslated,
    summaryTranslated,
    translatedLanguage: item.translatedLanguage,
    translated: item.translated ?? false,
    details: summaryTranslated,
    imageUrl: item.imageUrl?.trim() || null,
    source: item.sourceName,
    timeAgoMinutes: toMinutesAgo(item.publishedAt),
    timeAgoLabel: toRelativeTimeLabel(item.publishedAt, t),
    category: mapCategory(item.category),
    topicTags: normalizeTopicTags(item.topicTags, item.category),
    tags: relatedAssets,
    relatedAssets,
    correlationNote: '-',
    sparkline: [0, 0, 0, 0, 0, 0, 0, 0],
  }
}

function normalizeTopicTags(
  topicTags: string[] | undefined,
  wireCategory: string | null | undefined,
): Exclude<NewsCategory, 'all'>[] {
  const allowed = new Set<Exclude<NewsCategory, 'all'>>(['bist', 'viop', 'fx', 'crypto', 'macro'])
  const fromApi = (topicTags ?? [])
    .map((tag) => tag.toLowerCase())
    .filter((tag): tag is Exclude<NewsCategory, 'all'> => allowed.has(tag as Exclude<NewsCategory, 'all'>))
  if (fromApi.length > 0) {
    return [...new Set(fromApi)]
  }
  return [mapCategory(wireCategory)]
}

function mapCategory(category: string | null | undefined): Exclude<NewsCategory, 'all'> {
  const c = (category ?? '').toUpperCase()
  if (c === 'VIOP') return 'viop'
  if (c === 'CRYPTO') return 'crypto'
  if (c === 'FX') return 'fx'
  if (c === 'STOCK') return 'bist'
  if (c === 'FUND' || c === 'BOND' || c === 'GENERAL_ECONOMY') return 'macro'
  return 'macro'
}

function toMinutesAgo(publishedAt: string): number {
  const ts = Date.parse(publishedAt)
  if (Number.isNaN(ts)) {
    return 0
  }
  const diffMs = Date.now() - ts
  return Math.max(Math.floor(diffMs / 60000), 0)
}

function toRelativeTimeLabel(
  publishedAt: string,
  t: (key: string, options?: Record<string, unknown>) => string,
): string {
  const minutes = toMinutesAgo(publishedAt)
  if (minutes < 60) {
    return t('time.minutesAgo', { count: minutes })
  }
  const hours = Math.floor(minutes / 60)
  return t('time.hoursAgo', { count: hours })
}
