import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import axios from 'axios'
import { useDocumentTitle } from '../../shared/hooks/useDocumentTitle'
import { loginWithPortalPassword } from '../../shared/api/publicAuth'
import { persistAuthSession } from '../../shared/auth/session'

export function LoginPage() {
  const { t } = useTranslation('auth')
  useDocumentTitle(t('login.titleDoc'))

  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const registered = searchParams.get('registered') === '1'
  const sessionExpired = searchParams.get('session') === 'expired'

  const [identity, setIdentity] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
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
      const tokens = await loginWithPortalPassword(trimmed, password)
      persistAuthSession({
        accessToken: tokens.accessToken,
        refreshToken: tokens.refreshToken,
      })
      navigate('/app', { replace: true })
    } catch (e) {
      if (axios.isAxiosError(e) && e.response?.data && typeof e.response.data === 'object') {
        const body = e.response.data as { error?: { message?: string } }
        const msg = body.error?.message
        if (msg) {
          setError(msg)
          return
        }
      }
      if (axios.isAxiosError(e) && e.response?.status === 401) {
        setError(t('login.errors.invalidCredentials'))
        return
      }
      if (e instanceof Error && e.message) {
        setError(e.message)
        return
      }
      setError(t('login.errors.generic'))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="auth-page">
      <section className="auth-card-wrap">
        <article className="auth-card">
          <p className="auth-kicker">{t('login.kicker')}</p>
          <h2>{t('login.title')}</h2>
          <p className="auth-lead">{t('login.lead')}</p>

          {sessionExpired ? <p className="auth-lead">{t('login.sessionExpiredBanner')}</p> : null}
          {registered ? <p className="auth-lead">{t('login.registeredBanner')}</p> : null}

          <form className="auth-form" onSubmit={(e) => void handleSubmit(e)}>
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

            {error ? <p className="auth-error">{error}</p> : null}

            <p className="auth-footer-text" style={{ marginBottom: '0.75rem' }}>
              {t('login.signInHint')}
            </p>

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
