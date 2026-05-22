import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import {
  changePortalPassword,
  readApiErrorMessage,
  resetPortalPasswordForgot,
  sendPortalPasswordResetCode,
} from '../api/portalProfileApi'
import { ProfileSettingsPanel } from './ProfileSettingsPanel'

type Props = {
  accountEmail: string
}

export function ProfilePasswordCard({ accountEmail }: Props) {
  const { t } = useTranslation('common')
  const [expanded, setExpanded] = useState(false)
  const [recoveryMode, setRecoveryMode] = useState(false)
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
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

  const passwordsMatch = newPassword.length > 0 && newPassword === confirmPassword
  const passwordsMismatch = confirmPassword.length > 0 && newPassword !== confirmPassword

  const resetForm = () => {
    setCurrentPassword('')
    setNewPassword('')
    setConfirmPassword('')
    setVerificationCode('')
    setRecoveryMode(false)
    setCodeSent(false)
  }

  const startRecovery = async () => {
    setError(null)
    setSendingCode(true)
    try {
      const meta = await sendPortalPasswordResetCode()
      setRecoveryMode(true)
      setCodeSent(true)
      setCurrentPassword('')
      setResendCountdown(meta.resendInSeconds)
    } catch (err) {
      setError(readApiErrorMessage(err))
    } finally {
      setSendingCode(false)
    }
  }

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
    if (recoveryMode && !verificationCode.trim()) {
      setError(t('profileSettings.passwordRecoveryCodeRequired'))
      return
    }
    setBusy(true)
    try {
      if (recoveryMode) {
        await resetPortalPasswordForgot(verificationCode.trim(), newPassword)
      } else {
        await changePortalPassword(currentPassword, newPassword)
      }
      resetForm()
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
      onToggle={(next) => {
        setExpanded(next)
        if (!next) resetForm()
      }}
    >
      <p className="profile-settings-panel-hint">
        {recoveryMode ? t('profileSettings.passwordRecoveryLead', { email: accountEmail }) : t('profileSettings.passwordLead')}
      </p>
      <form className="profile-settings-form" onSubmit={(ev) => void handleSubmit(ev)}>
        {!recoveryMode ? (
          <>
            <div className="profile-settings-label-row">
              <label className="profile-settings-label" htmlFor="profile-pw-current">
                {t('profileSettings.passwordCurrent')}
              </label>
              <button
                type="button"
                className="profile-settings-link-btn"
                disabled={sendingCode}
                onClick={() => void startRecovery()}
              >
                {sendingCode ? t('profileSettings.saving') : t('profileSettings.passwordForgot')}
              </button>
            </div>
            <input
              id="profile-pw-current"
              type="password"
              autoComplete="current-password"
              className="profile-settings-input"
              value={currentPassword}
              onChange={(e) => setCurrentPassword(e.target.value)}
              required
            />
          </>
        ) : (
          <>
            <p className="profile-settings-hint">{t('profileSettings.passwordRecoveryHint', { email: accountEmail })}</p>
            <div className="profile-settings-inline-actions">
              <button
                type="button"
                className="profile-settings-btn-secondary"
                disabled={sendingCode || resendCountdown > 0}
                onClick={() => void startRecovery()}
              >
                {sendingCode
                  ? t('profileSettings.saving')
                  : resendCountdown > 0
                    ? t('profileSettings.emailResendWait', { seconds: resendCountdown })
                    : t('profileSettings.passwordResendCode')}
              </button>
              <button
                type="button"
                className="profile-settings-link-btn"
                onClick={() => {
                  setRecoveryMode(false)
                  setCodeSent(false)
                  setVerificationCode('')
                }}
              >
                {t('profileSettings.passwordUseCurrent')}
              </button>
            </div>
            <label className="profile-settings-label" htmlFor="profile-pw-code">
              {t('profileSettings.passwordRecoveryCode')}
            </label>
            <input
              id="profile-pw-code"
              type="text"
              inputMode="numeric"
              autoComplete="one-time-code"
              className="profile-settings-input"
              value={verificationCode}
              onChange={(e) => setVerificationCode(e.target.value)}
              required
            />
          </>
        )}
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
        {passwordsMatch ? (
          <p className="profile-settings-saved profile-settings-match-ok">{t('profileSettings.passwordMatchOk')}</p>
        ) : null}
        {passwordsMismatch ? (
          <p className="profile-settings-error">{t('profileSettings.passwordMismatch')}</p>
        ) : null}
        {error ? <p className="profile-settings-error">{error}</p> : null}
        {success ? <p className="profile-settings-saved">{t('profileSettings.passwordSuccess')}</p> : null}
        <div className="profile-settings-inline-actions">
          <button
            type="submit"
            className="profile-settings-btn-primary"
            disabled={busy || passwordsMismatch || (recoveryMode && !codeSent)}
          >
            {busy ? t('profileSettings.saving') : t('profileSettings.passwordSubmit')}
          </button>
        </div>
      </form>
    </ProfileSettingsPanel>
  )
}
