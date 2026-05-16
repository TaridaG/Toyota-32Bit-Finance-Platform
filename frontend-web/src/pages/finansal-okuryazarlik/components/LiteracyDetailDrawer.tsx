import { Link } from 'react-router-dom'
import type { LiteracyEntry } from '../types/financialLiteracy'
import { RelatedTerms } from './RelatedTerms'

type LiteracyDetailDrawerProps = {
  entry: LiteracyEntry | null
  onClose: () => void
  typeLabel: string
  difficultyLabel: string
  entriesByTitle: Map<string, LiteracyEntry>
  onSelectRelated: (entry: LiteracyEntry) => void
  sectionLabels: {
    shortDefinition: string
    financialMeaning: string
    usedInPortal: string
    howToInterpret: string
    commonMistake: string
    example: string
    relatedTerms: string
    goToPage: string
    close: string
  }
}

export function LiteracyDetailDrawer({
  entry,
  onClose,
  typeLabel,
  difficultyLabel,
  entriesByTitle,
  onSelectRelated,
  sectionLabels,
}: LiteracyDetailDrawerProps) {
  if (!entry) {
    return null
  }

  return (
    <div className="lit-drawer-wrap" role="presentation">
      <button type="button" className="lit-drawer-backdrop" aria-label={sectionLabels.close} onClick={onClose} />
      <aside className="lit-drawer card" role="dialog" aria-modal="true" aria-labelledby="lit-drawer-title">
        <header className="lit-drawer-head">
          <div>
            <h2 id="lit-drawer-title">{entry.title}</h2>
            <p className="lit-drawer-meta">
              <span className="lit-badge lit-badge-type">{typeLabel}</span>
              <span className="lit-badge lit-badge-difficulty">{difficultyLabel}</span>
            </p>
          </div>
          <button type="button" className="lit-drawer-close" onClick={onClose} aria-label={sectionLabels.close}>
            ×
          </button>
        </header>

        <div className="lit-drawer-body">
          <section>
            <h3>{sectionLabels.shortDefinition}</h3>
            <p>{entry.shortDefinition}</p>
          </section>

          <section>
            <h3>{sectionLabels.financialMeaning}</h3>
            <p>{entry.financialMeaning}</p>
          </section>

          <section>
            <h3>{sectionLabels.usedInPortal}</h3>
            <ul className="lit-drawer-portal-list">
              {entry.usedInPortal.map((usage) => (
                <li key={`${usage.page}-${usage.description}`}>
                  <p>{usage.description}</p>
                  {usage.route ? (
                    <Link to={usage.route} className="lit-btn-secondary lit-drawer-link">
                      {sectionLabels.goToPage}
                    </Link>
                  ) : null}
                </li>
              ))}
            </ul>
          </section>

          <section>
            <h3>{sectionLabels.howToInterpret}</h3>
            <p>{entry.howToInterpret}</p>
          </section>

          {entry.commonMistake ? (
            <section className="lit-drawer-warning">
              <h3>{sectionLabels.commonMistake}</h3>
              <p>{entry.commonMistake}</p>
            </section>
          ) : null}

          {entry.example ? (
            <section>
              <h3>{sectionLabels.example}</h3>
              <p>{entry.example}</p>
            </section>
          ) : null}

          <RelatedTerms
            title={sectionLabels.relatedTerms}
            terms={entry.relatedTerms}
            entriesByTitle={entriesByTitle}
            onSelect={onSelectRelated}
          />
        </div>
      </aside>
    </div>
  )
}
