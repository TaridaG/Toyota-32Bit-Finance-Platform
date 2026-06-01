import type { ReactNode } from 'react'
import { LiteracySearchBar } from './LiteracySearchBar'

type LiteracyHeroProps = {
  title: string
  lead: string
  searchValue: string
  onSearchChange: (value: string) => void
  searchPlaceholder: string
  searchAriaLabel: string
  stats: ReactNode
  adminActions?: ReactNode
  showFilterButton?: boolean
  onFilterClick?: () => void
  filtersOpen?: boolean
  activeFilterCount?: number
  filterButtonLabel?: string
  filterButtonAria?: string
}

function IconFilter() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true" className="fi-filter-icon">
      <path d="M4 6h16M7 12h10M10 18h4" />
    </svg>
  )
}

export function LiteracyHero({
  title,
  lead,
  searchValue,
  onSearchChange,
  searchPlaceholder,
  searchAriaLabel,
  stats,
  adminActions,
  showFilterButton = false,
  onFilterClick,
  filtersOpen = false,
  activeFilterCount = 0,
  filterButtonLabel = 'Filters',
  filterButtonAria = 'Open filters',
}: LiteracyHeroProps) {
  return (
    <header className="lit-hero card">
      <div className="lit-hero-top">
        <div className="lit-hero-text">
          <h2 id="finansal-okuryazarlik-heading">{title}</h2>
          <p className="lit-hero-lead">{lead}</p>
        </div>
        {adminActions ? <div className="lit-hero-admin">{adminActions}</div> : null}
      </div>
      <div className="lit-search-row">
        <LiteracySearchBar
          value={searchValue}
          onChange={onSearchChange}
          placeholder={searchPlaceholder}
          ariaLabel={searchAriaLabel}
        />
        {showFilterButton ? (
          <button
            type="button"
            className={`lit-filter-toggle fi-filter-toggle fi-inline-filter-button${filtersOpen ? ' lit-filter-toggle-active' : ''}${activeFilterCount > 0 ? ' lit-filter-toggle-has-active' : ''}`}
            onClick={onFilterClick}
            aria-expanded={filtersOpen}
            aria-label={filterButtonAria}
          >
            <IconFilter />
            <span>{filterButtonLabel}</span>
            {activeFilterCount > 0 ? (
              <span className="lit-filter-toggle-badge" aria-hidden="true">
                {activeFilterCount}
              </span>
            ) : null}
          </button>
        ) : null}
      </div>
      <div className="lit-hero-stats">{stats}</div>
    </header>
  )
}
