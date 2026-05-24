import { useCallback, useState } from 'react'
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
import { persistAuthSession } from '../../shared/auth/session'
import { fetchPortalProfile } from '../../features/profile/api/portalProfileApi'
import { normalizeLocale } from '../../shared/i18n'
import { useAppPreferences } from '../../shared/preferences/useAppPreferences'
import { SUPPORTED_CURRENCIES } from '../../shared/preferences/preferences'
import { PortalAlert } from '../../shared/components/PortalAlert'

type LoginStep = 'credentials' | 'mfa'

export function LoginPage() {
  const { t } = useTranslation('auth')
  useDocumentTitle(t('login.titleDoc'))
  const { setLanguage, setCurrency } = useAppPreferences()

  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const registered = searchParams.get('registered') === '1'
  const sessionExpired = searchParams.get('session') === 'expired'
  const accountFrozen = searchParams.get('frozen') === '1'
  const accountRemoved = searchParams.get('removed') === '1'

  const [step, setStep] = useState<LoginStep>('credentials')
  const [mfaChallengeId, setMfaChallengeId] = useState<string | null>(null)
  const [identity, setIdentity] = useState('')
  const [password, setPassword] = useState('')
  const [mfaCode, setMfaCode] = useState('')
  const [rememberMe, setRememberMe] = useState(false)
  const [trustDevice, setTrustDevice] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

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
        const body = e.response?.data
        const msg =
          body && typeof body === 'object'
            ? (body as { error?: { message?: string } }).error?.message
            : undefined
        if (msg) return msg
        return t('login.mfa.errors.invalidCode')
      }
      if (axios.isAxiosError(e) && e.response?.status === 403) {
        const body = e.response?.data
        const msg =
          body && typeof body === 'object'
            ? (body as { error?: { message?: string } }).error?.message
            : undefined
        if (msg && /donduruldu|suspended|frozen|gesperrt/i.test(msg)) {
          return t('login.errors.accountFrozen')
        }
        if (msg) return msg
      }
      if (axios.isAxiosError(e) && e.response?.status === 401) {
        const body = e.response?.data
        const msg =
          body && typeof body === 'object'
            ? (body as { error?: { message?: string } }).error?.message
            : undefined
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
      if (axios.isAxiosError(e) && e.response?.data && typeof e.response.data === 'object') {
        const body = e.response.data as { error?: { message?: string } }
        if (body.error?.message) return body.error.message
      }
      if (e instanceof Error && e.message) return e.message
      return t('login.errors.generic')
    },
    [t],
  )

  const mapCredentialsError = useCallback((e: unknown) => mapLoginError(e, false), [mapLoginError])
  const mapMfaError = useCallback((e: unknown) => mapLoginError(e, true), [mapLoginError])

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

  const handleBackToCredentials = () => {
    setStep('credentials')
    setMfaChallengeId(null)
    setMfaCode('')
    setError(null)
  }

  return (
    <div className="auth-page">
      <section className="auth-card-wrap">
        <article className="auth-card">
          <p className="auth-kicker">{step === 'mfa' ? t('login.mfa.kicker') : t('login.kicker')}</p>
          <h2>{step === 'mfa' ? t('login.mfa.title') : t('login.title')}</h2>
          <p className="auth-lead">{step === 'mfa' ? t('login.mfa.lead') : t('login.lead')}</p>

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
          ) : (
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
          )}

          <p className="auth-footer-text">
            {t('login.noAccount')} <Link to="/register">{t('login.registerNow')}</Link>
          </p>
        </article>
      </section>
    </div>
  )
}
