import { useMemo, useState } from 'react'
import type { SyntheticEvent } from 'react'
import type { NewsDataPoint } from '../types'
import { useTranslation } from 'react-i18next'
import { collectMatchedKeywords, highlightSearchText } from '../utils/highlightSearchText'
import {
  resolveNewsDisplayText,
  shouldShowNewsTranslationToggle,
} from '../utils/newsTranslationDisplay'

export function NewsCard({
  item,
  searchQuery,
  onOpen,
  onRequestOriginal,
  showFavoriteStar = false,
  isFavorite = false,
  favoritePending = false,
  onToggleFavorite,
}: {
  item: NewsDataPoint
  searchQuery?: string
  onOpen: (item: NewsDataPoint) => void
  onRequestOriginal: (id: string) => Promise<{ title: string; summary: string } | null>
  showFavoriteStar?: boolean
  isFavorite?: boolean
  favoritePending?: boolean
  onToggleFavorite?: (newsId: string) => void
}) {
  const { t, i18n } = useTranslation('newsPage')
  const [showOriginal, setShowOriginal] = useState(false)
  const [originalText, setOriginalText] = useState<{ title: string; summary: string } | null>(null)
  const [loadingOriginal, setLoadingOriginal] = useState(false)
  const [imageBroken, setImageBroken] = useState(false)
  const showImage = Boolean(item.imageUrl?.trim()) && !imageBroken
  const effectiveOriginal =
    originalText ??
    (item.titleOriginal
      ? { title: item.titleOriginal, summary: item.summaryOriginal ?? '' }
      : null)
  const translatedText = { title: item.title, summary: item.summary }
  const { title: displayTitle, summary: displaySummary } = resolveNewsDisplayText(
    showOriginal,
    translatedText,
    effectiveOriginal,
  )
  const showTranslationToggle = shouldShowNewsTranslationToggle(item)
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

  const handleImageError = (event: SyntheticEvent<HTMLImageElement>) => {
    event.currentTarget.style.display = 'none'
    setImageBroken(true)
  }

  const cardBody = (
    <>
      <div className="fi-news-card-head">
        <div className="fi-news-card-title-row">
          {showFavoriteStar ? (
            <button
              type="button"
              aria-label={isFavorite ? t('unfavorite') : t('favorite')}
              className={`fi-news-star${isFavorite ? ' fi-news-star-active' : ''}`}
              onMouseDown={(event) => event.stopPropagation()}
              onClick={(event) => {
                event.stopPropagation()
                onToggleFavorite?.(item.id)
              }}
              disabled={favoritePending}
            >
              {isFavorite ? '★' : '☆'}
            </button>
          ) : null}
          <strong>{activeSearch ? highlightSearchText(displayTitle, activeSearch) : displayTitle}</strong>
        </div>
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
        <small className="fi-news-card-source">{item.source}</small>
      </div>

      {showTranslationToggle ? (
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
    </>
  )

  return (
    <article
      className={`fi-news-card${showImage ? ' fi-news-card-has-media' : ''}`}
      onClick={() => onOpen(item)}
      role="button"
      tabIndex={0}
    >
      {showImage ? (
        <div className="fi-news-card-layout">
          <div className="fi-news-card-media">
            <img
              src={item.imageUrl ?? ''}
              alt=""
              loading="lazy"
              decoding="async"
              className="fi-news-card-thumb"
              onError={handleImageError}
            />
          </div>
          <div className="fi-news-card-body">{cardBody}</div>
        </div>
      ) : (
        cardBody
      )}
    </article>
  )
}

function formatAssetSymbol(symbol: string): string {
  if (symbol.endsWith('USDT') && symbol.length > 4) {
    return symbol.slice(0, -4)
  }
  return symbol
}
