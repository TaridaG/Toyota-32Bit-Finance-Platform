import { useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import {
  addNewsFavorite,
  fetchNewsFavorites,
  removeNewsFavorite,
} from '../../features/news/api/newsFavoritesApi'
import { fetchNewsOriginal } from '../../features/news/api/newsService'
import { useFavoriteNews } from '../../features/news/hooks/useFavoriteNews'
import type { NewsFetchFilters } from '../../features/news/api/newsService'
import { isAuthenticated } from '../../shared/auth/session'
import { NewsCard } from '../news/components/NewsCard'
import { NewsDetailModal } from '../news/components/NewsDetailModal'
import type { NewsDataPoint } from '../news/types'
import { mapNewsItem, stripHtml } from '../news/utils/newsItemMappers'

type MyNewsPanelProps = {
  /** Render inside portfolio shell (sidebar stays visible). */
  embedded?: boolean
}

export function MyNewsPanel({ embedded = false }: MyNewsPanelProps) {
  const { t, i18n } = useTranslation(['myNewsPage', 'newsPage', 'common'])
  const [page, setPage] = useState(0)
  const pageSize = 10
  const defaultFilters: NewsFetchFilters = { category: 'all', range: 'all' }
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
  } = useFavoriteNews(page, pageSize, i18n.language, appliedFilters, appliedSearch)
  const [selectedNewsId, setSelectedNewsId] = useState<string | null>(null)
  const [favoriteNewsIds, setFavoriteNewsIds] = useState<number[]>([])
  const [favoriteNotice, setFavoriteNotice] = useState<string | null>(null)
  const [favoritePendingIds, setFavoritePendingIds] = useState<number[]>([])

  const streamNews = useMemo<NewsDataPoint[]>(
    () => streamApiData.map((item) => mapNewsItem(item, (key, opts) => t(`newsPage:${key}`, opts))),
    [streamApiData, t],
  )

  useEffect(() => {
    void fetchNewsFavorites()
      .then((rows) => setFavoriteNewsIds(rows.map((item) => item.newsId)))
      .catch(() => {
        // keep optimistic state on refresh failure
      })
  }, [streamApiData])

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

  const isNewsFavorite = (newsId: string) => favoriteNewsIds.includes(Number(newsId))

  const showFavoriteErrorNotice = (message: string) => {
    setFavoriteNotice(message)
    window.setTimeout(() => {
      setFavoriteNotice((current) => (current === message ? null : current))
    }, 3500)
  }

  const toggleNewsFavorite = async (newsId: string) => {
    const numericId = Number(newsId)
    if (!Number.isFinite(numericId) || favoritePendingIds.includes(numericId)) {
      return
    }
    const currentlyFavorite = isNewsFavorite(newsId)
    setFavoritePendingIds((prev) => [...prev, numericId])
    try {
      if (currentlyFavorite) {
        await removeNewsFavorite(numericId)
        setFavoriteNewsIds((prev) => prev.filter((id) => id !== numericId))
      } else {
        await addNewsFavorite(numericId)
        setFavoriteNewsIds((prev) => (prev.includes(numericId) ? prev : [...prev, numericId]))
      }
      await refetchStream()
    } catch {
      showFavoriteErrorNotice(t('newsPage:favoriteUpdateError'))
    } finally {
      setFavoritePendingIds((prev) => prev.filter((id) => id !== numericId))
    }
  }

  if (!isAuthenticated()) {
    return (
      <section className={`fi-news-page fi-my-news-page${embedded ? ' fi-my-news-page--embedded' : ''}`}>
        <article className="card fi-news-feed">
          <p className="fi-empty">{t('myNewsPage:loginRequired')}</p>
        </article>
      </section>
    )
  }

  return (
    <>
      <section className={`fi-news-page fi-my-news-page${embedded ? ' fi-my-news-page--embedded' : ''}`}>
        <header className={embedded ? 'my-portfolio-section-head fi-my-news-head' : 'fi-my-news-head'}>
          {embedded ? <h2>{t('myNewsPage:title')}</h2> : <h1>{t('myNewsPage:title')}</h1>}
          <p>{t('myNewsPage:lead')}</p>
        </header>
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
                placeholder={t('newsPage:searchPlaceholder')}
                aria-label={t('newsPage:searchLabel')}
              />
              <button type="submit" className="fi-news-search-btn">
                {t('newsPage:searchAction')}
              </button>
              {appliedSearch ? (
                <button type="button" className="fi-news-search-clear" onClick={clearSearch}>
                  {t('newsPage:clearSearch')}
                </button>
              ) : null}
            </form>
            <div className="fi-inline-filter-wrap">
              <button
                type="button"
                className="fi-filter-toggle fi-inline-filter-button"
                onClick={() => setFiltersOpen((prev) => !prev)}
              >
                <IconFilter />
                {t('newsPage:filterToggle')}
                <span aria-hidden>{filtersOpen ? '▲' : '▼'}</span>
              </button>
              {filtersOpen ? (
                <div className="fi-inline-filter-popover">
                  <div className="fi-filter-group">
                    <span>{t('newsPage:categoryTitle')}</span>
                    <div>
                      {(['all', 'bist', 'viop', 'fx', 'crypto', 'macro'] as const).map((category) => (
                        <button
                          key={category}
                          type="button"
                          className={`fi-filter-chip${draftFilters.category === category ? ' fi-filter-chip-active' : ''}`}
                          onClick={() => setDraftFilters((prev) => ({ ...prev, category }))}
                        >
                          {t(`newsPage:categories.${category}`)}
                        </button>
                      ))}
                    </div>
                  </div>
                  <div className="fi-filter-group">
                    <span>{t('newsPage:timeRangeTitle')}</span>
                    <div>
                      {(['all', '1h', '6h', '24h'] as const).map((range) => (
                        <button
                          key={range}
                          type="button"
                          className={`fi-filter-chip${draftFilters.range === range ? ' fi-filter-chip-active' : ''}`}
                          onClick={() => setDraftFilters((prev) => ({ ...prev, range }))}
                        >
                          {range === 'all' ? t('newsPage:timeRangeAll') : range}
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
                      {t('newsPage:applyFilters')}
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
                      {t('newsPage:clearFilters')}
                    </button>
                  </div>
                </div>
              ) : null}
            </div>
          </div>
          {favoriteNotice ? (
            <div className="markets-error-wrap">
              <span>{favoriteNotice}</span>
            </div>
          ) : null}
          <div className="fi-news-list">
            {streamLoading ? (
              <p className="fi-empty">{t('common:loading')}</p>
            ) : streamError ? (
              <div className="markets-error-wrap">
                <span>{t(`newsPage:${streamError}`)}</span>
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
                  showFavoriteStar
                  isFavorite={isNewsFavorite(item.id)}
                  favoritePending={favoritePendingIds.includes(Number(item.id))}
                  onToggleFavorite={(id) => void toggleNewsFavorite(id)}
                  onOpen={(openItem) => setSelectedNewsId(openItem.id)}
                  onRequestOriginal={async (id) => {
                    try {
                      const response = await fetchNewsOriginal(Number(id))
                      return {
                        title: stripHtml(response.title || '-'),
                        summary: stripHtml(response.summary || '-'),
                      }
                    } catch {
                      return null
                    }
                  }}
                />
              ))
            ) : (
              <p className="fi-empty">
                {appliedSearch ? t('newsPage:noSearchResults') : t('myNewsPage:empty')}
              </p>
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
                ‹ {t('myNewsPage:prev')}
              </button>
              <span className="fi-pagination-summary">
                {t('newsPage:paginationSummary', {
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
                {t('myNewsPage:next')} ›
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
