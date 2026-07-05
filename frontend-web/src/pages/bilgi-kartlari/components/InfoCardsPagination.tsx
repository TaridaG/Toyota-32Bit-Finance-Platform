import { useTranslation } from 'react-i18next'

type InfoCardsPaginationProps = {
  page: number
  totalPages: number
  totalElements: number
  onPageChange: (page: number) => void
}

export function InfoCardsPagination({
  page,
  totalPages,
  totalElements,
  onPageChange,
}: InfoCardsPaginationProps) {
  const { t } = useTranslation('common')
  const { t: tPagination } = useTranslation('markets')

  if (totalElements === 0) {
    return null
  }

  return (
    <nav className="fi-pagination ic-list-pagination" aria-label={t('bilgiKartlariPage.paginationAria')}>
      <button
        type="button"
        className="lit-btn-secondary"
        disabled={page <= 0}
        onClick={() => onPageChange(page - 1)}
      >
        {tPagination('pagination.prev')}
      </button>
      <span className="fi-pagination-summary">
        {tPagination('pagination.summary', {
          page: page + 1,
          totalPages: Math.max(1, totalPages),
          totalElements,
        })}
      </span>
      <button
        type="button"
        className="lit-btn-secondary"
        disabled={page >= totalPages - 1}
        onClick={() => onPageChange(page + 1)}
      >
        {tPagination('pagination.next')}
      </button>
    </nav>
  )
}
