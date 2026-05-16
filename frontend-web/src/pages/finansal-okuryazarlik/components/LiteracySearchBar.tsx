type LiteracySearchBarProps = {
  value: string
  onChange: (value: string) => void
  placeholder: string
  ariaLabel: string
}

export function LiteracySearchBar({ value, onChange, placeholder, ariaLabel }: LiteracySearchBarProps) {
  return (
    <div className="lit-search-wrap">
      <svg className="lit-search-icon" viewBox="0 0 24 24" aria-hidden="true">
        <circle cx="11" cy="11" r="7" />
        <path d="m20 20-4.2-4.2" />
      </svg>
      <input
        type="search"
        className="lit-search-input"
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        aria-label={ariaLabel}
      />
    </div>
  )
}
