import type { SentimentType } from '../types'
import { useTranslation } from 'react-i18next'

type SentimentCounts = Record<SentimentType, number>

export function SentimentChart({ counts }: { counts: SentimentCounts }) {
  const { t } = useTranslation('newsPage')
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
      <h3>{t('sentimentTitle')}</h3>
      <div className="fi-sentiment-wrap">
        <div className="fi-sentiment-pie" style={{ background: gradient }}>
          <div>
            <strong>{total}</strong>
            <span>{t('newsCountLabel')}</span>
          </div>
        </div>
      </div>
      <ul className="fi-sentiment-legend">
        <li><span className="fi-dot fi-dot-positive" /> {t('sentiment.positive')} ({positivePct.toFixed(0)}%)</li>
        <li><span className="fi-dot fi-dot-negative" /> {t('sentiment.negative')} ({negativePct.toFixed(0)}%)</li>
        <li><span className="fi-dot fi-dot-neutral" /> {t('sentiment.neutral')} ({neutralPct.toFixed(0)}%)</li>
      </ul>
    </article>
  )
}
