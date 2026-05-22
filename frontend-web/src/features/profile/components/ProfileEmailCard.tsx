import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import {
  confirmPortalEmailChange,
  readApiErrorMessage,
  sendPortalEmailChangeCode,
} from '../api/portalProfileApi'
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

  useEffect(() => {
    if (resendCountdown <= 0) return
    const timer = window.setInterval(() => {
      setResendCountdown((prev) => (prev > 0 ? prev - 1 : 0))
    }, 1000)
    return () => window.clearInterval(timer)
  }, [resendCountdown])

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
    setSendingCode(true)
    try {
      const meta = await sendPortalEmailChangeCode(trimmed)
      setCodeSent(true)
      setResendCountdown(meta.resendInSeconds)
    } catch (err) {
      setError(readApiErrorMessage(err))
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
      setError(readApiErrorMessage(err))
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
