import type { TrendItem } from '../types'

export function TrendingList({ title, items }: { title: string; items: TrendItem[] }) {
  const maxAbs = Math.max(...items.map((item) => Math.abs(item.changePercent)), 1)

  return (
    <article className="card fi-panel-card">
      <h3>{title}</h3>
      <ul className="fi-trending-list">
        {items.map((item) => {
          const positive = item.changePercent >= 0
          const width = `${(Math.abs(item.changePercent) / maxAbs) * 100}%`
          return (
            <li key={item.asset}>
              <div>
                <strong>{item.asset}</strong>
                <span className={positive ? 'fi-up' : 'fi-down'}>
                  {positive ? '+' : ''}
                  {item.changePercent.toFixed(2)}%
                </span>
              </div>
              <div className="fi-trend-bar-track">
                <span className={positive ? 'fi-trend-bar fi-trend-bar-up' : 'fi-trend-bar fi-trend-bar-down'} style={{ width }} />
              </div>
            </li>
          )
        })}
      </ul>
    </article>
  )
}
