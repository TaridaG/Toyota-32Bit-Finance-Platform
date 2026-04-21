import type { NewsDataPoint } from '../types'

const sentimentLabel = {
  positive: 'Positive',
  negative: 'Negative',
  neutral: 'Neutral',
} as const

export function NewsCard({ item, onOpen }: { item: NewsDataPoint; onOpen: (item: NewsDataPoint) => void }) {
  const isPositive = item.reactionPercent1h >= 0

  return (
    <article className="fi-news-card" onClick={() => onOpen(item)}>
      <div className="fi-news-card-head">
        <strong>{item.title}</strong>
        <span>{item.timeAgoMinutes} min ago</span>
      </div>

      <p>{item.summary}</p>

      <div className="fi-news-card-meta">
        <small>{item.source}</small>
        <span className={`fi-sentiment fi-sentiment-${item.sentiment}`}>{sentimentLabel[item.sentiment]}</span>
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
        <small>Market Reaction</small>
        <strong className={isPositive ? 'fi-up' : 'fi-down'}>
          {isPositive ? '+' : ''}
          {item.reactionPercent1h.toFixed(2)}% in last 1h
        </strong>
      </div>
    </article>
  )
}
