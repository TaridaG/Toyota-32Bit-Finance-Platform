import { useEffect, useMemo, useState, type SyntheticEvent } from 'react'
import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'
import { fetchNewsDetail, fetchNewsOriginal, type NewsDetailApi } from '../../../features/news/api/newsService'
import { highlightSearchText } from '../utils/highlightSearchText'
import { resolveNewsArticleUrl } from '../utils/resolveNewsArticleUrl'
import {
  resolveNewsDisplayText,
  shouldShowNewsTranslationToggle,
} from '../utils/newsTranslationDisplay'

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
  const [showOriginal, setShowOriginal] = useState(false)
  const [originalText, setOriginalText] = useState<{ title: string; summary: string } | null>(null)
  const [loadingOriginal, setLoadingOriginal] = useState(false)
  const [detailImageBroken, setDetailImageBroken] = useState(false)
  const activeSearch = searchQuery?.trim() ?? ''
  const language = i18n.language.toLowerCase()
  const showOriginalLabel =
    language.startsWith('tr') ? 'Orijinali gör' : language.startsWith('de') ? 'Original anzeigen' : 'Show original'
  const showTranslatedLabel =
    language.startsWith('tr') ? 'Çeviriyi gör' : language.startsWith('de') ? 'Übersetzung anzeigen' : 'Show translation'

  useEffect(() => {
    setShowOriginal(false)
    setOriginalText(null)
    setDetailImageBroken(false)
  }, [newsId, i18n.language])

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

  const effectiveOriginal = useMemo(
    () =>
      originalText ??
      (detail?.titleOriginal
        ? { title: stripHtml(detail.titleOriginal), summary: stripHtml(detail.summaryOriginal?.trim() || '') }
        : null),
    [detail?.summaryOriginal, detail?.titleOriginal, originalText],
  )
  const displayText = useMemo(() => {
    if (!detail) {
      return { title: '', summary: '' }
    }
    return resolveNewsDisplayText(
      showOriginal,
      { title: stripHtml(detail.title), summary: stripHtml(detail.summary?.trim() || '') },
      effectiveOriginal,
    )
  }, [detail, effectiveOriginal, showOriginal])
  const showTranslationToggle = detail ? shouldShowNewsTranslationToggle(detail) : false
  const sourceUrl = detail ? resolveNewsArticleUrl(detail.articleUrl, detail.sourceName) : ''

  return (
    <div className="fi-modal-wrap fi-news-detail-modal" role="dialog" aria-modal="true">
      <button className="fi-modal-backdrop" onClick={onClose} aria-label={t('closeDetails')} />
      <article className="card fi-modal fi-news-detail-card">
        <div className="fi-modal-head">
          <h3>
            {detail
              ? activeSearch
                ? highlightSearchText(displayText.title, activeSearch)
                : displayText.title
              : t('common:loading')}
          </h3>
          <button type="button" onClick={onClose} aria-label={t('closeDetails')}>
            ×
          </button>
        </div>

        {loading ? <p className="fi-empty">{t('common:loading')}</p> : null}
        {error ? <p className="fi-empty">{t(error)}</p> : null}

        {!loading && !error && detail ? (
          <div className="fi-news-detail-body">
            {detail.imageUrl?.trim() && !detailImageBroken ? (
              <div className="fi-news-detail-media">
                <img
                  src={detail.imageUrl}
                  alt=""
                  className="fi-news-detail-thumb"
                  loading="lazy"
                  decoding="async"
                  onError={(event: SyntheticEvent<HTMLImageElement>) => {
                    event.currentTarget.style.display = 'none'
                    setDetailImageBroken(true)
                  }}
                />
              </div>
            ) : null}
            <div className="fi-news-detail-meta-row">
              <div className="fi-news-topic-tags">
                {(detail.topicTags?.length ? detail.topicTags : [detail.categoryUi]).map((tag) => (
                  <span key={tag} className="fi-news-category-chip">
                    {t(`categories.${mapCategoryUi(tag)}`)}
                  </span>
                ))}
              </div>
              <small>{detail.sourceName}</small>
              <small>{formatPublished(detail.publishedAt, t)}</small>
            </div>

            {showTranslationToggle ? (
              <div className="fi-news-translate-row">
                <button
                  type="button"
                  className="fi-translate-toggle"
                  onClick={async () => {
                    if (!showOriginal) {
                      if (!effectiveOriginal) {
                        try {
                          setLoadingOriginal(true)
                          const response = await fetchNewsOriginal(Number(newsId))
                          setOriginalText({
                            title: stripHtml(response.title),
                            summary: stripHtml(response.summary?.trim() || ''),
                          })
                        } finally {
                          setLoadingOriginal(false)
                        }
                      }
                      setShowOriginal(true)
                      return
                    }
                    setShowOriginal(false)
                  }}
                  disabled={loadingOriginal}
                >
                  👁 {loadingOriginal ? t('common:loading') : showOriginal ? showTranslatedLabel : showOriginalLabel}
                </button>
                <small>{showOriginal ? 'Original' : detail.translatedLanguage?.toUpperCase()}</small>
              </div>
            ) : null}

            <div className="fi-news-detail-text">
              {activeSearch ? highlightSearchText(displayText.summary, activeSearch) : displayText.summary}
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
