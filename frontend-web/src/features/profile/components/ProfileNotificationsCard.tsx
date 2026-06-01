import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { readApiErrorMessage, updatePortalNotifications } from '../api/portalProfileApi'
import type { PortalProfile } from '../types'
import { ProfileSettingsPanel } from './ProfileSettingsPanel'

type Props = {
  profile: PortalProfile
  onUpdated: (next: PortalProfile) => void
}

export function ProfileNotificationsCard({ profile, onUpdated }: Props) {
  const { t } = useTranslation('common')
  const [security, setSecurity] = useState(profile.notifySecurityAlerts)
  const [watchlist, setWatchlist] = useState(profile.notifyWatchlistAlerts)
  const [alarm, setAlarm] = useState(profile.notifyAlarmAlerts)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState(false)

  useEffect(() => {
    setSecurity(profile.notifySecurityAlerts)
    setWatchlist(profile.notifyWatchlistAlerts)
    setAlarm(profile.notifyAlarmAlerts)
  }, [profile.notifySecurityAlerts, profile.notifyWatchlistAlerts, profile.notifyAlarmAlerts])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    setSuccess(false)
    setBusy(true)
    try {
      const next = await updatePortalNotifications(security, watchlist, alarm)
      onUpdated(next)
      setSuccess(true)
      window.setTimeout(() => setSuccess(false), 2500)
    } catch (err) {
      setError(readApiErrorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  return (
    <ProfileSettingsPanel kicker={t('profileSettings.kickerNotifications')} summary={null} expanded collapsible={false}>
      <p className="profile-settings-panel-hint">{t('profileSettings.notificationsLead')}</p>
      <form className="profile-settings-form" onSubmit={(ev) => void handleSubmit(ev)}>
        <label className="profile-settings-toggle">
          <input type="checkbox" checked={security} onChange={(e) => setSecurity(e.target.checked)} />
          <span>
            <span className="profile-settings-toggle-title">{t('profileSettings.notifySecurity')}</span>
            <span className="profile-settings-toggle-desc">{t('profileSettings.notifySecurityDesc')}</span>
          </span>
        </label>
        <label className="profile-settings-toggle">
          <input type="checkbox" checked={watchlist} onChange={(e) => setWatchlist(e.target.checked)} />
          <span>
            <span className="profile-settings-toggle-title">{t('profileSettings.notifyWatchlist')}</span>
            <span className="profile-settings-toggle-desc">{t('profileSettings.notifyWatchlistDesc')}</span>
          </span>
        </label>
        <label className="profile-settings-toggle">
          <input type="checkbox" checked={alarm} onChange={(e) => setAlarm(e.target.checked)} />
          <span>
            <span className="profile-settings-toggle-title">{t('profileSettings.notifyAlarm')}</span>
            <span className="profile-settings-toggle-desc">{t('profileSettings.notifyAlarmDesc')}</span>
          </span>
        </label>
        {error ? <p className="profile-settings-error">{error}</p> : null}
        {success ? <p className="profile-settings-saved">{t('profileSettings.notifySuccess')}</p> : null}
        <div className="profile-settings-inline-actions">
          <button type="submit" className="profile-settings-btn-primary" disabled={busy}>
            {busy ? t('profileSettings.saving') : t('profileSettings.notifySubmit')}
          </button>
        </div>
      </form>
    </ProfileSettingsPanel>
  )
}
