import type { ReactNode } from 'react'
import { useTranslation } from 'react-i18next'

type Props = {
  /** Short uppercase label (e.g. USERNAME, PASSWORD). */
  kicker: string
  /** Shown when collapsed; optional. */
  summary?: string | null
  expanded: boolean
  onToggle?: (next: boolean) => void
  /** When false, no edit/collapse control (e.g. notifications always open). */
  collapsible?: boolean
  children: ReactNode
}

export function ProfileSettingsPanel({
  kicker,
  summary,
  expanded,
  onToggle,
  collapsible = true,
  children,
}: Props) {
  const { t } = useTranslation('common')

  const showBody = !collapsible || expanded

  return (
    <section className="profile-settings-panel">
      <div className="profile-settings-panel-head">
        <div className="profile-settings-panel-head-text">
          <p className="profile-settings-panel-kicker">{kicker}</p>
          {collapsible && !expanded && summary ? <p className="profile-settings-panel-summary">{summary}</p> : null}
        </div>
        {collapsible ? (
          <button
            type="button"
            className="profile-settings-btn-edit"
            onClick={() => onToggle?.(!expanded)}
            aria-expanded={expanded}
          >
            {expanded ? t('profileSettings.doneEditing') : t('profileSettings.edit')}
          </button>
        ) : null}
      </div>
      {showBody ? <div className="profile-settings-panel-body">{children}</div> : null}
    </section>
  )
}
