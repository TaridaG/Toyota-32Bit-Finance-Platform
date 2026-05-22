import { useCallback, useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import {
  deleteChartDrawingSave,
  fetchChartDrawingSavesPage,
  resolveDrawingMarkers,
  type ChartDrawingSaveSummary,
} from '../../features/analysis/api/chartDrawingService'
import { isAuthenticated } from '../../shared/auth/session'
import { DrawToolTypeBadge } from '../analysis/components/DrawToolTypeBadge'
import { DrawIconTrash } from '../analysis/components/chartDrawIcons'
import { SavedDrawingExpandPreview } from './SavedDrawingExpandPreview'
import { formatSavedAt } from './savedDrawingChartUtils'

type MyAnalysisPanelProps = {
  embedded?: boolean
}

export function MyAnalysisPanel({ embedded = false }: MyAnalysisPanelProps) {
  const { t, i18n } = useTranslation(['myAnalysisPage', 'analysis', 'common'])
  const [page, setPage] = useState(0)
  const pageSize = 10
  const [items, setItems] = useState<ChartDrawingSaveSummary[]>([])
  const [totalElements, setTotalElements] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [expandedId, setExpandedId] = useState<number | null>(null)
  const [deletingId, setDeletingId] = useState<number | null>(null)

  const authed = isAuthenticated()

  const loadPage = useCallback(async () => {
    if (!authed) {
      setItems([])
      setTotalElements(0)
      setTotalPages(0)
      setLoading(false)
      return
    }
    setLoading(true)
    setError(null)
    try {
      const result = await fetchChartDrawingSavesPage(page, pageSize)
      setItems(result.content)
      setTotalElements(result.totalElements)
      setTotalPages(result.totalPages)
    } catch {
      setError(t('myAnalysisPage:loadError'))
    } finally {
      setLoading(false)
    }
  }, [authed, page, pageSize, t])

  useEffect(() => {
    void loadPage()
  }, [loadPage])

  const toggleExpand = (id: number) => {
    setExpandedId((prev) => (prev === id ? null : id))
  }

  const handleDelete = async (id: number, event: React.MouseEvent) => {
    event.preventDefault()
    event.stopPropagation()
    if (deletingId != null) return
    setDeletingId(id)
    setError(null)
    try {
      await deleteChartDrawingSave(id)
      setItems((prev) => prev.filter((row) => row.id !== id))
      setTotalElements((prev) => Math.max(0, prev - 1))
      if (expandedId === id) setExpandedId(null)
    } catch {
      setError(t('analysis:chartDrawings.historyDeleteError'))
    } finally {
      setDeletingId(null)
    }
  }

  return (
    <section
      className={`fi-my-analysis-page${embedded ? ' fi-my-analysis-page--embedded' : ''}`}
      aria-labelledby="my-analysis-title"
    >
      <header className={`fi-my-analysis-head${embedded ? ' my-portfolio-section-head' : ''}`}>
        {embedded ? (
          <h2 id="my-analysis-title">{t('myAnalysisPage:title')}</h2>
        ) : (
          <h1 id="my-analysis-title">{t('myAnalysisPage:title')}</h1>
        )}
        <p>{t('myAnalysisPage:lead')}</p>
      </header>

      {!authed ? (
        <p className="fi-empty">{t('myAnalysisPage:loginRequired')}</p>
      ) : null}

      {authed && error ? <p className="fi-chart-drawing-save-error">{error}</p> : null}

      {authed && loading ? <p className="fi-empty">{t('common:loading')}</p> : null}

      {authed && !loading && !error && items.length === 0 ? (
        <p className="fi-empty">{t('myAnalysisPage:empty')}</p>
      ) : null}

      {authed && !loading && !error && items.length > 0 ? (
        <ul className="fi-my-analysis-list">
          {items.map((item) => {
            const expanded = expandedId === item.id
            return (
              <li key={item.id} className={`fi-my-analysis-row${expanded ? ' fi-my-analysis-row-expanded' : ''}`}>
                <div className="fi-my-analysis-row-head">
                  <button
                    type="button"
                    className="fi-my-analysis-row-toggle"
                    aria-expanded={expanded}
                    onClick={() => toggleExpand(item.id)}
                  >
                    <span className="fi-my-analysis-row-chevron" aria-hidden>
                      {expanded ? '▾' : '▸'}
                    </span>
                    <span className="fi-my-analysis-row-main">
                      <span className="fi-my-analysis-row-name">{item.name}</span>
                      <span className="fi-my-analysis-row-meta">
                        {item.assetSymbol} · {formatSavedAt(item.createdAt, i18n.language)} ·{' '}
                        {t('myAnalysisPage:drawingCount', {
                          count: item.drawingCount > 0 ? item.drawingCount : item.drawingTypes.length,
                        })}
                      </span>
                      <span className="fi-my-analysis-row-types" aria-hidden="true">
                        {resolveDrawingMarkers(item).map((marker, index) => (
                          <DrawToolTypeBadge
                            key={`${item.id}-${marker.type}-${marker.color}-${index}`}
                            tool={marker.type}
                            color={marker.color}
                          />
                        ))}
                      </span>
                      {(item.minPrice != null || item.maxPrice != null) && (
                        <span className="fi-my-analysis-row-range">
                          {t('analysis:chartDrawings.historyPriceRange', {
                            min: item.minPrice?.toFixed(2) ?? '—',
                            max: item.maxPrice?.toFixed(2) ?? '—',
                          })}
                        </span>
                      )}
                    </span>
                  </button>
                  <button
                    type="button"
                    className="fi-chart-drawing-history-delete"
                    disabled={deletingId === item.id}
                    aria-label={t('analysis:chartDrawings.historyDelete', { name: item.name })}
                    title={t('analysis:chartDrawings.historyDelete', { name: item.name })}
                    onClick={(event) => {
                      void handleDelete(item.id, event)
                    }}
                  >
                    <DrawIconTrash />
                  </button>
                </div>
                {expanded ? <SavedDrawingExpandPreview summary={item} /> : null}
              </li>
            )
          })}
        </ul>
      ) : null}

      {authed && !loading && !error && totalElements > 0 ? (
        <div className="fi-pagination">
          <button
            type="button"
            className="fi-filter-chip"
            disabled={page <= 0}
            onClick={() => {
              setExpandedId(null)
              setPage((prev) => Math.max(prev - 1, 0))
            }}
          >
            ‹ {t('myAnalysisPage:prev')}
          </button>
          <span className="fi-pagination-summary">
            {t('myAnalysisPage:paginationSummary', {
              page: page + 1,
              totalPages: Math.max(totalPages, 1),
              count: totalElements,
            })}
          </span>
          <button
            type="button"
            className="fi-filter-chip"
            disabled={page >= Math.max(totalPages - 1, 0)}
            onClick={() => {
              setExpandedId(null)
              setPage((prev) => Math.min(prev + 1, Math.max(totalPages - 1, 0)))
            }}
          >
            {t('myAnalysisPage:next')} ›
          </button>
        </div>
      ) : null}
    </section>
  )
}
