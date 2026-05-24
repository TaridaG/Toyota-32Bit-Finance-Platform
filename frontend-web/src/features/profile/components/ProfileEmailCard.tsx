import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import {
  checkPortalEmailAvailability,
  confirmPortalEmailChange,
  readApiErrorMessage,
  sendPortalEmailChangeCode,
} from '../api/portalProfileApi'
import {
  EMAIL_BLOCKED_CODE,
  EMAIL_IN_USE_CODE,
  parseRegistrationEmailApiError,
} from '../../../shared/api/registrationEmailErrors'
import { USERNAME_AVAILABILITY_DEBOUNCE_MS } from '../../../shared/validation/username'
import type { PortalProfile } from '../types'
import { ProfileSettingsPanel } from './ProfileSettingsPanel'

type Props = {
  profile: PortalProfile
  onEmailChanged: (next: PortalProfile) => void
}

export function ProfileEmailCard({ profile, onEmailChanged }: Props) {
  const { t } = useTranslation('common')
  const [expanded, setExpanded] = useState(false)
  const [newEmail, setNewEmail] = useState('')
  const [verificationCode, setVerificationCode] = useState('')
  const [codeSent, setCodeSent] = useState(false)
  const [resendCountdown, setResendCountdown] = useState(0)
  const [busy, setBusy] = useState(false)
  const [sendingCode, setSendingCode] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState(false)
  const [emailCheckState, setEmailCheckState] = useState<'idle' | 'checking' | 'available' | 'taken' | 'blocked'>(
    'idle',
  )
  const [emailSuggestions, setEmailSuggestions] = useState<string[]>([])

  useEffect(() => {
    if (resendCountdown <= 0) return
    const timer = window.setInterval(() => {
      setResendCountdown((prev) => (prev > 0 ? prev - 1 : 0))
    }, 1000)
    return () => window.clearInterval(timer)
  }, [resendCountdown])

  useEffect(() => {
    const trimmed = newEmail.trim().toLowerCase()
    if (!trimmed || !trimmed.includes('@')) {
      setEmailCheckState('idle')
      setEmailSuggestions([])
      return
    }
    if (trimmed === profile.email.trim().toLowerCase()) {
      setEmailCheckState('idle')
      setEmailSuggestions([])
      return
    }
    setEmailCheckState('checking')
    const timer = window.setTimeout(() => {
      void checkPortalEmailAvailability(trimmed, profile.email)
        .then((result) => {
          if (result.blocked) {
            setEmailCheckState('blocked')
            setEmailSuggestions([])
            return
          }
          setEmailCheckState(result.available ? 'available' : 'taken')
          setEmailSuggestions(result.available ? [] : result.suggestions.slice(0, 3))
        })
        .catch(() => {
          setEmailCheckState('idle')
          setEmailSuggestions([])
        })
    }, USERNAME_AVAILABILITY_DEBOUNCE_MS)
    return () => window.clearTimeout(timer)
  }, [newEmail, profile.email])

  const applyEmailApiError = (err: unknown) => {
    const parsed = parseRegistrationEmailApiError(err)
    if (parsed) {
      setError(parsed.message)
      if (parsed.code === EMAIL_IN_USE_CODE) {
        setEmailCheckState('taken')
        setEmailSuggestions(parsed.suggestions.slice(0, 3))
      } else if (parsed.code === EMAIL_BLOCKED_CODE) {
        setEmailCheckState('blocked')
        setEmailSuggestions([])
      }
      return true
    }
    return false
  }

  const handleSendCode = async () => {
    setError(null)
    const trimmed = newEmail.trim().toLowerCase()
    if (!trimmed || !trimmed.includes('@')) {
      setError(t('profileSettings.emailInvalid'))
      return
    }
    if (trimmed === profile.email.trim().toLowerCase()) {
      setError(t('profileSettings.emailUnchanged'))
      return
    }
    if (emailCheckState === 'checking') {
      setError(t('profileSettings.emailChecking'))
      return
    }
    if (emailCheckState === 'blocked') {
      setError(t('profileSettings.emailBlocked'))
      return
    }
    if (emailCheckState === 'taken') {
      setError(t('profileSettings.emailTaken'))
      return
    }
    setSendingCode(true)
    try {
      const meta = await sendPortalEmailChangeCode(trimmed)
      setCodeSent(true)
      setResendCountdown(meta.resendInSeconds)
    } catch (err) {
      if (!applyEmailApiError(err)) {
        setError(readApiErrorMessage(err))
      }
    } finally {
      setSendingCode(false)
    }
  }

  const handleConfirm = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    setSuccess(false)
    const trimmed = newEmail.trim().toLowerCase()
    if (!codeSent) {
      setError(t('profileSettings.emailSendCodeFirst'))
      return
    }
    if (!verificationCode.trim()) {
      setError(t('profileSettings.emailCodeRequired'))
      return
    }
    setBusy(true)
    try {
      const updated = await confirmPortalEmailChange(trimmed, verificationCode.trim())
      setNewEmail('')
      setVerificationCode('')
      setCodeSent(false)
      setSuccess(true)
      onEmailChanged(updated)
      window.setTimeout(() => setSuccess(false), 4000)
      setExpanded(false)
    } catch (err) {
      if (!applyEmailApiError(err)) {
        setError(readApiErrorMessage(err))
      }
    } finally {
      setBusy(false)
    }
  }

  return (
    <ProfileSettingsPanel
      kicker={t('profileSettings.kickerEmail')}
      summary={profile.email}
      expanded={expanded}
      onToggle={setExpanded}
    >
      <p className="profile-settings-panel-hint">{t('profileSettings.emailLead')}</p>
      <form className="profile-settings-form" onSubmit={(ev) => void handleConfirm(ev)}>
        <label className="profile-settings-label" htmlFor="profile-email-new">
          {t('profileSettings.emailNew')}
        </label>
        <div className="profile-settings-input-wrap">
          <input
            id="profile-email-new"
            type="email"
            autoComplete="email"
            className="profile-settings-input"
            value={newEmail}
            onChange={(e) => {
              setNewEmail(e.target.value)
              setCodeSent(false)
              setVerificationCode('')
            }}
            required
          />
          {emailCheckState === 'checking' ? (
            <span className="profile-settings-input-status profile-settings-input-status-spinner" aria-hidden />
          ) : null}
          {emailCheckState === 'available' ? (
            <span className="profile-settings-input-status profile-settings-input-status-ok" aria-hidden>
              ✓
            </span>
          ) : null}
          {(emailCheckState === 'taken' || emailCheckState === 'blocked') && (
            <span className="profile-settings-input-status profile-settings-input-status-bad" aria-hidden>
              ✕
            </span>
          )}
        </div>
        {emailCheckState === 'blocked' ? (
          <p className="profile-settings-hint profile-settings-hint-warn">{t('profileSettings.emailBlocked')}</p>
        ) : null}
        {emailCheckState === 'taken' ? (
          <p className="profile-settings-hint profile-settings-hint-warn">{t('profileSettings.emailTaken')}</p>
        ) : null}
        {emailSuggestions.length > 0 ? (
          <div className="profile-settings-username-suggestions">
            <span>{t('profileSettings.emailSuggestionsTitle')}</span>
            {emailSuggestions.map((suggestion) => (
              <button
                key={suggestion}
                type="button"
                className="profile-settings-username-suggestion"
                onClick={() => setNewEmail(suggestion)}
              >
                {suggestion}
              </button>
            ))}
          </div>
        ) : null}
        <div className="profile-settings-inline-actions">
          <button
            type="button"
            className="profile-settings-btn-secondary"
            disabled={sendingCode || resendCountdown > 0}
            onClick={() => void handleSendCode()}
          >
            {sendingCode
              ? t('profileSettings.saving')
              : resendCountdown > 0
                ? t('profileSettings.emailResendWait', { seconds: resendCountdown })
                : codeSent
                  ? t('profileSettings.emailResendCode')
                  : t('profileSettings.emailSendCode')}
          </button>
        </div>
        {codeSent ? (
          <>
            <label className="profile-settings-label" htmlFor="profile-email-code">
              {t('profileSettings.emailVerificationCode')}
            </label>
            <input
              id="profile-email-code"
              type="text"
              inputMode="numeric"
              autoComplete="one-time-code"
              className="profile-settings-input"
              value={verificationCode}
              onChange={(e) => setVerificationCode(e.target.value)}
              required
            />
          </>
        ) : null}
        {error ? <p className="profile-settings-error">{error}</p> : null}
        {success ? <p className="profile-settings-saved">{t('profileSettings.emailSuccess')}</p> : null}
        <div className="profile-settings-inline-actions">
          <button type="submit" className="profile-settings-btn-primary" disabled={busy || !codeSent}>
            {busy ? t('profileSettings.saving') : t('profileSettings.emailSubmit')}
          </button>
        </div>
      </form>
    </ProfileSettingsPanel>
  )
}
