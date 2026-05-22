import { describe, expect, it } from 'vitest'
import type { NewsApiItem } from '../../../features/news/api/newsService'
import { buildNewsSidebarStats, isNewsRelatedToPortfolio } from './buildNewsSidebarStats'

const sample = (partial: Partial<NewsApiItem>): NewsApiItem => ({
  id: 1,
  title: 't',
  summary: 's',
  sourceName: 'CoinTelegraph',
  category: 'CRYPTO',
  publishedAt: new Date().toISOString(),
  relatedSymbols: ['BTCUSDT'],
  topicTags: ['crypto'],
  ...partial,
})

describe('buildNewsSidebarStats', () => {
  it('aggregates topics, assets, sources and portfolio matches', () => {
    const items = [
      sample({ id: 1, relatedSymbols: ['BTCUSDT'], topicTags: ['crypto'], sourceName: 'CoinTelegraph' }),
      sample({ id: 2, relatedSymbols: ['NVDA'], topicTags: ['macro'], sourceName: 'Bloomberg' }),
      sample({ id: 3, relatedSymbols: ['BTCUSDT'], topicTags: ['crypto'], sourceName: 'CoinTelegraph' }),
    ]
    const stats = buildNewsSidebarStats(items, ['BTCUSDT'])
    expect(stats.topics.find((t) => t.key === 'crypto')?.count).toBe(2)
    expect(stats.totalSampled).toBe(3)
    const topicPercentSum = stats.topics.reduce((sum, row) => sum + row.percent, 0)
    expect(topicPercentSum).toBeLessThanOrEqual(100)
    expect(stats.topAssets[0]?.symbol).toBe('BTC')
    expect(stats.sources[0]?.name).toBe('CoinTelegraph')
    expect(stats.portfolioRelatedCount).toBe(2)
  })

  it('detects portfolio relation by normalized symbol', () => {
    const item = sample({ relatedSymbols: ['BTCUSDT'] })
    expect(isNewsRelatedToPortfolio(item, ['BTC'])).toBe(true)
  })
})
