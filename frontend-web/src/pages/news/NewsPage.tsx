import { useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useDocumentTitle } from '../../shared/hooks/useDocumentTitle'
import type { NewsCategory, NewsDataPoint, SentimentType } from './types'
import { fetchNewsOriginal, type NewsApiItem } from '../../features/news/api/newsService'
import { useNews } from '../../features/news/hooks/useNews'
import type { NewsFetchFilters } from '../../features/news/api/newsService'
import { NewsCard } from './components/NewsCard'
import { NewsDetailModal } from './components/NewsDetailModal'

export function NewsPage() {
  const { t, i18n } = useTranslation('newsPage')
  const [page, setPage] = useState(0)
  const pageSize = 10
  const defaultFilters: NewsFetchFilters = { category: 'all', range: 'all', sentiment: 'all' }
  const [filtersOpen, setFiltersOpen] = useState(false)
  const [draftFilters, setDraftFilters] = useState<NewsFetchFilters>(defaultFilters)
  const [appliedFilters, setAppliedFilters] = useState<NewsFetchFilters>(defaultFilters)
  const [searchDraft, setSearchDraft] = useState('')
  const [appliedSearch, setAppliedSearch] = useState('')
  const {
    data: streamApiData,
    loading: streamLoading,
    error: streamError,
    refetch: refetchStream,
    totalElements,
    totalPages,
  } = useNews(page, pageSize, i18n.language, appliedFilters, appliedSearch)
  const [selectedNewsId, setSelectedNewsId] = useState<string | null>(null)
  useDocumentTitle(t('titleDoc'))

  const streamNews = useMemo<NewsDataPoint[]>(
    () => streamApiData.map((item) => mapNewsItem(item, t)),
    [streamApiData, t],
  )

  useEffect(() => {
    const trimmed = searchDraft.trim()
    if (trimmed === appliedSearch) {
      return
    }
    const timer = window.setTimeout(() => {
      setAppliedSearch(trimmed)
      setPage(0)
    }, 400)
    return () => window.clearTimeout(timer)
  }, [appliedSearch, searchDraft])

  const applySearchNow = () => {
    const trimmed = searchDraft.trim()
    setAppliedSearch(trimmed)
    setPage(0)
  }

  const clearSearch = () => {
    setSearchDraft('')
    setAppliedSearch('')
    setPage(0)
  }

  return (
    <>
      <section className="fi-news-page">
        <article className="card fi-news-feed">
            <div className="fi-news-feed-head">
              <form
                className="fi-news-search"
                onSubmit={(event) => {
                  event.preventDefault()
                  applySearchNow()
                }}
              >
                <input
                  type="search"
                  value={searchDraft}
                  onChange={(event) => setSearchDraft(event.target.value)}
                  placeholder={t('searchPlaceholder')}
                  aria-label={t('searchLabel')}
                />
                <button type="submit" className="fi-news-search-btn">
                  {t('searchAction')}
                </button>
                {appliedSearch ? (
                  <button type="button" className="fi-news-search-clear" onClick={clearSearch}>
                    {t('clearSearch')}
                  </button>
                ) : null}
              </form>
              <div className="fi-inline-filter-wrap">
                <button type="button" className="fi-filter-toggle fi-inline-filter-button" onClick={() => setFiltersOpen((prev) => !prev)}>
                  <IconFilter />
                  {t('filterToggle')}
                  <span aria-hidden>{filtersOpen ? '▲' : '▼'}</span>
                </button>
                {filtersOpen ? (
                  <div className="fi-inline-filter-popover">
                    <div className="fi-filter-group">
                      <span>{t('categoryTitle')}</span>
                      <div>
                        {(['all', 'bist', 'viop', 'fx', 'crypto', 'macro'] as const).map((category) => (
                          <button
                            key={category}
                            type="button"
                            className={`fi-filter-chip${draftFilters.category === category ? ' fi-filter-chip-active' : ''}`}
                            onClick={() => setDraftFilters((prev) => ({ ...prev, category }))}
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
                            className={`fi-filter-chip${draftFilters.range === range ? ' fi-filter-chip-active' : ''}`}
                            onClick={() => setDraftFilters((prev) => ({ ...prev, range }))}
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
                            className={`fi-filter-chip${draftFilters.sentiment === sentiment ? ' fi-filter-chip-active' : ''}`}
                            onClick={() => setDraftFilters((prev) => ({ ...prev, sentiment: sentiment as 'all' | SentimentType }))}
                          >
                            {t(`sentiment.${sentiment}`)}
                          </button>
                        ))}
                      </div>
                    </div>
                    <div className="fi-filter-actions">
                      <button
                        type="button"
                        className="fi-filter-chip fi-filter-chip-active"
                        onClick={() => {
                          setAppliedFilters(draftFilters)
                          setPage(0)
                          setFiltersOpen(false)
                        }}
                      >
                        {t('applyFilters')}
                      </button>
                      <button
                        type="button"
                        className="fi-filter-chip"
                        onClick={() => {
                          setDraftFilters(defaultFilters)
                          setAppliedFilters(defaultFilters)
                          setPage(0)
                          setFiltersOpen(false)
                        }}
                      >
                        {t('clearFilters')}
                      </button>
                    </div>
                  </div>
                ) : null}
              </div>
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
              ) : streamNews.length > 0 ? (
                streamNews.map((item) => (
                  <NewsCard
                    key={item.id}
                    item={item}
                    searchQuery={appliedSearch}
                    onOpen={(item) => setSelectedNewsId(item.id)}
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
                <p className="fi-empty">{appliedSearch ? t('noSearchResults') : t('noNews')}</p>
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
                  {t('paginationSummary', {
                    page: page + 1,
                    totalPages: Math.max(totalPages, 1),
                    count: totalElements,
                  })}
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
      </section>

      {selectedNewsId ? (
        <NewsDetailModal
          newsId={selectedNewsId}
          searchQuery={appliedSearch}
          onClose={() => setSelectedNewsId(null)}
        />
      ) : null}
    </>
  )
}

function IconFilter() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true" className="fi-filter-icon">
      <path d="M4 6h16M7 12h10M10 18h4" />
    </svg>
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
    topicTags: normalizeTopicTags(item.topicTags, item.category),
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

function toRelativeTimeLabel(publishedAt: string, t: (key: string, options?: Record<string, unknown>) => string): string {
  const minutes = toMinutesAgo(publishedAt)
  if (minutes < 60) {
    return t('time.minutesAgo', { count: minutes })
  }
  const hours = Math.floor(minutes / 60)
  return t('time.hoursAgo', { count: hours })
}
