import { useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { getMyPortfolioOverview } from '../../features/portfolio/api/portfolioApi'
import { useDocumentTitle } from '../../shared/hooks/useDocumentTitle'
import { isAuthenticated } from '../../shared/auth/session'
import type { NewsDataPoint } from './types'
import { mapNewsItem, stripHtml } from './utils/newsItemMappers'
import { fetchNewsOriginal } from '../../features/news/api/newsService'
import {
  addNewsFavorite,
  fetchNewsFavorites,
  removeNewsFavorite,
} from '../../features/news/api/newsFavoritesApi'
import { useNews } from '../../features/news/hooks/useNews'
import type { NewsFetchFilters, NewsFetchOptions } from '../../features/news/api/newsService'
import { NewsCard } from './components/NewsCard'
import { NewsDetailModal } from './components/NewsDetailModal'
import { NewsPageSidebar } from './components/NewsPageSidebar'
import { useNewsSidebarInsights } from './hooks/useNewsSidebarInsights'
import type { NewsTopicKey } from './lib/buildNewsSidebarStats'

type WeeklyFilterSnapshot = {
  filters: NewsFetchFilters
  searchDraft: string
  appliedSearch: string
  showFavoritesOnly: boolean
}

type ActiveWeeklySidebarFilter =
  | { kind: 'portfolio' }
  | { kind: 'topic'; key: NewsTopicKey }
  | { kind: 'asset'; symbol: string }
  | { kind: 'source'; name: string }
  | null

const EMPTY_NEWS_FETCH_OPTIONS: NewsFetchOptions = {}

export function NewsPage() {
  const { t, i18n } = useTranslation('newsPage')
  const [page, setPage] = useState(0)
  const pageSize = 10
  const defaultFilters: NewsFetchFilters = { category: 'all', range: 'all' }
  const [filtersOpen, setFiltersOpen] = useState(false)
  const [draftFilters, setDraftFilters] = useState<NewsFetchFilters>(defaultFilters)
  const [appliedFilters, setAppliedFilters] = useState<NewsFetchFilters>(defaultFilters)
  const [searchDraft, setSearchDraft] = useState('')
  const [appliedSearch, setAppliedSearch] = useState('')
  const [authenticated, setAuthenticated] = useState(isAuthenticated)
  const [portfolioSymbols, setPortfolioSymbols] = useState<string[]>([])
  const [portfolioSymbolsHydrated, setPortfolioSymbolsHydrated] = useState(() => !isAuthenticated())
  const [activeWeeklySidebarFilter, setActiveWeeklySidebarFilter] = useState<ActiveWeeklySidebarFilter>(null)
  const [weeklyFilterSnapshot, setWeeklyFilterSnapshot] = useState<WeeklyFilterSnapshot | null>(null)
  const [showFavoritesOnly, setShowFavoritesOnly] = useState(false)
  const [favoriteNewsIds, setFavoriteNewsIds] = useState<number[]>([])
  const [favoriteNotice, setFavoriteNotice] = useState<string | null>(null)
  const [favoritePendingIds, setFavoritePendingIds] = useState<number[]>([])
  useDocumentTitle(t('titleDoc'))
  const isWeeklySidebarFilterActive = activeWeeklySidebarFilter != null
  const newsRequestOptions = useMemo<NewsFetchOptions>(
    () => {
      if (activeWeeklySidebarFilter == null) {
        return EMPTY_NEWS_FETCH_OPTIONS
      }
      switch (activeWeeklySidebarFilter.kind) {
        case 'portfolio':
          return {
            relatedSymbols: portfolioSymbols.length > 0 ? portfolioSymbols : undefined,
          }
        case 'topic':
          return {
            primaryTopic: activeWeeklySidebarFilter.key,
          }
        case 'asset':
          return {
            assetKey: activeWeeklySidebarFilter.symbol,
          }
        case 'source':
          return {
            sourceName: activeWeeklySidebarFilter.name,
          }
      }
    },
    [activeWeeklySidebarFilter, portfolioSymbols],
  )
  const {
    data: streamApiData,
    loading: streamLoading,
    error: streamError,
    refetch: refetchStream,
    totalElements,
    totalPages,
  } = useNews(page, pageSize, i18n.language, appliedFilters, appliedSearch, newsRequestOptions)
  const [selectedNewsId, setSelectedNewsId] = useState<string | null>(null)

  const { stats: sidebarStats, loading: sidebarLoading } = useNewsSidebarInsights(
    i18n.language,
    portfolioSymbols,
    !authenticated || portfolioSymbolsHydrated,
  )

  useEffect(() => {
    const syncAuth = () => setAuthenticated(isAuthenticated())
    window.addEventListener('storage', syncAuth)
    return () => window.removeEventListener('storage', syncAuth)
  }, [])

  useEffect(() => {
    if (!authenticated) {
      setPortfolioSymbols([])
      setPortfolioSymbolsHydrated(true)
      setActiveWeeklySidebarFilter(null)
      setWeeklyFilterSnapshot(null)
      return
    }
    let cancelled = false
    setPortfolioSymbolsHydrated(false)
    void getMyPortfolioOverview()
      .then((overview) => {
        if (!cancelled) {
          setPortfolioSymbols(overview.items.map((item) => item.symbol).filter(Boolean))
        }
      })
      .catch(() => {
        if (!cancelled) {
          setPortfolioSymbols([])
        }
      })
      .finally(() => {
        if (!cancelled) {
          setPortfolioSymbolsHydrated(true)
        }
      })
    return () => {
      cancelled = true
    }
  }, [authenticated, i18n.language])

  const streamNews = useMemo<NewsDataPoint[]>(
    () => streamApiData.map((item) => mapNewsItem(item, t)),
    [streamApiData, t],
  )

  const isNewsFavorite = (newsId: string) => favoriteNewsIds.includes(Number(newsId))

  useEffect(() => {
    if (!authenticated) {
      setFavoriteNewsIds([])
      setShowFavoritesOnly(false)
      return
    }
    void fetchNewsFavorites()
      .then((rows) => setFavoriteNewsIds(rows.map((item) => item.newsId)))
      .catch(() => {
        // keep current client state if favorites fetch fails
      })
  }, [authenticated])

  const showFavoriteErrorNotice = (message: string) => {
    setFavoriteNotice(message)
    window.setTimeout(() => {
      setFavoriteNotice((current) => (current === message ? null : current))
    }, 3500)
  }

  const toggleNewsFavorite = async (newsId: string) => {
    const numericId = Number(newsId)
    if (!Number.isFinite(numericId)) {
      return
    }
    if (favoritePendingIds.includes(numericId)) {
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
    } catch {
      showFavoriteErrorNotice(t('favoriteUpdateError'))
    } finally {
      setFavoritePendingIds((prev) => prev.filter((id) => id !== numericId))
    }
  }

  const visibleNews = useMemo(() => {
    let rows = streamNews
    if (showFavoritesOnly) {
      rows = rows.filter((item) => isNewsFavorite(item.id))
    }
    return rows
  }, [favoriteNewsIds, showFavoritesOnly, streamNews])

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

  const activateWeeklySidebarFilter = (nextFilter: Exclude<ActiveWeeklySidebarFilter, null>) => {
    if (nextFilter.kind === 'portfolio' && portfolioSymbols.length === 0) {
      return
    }
    if (!isWeeklySidebarFilterActive) {
      setWeeklyFilterSnapshot({
        filters: appliedFilters,
        searchDraft,
        appliedSearch,
        showFavoritesOnly,
      })
    }
    setActiveWeeklySidebarFilter(nextFilter)
    setShowFavoritesOnly(false)
    setSearchDraft('')
    setAppliedSearch('')
    const weeklyFilters: NewsFetchFilters = { category: 'all', range: '7d' }
    setDraftFilters(weeklyFilters)
    setAppliedFilters(weeklyFilters)
    setPage(0)
  }

  const clearWeeklySidebarFilter = () => {
    setActiveWeeklySidebarFilter(null)
    if (weeklyFilterSnapshot) {
      setDraftFilters(weeklyFilterSnapshot.filters)
      setAppliedFilters(weeklyFilterSnapshot.filters)
      setSearchDraft(weeklyFilterSnapshot.searchDraft)
      setAppliedSearch(weeklyFilterSnapshot.appliedSearch)
      setShowFavoritesOnly(weeklyFilterSnapshot.showFavoritesOnly)
      setWeeklyFilterSnapshot(null)
    } else {
      setDraftFilters(defaultFilters)
      setAppliedFilters(defaultFilters)
    }
    setPage(0)
  }

  const activeWeeklyFilterLabel = useMemo(() => {
    if (activeWeeklySidebarFilter == null) return ''
    switch (activeWeeklySidebarFilter.kind) {
      case 'portfolio':
        return t('sidebar.portfolioTitle')
      case 'topic':
        return t(`categories.${activeWeeklySidebarFilter.key}`)
      case 'asset':
        return activeWeeklySidebarFilter.symbol
      case 'source':
        return activeWeeklySidebarFilter.name
    }
  }, [activeWeeklySidebarFilter, t])

  return (
    <>
      <section className="fi-news-page">
        <div className="fi-news-layout">
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
              <div className="fi-news-feed-actions">
                {authenticated ? (
                  <button
                    type="button"
                    className={`markets-filter${showFavoritesOnly ? ' markets-filter-active' : ''}`}
                    onClick={() => setShowFavoritesOnly((prev) => !prev)}
                  >
                    {t('favoritesOnly')}
                  </button>
                ) : null}
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
                            disabled={isWeeklySidebarFilterActive}
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
                        {(['all', '1h', '6h', '24h', '7d'] as const).map((range) => (
                          <button
                            key={range}
                            type="button"
                            className={`fi-filter-chip${draftFilters.range === range ? ' fi-filter-chip-active' : ''}`}
                            disabled={isWeeklySidebarFilterActive && range !== '7d'}
                            onClick={() => setDraftFilters((prev) => ({ ...prev, range }))}
                          >
                            {range === 'all' ? t('timeRangeAll') : range === '7d' ? t('timeRange7d') : range}
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
                          const clearedFilters = isWeeklySidebarFilterActive
                            ? { ...defaultFilters, range: '7d' as const }
                            : defaultFilters
                          setDraftFilters(clearedFilters)
                          setAppliedFilters(clearedFilters)
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
            </div>
            {favoriteNotice ? (
              <div className="markets-error-wrap">
                <span>{favoriteNotice}</span>
              </div>
            ) : null}
            {isWeeklySidebarFilterActive ? (
              <p className="fi-news-portfolio-filter-banner">
                {t('sidebar.weeklyFilterActive', { label: activeWeeklyFilterLabel })}
                <button
                  type="button"
                  className="fi-news-search-clear"
                  onClick={clearWeeklySidebarFilter}
                >
                  {t('sidebar.clearWeeklyFilter')}
                </button>
              </p>
            ) : null}
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
              ) : visibleNews.length > 0 ? (
                visibleNews.map((item) => (
                  <NewsCard
                    key={item.id}
                    item={item}
                    searchQuery={appliedSearch}
                    showFavoriteStar={authenticated}
                    isFavorite={isNewsFavorite(item.id)}
                    favoritePending={favoritePendingIds.includes(Number(item.id))}
                    onToggleFavorite={(id) => void toggleNewsFavorite(id)}
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
                <p className="fi-empty">
                  {showFavoritesOnly
                    ? t('noFavoriteNews')
                    : isWeeklySidebarFilterActive
                      ? t('sidebar.noWeeklyFilterResults')
                      : appliedSearch
                        ? t('noSearchResults')
                        : t('noNews')}
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

        <NewsPageSidebar
          stats={sidebarStats}
          loading={sidebarLoading}
          authenticated={authenticated}
          activeTopicKey={activeWeeklySidebarFilter?.kind === 'topic' ? activeWeeklySidebarFilter.key : null}
          activeAssetSymbol={activeWeeklySidebarFilter?.kind === 'asset' ? activeWeeklySidebarFilter.symbol : null}
          activeSourceName={activeWeeklySidebarFilter?.kind === 'source' ? activeWeeklySidebarFilter.name : null}
          portfolioActive={activeWeeklySidebarFilter?.kind === 'portfolio'}
          onPortfolioNewsClick={() => activateWeeklySidebarFilter({ kind: 'portfolio' })}
          onTopicClick={(key) => activateWeeklySidebarFilter({ kind: 'topic', key })}
          onAssetClick={(symbol) => activateWeeklySidebarFilter({ kind: 'asset', symbol })}
          onSourceClick={(name) => activateWeeklySidebarFilter({ kind: 'source', name })}
        />
        </div>
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

