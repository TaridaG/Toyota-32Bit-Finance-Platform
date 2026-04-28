import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useDocumentTitle } from '../../shared/hooks/useDocumentTitle'
import { persistAuthToken } from '../../shared/auth/session'

export function LoginPage() {
  const { t } = useTranslation('auth')
  useDocumentTitle(t('login.titleDoc'))

  const navigate = useNavigate()
  const [identity, setIdentity] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const handleSubmit = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setError(null)

    if (!identity.trim()) {
      setError(t('login.errors.identityRequired'))
      return
    }

    if (password.trim().length < 6) {
      setError(t('login.errors.passwordMin'))
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
          <p className="auth-kicker">{t('login.kicker')}</p>
          <h2>{t('login.title')}</h2>
          <p className="auth-lead">{t('login.lead')}</p>

          <form className="auth-form" onSubmit={handleSubmit}>
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

            {error ? <p className="auth-error">{error}</p> : null}

            <button type="submit" className="auth-submit" disabled={submitting}>
              {submitting ? t('login.submitting') : t('login.submit')}
            </button>
          </form>

          <p className="auth-footer-text">
            {t('login.noAccount')} <Link to="/register">{t('login.registerNow')}</Link>
          </p>
        </article>
      </section>
    </div>
  )
}

