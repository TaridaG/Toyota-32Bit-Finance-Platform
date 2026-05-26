import { memo, useMemo, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import type { NewsApiItem } from '../../../features/news/api/newsService'
import { buildNewsSidebarStats, type NewsSidebarAssetRow, type NewsSidebarSourceRow, type NewsSidebarTopicRow } from '../../news/lib/buildNewsSidebarStats'
import { mapNewsItem, stripHtml } from '../../news/utils/newsItemMappers'

type NewsPreviewProps = {
  items: NewsApiItem[]
  loading: boolean
  error: boolean
}

const FALLBACK_ITEMS: NewsApiItem[] = [
  {
    id: 1,
    title: 'Merkez bankası sinyalleriyle piyasa akışı yeniden hizalandı',
    summary: 'Makro ve döviz odaklı haberler tek ekranda özetlenir, filtrelenir ve hızlı aksiyona dönüştürülür.',
    sourceName: '32Bit Wire',
    category: 'GENERAL_ECONOMY',
    publishedAt: new Date(Date.now() - 26 * 60 * 1000).toISOString(),
    sentiment: 'positive',
    relatedSymbols: ['USDTRY', 'XU100'],
    topicTags: ['macro', 'fx'],
    reactionPercent1h: 1.24,
    imageUrl: null,
  },
  {
    id: 2,
    title: 'Kripto ve emtia başlıkları tek akışta okunabilir hale geliyor',
    summary: 'Görseller, semboller ve kategori etiketleri ile haber yoğunluğu daha hızlı taranır.',
    sourceName: 'Market Pulse',
    category: 'CRYPTO',
    publishedAt: new Date(Date.now() - 72 * 60 * 1000).toISOString(),
    sentiment: 'neutral',
    relatedSymbols: ['BTCUSDT', 'ETHUSDT'],
    topicTags: ['crypto'],
    reactionPercent1h: -0.82,
    imageUrl: null,
  },
  {
    id: 3,
    title: 'Kullanıcılar favori haberleri yıldızlayıp daha sonra tekrar açabilir',
    summary: 'TR, EN ve DE dil seçenekleri ile aynı haber akışına farklı dil katmanlarıyla erişilir.',
    sourceName: 'Portal Notes',
    category: 'STOCK',
    publishedAt: new Date(Date.now() - 3 * 60 * 60 * 1000).toISOString(),
    sentiment: 'neutral',
    relatedSymbols: ['THYAO', 'GARAN'],
    topicTags: ['bist'],
    reactionPercent1h: 0.46,
    imageUrl: null,
  },
]

export const NewsPreview = memo(function NewsPreview({ items, loading, error }: NewsPreviewProps) {
  const { t, i18n } = useTranslation(['landing', 'newsPage', 'common'])
  const previewItems = items.length > 0 ? items.slice(0, 2) : FALLBACK_ITEMS.slice(0, 2)
  const statsItems = items.length > 0 ? items.slice(0, 8) : FALLBACK_ITEMS
  const mappedItems = useMemo(
    () => previewItems.map((item) => mapNewsItem(item, (key, options) => t(`newsPage:${key}`, options))),
    [previewItems, t],
  )
  const sidebarStats = useMemo(() => buildNewsSidebarStats(statsItems), [statsItems])
  const languageCode = (i18n.language || 'tr').split('-')[0].toUpperCase()
  const originalToggleLabel = resolveOriginalToggleLabel(i18n.language)
  const statusKey = loading ? 'statusLoading' : error ? 'statusOffline' : 'statusLive'

  return (
    <div className="news-preview" aria-hidden="true">
      <div className="news-preview-shell">
        <div className="news-preview-topbar">
          <div className="news-preview-searchbar">
            <span className="news-preview-search-placeholder">{t('newsPage:searchPlaceholder')}</span>
            <button type="button" className="news-preview-search-button">
              {t('newsPage:searchAction')}
            </button>
          </div>

          <button type="button" className="news-preview-filter-button">
            <IconFilter />
            {t('newsPage:filterToggle')}
          </button>
        </div>

        <div className="news-preview-layout">
          <div className="news-preview-feed">
            <div className="news-preview-status-row">
              <div className="news-preview-brand">
                <span className="news-preview-brand-badge">
                  <IconNews />
                </span>
                <div className="news-preview-brand-copy">
                  <strong>{t('landing:news.preview.product')}</strong>
                  <span>{t(`landing:news.preview.${statusKey}`)}</span>
                </div>
              </div>
              <span className="news-preview-favorite-hint">
                <IconStar />
                {t('landing:news.preview.favoriteTitle')}
              </span>
            </div>

            <div className="news-preview-story-list">
              {mappedItems.map((item, index) => (
                <article key={item.id} className="news-preview-story-card">
                  <div className="news-preview-story-layout">
                    <div className="news-preview-story-media" style={buildMediaStyle(previewItems[index]?.imageUrl)}>
                      {!previewItems[index]?.imageUrl ? (
                        <div className="news-preview-media-fallback">{pickBadgeLabel(previewItems[index])}</div>
                      ) : null}
                    </div>

                    <div className="news-preview-story-body">
                      <div className="news-preview-story-head">
                        <div className="news-preview-story-title-row">
                          <button type="button" className={`news-preview-story-star${index === 0 ? ' is-active' : ''}`}>
                            {index === 0 ? '★' : '☆'}
                          </button>
                          <strong>{normalizeCopy(item.title)}</strong>
                        </div>
                        <span>{item.timeAgoLabel ?? formatAge(previewItems[index].publishedAt)}</span>
                      </div>

                      <p>{summarize(item.summary, 126)}</p>

                      <div className="news-preview-story-meta">
                        <div className="news-preview-topic-tags">
                          {item.topicTags.slice(0, 2).map((tag) => (
                            <span key={tag} className={`news-preview-topic-chip news-preview-topic-chip-${tag}`}>
                              {t(`newsPage:categories.${tag}`)}
                            </span>
                          ))}
                          <small>{item.source}</small>
                        </div>
                      </div>

                      <div className="news-preview-story-original-row">
                        <span className="news-preview-original-pill">
                          <IconEye />
                          {originalToggleLabel}
                        </span>
                        <span className="news-preview-language-code">{item.translatedLanguage?.toUpperCase() ?? languageCode}</span>
                      </div>

                      <div className="news-preview-related">
                        <small>{t('newsPage:relatedAssetsInline')}</small>
                        <div>
                          {item.relatedAssets.slice(0, 3).map((asset) => (
                            <span key={asset} className="news-preview-related-chip">
                              {formatAsset(asset)}
                            </span>
                          ))}
                        </div>
                      </div>
                    </div>
                  </div>
                </article>
              ))}
            </div>
          </div>

          <aside className="news-preview-sidebar">
            <header className="news-preview-sidebar-header">
              <h3>{t('newsPage:sidebar.weeklyHeading')}</h3>
            </header>

            <SidebarBlock title={t('newsPage:sidebar.categoryBreakdownTitle')}>
              <ul className="news-preview-side-list">
                {sidebarStats.topics.slice(0, 3).map((row) => (
                  <TopicRow key={row.key} row={row} />
                ))}
              </ul>
            </SidebarBlock>

            <SidebarBlock title={t('newsPage:sidebar.topAssetsTitle')}>
              <ul className="news-preview-side-list news-preview-side-list-compact">
                {sidebarStats.topAssets.slice(0, 5).map((row) => (
                  <AssetRow key={row.symbol} row={row} />
                ))}
              </ul>
            </SidebarBlock>

            <SidebarBlock title={t('newsPage:sidebar.portfolioTitle')}>
              <p className="news-preview-side-login">{t('newsPage:sidebar.loginRequired')}</p>
            </SidebarBlock>

            <SidebarBlock title={t('newsPage:sidebar.sourcesTitle')}>
              <ul className="news-preview-side-list news-preview-side-list-compact">
                {sidebarStats.sources.slice(0, 5).map((row) => (
                  <SourceRow key={row.name} row={row} />
                ))}
              </ul>
            </SidebarBlock>
          </aside>
        </div>
      </div>
    </div>
  )
})

function normalizeCopy(value: string | null | undefined): string {
  const text = stripHtml(value?.trim() || '')
  return text || '...'
}

function summarize(value: string | null | undefined, maxLength: number): string {
  const text = normalizeCopy(value)
  if (text.length <= maxLength) {
    return text
  }
  return `${text.slice(0, maxLength - 1).trimEnd()}...`
}

function formatAge(publishedAt: string): string {
  const parsed = Date.parse(publishedAt)
  if (Number.isNaN(parsed)) {
    return '--'
  }

  const diffMinutes = Math.max(Math.floor((Date.now() - parsed) / 60000), 0)
  if (diffMinutes < 60) {
    return `${diffMinutes}m`
  }
  const diffHours = Math.floor(diffMinutes / 60)
  if (diffHours < 24) {
    return `${diffHours}h`
  }
  return `${Math.floor(diffHours / 24)}d`
}

function buildMediaStyle(imageUrl: string | null | undefined) {
  if (!imageUrl) {
    return undefined
  }
  return { backgroundImage: `url(${imageUrl})` }
}

function pickBadgeLabel(item: NewsApiItem): string {
  const firstSymbol = item.relatedSymbols?.find(Boolean)
  if (firstSymbol) {
    return firstSymbol.toUpperCase()
  }
  return item.sourceName.slice(0, 2).toUpperCase()
}

function formatAsset(value: string): string {
  return value.endsWith('USDT') && value.length > 4 ? value.slice(0, -4) : value
}

function resolveOriginalToggleLabel(language: string | undefined): string {
  const normalized = (language ?? 'tr').toLowerCase()
  if (normalized.startsWith('de')) {
    return 'Original anzeigen'
  }
  if (normalized.startsWith('en')) {
    return 'Show original'
  }
  return 'Orijinali gör'
}

function IconNews() {
  return (
    <svg viewBox="0 0 16 16" aria-hidden>
      <rect x="2.2" y="2.4" width="11.6" height="11.2" rx="2.2" fill="none" stroke="currentColor" strokeWidth="1.2" />
      <path d="M5 5.4h6M5 8h6M5 10.6h3.8" fill="none" stroke="currentColor" strokeWidth="1.2" strokeLinecap="round" />
    </svg>
  )
}

function IconStar() {
  return (
    <svg viewBox="0 0 16 16" aria-hidden>
      <path
        d="m8 2.2 1.7 3.4 3.8.6-2.8 2.7.6 3.8L8 10.9 4.7 12.7l.6-3.8-2.8-2.7 3.8-.6L8 2.2Z"
        fill="currentColor"
      />
    </svg>
  )
}

function IconFilter() {
  return (
    <svg viewBox="0 0 16 16" aria-hidden>
      <path d="M2.5 4.2h11M4.8 8h6.4M6.4 11.8h3.2" fill="none" stroke="currentColor" strokeWidth="1.3" strokeLinecap="round" />
    </svg>
  )
}

function IconEye() {
  return (
    <svg viewBox="0 0 16 16" aria-hidden>
      <path d="M1.8 8s2.1-3.2 6.2-3.2S14.2 8 14.2 8s-2.1 3.2-6.2 3.2S1.8 8 1.8 8Z" fill="none" stroke="currentColor" strokeWidth="1.2" />
      <circle cx="8" cy="8" r="1.7" fill="none" stroke="currentColor" strokeWidth="1.2" />
    </svg>
  )
}

function SidebarBlock({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="news-preview-side-card">
      <h4 className="news-preview-side-card-title">{title}</h4>
      {children}
    </section>
  )
}

function TopicRow({ row }: { row: NewsSidebarTopicRow }) {
  const { t } = useTranslation('newsPage')
  return (
    <li>
      <div className="news-preview-side-row news-preview-side-row-topics">
        <span className={`news-preview-side-dot news-preview-side-dot-${row.key}`} aria-hidden />
        <span className="news-preview-side-label">{t(`categories.${row.key}`)}</span>
        <span className="news-preview-side-value">{t('sidebar.weeklyNewsCount', { count: row.count })}</span>
        <span className="news-preview-side-pct">%{row.percent}</span>
      </div>
    </li>
  )
}

function AssetRow({ row }: { row: NewsSidebarAssetRow }) {
  const { t } = useTranslation('newsPage')
  return (
    <li>
      <div className="news-preview-side-row">
        <span className="news-preview-side-avatar" aria-hidden>
          {row.symbol.slice(0, 1)}
        </span>
        <span className="news-preview-side-label">{row.symbol}</span>
        <span className="news-preview-side-value">{t('sidebar.weeklyNewsCount', { count: row.count })}</span>
      </div>
    </li>
  )
}

function SourceRow({ row }: { row: NewsSidebarSourceRow }) {
  const { t } = useTranslation('newsPage')
  return (
    <li>
      <div className="news-preview-side-row">
        <span className="news-preview-side-avatar" aria-hidden>
          {row.name.slice(0, 1).toUpperCase()}
        </span>
        <span className="news-preview-side-label">{row.name}</span>
        <span className="news-preview-side-value">{t('sidebar.weeklyNewsCount', { count: row.count })}</span>
      </div>
    </li>
  )
}
