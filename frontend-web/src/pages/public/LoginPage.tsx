import { useCallback, useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import axios from 'axios'
import { useDocumentTitle } from '../../shared/hooks/useDocumentTitle'
import {
  loginWithPortalPassword,
  verifyPortalLoginMfa,
  type PortalLoginTokens,
} from '../../shared/api/publicAuth'
import {
  resetPublicPassword,
  sendPublicPasswordResetCode,
} from '../../shared/api/publicPasswordReset'
import { persistAuthSession } from '../../shared/auth/session'
import { fetchPortalProfile } from '../../features/profile/api/portalProfileApi'
import { normalizeLocale } from '../../shared/i18n'
import { useAppPreferences } from '../../shared/preferences/useAppPreferences'
import { SUPPORTED_CURRENCIES } from '../../shared/preferences/preferences'
import { PortalAlert } from '../../shared/components/PortalAlert'

type LoginStep = 'credentials' | 'mfa' | 'forgot'
type ForgotPhase = 'email' | 'reset'

function readApiMessage(e: unknown): string | undefined {
  if (axios.isAxiosError(e) && e.response?.data && typeof e.response.data === 'object') {
    const body = e.response.data as { error?: { message?: string } }
    return body.error?.message
  }
  if (e instanceof Error && e.message) {
    return e.message
  }
  return undefined
}

export function LoginPage() {
  const { t, i18n } = useTranslation('auth')
  useDocumentTitle(t('login.titleDoc'))
  const { setLanguage, setCurrency } = useAppPreferences()

  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const registered = searchParams.get('registered') === '1'
  const sessionExpired = searchParams.get('session') === 'expired'
  const accountFrozen = searchParams.get('frozen') === '1'
  const accountRemoved = searchParams.get('removed') === '1'

  const [step, setStep] = useState<LoginStep>('credentials')
  const [forgotPhase, setForgotPhase] = useState<ForgotPhase>('email')
  const [mfaChallengeId, setMfaChallengeId] = useState<string | null>(null)
  const [identity, setIdentity] = useState('')
  const [password, setPassword] = useState('')
  const [mfaCode, setMfaCode] = useState('')
  const [rememberMe, setRememberMe] = useState(false)
  const [trustDevice, setTrustDevice] = useState(true)
  const [forgotEmail, setForgotEmail] = useState('')
  const [forgotCode, setForgotCode] = useState('')
  const [forgotNewPassword, setForgotNewPassword] = useState('')
  const [forgotConfirmPassword, setForgotConfirmPassword] = useState('')
  const [codeSent, setCodeSent] = useState(false)
  const [resendCountdown, setResendCountdown] = useState(0)
  const [forgotSuccess, setForgotSuccess] = useState(false)
  const [sendingCode, setSendingCode] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  useEffect(() => {
    if (step !== 'forgot' || resendCountdown <= 0) return
    const timer = window.setInterval(() => {
      setResendCountdown((prev) => (prev > 0 ? prev - 1 : 0))
    }, 1000)
    return () => window.clearInterval(timer)
  }, [step, resendCountdown])

  const finishSession = useCallback(
    async (tokens: PortalLoginTokens, remember: boolean) => {
      persistAuthSession({
        accessToken: tokens.accessToken,
        refreshToken: tokens.refreshToken,
        rememberMe: remember,
      })
      try {
        const profile = await fetchPortalProfile()
        const locale = normalizeLocale(profile.preferredLocale)
        if (locale) {
          await setLanguage(locale)
        }
        const currency = profile.preferredCurrency?.trim().toUpperCase()
        if (currency && (SUPPORTED_CURRENCIES as readonly string[]).includes(currency)) {
          setCurrency(currency as (typeof SUPPORTED_CURRENCIES)[number])
        }
      } catch {
        // keep current local preferences if profile preferences cannot be fetched now
      }
      navigate('/app', { replace: true })
    },
    [navigate, setCurrency, setLanguage],
  )

  const mapLoginError = useCallback(
    (e: unknown, mfaStep: boolean): string => {
      if (mfaStep && axios.isAxiosError(e) && e.response?.status === 401) {
        const msg = readApiMessage(e)
        if (msg) return msg
        return t('login.mfa.errors.invalidCode')
      }
      if (axios.isAxiosError(e) && e.response?.status === 403) {
        const msg = readApiMessage(e)
        if (msg && /donduruldu|suspended|frozen|gesperrt/i.test(msg)) {
          return t('login.errors.accountFrozen')
        }
        if (msg) return msg
      }
      if (axios.isAxiosError(e) && e.response?.status === 401) {
        const msg = readApiMessage(e)
        const isCredentialFailure =
          !msg ||
          /invalid user credentials|invalid_grant|invalid username|invalid password|e-posta veya parola|parola hatalı|incorrect email/i.test(
            msg,
          )
        if (isCredentialFailure) {
          return t('login.errors.invalidCredentials')
        }
        if (msg && /invalid verification|verification session|doğrulama/i.test(msg)) {
          return t('login.mfa.errors.invalidCode')
        }
        if (msg) return msg
        return t('login.errors.invalidCredentials')
      }
      const msg = readApiMessage(e)
      if (msg) return msg
      return t('login.errors.generic')
    },
    [t],
  )

  const mapForgotPasswordError = useCallback(
    (e: unknown): string => {
      const status = axios.isAxiosError(e) ? e.response?.status : undefined
      const msg = readApiMessage(e) ?? ''

      if (status === 404 || /bulunamadı|not found|kein konto/i.test(msg)) {
        return t('login.forgot.errors.accountNotFound')
      }
      if (status === 403 || /donduruldu|suspended|frozen|gesperrt/i.test(msg)) {
        return t('login.errors.accountFrozen')
      }
      if (/kayıtlı e-posta yok|no email on file|keine e-mail/i.test(msg)) {
        return t('login.forgot.errors.noEmailOnAccount')
      }
      if (/verification code is invalid|doğrulama kodu hatalı|code ungültig|falsch/i.test(msg)) {
        return t('login.forgot.errors.invalidCode')
      }
      if (/verification code expired|süresi doldu|abgelaufen/i.test(msg)) {
        return t('login.forgot.errors.codeExpired')
      }
      if (/attempts exceeded|çok fazla|fehlversuche/i.test(msg)) {
        return t('login.forgot.errors.tooManyAttempts')
      }
      if (status === 429 || /please wait/i.test(msg)) {
        const match = msg.match(/(\d+)/)
        const seconds = match ? match[1] : '15'
        return t('login.forgot.errors.resendWait', { seconds })
      }
      if (msg) return msg
      return t('login.forgot.errors.generic')
    },
    [t],
  )

  const mapCredentialsError = useCallback((e: unknown) => mapLoginError(e, false), [mapLoginError])
  const mapMfaError = useCallback((e: unknown) => mapLoginError(e, true), [mapLoginError])

  const resetForgotForm = () => {
    setForgotCode('')
    setForgotNewPassword('')
    setForgotConfirmPassword('')
    setCodeSent(false)
    setResendCountdown(0)
    setForgotSuccess(false)
    setForgotPhase('email')
  }

  const handleBackToCredentials = () => {
    setStep('credentials')
    setMfaChallengeId(null)
    setMfaCode('')
    resetForgotForm()
    setError(null)
  }

  const handleStartForgot = () => {
    setStep('forgot')
    setForgotPhase('email')
    setForgotEmail(identity.includes('@') ? identity.trim().toLowerCase() : '')
    resetForgotForm()
    setError(null)
  }

  const sendForgotCode = async (email: string) => {
    setError(null)
    setSendingCode(true)
    try {
      const meta = await sendPublicPasswordResetCode(email, i18n.language)
      setCodeSent(true)
      setForgotPhase('reset')
      setResendCountdown(meta.resendInSeconds)
    } catch (e) {
      setError(mapForgotPasswordError(e))
    } finally {
      setSendingCode(false)
    }
  }

  const handleForgotEmailSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError(null)
    setForgotSuccess(false)

    const trimmed = forgotEmail.trim().toLowerCase()
    if (!trimmed) {
      setError(t('login.forgot.errors.emailRequired'))
      return
    }
    if (!trimmed.includes('@')) {
      setError(t('login.forgot.errors.emailInvalid'))
      return
    }

    await sendForgotCode(trimmed)
  }

  const handleForgotResetSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError(null)
    setForgotSuccess(false)

    const email = forgotEmail.trim().toLowerCase()
    const code = forgotCode.trim()
    if (!code) {
      setError(t('login.forgot.errors.codeRequired'))
      return
    }
    if (forgotNewPassword.length < 8) {
      setError(t('login.forgot.errors.passwordMin'))
      return
    }
    if (forgotNewPassword !== forgotConfirmPassword) {
      setError(t('login.forgot.errors.passwordMismatch'))
      return
    }
    if (!codeSent) {
      setError(t('login.forgot.errors.generic'))
      return
    }

    setSubmitting(true)
    try {
      await resetPublicPassword(email, code, forgotNewPassword)
      resetForgotForm()
      setForgotSuccess(true)
      setStep('credentials')
      setPassword('')
    } catch (e) {
      setError(mapForgotPasswordError(e))
    } finally {
      setSubmitting(false)
    }
  }

  const handleCredentialsSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError(null)

    const trimmed = identity.trim()
    if (!trimmed) {
      setError(t('login.errors.identityRequired'))
      return
    }
    if (!password) {
      setError(t('login.errors.passwordRequired'))
      return
    }

    setSubmitting(true)
    try {
      const result = await loginWithPortalPassword(trimmed, password)
      if (result.kind === 'mfaRequired') {
        setMfaChallengeId(result.challengeId)
        setMfaCode('')
        setStep('mfa')
        return
      }
      await finishSession(result.tokens, rememberMe)
    } catch (e) {
      setError(mapCredentialsError(e))
    } finally {
      setSubmitting(false)
    }
  }

  const handleMfaSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError(null)
    const code = mfaCode.replace(/\s+/g, '')
    if (code.length !== 6) {
      setError(t('login.mfa.errors.codeRequired'))
      return
    }
    if (!mfaChallengeId) {
      setError(t('login.mfa.errors.sessionExpired'))
      setStep('credentials')
      return
    }

    setSubmitting(true)
    try {
      const tokens = await verifyPortalLoginMfa(mfaChallengeId, code, rememberMe && trustDevice)
      await finishSession(tokens, rememberMe)
    } catch (e) {
      setError(mapMfaError(e))
    } finally {
      setSubmitting(false)
    }
  }

  const stepKicker =
    step === 'mfa' ? t('login.mfa.kicker') : step === 'forgot' ? t('login.forgot.kicker') : t('login.kicker')
  const stepTitle =
    step === 'mfa' ? t('login.mfa.title') : step === 'forgot' ? t('login.forgot.title') : t('login.title')
  const stepLead =
    step === 'mfa'
      ? t('login.mfa.lead')
      : step === 'forgot'
        ? forgotPhase === 'email'
          ? t('login.forgot.leadEmail')
          : t('login.forgot.leadReset', { email: forgotEmail.trim().toLowerCase() })
        : t('login.lead')

  const passwordsMismatch =
    forgotConfirmPassword.length > 0 && forgotNewPassword !== forgotConfirmPassword

  return (
    <div className="auth-page">
      <section className="auth-card-wrap">
        <article className="auth-card">
          <p className="auth-kicker">{stepKicker}</p>
          <h2>{stepTitle}</h2>
          <p className="auth-lead">{stepLead}</p>

          <div className="auth-notices">
            {accountRemoved ? (
              <PortalAlert variant="error" title={t('login.alerts.removedTitle')}>
                {t('login.removedBanner')}
              </PortalAlert>
            ) : null}
            {accountFrozen ? (
              <PortalAlert variant="error" title={t('login.alerts.frozenTitle')}>
                {t('login.frozenBanner')}
              </PortalAlert>
            ) : null}
            {sessionExpired ? (
              <PortalAlert variant="warning" title={t('login.alerts.sessionTitle')}>
                {t('login.sessionExpiredBanner')}
              </PortalAlert>
            ) : null}
            {registered ? (
              <PortalAlert variant="success" title={t('login.alerts.registeredTitle')}>
                {t('login.registeredBanner')}
              </PortalAlert>
            ) : null}
            {forgotSuccess ? (
              <PortalAlert variant="success" title={t('login.forgot.successTitle')}>
                {t('login.forgot.success')}
              </PortalAlert>
            ) : null}
          </div>

          {step === 'credentials' ? (
            <form className="auth-form" onSubmit={(e) => void handleCredentialsSubmit(e)}>
              <label className="auth-label" htmlFor="identity">
                {t('login.identityLabel')}
              </label>
              <input
                id="identity"
                className="auth-input"
                value={identity}
                onChange={(e) => setIdentity(e.target.value)}
                placeholder={t('login.identityPlaceholder')}
                autoComplete="username"
              />
              <p className="auth-footer-text" style={{ marginTop: '-0.35rem', marginBottom: '0.5rem' }}>
                {t('login.credentialHint')}
              </p>

              <label className="auth-label" htmlFor="password">
                {t('login.passwordLabel')}
              </label>
              <input
                id="password"
                className="auth-input"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder={t('login.passwordPlaceholder')}
                autoComplete="current-password"
              />

              <label className="auth-remember">
                <input
                  type="checkbox"
                  checked={rememberMe}
                  onChange={(e) => setRememberMe(e.target.checked)}
                />
                <span>
                  <strong>{t('login.rememberMe')}</strong>
                  <small>{t('login.rememberMeHint')}</small>
                </span>
              </label>

              <button
                type="button"
                className="auth-link-btn"
                style={{ marginTop: '0.1rem', marginBottom: '0.35rem', textAlign: 'left' }}
                onClick={handleStartForgot}
              >
                {t('login.forgot.link')}
              </button>

              {error ? (
                <PortalAlert variant="error" title={t('login.alerts.errorTitle')}>
                  {error}
                </PortalAlert>
              ) : null}

              <p className="auth-footer-text" style={{ marginBottom: '0.75rem' }}>
                {t('login.signInHint')}
              </p>

              <button type="submit" className="auth-submit" disabled={submitting}>
                {submitting ? t('login.submitting') : t('login.submit')}
              </button>
            </form>
          ) : step === 'mfa' ? (
            <form className="auth-form" onSubmit={(e) => void handleMfaSubmit(e)}>
              <p className="auth-footer-text" style={{ marginTop: 0, marginBottom: '0.75rem' }}>
                {t('login.mfa.hint')}
              </p>
              <label className="auth-label" htmlFor="mfa-code">
                {t('login.mfa.codeLabel')}
              </label>
              <input
                id="mfa-code"
                className="auth-input auth-input--otp"
                inputMode="numeric"
                autoComplete="one-time-code"
                maxLength={6}
                value={mfaCode}
                onChange={(e) => setMfaCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
                placeholder="000000"
              />

              {rememberMe ? (
                <label className="auth-remember">
                  <input
                    type="checkbox"
                    checked={trustDevice}
                    onChange={(e) => setTrustDevice(e.target.checked)}
                  />
                  <span>
                    <strong>{t('login.mfa.trustDevice')}</strong>
                    <small>{t('login.mfa.trustDeviceHint')}</small>
                  </span>
                </label>
              ) : null}

              {error ? (
                <PortalAlert variant="error" title={t('login.alerts.errorTitle')}>
                  {error}
                </PortalAlert>
              ) : null}

              <button type="submit" className="auth-submit" disabled={submitting}>
                {submitting ? t('login.mfa.submitting') : t('login.mfa.submit')}
              </button>
              <button type="button" className="auth-link-btn" disabled={submitting} onClick={handleBackToCredentials}>
                {t('login.backToLogin')}
              </button>
            </form>
          ) : forgotPhase === 'email' ? (
            <form className="auth-form" onSubmit={(e) => void handleForgotEmailSubmit(e)}>
              <label className="auth-label" htmlFor="forgot-email">
                {t('login.forgot.emailLabel')}
              </label>
              <input
                id="forgot-email"
                className="auth-input"
                type="email"
                value={forgotEmail}
                onChange={(e) => setForgotEmail(e.target.value)}
                placeholder={t('login.forgot.emailPlaceholder')}
                autoComplete="email"
              />

              {error ? (
                <PortalAlert variant="error" title={t('login.alerts.errorTitle')}>
                  {error}
                </PortalAlert>
              ) : null}

              <button type="submit" className="auth-submit" disabled={sendingCode}>
                {sendingCode ? t('login.forgot.sendingCode') : t('login.forgot.sendCode')}
              </button>
              <button type="button" className="auth-link-btn" disabled={sendingCode} onClick={handleBackToCredentials}>
                {t('login.forgot.backToLogin')}
              </button>
            </form>
          ) : (
            <form className="auth-form" onSubmit={(e) => void handleForgotResetSubmit(e)}>
              <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap', marginBottom: '0.5rem' }}>
                <button
                  type="button"
                  className="auth-link-btn"
                  style={{ marginTop: 0 }}
                  disabled={sendingCode || resendCountdown > 0}
                  onClick={() => void sendForgotCode(forgotEmail.trim().toLowerCase())}
                >
                  {sendingCode
                    ? t('login.forgot.sendingCode')
                    : resendCountdown > 0
                      ? t('login.forgot.resendWait', { seconds: resendCountdown })
                      : t('login.forgot.resendCode')}
                </button>
              </div>

              <label className="auth-label" htmlFor="forgot-code">
                {t('login.forgot.codeLabel')}
              </label>
              <input
                id="forgot-code"
                className="auth-input"
                inputMode="numeric"
                autoComplete="one-time-code"
                value={forgotCode}
                onChange={(e) => setForgotCode(e.target.value)}
                placeholder={t('login.forgot.codePlaceholder')}
              />

              <label className="auth-label" htmlFor="forgot-new-password">
                {t('login.forgot.newPassword')}
              </label>
              <input
                id="forgot-new-password"
                className="auth-input"
                type="password"
                autoComplete="new-password"
                value={forgotNewPassword}
                onChange={(e) => setForgotNewPassword(e.target.value)}
                placeholder={t('login.forgot.newPasswordPlaceholder')}
                minLength={8}
              />

              <label className="auth-label" htmlFor="forgot-confirm-password">
                {t('login.forgot.confirmPassword')}
              </label>
              <input
                id="forgot-confirm-password"
                className="auth-input"
                type="password"
                autoComplete="new-password"
                value={forgotConfirmPassword}
                onChange={(e) => setForgotConfirmPassword(e.target.value)}
                placeholder={t('login.forgot.confirmPasswordPlaceholder')}
                minLength={8}
              />

              {passwordsMismatch ? (
                <p className="auth-footer-text" style={{ color: 'var(--color-danger, #dc2626)', marginTop: '0.25rem' }}>
                  {t('login.forgot.errors.passwordMismatch')}
                </p>
              ) : null}

              {error ? (
                <PortalAlert variant="error" title={t('login.alerts.errorTitle')}>
                  {error}
                </PortalAlert>
              ) : null}

              <button
                type="submit"
                className="auth-submit"
                disabled={submitting || passwordsMismatch || !codeSent}
              >
                {submitting ? t('login.forgot.submitting') : t('login.forgot.submit')}
              </button>
              <button type="button" className="auth-link-btn" disabled={submitting} onClick={handleBackToCredentials}>
                {t('login.forgot.backToLogin')}
              </button>
            </form>
          )}

          {step === 'credentials' ? (
            <p className="auth-footer-text">
              {t('login.noAccount')} <Link to="/register">{t('login.registerNow')}</Link>
            </p>
          ) : null}
        </article>
      </section>
    </div>
  )
}
