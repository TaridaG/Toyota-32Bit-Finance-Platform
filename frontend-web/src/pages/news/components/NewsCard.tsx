import type { NewsDataPoint } from '../types'
import { useTranslation } from 'react-i18next'

export function NewsCard({ item, onOpen }: { item: NewsDataPoint; onOpen: (item: NewsDataPoint) => void }) {
  const { t } = useTranslation('newsPage')
  const isPositive = item.reactionPercent1h >= 0

  return (
    <article className="fi-news-card" onClick={() => onOpen(item)}>
      <div className="fi-news-card-head">
        <strong>{item.title}</strong>
        <span>{item.timeAgoLabel ?? t('time.minutesAgo', { count: item.timeAgoMinutes })}</span>
      </div>

      <p>{item.summary}</p>

      <div className="fi-news-card-meta">
        <small>{item.source}</small>
        <span className={`fi-sentiment fi-sentiment-${item.sentiment}`}>{t(`sentiment.${item.sentiment}`)}</span>
      </div>

      <div className="fi-news-tags">
        {item.tags.map((tag) => (
          <span key={tag}>{tag}</span>
        ))}
      </div>

      <div className="fi-news-assets">
        {item.relatedAssets.map((asset) => (
          <span key={asset}>{asset}</span>
        ))}
      </div>

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
