import { useEffect, useState } from 'react'
import { isAxiosError } from 'axios'
import { useTranslation } from 'react-i18next'
import {
  changePortalUsername,
  checkPortalUsernameAvailability,
  readApiErrorMessage,
} from '../api/portalProfileApi'
import type { PortalProfile } from '../types'
import { isRememberMeEnabled, persistAuthSession } from '../../../shared/auth/session'
import {
  isUsernameFormatValid,
  normalizeUsernameInput,
  USERNAME_AVAILABILITY_DEBOUNCE_MS,
} from '../../../shared/validation/username'
import { ProfileSettingsPanel } from './ProfileSettingsPanel'

type UsernameCheckState = 'idle' | 'checking' | 'available' | 'taken' | 'invalid' | 'unchanged' | 'error'

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
  const [usernameCheckState, setUsernameCheckState] = useState<UsernameCheckState>('idle')
  const [usernameSuggestions, setUsernameSuggestions] = useState<string[]>([])

  const summary = `@${profile.username}`

  useEffect(() => {
    if (!expanded) {
      return
    }
    const trimmed = normalizeUsernameInput(newUsername)
    if (!trimmed) {
      setUsernameCheckState('idle')
      setUsernameSuggestions([])
      return
    }
    if (trimmed === profile.username) {
      setUsernameCheckState('unchanged')
      setUsernameSuggestions([])
      return
    }
    if (!isUsernameFormatValid(trimmed)) {
      setUsernameCheckState('invalid')
      setUsernameSuggestions([])
      return
    }
    setUsernameCheckState('checking')
    const timer = window.setTimeout(() => {
      void checkPortalUsernameAvailability(trimmed, profile.username)
        .then((result) => {
          setUsernameCheckState(result.available ? 'available' : 'taken')
          setUsernameSuggestions(result.available ? [] : result.suggestions.slice(0, 3))
        })
        .catch(() => {
          setUsernameCheckState('error')
          setUsernameSuggestions([])
        })
    }, USERNAME_AVAILABILITY_DEBOUNCE_MS)
    return () => window.clearTimeout(timer)
  }, [newUsername, expanded, profile.username])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    setSuccess(false)
    const next = normalizeUsernameInput(newUsername)
    if (!next) {
      setError(t('profileSettings.usernameRequired'))
      return
    }
    if (!isUsernameFormatValid(next)) {
      setError(t('profileSettings.usernameInvalid'))
      return
    }
    if (next === profile.username) {
      setError(t('profileSettings.usernameUnchanged'))
      return
    }
    if (usernameCheckState === 'checking') {
      setError(t('profileSettings.usernameChecking'))
      return
    }
    if (usernameCheckState === 'taken') {
      setError(t('profileSettings.usernameTaken'))
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
        rememberMe: isRememberMeEnabled(),
      })
      setNewUsername('')
      setCurrentPassword('')
      setSuccess(true)
      onUsernameChanged({ ...profile, username: next })
      window.setTimeout(() => setSuccess(false), 4000)
      setExpanded(false)
    } catch (err) {
      if (isAxiosError(err) && err.response?.status === 409) {
        setUsernameCheckState('taken')
        setError(t('profileSettings.usernameTaken'))
        try {
          const result = await checkPortalUsernameAvailability(next, profile.username)
          setUsernameSuggestions(result.suggestions.slice(0, 3))
        } catch {
          setUsernameSuggestions([])
        }
      } else {
        setError(readApiErrorMessage(err))
      }
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
      <form className="profile-settings-form" noValidate onSubmit={(ev) => void handleSubmit(ev)}>
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
            placeholder={t('profileSettings.usernamePatternHint')}
            aria-invalid={usernameCheckState === 'taken' || usernameCheckState === 'invalid'}
            aria-describedby="profile-un-new-hint"
          />
          {usernameCheckState === 'checking' ? (
            <span
              className="profile-settings-input-status profile-settings-input-status-spinner"
              aria-label={t('profileSettings.usernameChecking')}
            />
          ) : null}
          {usernameCheckState === 'available' ? (
            <span
              className="profile-settings-input-status profile-settings-input-status-ok"
              aria-label={t('profileSettings.usernameAvailable')}
            >
              ✓
            </span>
          ) : null}
          {usernameCheckState === 'taken' ? (
            <span
              className="profile-settings-input-status profile-settings-input-status-bad"
              aria-label={t('profileSettings.usernameTaken')}
            >
              ✕
            </span>
          ) : null}
        </div>
        <p id="profile-un-new-hint" className="profile-settings-hint">
          {t('profileSettings.usernamePatternHint')}
        </p>
        {usernameCheckState === 'unchanged' ? (
          <p className="profile-settings-hint">{t('profileSettings.usernameUnchangedHint')}</p>
        ) : null}
        {usernameCheckState === 'invalid' ? (
          <p className="profile-settings-hint profile-settings-hint-warning">{t('profileSettings.usernameInvalid')}</p>
        ) : null}
        {usernameCheckState === 'taken' ? (
          <p className="profile-settings-hint profile-settings-hint-warning">{t('profileSettings.usernameTaken')}</p>
        ) : null}
        {usernameCheckState === 'error' ? (
          <p className="profile-settings-hint profile-settings-hint-warning">{t('profileSettings.usernameCheckError')}</p>
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
