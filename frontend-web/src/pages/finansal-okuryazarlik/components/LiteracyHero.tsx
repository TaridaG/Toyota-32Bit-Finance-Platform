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
}

export function LiteracyHero({
  title,
  lead,
  searchValue,
  onSearchChange,
  searchPlaceholder,
  searchAriaLabel,
  stats,
}: LiteracyHeroProps) {
  return (
    <header className="lit-hero card">
      <div className="lit-hero-text">
        <h2 id="finansal-okuryazarlik-heading">{title}</h2>
        <p className="lit-hero-lead">{lead}</p>
      </div>
      <LiteracySearchBar
        value={searchValue}
        onChange={onSearchChange}
        placeholder={searchPlaceholder}
        ariaLabel={searchAriaLabel}
      />
      <div className="lit-hero-stats">{stats}</div>
    </header>
  )
}
