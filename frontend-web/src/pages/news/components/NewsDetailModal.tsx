import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { fetchNewsDetail, type NewsDetailApi } from '../../../features/news/api/newsService'
import { highlightSearchText } from '../utils/highlightSearchText'
import { resolveNewsArticleUrl } from '../utils/resolveNewsArticleUrl'

type NewsDetailModalProps = {
  newsId: string
  searchQuery?: string
  onClose: () => void
}

const CHANGE_COLUMNS = [
  { key: 'change1d' as const, labelKey: 'horizon.d' },
  { key: 'change1w' as const, labelKey: 'horizon.w' },
  { key: 'change1m' as const, labelKey: 'horizon.m' },
  { key: 'change3m' as const, labelKey: 'horizon.3m' },
  { key: 'change6m' as const, labelKey: 'horizon.6m' },
  { key: 'change1y' as const, labelKey: 'horizon.1y' },
]

export function NewsDetailModal({ newsId, searchQuery, onClose }: NewsDetailModalProps) {
  const { t, i18n } = useTranslation('newsPage')
  const [detail, setDetail] = useState<NewsDetailApi | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const activeSearch = searchQuery?.trim() ?? ''

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError(null)
    void fetchNewsDetail(Number(newsId), i18n.language)
      .then((data) => {
        if (!cancelled) {
          setDetail(data)
        }
      })
      .catch(() => {
        if (!cancelled) {
          setError('loadError')
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false)
        }
      })
    return () => {
      cancelled = true
    }
  }, [i18n.language, newsId])

  const bodyText = stripHtml(detail?.summary?.trim() || '')
  const sourceUrl = detail ? resolveNewsArticleUrl(detail.articleUrl, detail.sourceName) : ''

  return (
    <div className="fi-modal-wrap fi-news-detail-modal" role="dialog" aria-modal="true">
      <button className="fi-modal-backdrop" onClick={onClose} aria-label={t('closeDetails')} />
      <article className="card fi-modal fi-news-detail-card">
        <div className="fi-modal-head">
          <h3>{detail ? (activeSearch ? highlightSearchText(detail.title, activeSearch) : detail.title) : t('common:loading')}</h3>
          <button type="button" onClick={onClose} aria-label={t('closeDetails')}>
            ×
          </button>
        </div>

        {loading ? <p className="fi-empty">{t('common:loading')}</p> : null}
        {error ? <p className="fi-empty">{t(error)}</p> : null}

        {!loading && !error && detail ? (
          <div className="fi-news-detail-body">
            <div className="fi-news-detail-meta-row">
              <div className="fi-news-topic-tags">
                {(detail.topicTags?.length ? detail.topicTags : [detail.categoryUi]).map((tag) => (
                  <span key={tag} className="fi-news-category-chip">
                    {t(`categories.${mapCategoryUi(tag)}`)}
                  </span>
                ))}
              </div>
              <span className={`fi-sentiment fi-sentiment-${detail.sentiment}`}>{t(`sentiment.${detail.sentiment}`)}</span>
              <small>{detail.sourceName}</small>
              <small>{formatPublished(detail.publishedAt, t)}</small>
            </div>

            <div className="fi-news-detail-text">
              {activeSearch ? highlightSearchText(bodyText, activeSearch) : bodyText}
            </div>

            {sourceUrl ? (
              <a className="fi-news-detail-source-link" href={sourceUrl} target="_blank" rel="noreferrer">
                {t('openSource')}
              </a>
            ) : null}

            {detail.relatedAssets.length > 0 ? (
              <section className="fi-news-detail-assets">
                <h4>{t('relatedAssetsTitle')}</h4>
                <div className="fi-news-detail-assets-scroll">
                  <div className="fi-news-detail-assets-table">
                    <div className="fi-news-detail-assets-head">
                      <span>{t('assetColumn')}</span>
                      {CHANGE_COLUMNS.map((col) => (
                        <span key={col.key}>{t(col.labelKey)}</span>
                      ))}
                      <span />
                    </div>
                    {detail.relatedAssets.map((asset) => (
                      <div key={asset.symbol} className="fi-news-detail-assets-row">
                        <div className="fi-news-detail-asset-id">
                          <strong>{formatAssetSymbol(asset.symbol)}</strong>
                          <small>{asset.name}</small>
                        </div>
                        {CHANGE_COLUMNS.map((col) => (
                          <span key={col.key} className={changeClass(asset[col.key])}>
                            {formatChange(asset[col.key])}
                          </span>
                        ))}
                        <Link
                          className="fi-news-detail-analyze-btn"
                          to={buildAnalysisPath(asset.symbol)}
                          onClick={onClose}
                        >
                          {t('analyzeAsset')}
                        </Link>
                      </div>
                    ))}
                  </div>
                </div>
              </section>
            ) : (
              <p className="fi-news-detail-empty-assets">{t('noRelatedAssets')}</p>
            )}
          </div>
        ) : null}
      </article>
    </div>
  )
}

function stripHtml(value: string): string {
  return value.replace(/<[^>]*>/g, ' ').replace(/\s+/g, ' ').trim()
}

function mapCategoryUi(categoryUi: string): string {
  const normalized = categoryUi?.toLowerCase() ?? 'macro'
  if (['all', 'bist', 'viop', 'fx', 'crypto', 'macro'].includes(normalized)) {
    return normalized
  }
  return 'macro'
}

function formatPublished(publishedAt: string, t: (key: string, options?: Record<string, unknown>) => string): string {
  const ts = Date.parse(publishedAt)
  if (Number.isNaN(ts)) {
    return ''
  }
  const minutes = Math.max(Math.floor((Date.now() - ts) / 60000), 0)
  if (minutes < 60) {
    return t('time.minutesAgo', { count: minutes })
  }
  return t('time.hoursAgo', { count: Math.floor(minutes / 60) })
}

function formatChange(value: number | null): string {
  if (value == null || Number.isNaN(value)) {
    return '—'
  }
  const sign = value > 0 ? '+' : ''
  return `${sign}${value.toFixed(2)}%`
}

function formatAssetSymbol(symbol: string): string {
  if (symbol.endsWith('USDT') && symbol.length > 4) {
    return symbol.slice(0, -4)
  }
  return symbol
}

function buildAnalysisPath(symbol: string): string {
  return `/app/analysis?symbol=${encodeURIComponent(symbol.replace('/', '').toUpperCase())}`
}

function changeClass(value: number | null): string {
  if (value == null || Number.isNaN(value)) {
    return 'fi-news-change-neutral'
  }
  return value >= 0 ? 'fi-news-change-up' : 'fi-news-change-down'
}
