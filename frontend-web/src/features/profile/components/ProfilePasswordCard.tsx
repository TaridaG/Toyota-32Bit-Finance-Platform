import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { changePortalPassword, readApiErrorMessage } from '../api/portalProfileApi'
import { ProfileSettingsPanel } from './ProfileSettingsPanel'

export function ProfilePasswordCard() {
  const { t } = useTranslation('common')
  const [expanded, setExpanded] = useState(false)
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState(false)

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    setSuccess(false)
    if (newPassword.length < 8) {
      setError(t('profileSettings.passwordTooShort'))
      return
    }
    if (newPassword !== confirmPassword) {
      setError(t('profileSettings.passwordMismatch'))
      return
    }
    setBusy(true)
    try {
      await changePortalPassword(currentPassword, newPassword)
      setCurrentPassword('')
      setNewPassword('')
      setConfirmPassword('')
      setSuccess(true)
      window.setTimeout(() => setSuccess(false), 3000)
      setExpanded(false)
    } catch (err) {
      setError(readApiErrorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <ProfileSettingsPanel
      kicker={t('profileSettings.kickerPassword')}
      summary={t('profileSettings.passwordSummary')}
      expanded={expanded}
      onToggle={setExpanded}
    >
      <p className="profile-settings-panel-hint">{t('profileSettings.passwordLead')}</p>
      <form className="profile-settings-form" onSubmit={(ev) => void handleSubmit(ev)}>
        <label className="profile-settings-label" htmlFor="profile-pw-current">
          {t('profileSettings.passwordCurrent')}
        </label>
        <input
          id="profile-pw-current"
          type="password"
          autoComplete="current-password"
          className="profile-settings-input"
          value={currentPassword}
          onChange={(e) => setCurrentPassword(e.target.value)}
          required
        />
        <label className="profile-settings-label" htmlFor="profile-pw-new">
          {t('profileSettings.passwordNew')}
        </label>
        <input
          id="profile-pw-new"
          type="password"
          autoComplete="new-password"
          className="profile-settings-input"
          value={newPassword}
          onChange={(e) => setNewPassword(e.target.value)}
          required
          minLength={8}
        />
        <label className="profile-settings-label" htmlFor="profile-pw-confirm">
          {t('profileSettings.passwordConfirm')}
        </label>
        <input
          id="profile-pw-confirm"
          type="password"
          autoComplete="new-password"
          className="profile-settings-input"
          value={confirmPassword}
          onChange={(e) => setConfirmPassword(e.target.value)}
          required
          minLength={8}
        />
        {error ? <p className="profile-settings-error">{error}</p> : null}
        {success ? <p className="profile-settings-saved">{t('profileSettings.passwordSuccess')}</p> : null}
        <div className="profile-settings-inline-actions">
          <button type="submit" className="profile-settings-btn-primary" disabled={busy}>
            {busy ? t('profileSettings.saving') : t('profileSettings.passwordSubmit')}
          </button>
        </div>
      </form>
    </ProfileSettingsPanel>
  )
}
