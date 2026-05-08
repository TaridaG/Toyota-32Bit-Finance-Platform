import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import axios from 'axios'
import { useDocumentTitle } from '../../shared/hooks/useDocumentTitle'
import {
  checkUsernameAvailability,
  registerPortalUser,
  sendRegistrationVerificationCode,
} from '../../shared/api/publicRegistration'

export function RegisterPage() {
  const { t } = useTranslation('auth')
  useDocumentTitle(t('register.titleDoc'))

  const navigate = useNavigate()
  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [passwordAgain, setPasswordAgain] = useState('')
  const [acceptedTerms, setAcceptedTerms] = useState(false)
  const [verificationCode, setVerificationCode] = useState('')
  const [verificationStep, setVerificationStep] = useState(false)
  const [verifyCountdown, setVerifyCountdown] = useState(90)
  const [resendCountdown, setResendCountdown] = useState(15)
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [sendingCode, setSendingCode] = useState(false)
  const [usernameCheckState, setUsernameCheckState] = useState<'idle' | 'checking' | 'available' | 'taken'>('idle')
  const [usernameSuggestions, setUsernameSuggestions] = useState<string[]>([])

  useEffect(() => {
    if (!verificationStep) {
      return
    }
    const timer = window.setInterval(() => {
      setVerifyCountdown((prev) => (prev > 0 ? prev - 1 : 0))
      setResendCountdown((prev) => (prev > 0 ? prev - 1 : 0))
    }, 1000)
    return () => window.clearInterval(timer)
  }, [verificationStep])

  useEffect(() => {
    if (verificationStep) {
      return
    }
    const trimmed = username.trim().toLowerCase()
    if (!trimmed) {
      setUsernameCheckState('idle')
      setUsernameSuggestions([])
      return
    }
    if (!/^[a-zA-Z0-9._-]+$/.test(trimmed)) {
      setUsernameCheckState('idle')
      setUsernameSuggestions([])
      return
    }
    setUsernameCheckState('checking')
    const timer = window.setTimeout(() => {
      void checkUsernameAvailability(trimmed)
        .then((result) => {
          setUsernameCheckState(result.available ? 'available' : 'taken')
          setUsernameSuggestions(result.available ? [] : result.suggestions.slice(0, 3))
        })
        .catch(() => {
          setUsernameCheckState('idle')
          setUsernameSuggestions([])
        })
    }, 2000)
    return () => window.clearTimeout(timer)
  }, [username, verificationStep])

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError(null)

    if (!fullName.trim()) {
      setError(t('register.errors.fullNameRequired'))
      return
    }

    if (!email.trim() || !email.includes('@')) {
      setError(t('register.errors.emailInvalid'))
      return
    }

    const trimmedUsername = username.trim()
    if (!trimmedUsername) {
      setError(t('register.errors.usernameRequired'))
      return
    }
    if (!/^[a-zA-Z0-9._-]+$/.test(trimmedUsername)) {
      setError(t('register.errors.usernameInvalid'))
      return
    }

    if (password.length < 8) {
      setError(t('register.errors.passwordMin'))
      return
    }

    if (password !== passwordAgain) {
      setError(t('register.errors.passwordsNotMatch'))
      return
    }

    if (!acceptedTerms) {
      setError(t('register.errors.termsRequired'))
      return
    }

    if (!verificationStep) {
      if (usernameCheckState === 'checking') {
        setError(t('register.usernameChecking'))
        return
      }
      if (usernameCheckState === 'taken') {
        setError(t('register.usernameTaken'))
        return
      }
      setSendingCode(true)
      try {
        const sent = await sendRegistrationVerificationCode(email.trim().toLowerCase())
        setVerifyCountdown(sent.expiresInSeconds)
        setResendCountdown(sent.resendInSeconds)
        setVerificationStep(true)
        return
      } catch (e) {
        if (e instanceof Error && e.message) {
          setError(e.message)
          return
        }
        setError(t('register.errors.generic'))
      } finally {
        setSendingCode(false)
      }
      return
    }

    if (!verificationCode.trim()) {
      setError(t('register.errors.verificationCodeRequired'))
      return
    }

    setSubmitting(true)
    try {
      await registerPortalUser({
        email: email.trim().toLowerCase(),
        username: trimmedUsername.toLowerCase(),
        password,
        verificationCode: verificationCode.trim(),
      })
      navigate('/login?registered=1', { replace: true })
    } catch (e) {
      if (axios.isAxiosError(e) && e.response?.data && typeof e.response.data === 'object') {
        const body = e.response.data as { error?: { message?: string } }
        const msg = body.error?.message
        if (msg) {
          setError(msg)
          return
        }
      }
      if (e instanceof Error && e.message) {
        setError(e.message)
        return
      }
      setError(t('register.errors.generic'))
    } finally {
      setSubmitting(false)
    }
  }

  const handleResend = async () => {
    if (resendCountdown > 0 || sendingCode) {
      return
    }
    setError(null)
    setSendingCode(true)
    try {
      const sent = await sendRegistrationVerificationCode(email.trim().toLowerCase())
      setVerifyCountdown(sent.expiresInSeconds)
      setResendCountdown(sent.resendInSeconds)
    } catch (e) {
      if (e instanceof Error && e.message) {
        setError(e.message)
        return
      }
      setError(t('register.errors.generic'))
    } finally {
      setSendingCode(false)
    }
  }

  return (
    <div className="auth-page">
      <section className="auth-card-wrap">
        <article className="auth-card">
          <p className="auth-kicker">{t('register.kicker')}</p>
          <h2>{t('register.title')}</h2>
          <p className="auth-lead">{t('register.lead')}</p>

          <form className="auth-form" onSubmit={(e) => void handleSubmit(e)}>
            <label className="auth-label" htmlFor="fullName">
              {t('register.fullNameLabel')}
            </label>
            <input
              id="fullName"
              className="auth-input"
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
              placeholder={t('register.fullNamePlaceholder')}
              autoComplete="name"
              disabled={verificationStep}
            />

            <label className="auth-label" htmlFor="email">
              {t('register.emailLabel')}
            </label>
            <input
              id="email"
              className="auth-input"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder={t('register.emailPlaceholder')}
              autoComplete="email"
              disabled={verificationStep}
            />

            <label className="auth-label" htmlFor="username">
              {t('register.usernameLabel')}
            </label>
            <div className="auth-input-wrap">
              <input
                id="username"
                className="auth-input"
                value={username}
                onChange={(e) => setUsername(e.target.value)}
                placeholder={t('register.usernamePlaceholder')}
                autoComplete="username"
                disabled={verificationStep}
              />
              {usernameCheckState === 'checking' ? (
                <span className="auth-input-status auth-input-status-spinner" aria-label={t('register.usernameChecking')} />
              ) : null}
              {usernameCheckState === 'available' ? (
                <span className="auth-input-status auth-input-status-ok" aria-label={t('register.usernameAvailable')}>✓</span>
              ) : null}
              {usernameCheckState === 'taken' ? (
                <span className="auth-input-status auth-input-status-bad" aria-label={t('register.usernameTaken')}>✕</span>
              ) : null}
            </div>
            {usernameCheckState === 'taken' ? (
              <p className="auth-help auth-help-warning">{t('register.usernameTaken')}</p>
            ) : null}
            {usernameSuggestions.length > 0 ? (
              <div className="auth-username-suggestions">
                <span>{t('register.usernameSuggestionsTitle')}</span>
                <div>
                  {usernameSuggestions.map((suggestion) => (
                    <button
                      key={suggestion}
                      type="button"
                      className="auth-username-suggestion"
                      onClick={() => setUsername(suggestion)}
                      disabled={verificationStep}
                    >
                      {suggestion}
                    </button>
                  ))}
                </div>
              </div>
            ) : null}

            <label className="auth-label" htmlFor="newPassword">
              {t('register.passwordLabel')}
            </label>
            <input
              id="newPassword"
              className="auth-input"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder={t('register.passwordPlaceholder')}
              autoComplete="new-password"
              disabled={verificationStep}
            />

            <label className="auth-label" htmlFor="passwordAgain">
              {t('register.passwordAgainLabel')}
            </label>
            <input
              id="passwordAgain"
              className="auth-input"
              type="password"
              value={passwordAgain}
              onChange={(e) => setPasswordAgain(e.target.value)}
              placeholder={t('register.passwordAgainPlaceholder')}
              autoComplete="new-password"
              disabled={verificationStep}
            />

            <label className="auth-check">
              <input
                type="checkbox"
                checked={acceptedTerms}
                onChange={(e) => setAcceptedTerms(e.target.checked)}
                disabled={verificationStep}
              />
              <span>{t('register.termsLabel')}</span>
            </label>

            {verificationStep ? (
              <>
                <label className="auth-label" htmlFor="verificationCode">
                  {t('register.verificationCodeLabel')}
                </label>
                <input
                  id="verificationCode"
                  className="auth-input"
                  value={verificationCode}
                  onChange={(e) => setVerificationCode(e.target.value)}
                  placeholder={t('register.verificationCodePlaceholder')}
                  inputMode="numeric"
                  autoComplete="one-time-code"
                />
                <p className="auth-help">
                  {t('register.verificationExpiry', { seconds: verifyCountdown })}
                </p>
                <button
                  type="button"
                  className="auth-submit auth-submit-secondary"
                  onClick={() => void handleResend()}
                  disabled={resendCountdown > 0 || sendingCode}
                >
                  {resendCountdown > 0
                    ? t('register.resendIn', { seconds: resendCountdown })
                    : sendingCode
                      ? t('register.resending')
                      : t('register.resendCode')}
                </button>
              </>
            ) : null}

            {error ? <p className="auth-error">{error}</p> : null}

            <button type="submit" className="auth-submit" disabled={submitting}>
              {verificationStep
                ? submitting
                  ? t('register.verifying')
                  : t('register.verifyAndSubmit')
                : sendingCode
                  ? t('register.sendingCode')
                  : t('register.submit')}
            </button>
          </form>

          <p className="auth-footer-text">
            {t('register.haveAccount')} <Link to="/login">{t('register.loginNow')}</Link>
          </p>
        </article>
      </section>
    </div>
  )
}
