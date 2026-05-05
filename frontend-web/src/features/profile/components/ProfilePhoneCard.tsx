import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { readApiErrorMessage, updatePortalPhone } from '../api/portalProfileApi'
import type { PortalProfile } from '../types'
import { ProfileSettingsPanel } from './ProfileSettingsPanel'

type Props = {
  profile: PortalProfile
  onUpdated: (next: PortalProfile) => void
}

export function ProfilePhoneCard({ profile, onUpdated }: Props) {
  const { t } = useTranslation('common')
  const [expanded, setExpanded] = useState(false)
  const [phone, setPhone] = useState(profile.phone ?? '')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState(false)

  useEffect(() => {
    setPhone(profile.phone ?? '')
  }, [profile.phone])

  const summary = profile.phone?.trim() ? profile.phone : t('profileSettings.phoneNotSet')

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    setSuccess(false)
    setBusy(true)
    try {
      const next = await updatePortalPhone(phone.trim())
      onUpdated(next)
      setSuccess(true)
      window.setTimeout(() => setSuccess(false), 2500)
      setExpanded(false)
    } catch (err) {
      setError(readApiErrorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <ProfileSettingsPanel
      kicker={t('profileSettings.kickerPhone')}
      summary={summary}
      expanded={expanded}
      onToggle={setExpanded}
    >
      <p className="profile-settings-panel-hint">{t('profileSettings.phoneLead')}</p>
      <form className="profile-settings-form" onSubmit={(ev) => void handleSubmit(ev)}>
        <label className="profile-settings-label" htmlFor="profile-phone">
          {t('profileSettings.phoneLabel')}
        </label>
        <input
          id="profile-phone"
          type="tel"
          autoComplete="tel"
          className="profile-settings-input"
          value={phone}
          onChange={(e) => setPhone(e.target.value)}
          maxLength={32}
          placeholder={t('profileSettings.phonePlaceholder')}
        />
        <p className="profile-settings-hint profile-settings-hint-tight">{t('profileSettings.phoneHint')}</p>
        {error ? <p className="profile-settings-error">{error}</p> : null}
        {success ? <p className="profile-settings-saved">{t('profileSettings.phoneSuccess')}</p> : null}
        <div className="profile-settings-inline-actions">
          <button type="submit" className="profile-settings-btn-primary" disabled={busy}>
            {busy ? t('profileSettings.saving') : t('profileSettings.phoneSubmit')}
          </button>
        </div>
      </form>
    </ProfileSettingsPanel>
  )
}
