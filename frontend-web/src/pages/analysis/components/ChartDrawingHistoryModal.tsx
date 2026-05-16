import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import {
  deleteChartDrawingSave,
  fetchChartDrawingSaves,
  resolveDrawingMarkers,
  type ChartDrawingSaveSummary,
} from '../../../features/analysis/api/chartDrawingService'
import { DrawIconTrash } from './chartDrawIcons'
import { DrawToolTypeBadge } from './DrawToolTypeBadge'

type ChartDrawingHistoryModalProps = {
  assetKey: string
  symbol: string
  loadingId: number | null
  onClose: () => void
  onSelect: (summary: ChartDrawingSaveSummary) => void
}

function formatSavedAt(iso: string, locale: string): string {
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return iso
  return new Intl.DateTimeFormat(locale, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(date)
}

export function ChartDrawingHistoryModal({
  assetKey,
  symbol,
  loadingId,
  onClose,
  onSelect,
}: ChartDrawingHistoryModalProps) {
  const { t, i18n } = useTranslation('analysis')
  const [items, setItems] = useState<ChartDrawingSaveSummary[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [deletingId, setDeletingId] = useState<number | null>(null)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError(null)
    void fetchChartDrawingSaves(assetKey)
      .then((rows) => {
        if (!cancelled) setItems(rows)
      })
      .catch(() => {
        if (!cancelled) setError(t('chartDrawings.historyLoadError'))
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })
    return () => {
      cancelled = true
    }
  }, [assetKey, t])

  const handleDelete = async (id: number, event: React.MouseEvent) => {
    event.preventDefault()
    event.stopPropagation()
    if (deletingId != null) return
    setDeletingId(id)
    setError(null)
    try {
      await deleteChartDrawingSave(id)
      setItems((prev) => prev.filter((row) => row.id !== id))
    } catch {
      setError(t('chartDrawings.historyDeleteError'))
    } finally {
      setDeletingId(null)
    }
  }

  return (
    <div className="fi-modal-wrap" role="dialog" aria-modal="true">
      <button type="button" className="fi-modal-backdrop" onClick={onClose} aria-label={t('chartDrawings.historyClose')} />
      <article className="card fi-modal fi-chart-drawing-history-modal">
        <div className="fi-modal-head">
          <h3>{t('chartDrawings.historyTitle', { symbol })}</h3>
          <button type="button" onClick={onClose} aria-label={t('chartDrawings.historyClose')}>
            ×
          </button>
        </div>

        {loading ? <p className="fi-empty">{t('common:loading')}</p> : null}
        {error ? <p className="fi-chart-drawing-save-error">{error}</p> : null}

        {!loading && !error && items.length === 0 ? (
          <p className="fi-empty">{t('chartDrawings.historyEmpty')}</p>
        ) : null}

        {!loading && !error && items.length > 0 ? (
          <ul className="fi-chart-drawing-history-list">
            {items.map((item) => (
              <li key={item.id} className="fi-chart-drawing-history-row">
                <button
                  type="button"
                  className="fi-chart-drawing-history-select"
                  disabled={loadingId === item.id || deletingId === item.id}
                  onClick={() => onSelect(item)}
                >
                  <span className="fi-chart-drawing-history-name">{item.name}</span>
                  <span className="fi-chart-drawing-history-meta">
                    {formatSavedAt(item.createdAt, i18n.language)}
                  </span>
                  <span className="fi-chart-drawing-history-types" aria-hidden="true">
                    {resolveDrawingMarkers(item).map((marker, index) => (
                      <DrawToolTypeBadge
                        key={`${item.id}-${marker.type}-${marker.color}-${index}`}
                        tool={marker.type}
                        color={marker.color}
                      />
                    ))}
                  </span>
                  {(item.minPrice != null || item.maxPrice != null) && (
                    <span className="fi-chart-drawing-history-range">
                      {t('chartDrawings.historyPriceRange', {
                        min: item.minPrice?.toFixed(2) ?? '—',
                        max: item.maxPrice?.toFixed(2) ?? '—',
                      })}
                    </span>
                  )}
                </button>
                <button
                  type="button"
                  className="fi-chart-drawing-history-delete"
                  disabled={deletingId === item.id}
                  aria-label={t('chartDrawings.historyDelete', { name: item.name })}
                  title={t('chartDrawings.historyDelete', { name: item.name })}
                  onClick={(event) => {
                    void handleDelete(item.id, event)
                  }}
                >
                  <DrawIconTrash />
                </button>
              </li>
            ))}
          </ul>
        ) : null}
      </article>
    </div>
  )
}
