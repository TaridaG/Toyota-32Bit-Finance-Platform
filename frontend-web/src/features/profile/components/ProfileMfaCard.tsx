import { useCallback, useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import {
  beginPortalMfaSetup,
  confirmPortalMfaSetup,
  disablePortalMfa,
  fetchPortalMfaStatus,
  type PortalMfaSetup,
} from '../api/portalMfaApi'
import { readApiErrorMessage } from '../api/portalProfileApi'

type Props = {
  totpEnabled: boolean
  onTotpEnabledChange: (enabled: boolean) => void
}

type View = 'status' | 'setup' | 'disable'

export function ProfileMfaCard({ totpEnabled, onTotpEnabledChange }: Props) {
  const { t } = useTranslation('common')
  const [view, setView] = useState<View>('status')
  const [setup, setSetup] = useState<PortalMfaSetup | null>(null)
  const [code, setCode] = useState('')
  const [password, setPassword] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)

  const resetTransient = useCallback(() => {
    setCode('')
    setPassword('')
    setError(null)
    setSuccess(null)
  }, [])

  useEffect(() => {
    if (view === 'status') {
      setSetup(null)
    }
  }, [view])

  const handleEnableStart = async () => {
    resetTransient()
    setBusy(true)
    try {
      const payload = await beginPortalMfaSetup()
      setSetup(payload)
      setView('setup')
    } catch (err) {
      setError(readApiErrorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  const handleConfirmSetup = async () => {
    const trimmed = code.replace(/\s+/g, '')
    if (trimmed.length !== 6) {
      setError(t('profileSettings.mfa.codeInvalid'))
      return
    }
    setBusy(true)
    setError(null)
    try {
      const status = await confirmPortalMfaSetup(trimmed)
      onTotpEnabledChange(status.enabled)
      setSuccess(t('profileSettings.mfa.enabledSuccess'))
      setView('status')
      setSetup(null)
      setCode('')
    } catch (err) {
      setError(readApiErrorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  const handleDisable = async () => {
    const trimmed = code.replace(/\s+/g, '')
    if (!password || trimmed.length !== 6) {
      setError(t('profileSettings.mfa.disableFieldsRequired'))
      return
    }
    setBusy(true)
    setError(null)
    try {
      const status = await disablePortalMfa(password, trimmed)
      onTotpEnabledChange(status.enabled)
      setSuccess(t('profileSettings.mfa.disabledSuccess'))
      setView('status')
      resetTransient()
    } catch (err) {
      setError(readApiErrorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  const handleCancelSetup = () => {
    setView('status')
    setSetup(null)
    resetTransient()
    void fetchPortalMfaStatus().catch(() => undefined)
  }

  const enabled = totpEnabled && view === 'status'

  return (
    <section className="profile-mfa-card" aria-label={t('profileSettings.mfa.cardAria')}>
      <h2 className="profile-mfa-card-title">{t('profileSettings.mfa.title')}</h2>

      {view === 'status' && !enabled ? (
        <>
          <p className="profile-mfa-card-lead">{t('profileSettings.mfa.disabledLead')}</p>
          <p className="profile-mfa-card-hint">{t('profileSettings.mfa.disabledHint')}</p>
          {success ? <p className="profile-settings-saved">{success}</p> : null}
          <button
            type="button"
            className="profile-settings-btn-primary profile-mfa-card-cta"
            disabled={busy}
            onClick={() => void handleEnableStart()}
          >
            {busy ? t('profileSettings.saving') : t('profileSettings.mfa.enable')}
          </button>
        </>
      ) : null}

      {view === 'status' && enabled ? (
        <>
          <p className="profile-mfa-card-lead profile-mfa-card-lead--ok">{t('profileSettings.mfa.enabledLead')}</p>
          {success ? <p className="profile-settings-saved">{success}</p> : null}
          <button
            type="button"
            className="profile-settings-btn-secondary profile-mfa-card-cta"
            disabled={busy}
            onClick={() => {
              resetTransient()
              setView('disable')
            }}
          >
            {t('profileSettings.mfa.disable')}
          </button>
        </>
      ) : null}

      {view === 'setup' && setup ? (
        <div className="profile-mfa-setup">
          <p className="profile-mfa-card-hint">{t('profileSettings.mfa.setupHint')}</p>
          {setup.qrCodeBase64?.trim() ? (
            <div className="profile-mfa-qr-wrap">
              <img
                src={`data:image/png;base64,${setup.qrCodeBase64}`}
                alt=""
                className="profile-mfa-qr"
                width={180}
                height={180}
              />
            </div>
          ) : (
            <p className="profile-settings-hint profile-settings-hint-warning">{t('profileSettings.mfa.qrUnavailable')}</p>
          )}
          <p className="profile-mfa-secret-label">{t('profileSettings.mfa.manualSecret')}</p>
          <code className="profile-mfa-secret">{setup.secret}</code>
          <label className="profile-settings-label" htmlFor="profile-mfa-setup-code">
            {t('profileSettings.mfa.codeLabel')}
          </label>
          <input
            id="profile-mfa-setup-code"
            className="profile-settings-input profile-mfa-code-input"
            inputMode="numeric"
            autoComplete="one-time-code"
            maxLength={6}
            value={code}
            onChange={(e) => setCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
            placeholder="000000"
          />
          <div className="profile-mfa-actions">
            <button
              type="button"
              className="profile-settings-btn-primary"
              disabled={busy}
              onClick={() => void handleConfirmSetup()}
            >
              {busy ? t('profileSettings.saving') : t('profileSettings.mfa.confirmEnable')}
            </button>
            <button type="button" className="profile-settings-btn-secondary" disabled={busy} onClick={handleCancelSetup}>
              {t('profileSettings.mfa.cancel')}
            </button>
          </div>
        </div>
      ) : null}

      {view === 'disable' ? (
        <div className="profile-mfa-disable">
          <p className="profile-mfa-card-hint">{t('profileSettings.mfa.disableHint')}</p>
          <label className="profile-settings-label" htmlFor="profile-mfa-disable-pw">
            {t('profileSettings.passwordCurrent')}
          </label>
          <input
            id="profile-mfa-disable-pw"
            type="password"
            className="profile-settings-input"
            autoComplete="current-password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
          <label className="profile-settings-label" htmlFor="profile-mfa-disable-code">
            {t('profileSettings.mfa.codeLabel')}
          </label>
          <input
            id="profile-mfa-disable-code"
            className="profile-settings-input profile-mfa-code-input"
            inputMode="numeric"
            autoComplete="one-time-code"
            maxLength={6}
            value={code}
            onChange={(e) => setCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
            placeholder="000000"
          />
          <div className="profile-mfa-actions">
            <button
              type="button"
              className="profile-settings-btn-primary"
              disabled={busy}
              onClick={() => void handleDisable()}
            >
              {busy ? t('profileSettings.saving') : t('profileSettings.mfa.confirmDisable')}
            </button>
            <button
              type="button"
              className="profile-settings-btn-secondary"
              disabled={busy}
              onClick={() => {
                setView('status')
                resetTransient()
              }}
            >
              {t('profileSettings.mfa.cancel')}
            </button>
          </div>
        </div>
      ) : null}

      {error ? <p className="profile-settings-error">{error}</p> : null}
    </section>
  )
}
