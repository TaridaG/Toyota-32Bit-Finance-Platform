import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useDocumentTitle } from '../../shared/hooks/useDocumentTitle'
import { persistAuthToken } from '../../shared/auth/session'

export function RegisterPage() {
  const { t } = useTranslation('auth')
  useDocumentTitle(t('register.titleDoc'))

  const navigate = useNavigate()
  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [passwordAgain, setPasswordAgain] = useState('')
  const [acceptedTerms, setAcceptedTerms] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
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

    setSubmitting(true)

    window.setTimeout(() => {
      persistAuthToken(`session_${Date.now()}`)
      navigate('/app', { replace: true })
    }, 350)
  }

  return (
    <div className="auth-page">
      <section className="auth-card-wrap">
        <article className="auth-card">
          <p className="auth-kicker">{t('register.kicker')}</p>
          <h2>{t('register.title')}</h2>
          <p className="auth-lead">{t('register.lead')}</p>

          <form className="auth-form" onSubmit={handleSubmit}>
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
            />

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
            />

            <label className="auth-check">
              <input
                type="checkbox"
                checked={acceptedTerms}
                onChange={(e) => setAcceptedTerms(e.target.checked)}
              />
              <span>{t('register.termsLabel')}</span>
            </label>

            {error ? <p className="auth-error">{error}</p> : null}

            <button type="submit" className="auth-submit" disabled={submitting}>
              {submitting ? t('register.submitting') : t('register.submit')}
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

