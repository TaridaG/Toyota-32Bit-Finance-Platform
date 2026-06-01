import { useEffect } from 'react'
import { LiteracySidebarFilters, type LiteracySidebarFiltersProps } from './LiteracySidebarFilters'

type LiteracyFiltersDrawerProps = LiteracySidebarFiltersProps & {
  open: boolean
  onClose: () => void
  closeLabel: string
}

export function LiteracyFiltersDrawer({
  open,
  onClose,
  closeLabel,
  labels,
  ...filterProps
}: LiteracyFiltersDrawerProps) {
  useEffect(() => {
    if (!open) {
      return undefined
    }

    const previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'

    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        onClose()
      }
    }

    window.addEventListener('keydown', onKeyDown)

    return () => {
      document.body.style.overflow = previousOverflow
      window.removeEventListener('keydown', onKeyDown)
    }
  }, [open, onClose])

  if (!open) {
    return null
  }

  return (
    <div className="lit-drawer-wrap lit-filters-drawer-wrap" role="presentation">
      <button type="button" className="lit-drawer-backdrop" aria-label={closeLabel} onClick={onClose} />
      <aside
        className="lit-drawer lit-filters-drawer card"
        role="dialog"
        aria-modal="true"
        aria-labelledby="lit-filters-drawer-title"
      >
        <header className="lit-drawer-head">
          <h2 id="lit-filters-drawer-title">{labels.filtersTitle}</h2>
          <button type="button" className="lit-drawer-close" onClick={onClose} aria-label={closeLabel}>
            ×
          </button>
        </header>
        <div className="lit-filters-drawer-body">
          <LiteracySidebarFilters labels={labels} {...filterProps} />
        </div>
      </aside>
    </div>
  )
}
