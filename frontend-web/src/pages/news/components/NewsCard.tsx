import { useState } from 'react'
import type { NewsDataPoint } from '../types'
import { useTranslation } from 'react-i18next'

export function NewsCard({
  item,
  onOpen,
  onRequestOriginal,
}: {
  item: NewsDataPoint
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

  return (
    <article className="fi-news-card" onClick={() => onOpen(item)} role="button" tabIndex={0}>
      <div className="fi-news-card-head">
        <strong>{displayTitle}</strong>
        <span>{item.timeAgoLabel ?? t('time.minutesAgo', { count: item.timeAgoMinutes })}</span>
      </div>

      <p>{displaySummary}</p>

      <div className="fi-news-card-meta">
        <small>{item.source}</small>
        <span className={`fi-sentiment fi-sentiment-${item.sentiment}`}>{t(`sentiment.${item.sentiment}`)}</span>
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

      {item.tags.length > 0 ? (
        <div className="fi-news-tags">
          {item.tags.map((tag) => (
            <span key={tag}>{tag}</span>
          ))}
        </div>
      ) : null}

      {item.relatedAssets.length > 0 ? (
        <div className="fi-news-assets">
          {item.relatedAssets.map((asset) => (
            <span key={asset}>{asset}</span>
          ))}
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
