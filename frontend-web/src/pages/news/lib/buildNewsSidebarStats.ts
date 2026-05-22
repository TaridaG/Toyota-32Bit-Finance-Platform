import type { NewsApiItem } from '../../../features/news/api/newsService'

export type NewsTopicKey = 'bist' | 'viop' | 'fx' | 'crypto' | 'macro'

export type NewsSidebarTopicRow = {
  key: NewsTopicKey
  count: number
  percent: number
}

export type NewsSidebarAssetRow = {
  symbol: string
  count: number
}

export type NewsSidebarSourceRow = {
  name: string
  count: number
}

export type NewsSidebarStats = {
  totalSampled: number
  topics: NewsSidebarTopicRow[]
  topAssets: NewsSidebarAssetRow[]
  sources: NewsSidebarSourceRow[]
  portfolioRelatedCount: number
}

const TOPIC_ORDER: NewsTopicKey[] = ['crypto', 'macro', 'bist', 'fx', 'viop']

function normalizeAssetSymbol(symbol: string): string {
  const upper = symbol.trim().toUpperCase()
  if (upper.endsWith('USDT') && upper.length > 4) {
    return upper.slice(0, -4)
  }
  return upper
}

function resolveTopics(item: NewsApiItem): NewsTopicKey[] {
  const allowed = new Set<NewsTopicKey>(TOPIC_ORDER)
  const fromApi = (item.topicTags ?? [])
    .map((tag) => tag.toLowerCase() as NewsTopicKey)
    .filter((tag): tag is NewsTopicKey => allowed.has(tag))
  if (fromApi.length > 0) {
    return [...new Set(fromApi)]
  }
  const category = (item.category ?? '').toUpperCase()
  if (category === 'CRYPTO') return ['crypto']
  if (category === 'FX') return ['fx']
  if (category === 'STOCK') return ['bist']
  if (category === 'VIOP') return ['viop']
  return ['macro']
}

function buildPortfolioSymbolSet(symbols: string[]): Set<string> {
  const set = new Set<string>()
  for (const raw of symbols) {
    const trimmed = raw.trim().toUpperCase()
    if (!trimmed) continue
    set.add(trimmed)
    set.add(normalizeAssetSymbol(trimmed))
  }
  return set
}

function newsMatchesPortfolio(item: NewsApiItem, portfolioSymbols: Set<string>): boolean {
  if (portfolioSymbols.size === 0) return false
  for (const raw of item.relatedSymbols ?? []) {
    const upper = raw.trim().toUpperCase()
    if (!upper) continue
    if (portfolioSymbols.has(upper) || portfolioSymbols.has(normalizeAssetSymbol(upper))) {
      return true
    }
  }
  return false
}

export function isNewsRelatedToPortfolio(item: NewsApiItem, portfolioSymbols: string[]): boolean {
  return buildNewsSidebarStats([item], portfolioSymbols).portfolioRelatedCount > 0
}

export function buildNewsSidebarStats(
  items: NewsApiItem[],
  portfolioSymbols: string[] = [],
): NewsSidebarStats {
  const topicCounts = new Map<NewsTopicKey, number>()
  const assetCounts = new Map<string, number>()
  const sourceCounts = new Map<string, number>()
  const portfolioSet = buildPortfolioSymbolSet(portfolioSymbols)
  let portfolioRelatedCount = 0

  for (const item of items) {
    const topics = resolveTopics(item)
    const primaryTopic = topics[0]
    if (primaryTopic) {
      topicCounts.set(primaryTopic, (topicCounts.get(primaryTopic) ?? 0) + 1)
    }
    for (const raw of item.relatedSymbols ?? []) {
      const symbol = normalizeAssetSymbol(raw)
      if (!symbol) continue
      assetCounts.set(symbol, (assetCounts.get(symbol) ?? 0) + 1)
    }
    const source = item.sourceName?.trim()
    if (source) {
      sourceCounts.set(source, (sourceCounts.get(source) ?? 0) + 1)
    }
    if (newsMatchesPortfolio(item, portfolioSet)) {
      portfolioRelatedCount++
    }
  }

  const totalSampled = items.length
  const topics = TOPIC_ORDER.map((key) => {
    const count = topicCounts.get(key) ?? 0
    return {
      key,
      count,
      percent: totalSampled > 0 ? Math.round((count / totalSampled) * 100) : 0,
    }
  }).filter((row) => row.count > 0)

  const topAssets = [...assetCounts.entries()]
    .map(([symbol, count]) => ({ symbol, count }))
    .sort((a, b) => b.count - a.count)
    .slice(0, 5)

  const sources = [...sourceCounts.entries()]
    .map(([name, count]) => ({ name, count }))
    .sort((a, b) => b.count - a.count)
    .slice(0, 5)

  return {
    totalSampled,
    topics,
    topAssets,
    sources,
    portfolioRelatedCount,
  }
}
