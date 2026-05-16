import { Link } from 'react-router-dom'
import type { LiteracyEntry } from '../types/financialLiteracy'

type LiteracyTermCardProps = {
  entry: LiteracyEntry
  typeLabel: string
  difficultyLabel: string
  usedInLabel: string
  detailsLabel: string
  portalPageLabel: (page: LiteracyEntry['usedInPortal'][number]['page']) => string
  onOpenDetails: (entry: LiteracyEntry) => void
}

export function LiteracyTermCard({
  entry,
  typeLabel,
  difficultyLabel,
  usedInLabel,
  detailsLabel,
  portalPageLabel,
  onOpenDetails,
}: LiteracyTermCardProps) {
  const portalPages = [...new Set(entry.usedInPortal.map((u) => u.page))]

  return (
    <article className="lit-term-card card">
      <header className="lit-term-card-head">
        <h3>{entry.title}</h3>
        <div className="lit-term-card-badges">
          <span className="lit-badge lit-badge-type">{typeLabel}</span>
          <span className="lit-badge lit-badge-difficulty">{difficultyLabel}</span>
        </div>
      </header>
      <p className="lit-term-card-def">{entry.shortDefinition}</p>
      <p className="lit-term-card-used">
        <strong>{usedInLabel}: </strong>
        {portalPages.map(portalPageLabel).join(' · ')}
      </p>
      {entry.relatedTerms.length > 0 ? (
        <div className="lit-term-card-tags">
          {entry.relatedTerms.slice(0, 4).map((term) => (
            <span key={term} className="lit-tag">
              {term}
            </span>
          ))}
        </div>
      ) : null}
      <footer className="lit-term-card-actions">
        <button type="button" className="lit-btn-primary" onClick={() => onOpenDetails(entry)}>
          {detailsLabel}
        </button>
        {entry.usedInPortal[0]?.route ? (
          <Link to={entry.usedInPortal[0].route} className="lit-btn-secondary">
            {entry.usedInPortal[0].description.split(' ').slice(0, 3).join(' ')}…
          </Link>
        ) : null}
      </footer>
    </article>
  )
}
