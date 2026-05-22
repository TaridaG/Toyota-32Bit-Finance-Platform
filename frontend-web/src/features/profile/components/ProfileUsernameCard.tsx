import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import {
  changePortalUsername,
  checkPortalUsernameAvailability,
  readApiErrorMessage,
} from '../api/portalProfileApi'
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
  const [usernameCheckState, setUsernameCheckState] = useState<'idle' | 'checking' | 'available' | 'taken'>('idle')
  const [usernameSuggestions, setUsernameSuggestions] = useState<string[]>([])

  const summary = `@${profile.username}`

  useEffect(() => {
    if (!expanded) {
      return
    }
    const trimmed = newUsername.trim().toLowerCase()
    if (!trimmed) {
      setUsernameCheckState('idle')
      setUsernameSuggestions([])
      return
    }
    if (!/^[a-zA-Z0-9._-]{3,36}$/.test(trimmed)) {
      setUsernameCheckState('idle')
      setUsernameSuggestions([])
      return
    }
    if (trimmed === profile.username) {
      setUsernameCheckState('idle')
      setUsernameSuggestions([])
      return
    }
    setUsernameCheckState('checking')
    const timer = window.setTimeout(() => {
      void checkPortalUsernameAvailability(trimmed)
        .then((result) => {
          setUsernameCheckState(result.available ? 'available' : 'taken')
          setUsernameSuggestions(result.available ? [] : result.suggestions.slice(0, 3))
        })
        .catch(() => {
          setUsernameCheckState('idle')
          setUsernameSuggestions([])
        })
    }, 600)
    return () => window.clearTimeout(timer)
  }, [newUsername, expanded, profile.username])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    setSuccess(false)
    const next = newUsername.trim().toLowerCase()
    if (next === profile.username) {
      setError(t('profileSettings.usernameUnchanged'))
      return
    }
    if (usernameCheckState !== 'available') {
      setError(t('profileSettings.usernameNotAvailable'))
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
        <div className="profile-settings-input-wrap">
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
          {usernameCheckState === 'checking' ? (
            <span
              className="profile-settings-input-status profile-settings-input-status-spinner"
              aria-label={t('profileSettings.usernameChecking')}
            />
          ) : null}
          {usernameCheckState === 'available' ? (
            <span className="profile-settings-input-status profile-settings-input-status-ok" aria-label={t('profileSettings.usernameAvailable')}>
              ✓
            </span>
          ) : null}
          {usernameCheckState === 'taken' ? (
            <span className="profile-settings-input-status profile-settings-input-status-bad" aria-label={t('profileSettings.usernameTaken')}>
              ✕
            </span>
          ) : null}
        </div>
        {usernameCheckState === 'taken' ? (
          <p className="profile-settings-hint profile-settings-hint-warning">{t('profileSettings.usernameTaken')}</p>
        ) : null}
        {usernameSuggestions.length > 0 ? (
          <div className="profile-settings-username-suggestions">
            <span>{t('profileSettings.usernameSuggestionsTitle')}</span>
            <div>
              {usernameSuggestions.map((suggestion) => (
                <button
                  key={suggestion}
                  type="button"
                  className="profile-settings-username-suggestion"
                  onClick={() => setNewUsername(suggestion)}
                >
                  {suggestion}
                </button>
              ))}
            </div>
          </div>
        ) : null}
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
          <button
            type="submit"
            className="profile-settings-btn-primary"
            disabled={busy || usernameCheckState !== 'available'}
          >
            {busy ? t('profileSettings.saving') : t('profileSettings.usernameSubmit')}
          </button>
        </div>
      </form>
    </ProfileSettingsPanel>
  )
}
