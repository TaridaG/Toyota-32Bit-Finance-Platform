import { useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useDocumentTitle } from '../../shared/hooks/useDocumentTitle'
import { tickerItems, topGainers, topLosers } from './mockData'
import type { NewsCategory, NewsDataPoint, SentimentType } from './types'
import { fetchNewsOriginal, type NewsApiItem } from '../../features/news/api/newsService'
import { useNews } from '../../features/news/hooks/useNews'
import { MarketTicker } from './components/MarketTicker'
import { NewsCard } from './components/NewsCard'
import { TrendingList } from './components/TrendingList'
import { SentimentChart } from './components/SentimentChart'
import { InsightBox } from './components/InsightBox'

export function NewsPage() {
  const { t, i18n } = useTranslation('newsPage')
  const [page, setPage] = useState(0)
  const pageSize = 10
  const {
    data: streamApiData,
    loading: streamLoading,
    error: streamError,
    refetch: refetchStream,
    totalElements,
    totalPages,
  } = useNews(page, pageSize, i18n.language)
  const [selectedCategory, setSelectedCategory] = useState<NewsCategory>('all')
  /** Default "all" so server-side pagination is not wiped by a 24h window (older pages are always >24h old). */
  const [selectedRange, setSelectedRange] = useState<'all' | '1h' | '6h' | '24h'>('all')
  const [selectedSentiment, setSelectedSentiment] = useState<'all' | SentimentType>('all')
  const [selectedNews, setSelectedNews] = useState<NewsDataPoint | null>(null)
  useDocumentTitle(t('titleDoc'))

  const streamNews = useMemo<NewsDataPoint[]>(
    () => streamApiData.map((item) => mapNewsItem(item, t)),
    [streamApiData, t],
  )

  const filteredStreamNews = useMemo(() => {
    const byCategory =
      selectedCategory === 'all' ? streamNews : streamNews.filter((item) => item.category === selectedCategory)
    const bySentiment =
      selectedSentiment === 'all' ? byCategory : byCategory.filter((item) => item.sentiment === selectedSentiment)
    if (selectedRange === 'all') {
      return bySentiment
    }
    const maxMinutes = selectedRange === '1h' ? 60 : selectedRange === '6h' ? 360 : 1440
    return bySentiment.filter((item) => item.timeAgoMinutes <= maxMinutes)
  }, [selectedCategory, selectedRange, selectedSentiment, streamNews])

  const sentimentCounts = useMemo(
    () => ({
      positive: filteredStreamNews.filter((item) => item.sentiment === 'positive').length,
      negative: filteredStreamNews.filter((item) => item.sentiment === 'negative').length,
      neutral: filteredStreamNews.filter((item) => item.sentiment === 'neutral').length,
    }),
    [filteredStreamNews],
  )

  const aiInsight = useMemo(() => {
    if (sentimentCounts.positive > sentimentCounts.negative) {
      return t('aiInsightPositive')
    }
    if (sentimentCounts.negative > sentimentCounts.positive) {
      return t('aiInsightNegative')
    }
    return t('aiInsightNeutral')
  }, [sentimentCounts.negative, sentimentCounts.positive, t])

  return (
    <>
      <section className="fi-news-page">
        <header className="card fi-news-header">
          <div>
            <p className="fi-news-kicker">{t('kicker')}</p>
            <h2>{t('title')}</h2>
            <p>{t('lead')}</p>
          </div>
          <div className="fi-news-ticker-grid">
            {tickerItems.map((item) => (
              <MarketTicker key={item.symbol} item={item} />
            ))}
          </div>
        </header>

        <section className="card fi-filter-bar">
          <div className="fi-filter-group">
            <span>{t('categoryTitle')}</span>
            <div>
              {(['all', 'bist', 'viop', 'fx', 'crypto', 'macro'] as const).map((category) => (
                <button
                  key={category}
                  type="button"
                  className={`fi-filter-chip${selectedCategory === category ? ' fi-filter-chip-active' : ''}`}
                  onClick={() => setSelectedCategory(category)}
                >
                  {t(`categories.${category}`)}
                </button>
              ))}
            </div>
          </div>

          <div className="fi-filter-group">
            <span>{t('timeRangeTitle')}</span>
            <div>
              {(['all', '1h', '6h', '24h'] as const).map((range) => (
                <button
                  key={range}
                  type="button"
                  className={`fi-filter-chip${selectedRange === range ? ' fi-filter-chip-active' : ''}`}
                  onClick={() => setSelectedRange(range)}
                >
                  {range === 'all' ? t('timeRangeAll') : range}
                </button>
              ))}
            </div>
          </div>

          <div className="fi-filter-group">
            <span>{t('sentimentTitle')}</span>
            <div>
              {(['all', 'positive', 'negative', 'neutral'] as const).map((sentiment) => (
                <button
                  key={sentiment}
                  type="button"
                  className={`fi-filter-chip${selectedSentiment === sentiment ? ' fi-filter-chip-active' : ''}`}
                  onClick={() => setSelectedSentiment(sentiment)}
                >
                  {t(`sentiment.${sentiment}`)}
                </button>
              ))}
            </div>
          </div>
        </section>

        <div className="fi-main-grid">
          <article className="card fi-news-feed">
            <div className="fi-panel-head">
              <h3>{t('streamTitle')}</h3>
            </div>
            <div className="fi-news-list">
              {streamLoading ? (
                <p className="fi-empty">{t('common:loading')}</p>
              ) : streamError ? (
                <div className="markets-error-wrap">
                  <span>{streamError ? t(streamError) : t('loadError')}</span>
                  <button type="button" className="markets-filter" onClick={() => void refetchStream()}>
                    {t('common:retry')}
                  </button>
                </div>
              ) : filteredStreamNews.length > 0 ? (
                filteredStreamNews.map((item) => (
                  <NewsCard
                    key={item.id}
                    item={item}
                    onOpen={setSelectedNews}
                    onRequestOriginal={async (id) => {
                      try {
                        const response = await fetchNewsOriginal(Number(id))
                        return { title: stripHtml(response.title || '-'), summary: stripHtml(response.summary || '-') }
                      } catch {
                        return null
                      }
                    }}
                  />
                ))
              ) : (
                <p className="fi-empty">{t('noNews')}</p>
              )}
            </div>
            {!streamLoading && !streamError && totalElements > 0 ? (
              <div className="fi-pagination">
                <button
                  type="button"
                  className="fi-filter-chip"
                  disabled={page <= 0}
                  onClick={() => setPage((prev) => Math.max(prev - 1, 0))}
                >
                  ‹ Önceki
                </button>
                <span className="fi-pagination-summary">
                  Sayfa {page + 1} / {Math.max(totalPages, 1)} · {totalElements} haber
                </span>
                <button
                  type="button"
                  className="fi-filter-chip"
                  disabled={page >= Math.max(totalPages - 1, 0)}
                  onClick={() => setPage((prev) => Math.min(prev + 1, Math.max(totalPages - 1, 0)))}
                >
                  Sonraki ›
                </button>
              </div>
            ) : null}
          </article>

          <aside className="fi-right-column">
            <TrendingList title={t('risingTitle')} items={topGainers} />
            <TrendingList title={t('fallingTitle')} items={topLosers} />
            <SentimentChart counts={sentimentCounts} />
            <InsightBox text={aiInsight} />
          </aside>
        </div>
      </section>

      {selectedNews ? (
        <div className="fi-modal-wrap" role="dialog" aria-modal="true">
          <button className="fi-modal-backdrop" onClick={() => setSelectedNews(null)} aria-label={t('closeDetails')} />
          <article className="card fi-modal">
            <div className="fi-modal-head">
              <h3>{selectedNews.title}</h3>
              <button type="button" onClick={() => setSelectedNews(null)}>
                ×
              </button>
            </div>
            <p>{selectedNews.details}</p>
            <p className="fi-modal-corr">{selectedNews.correlationNote}</p>
            <div className="fi-modal-tags">
              {selectedNews.relatedAssets.map((asset) => (
                <span key={asset}>{asset}</span>
              ))}
            </div>
          </article>
        </div>
      ) : null}
    </>
  )
}

function mapNewsItem(item: NewsApiItem, t: (key: string, options?: Record<string, unknown>) => string): NewsDataPoint {
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
    source: item.sourceName,
    timeAgoMinutes: toMinutesAgo(item.publishedAt),
    timeAgoLabel: toRelativeTimeLabel(item.publishedAt, t),
    category: mapCategory(item.category),
    sentiment: item.sentiment,
    tags: relatedAssets,
    relatedAssets,
    reactionPercent1h: item.reactionPercent1h ?? 0,
    correlationNote: '-',
    sparkline: [0, 0, 0, 0, 0, 0, 0, 0],
  }
}

function stripHtml(value: string): string {
  return value.replace(/<[^>]*>/g, ' ').replace(/\s+/g, ' ').trim()
}

function mapCategory(category: string | null | undefined): Exclude<NewsCategory, 'all'> {
  const c = (category ?? '').toUpperCase()
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

function toRelativeTimeLabel(publishedAt: string, t: (key: string, options?: Record<string, unknown>) => string): string {
  const minutes = toMinutesAgo(publishedAt)
  if (minutes < 60) {
    return t('time.minutesAgo', { count: minutes })
  }
  const hours = Math.floor(minutes / 60)
  return t('time.hoursAgo', { count: hours })
}
