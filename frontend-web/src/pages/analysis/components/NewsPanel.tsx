import type { AssetNewsItem } from '../types'

type NewsPanelProps = {
  items: AssetNewsItem[]
  selectedId: string | null
  onSelect: (item: AssetNewsItem) => void
}

export function NewsPanel({ items, selectedId, onSelect }: NewsPanelProps) {
  return (
    <article className="card fi-analysis-news-panel">
      <div className="fi-panel-head">
        <h3>Related News</h3>
      </div>
      <ul>
        {items.map((item) => (
          <li key={item.id}>
            <button
              type="button"
              className={`fi-analysis-news-item${selectedId === item.id ? ' fi-analysis-news-item-active' : ''}`}
              onClick={() => onSelect(item)}
            >
              <strong>{item.title}</strong>
              <small>
                {item.source} • {new Date(item.createdAt * 1000).toLocaleTimeString()}
              </small>
            </button>
          </li>
        ))}
      </ul>
    </article>
  )
}
