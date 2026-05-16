import type {
  LiteracyCategory,
  LiteracyContentType,
  LiteracyDifficulty,
  LiteracyPortalPage,
} from '../types/financialLiteracy'
import {
  LITERACY_CATEGORIES,
  LITERACY_CONTENT_TYPES,
  LITERACY_DIFFICULTIES,
  LITERACY_PORTAL_PAGES,
} from '../types/financialLiteracy'

type LiteracySidebarFiltersProps = {
  category: LiteracyCategory | 'ALL'
  onCategoryChange: (category: LiteracyCategory | 'ALL') => void
  difficulties: LiteracyDifficulty[]
  onDifficultyToggle: (difficulty: LiteracyDifficulty) => void
  contentTypes: LiteracyContentType[]
  onContentTypeToggle: (type: LiteracyContentType) => void
  portalPages: LiteracyPortalPage[]
  onPortalPageToggle: (page: LiteracyPortalPage) => void
  showSystemCategory: boolean
  labels: {
    filtersTitle: string
    categoryTitle: string
    allCategories: string
    difficultyTitle: string
    contentTypeTitle: string
    portalPageTitle: string
    categoryLabel: (c: LiteracyCategory) => string
    difficultyLabel: (d: LiteracyDifficulty) => string
    contentTypeLabel: (t: LiteracyContentType) => string
    portalPageLabel: (p: LiteracyPortalPage) => string
  }
}

export function LiteracySidebarFilters({
  category,
  onCategoryChange,
  difficulties,
  onDifficultyToggle,
  contentTypes,
  onContentTypeToggle,
  portalPages,
  onPortalPageToggle,
  showSystemCategory,
  labels,
}: LiteracySidebarFiltersProps) {
  const categories = LITERACY_CATEGORIES.filter(
    (c) => showSystemCategory || c !== 'SYSTEM_OBSERVABILITY',
  )

  return (
    <aside className="lit-sidebar card" aria-label={labels.filtersTitle}>
      <h3>{labels.filtersTitle}</h3>

      <section className="lit-filter-group">
        <h4>{labels.categoryTitle}</h4>
        <ul className="lit-filter-list">
          <li>
            <button
              type="button"
              className={`lit-filter-item${category === 'ALL' ? ' lit-filter-item-active' : ''}`}
              onClick={() => onCategoryChange('ALL')}
            >
              {labels.allCategories}
            </button>
          </li>
          {categories.map((c) => (
            <li key={c}>
              <button
                type="button"
                className={`lit-filter-item${category === c ? ' lit-filter-item-active' : ''}`}
                onClick={() => onCategoryChange(c)}
              >
                {labels.categoryLabel(c)}
              </button>
            </li>
          ))}
        </ul>
      </section>

      <section className="lit-filter-group">
        <h4>{labels.difficultyTitle}</h4>
        <ul className="lit-filter-checks">
          {LITERACY_DIFFICULTIES.map((d) => (
            <li key={d}>
              <label className="lit-check">
                <input
                  type="checkbox"
                  checked={difficulties.includes(d)}
                  onChange={() => onDifficultyToggle(d)}
                />
                <span>{labels.difficultyLabel(d)}</span>
              </label>
            </li>
          ))}
        </ul>
      </section>

      <section className="lit-filter-group">
        <h4>{labels.contentTypeTitle}</h4>
        <ul className="lit-filter-checks">
          {LITERACY_CONTENT_TYPES.map((t) => (
            <li key={t}>
              <label className="lit-check">
                <input
                  type="checkbox"
                  checked={contentTypes.includes(t)}
                  onChange={() => onContentTypeToggle(t)}
                />
                <span>{labels.contentTypeLabel(t)}</span>
              </label>
            </li>
          ))}
        </ul>
      </section>

      <section className="lit-filter-group">
        <h4>{labels.portalPageTitle}</h4>
        <ul className="lit-filter-checks">
          {LITERACY_PORTAL_PAGES.map((p) => (
            <li key={p}>
              <label className="lit-check">
                <input
                  type="checkbox"
                  checked={portalPages.includes(p)}
                  onChange={() => onPortalPageToggle(p)}
                />
                <span>{labels.portalPageLabel(p)}</span>
              </label>
            </li>
          ))}
        </ul>
      </section>
    </aside>
  )
}
