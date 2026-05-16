import { useTranslation } from 'react-i18next'
import { Link } from 'react-router-dom'

type ChartDrawingLoginPromptProps = {
  onClose: () => void
}

export function ChartDrawingLoginPrompt({ onClose }: ChartDrawingLoginPromptProps) {
  const { t } = useTranslation('analysis')

  return (
    <div className="fi-modal-wrap" role="dialog" aria-modal="true">
      <button type="button" className="fi-modal-backdrop" onClick={onClose} aria-label={t('chartDrawings.loginClose')} />
      <article className="card fi-modal fi-chart-drawing-login-modal">
        <div className="fi-modal-head">
          <h3>{t('chartDrawings.loginTitle')}</h3>
          <button type="button" onClick={onClose} aria-label={t('chartDrawings.loginClose')}>
            ×
          </button>
        </div>
        <p className="fi-chart-drawing-save-lead">{t('chartDrawings.loginLead')}</p>
        <div className="fi-chart-drawing-save-actions">
          <button type="button" className="btn btn-ghost" onClick={onClose}>
            {t('chartDrawings.cancel')}
          </button>
          <Link to="/login" className="btn btn-primary" onClick={onClose}>
            {t('chartDrawings.loginAction')}
          </Link>
        </div>
      </article>
    </div>
  )
}
