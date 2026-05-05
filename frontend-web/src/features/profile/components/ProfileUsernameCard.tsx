import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { changePortalUsername, readApiErrorMessage } from '../api/portalProfileApi'
import type { PortalProfile } from '../types'
import { persistAuthSession } from '../../../shared/auth/session'
import { ProfileSettingsPanel } from './ProfileSettingsPanel'

type Props = {
  profile: PortalProfile
  onUsernameChanged: (next: PortalProfile) => void
}

export function ProfileUsernameCard({ profile, onUsernameChanged }: Props) {
  const { t } = useTranslation('common')
  const [expanded, setExpanded] = useState(false)
  const [newUsername, setNewUsername] = useState('')
  const [currentPassword, setCurrentPassword] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState(false)

  const summary = `@${profile.username}`

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    setSuccess(false)
    const next = newUsername.trim().toLowerCase()
    if (next === profile.username) {
      setError(t('profileSettings.usernameUnchanged'))
      return
    }
    setBusy(true)
    try {
      const tokens = await changePortalUsername(next, currentPassword)
      persistAuthSession({
        accessToken: tokens.accessToken,
        refreshToken: tokens.refreshToken,
      })
      setNewUsername('')
      setCurrentPassword('')
      setSuccess(true)
      onUsernameChanged({ ...profile, username: next })
      window.setTimeout(() => setSuccess(false), 4000)
      setExpanded(false)
    } catch (err) {
      setError(readApiErrorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <ProfileSettingsPanel
      kicker={t('profileSettings.kickerUsername')}
      summary={summary}
      expanded={expanded}
      onToggle={setExpanded}
    >
      <p className="profile-settings-panel-hint">{t('profileSettings.usernameLead')}</p>
      <form className="profile-settings-form" onSubmit={(ev) => void handleSubmit(ev)}>
        <label className="profile-settings-label" htmlFor="profile-un-new">
          {t('profileSettings.usernameNew')}
        </label>
        <input
          id="profile-un-new"
          type="text"
          autoComplete="username"
          className="profile-settings-input"
          value={newUsername}
          onChange={(e) => setNewUsername(e.target.value)}
          pattern="^[a-zA-Z0-9._-]{3,36}$"
          title={t('profileSettings.usernamePatternHint')}
          required
        />
        <label className="profile-settings-label" htmlFor="profile-un-pw">
          {t('profileSettings.usernamePasswordConfirm')}
        </label>
        <input
          id="profile-un-pw"
          type="password"
          autoComplete="current-password"
          className="profile-settings-input"
          value={currentPassword}
          onChange={(e) => setCurrentPassword(e.target.value)}
          required
        />
        {error ? <p className="profile-settings-error">{error}</p> : null}
        {success ? <p className="profile-settings-saved">{t('profileSettings.usernameSuccess')}</p> : null}
        <div className="profile-settings-inline-actions">
          <button type="submit" className="profile-settings-btn-primary" disabled={busy}>
            {busy ? t('profileSettings.saving') : t('profileSettings.usernameSubmit')}
          </button>
        </div>
      </form>
    </ProfileSettingsPanel>
  )
}
