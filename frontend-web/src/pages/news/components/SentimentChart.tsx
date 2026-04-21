import type { SentimentType } from '../types'

type SentimentCounts = Record<SentimentType, number>

export function SentimentChart({ counts }: { counts: SentimentCounts }) {
  const total = counts.positive + counts.negative + counts.neutral || 1
  const positivePct = (counts.positive / total) * 100
  const negativePct = (counts.negative / total) * 100
  const neutralPct = (counts.neutral / total) * 100

  const gradient = `conic-gradient(
      #22c55e 0 ${positivePct}%,
      #ef4444 ${positivePct}% ${positivePct + negativePct}%,
      #94a3b8 ${positivePct + negativePct}% 100%
    )`

  return (
    <article className="card fi-panel-card">
      <h3>Sentiment Overview</h3>
      <div className="fi-sentiment-wrap">
        <div className="fi-sentiment-pie" style={{ background: gradient }}>
          <div>
            <strong>{total}</strong>
            <span>news</span>
          </div>
        </div>
      </div>
      <ul className="fi-sentiment-legend">
        <li><span className="fi-dot fi-dot-positive" /> Positive ({positivePct.toFixed(0)}%)</li>
        <li><span className="fi-dot fi-dot-negative" /> Negative ({negativePct.toFixed(0)}%)</li>
        <li><span className="fi-dot fi-dot-neutral" /> Neutral ({neutralPct.toFixed(0)}%)</li>
      </ul>
    </article>
  )
}
