import type { CSSProperties } from 'react'
import { useTranslation } from 'react-i18next'

export type InfoCardListStatusFilter = 'ALL' | 'ACTIVE' | 'PASSIVE'

const OPTIONS: InfoCardListStatusFilter[] = ['ALL', 'ACTIVE', 'PASSIVE']

type InfoCardsStatusSegmentProps = {
  value: InfoCardListStatusFilter
  onChange: (value: InfoCardListStatusFilter) => void
}

export function InfoCardsStatusSegment({ value, onChange }: InfoCardsStatusSegmentProps) {
  const { t } = useTranslation('common')
  const activeIndex = Math.max(0, OPTIONS.indexOf(value))

  return (
    <div
      className="ic-status-segment"
      role="tablist"
      aria-label={t('bilgiKartlariPage.statusFilter.aria')}
      style={{ '--ic-seg-index': activeIndex } as CSSProperties}
    >
      <span className="ic-status-segment-thumb" aria-hidden="true" />
      {OPTIONS.map((option) => (
        <button
          key={option}
          type="button"
          role="tab"
          aria-selected={value === option}
          className={`ic-status-segment-btn${value === option ? ' ic-status-segment-btn-active' : ''}`}
          onClick={() => onChange(option)}
        >
          {t(`bilgiKartlariPage.statusFilter.${option.toLowerCase()}`)}
        </button>
      ))}
    </div>
  )
}
