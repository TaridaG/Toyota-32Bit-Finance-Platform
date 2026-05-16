import type { LiteracyEntry } from '../types/financialLiteracy'

type RelatedTermsProps = {
  title: string
  terms: string[]
  entriesByTitle: Map<string, LiteracyEntry>
  onSelect: (entry: LiteracyEntry) => void
}

export function RelatedTerms({ title, terms, entriesByTitle, onSelect }: RelatedTermsProps) {
  if (terms.length === 0) {
    return null
  }

  return (
    <section className="lit-related">
      <h4>{title}</h4>
      <div className="lit-related-list">
        {terms.map((term) => {
          const entry = entriesByTitle.get(term)
          if (entry) {
            return (
              <button key={term} type="button" className="lit-tag lit-tag-button" onClick={() => onSelect(entry)}>
                {term}
              </button>
            )
          }
          return (
            <span key={term} className="lit-tag">
              {term}
            </span>
          )
        })}
      </div>
    </section>
  )
}
