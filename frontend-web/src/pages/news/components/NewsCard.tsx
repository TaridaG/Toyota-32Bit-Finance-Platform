import { useMemo, useState } from 'react'
import type { NewsDataPoint } from '../types'
import { useTranslation } from 'react-i18next'
import { collectMatchedKeywords, highlightSearchText } from '../utils/highlightSearchText'

export function NewsCard({
  item,
  searchQuery,
  onOpen,
  onRequestOriginal,
}: {
  item: NewsDataPoint
  searchQuery?: string
  onOpen: (item: NewsDataPoint) => void
  onRequestOriginal: (id: string) => Promise<{ title: string; summary: string } | null>
}) {
  const { t, i18n } = useTranslation('newsPage')
  const isPositive = item.reactionPercent1h >= 0
  const [showOriginal, setShowOriginal] = useState(false)
  const [originalText, setOriginalText] = useState<{ title: string; summary: string } | null>(null)
  const [loadingOriginal, setLoadingOriginal] = useState(false)
  const effectiveOriginal = originalText ?? (item.titleOriginal ? { title: item.titleOriginal, summary: item.summaryOriginal ?? '' } : null)
  const displayTitle = showOriginal ? effectiveOriginal?.title ?? item.title : item.title
  const displaySummary = showOriginal ? effectiveOriginal?.summary ?? item.summary : item.summary
  const language = i18n.language.toLowerCase()
  const showOriginalLabel =
    language.startsWith('tr') ? 'Orijinali gör' : language.startsWith('de') ? 'Original anzeigen' : 'Show original'
  const showTranslatedLabel =
    language.startsWith('tr') ? 'Çeviriyi gör' : language.startsWith('de') ? 'Übersetzung anzeigen' : 'Show translation'
  const activeSearch = searchQuery?.trim() ?? ''
  const matchedKeywords = useMemo(() => {
    if (!activeSearch) {
      return []
    }
    return collectMatchedKeywords(
      `${displayTitle} ${displaySummary}`,
      activeSearch,
      item.relatedAssets,
    )
  }, [activeSearch, displaySummary, displayTitle, item.relatedAssets])

  return (
    <article className="fi-news-card" onClick={() => onOpen(item)} role="button" tabIndex={0}>
      <div className="fi-news-card-head">
        <strong>{activeSearch ? highlightSearchText(displayTitle, activeSearch) : displayTitle}</strong>
        <span>{item.timeAgoLabel ?? t('time.minutesAgo', { count: item.timeAgoMinutes })}</span>
      </div>

      <p className="fi-news-card-summary">
        {activeSearch ? highlightSearchText(displaySummary, activeSearch) : displaySummary}
      </p>

      <div className="fi-news-card-labels">
        <div className="fi-news-topic-tags">
          {item.topicTags.map((tag) => (
            <span key={tag} className="fi-news-category-chip">
              {t(`categories.${tag}`)}
            </span>
          ))}
        </div>
        <span className={`fi-sentiment fi-sentiment-${item.sentiment}`}>{t(`sentiment.${item.sentiment}`)}</span>
        <small className="fi-news-card-source">{item.source}</small>
      </div>

      {item.translated ? (
        <div className="fi-news-translate-row">
          <button
            type="button"
            className="fi-translate-toggle"
            onClick={async (event) => {
              event.stopPropagation()
              if (!showOriginal) {
                if (!effectiveOriginal) {
                  try {
                    setLoadingOriginal(true)
                    const fetched = await onRequestOriginal(item.id)
                    if (fetched) {
                      setOriginalText(fetched)
                    }
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
          <small>{showOriginal ? 'Original' : item.translatedLanguage?.toUpperCase()}</small>
        </div>
      ) : null}

      {matchedKeywords.length > 0 ? (
        <div className="fi-news-keywords">
          <small>{t('matchedKeywords')}</small>
          <div>
            {matchedKeywords.map((keyword) => (
              <span key={keyword} className="fi-news-keyword-chip">
                {keyword}
              </span>
            ))}
          </div>
        </div>
      ) : null}

      {item.relatedAssets.length > 0 ? (
        <div className="fi-news-assets fi-news-assets-inline">
          <small>{t('relatedAssetsInline')}</small>
          <div>
            {item.relatedAssets.map((asset) => (
              <span
                key={asset}
                className={
                  activeSearch && asset.toLowerCase().includes(activeSearch.toLowerCase())
                    ? 'fi-news-asset-chip fi-news-tag-match'
                    : 'fi-news-asset-chip'
                }
              >
                {formatAssetSymbol(asset)}
              </span>
            ))}
          </div>
        </div>
      ) : null}

      <div className="fi-news-reaction">
        <small>{t('marketReaction')}</small>
        <strong className={isPositive ? 'fi-up' : 'fi-down'}>
          {isPositive ? '+' : ''}
          {t('reactionLast1h', { value: item.reactionPercent1h.toFixed(2) })}
        </strong>
      </div>
    </article>
  )
}

function formatAssetSymbol(symbol: string): string {
  if (symbol.endsWith('USDT') && symbol.length > 4) {
    return symbol.slice(0, -4)
  }
  return symbol
}
