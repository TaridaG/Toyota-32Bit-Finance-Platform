import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import type { InfoCard } from '../../types/infoCards'

type HelpCardPopoverProps = {
  card: InfoCard
  anchor: DOMRect
  onClose: () => void
  onDeactivate?: () => void
  centered?: boolean
}

export function HelpCardPopover({
  card,
  anchor,
  onClose,
  onDeactivate,
  centered = false,
}: HelpCardPopoverProps) {
  const { t } = useTranslation('common')

  return (
    <div
      className={`lit-help-popover card help-card-popover${centered ? ' help-card-popover-centered' : ''}`}
      role="dialog"
      aria-labelledby="help-card-popover-title"
      style={
        centered
          ? undefined
          : {
              top: Math.min(anchor.bottom + 8, window.innerHeight - 280),
              left: Math.min(Math.max(12, anchor.left), window.innerWidth - 340),
            }
      }
    >
      <header className="lit-help-popover-head">
        <div>
          <h3 id="help-card-popover-title">{card.title}</h3>
          <p className="help-card-popover-meta">
            <span className="lit-badge lit-badge-type">
              {t(`finansalOkuryazarlikPage.contentTypes.${card.type}`)}
            </span>
            <span className="lit-badge lit-badge-difficulty">
              {t(`finansalOkuryazarlikPage.difficulties.${card.difficulty}`)}
            </span>
          </p>
        </div>
        <button
          type="button"
          className="lit-help-popover-close"
          aria-label={t('header.literacyHelp.closePopover')}
          onClick={onClose}
        >
          ×
        </button>
      </header>
      <p className="lit-help-popover-def">{card.shortDescription}</p>
      {card.howToInterpret ? <p className="lit-help-popover-hint">{card.howToInterpret}</p> : null}
      <div className="help-card-popover-actions">
        <Link
          to={`/finansal-okuryazarlik?term=${card.slug}`}
          className="lit-help-popover-link"
          onClick={onDeactivate}
        >
          {t('header.literacyHelp.openFull')}
        </Link>
        <button type="button" className="lit-btn-secondary" onClick={onClose}>
          {t('header.literacyHelp.closePopover')}
        </button>
      </div>
    </div>
  )
}
