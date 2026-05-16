import { useState } from 'react'
import { useTranslation } from 'react-i18next'

type ChartDrawingSaveModalProps = {
  symbol: string
  drawingCount: number
  saving: boolean
  error: string | null
  onClose: () => void
  onSave: (name: string) => void
}

export function ChartDrawingSaveModal({
  symbol,
  drawingCount,
  saving,
  error,
  onClose,
  onSave,
}: ChartDrawingSaveModalProps) {
  const { t } = useTranslation('analysis')
  const [name, setName] = useState('')

  const submit = (event: React.FormEvent) => {
    event.preventDefault()
    const trimmed = name.trim()
    if (!trimmed || saving) return
    onSave(trimmed)
  }

  return (
    <div className="fi-modal-wrap" role="dialog" aria-modal="true">
      <button type="button" className="fi-modal-backdrop" onClick={onClose} aria-label={t('chartDrawings.saveClose')} />
      <form className="card fi-modal fi-chart-drawing-save-modal" onSubmit={submit}>
        <div className="fi-modal-head">
          <h3>{t('chartDrawings.saveTitle')}</h3>
          <button type="button" onClick={onClose} aria-label={t('chartDrawings.saveClose')}>
            ×
          </button>
        </div>
        <p className="fi-chart-drawing-save-lead">
          {t('chartDrawings.saveLead', { symbol, count: drawingCount })}
        </p>
        <label className="fi-chart-drawing-save-label" htmlFor="chart-drawing-save-name">
          {t('chartDrawings.saveNameLabel')}
        </label>
        <input
          id="chart-drawing-save-name"
          className="fi-chart-drawing-save-input"
          value={name}
          onChange={(event) => setName(event.target.value)}
          placeholder={t('chartDrawings.saveNamePlaceholder')}
          maxLength={120}
          autoFocus
          disabled={saving}
        />
        {error ? <p className="fi-chart-drawing-save-error">{error}</p> : null}
        <div className="fi-chart-drawing-save-actions">
          <button type="button" className="btn btn-ghost" onClick={onClose} disabled={saving}>
            {t('chartDrawings.cancel')}
          </button>
          <button type="submit" className="btn btn-primary" disabled={saving || !name.trim()}>
            {saving ? t('chartDrawings.saving') : t('chartDrawings.saveConfirm')}
          </button>
        </div>
      </form>
    </div>
  )
}
