import type { LiteracyCategory } from '../types/financialLiteracy'
import { LITERACY_CATEGORIES } from '../types/financialLiteracy'

type LiteracyCategoryCardsProps = {
  activeCategory: LiteracyCategory | 'ALL'
  onSelect: (category: LiteracyCategory | 'ALL') => void
  labelForCategory: (category: LiteracyCategory) => string
  descriptionForCategory: (category: LiteracyCategory) => string
  countForCategory: (category: LiteracyCategory) => number
  allLabel: string
  allCount: number
  showSystemCategory?: boolean
}

export function LiteracyCategoryCards({
  activeCategory,
  onSelect,
  labelForCategory,
  descriptionForCategory,
  countForCategory,
  allLabel,
  allCount,
  showSystemCategory = false,
}: LiteracyCategoryCardsProps) {
  const categories = LITERACY_CATEGORIES.filter(
    (c) => showSystemCategory || c !== 'SYSTEM_OBSERVABILITY',
  )

  return (
    <div className="lit-category-grid" role="tablist" aria-label={allLabel}>
      <button
        type="button"
        role="tab"
        aria-selected={activeCategory === 'ALL'}
        className={`lit-category-card${activeCategory === 'ALL' ? ' lit-category-card-active' : ''}`}
        onClick={() => onSelect('ALL')}
      >
        <span className="lit-category-card-title">{allLabel}</span>
        <span className="lit-category-card-count">{allCount}</span>
      </button>
      {categories.map((category) => (
        <button
          key={category}
          type="button"
          role="tab"
          aria-selected={activeCategory === category}
          className={`lit-category-card${activeCategory === category ? ' lit-category-card-active' : ''}`}
          onClick={() => onSelect(category)}
        >
          <span className="lit-category-card-title">{labelForCategory(category)}</span>
          <span className="lit-category-card-desc">{descriptionForCategory(category)}</span>
          <span className="lit-category-card-count">{countForCategory(category)}</span>
        </button>
      ))}
    </div>
  )
}
