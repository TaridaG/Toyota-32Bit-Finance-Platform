import { useCallback, useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { fetchPortalProfile, readApiErrorMessage } from '../../features/profile/api/portalProfileApi'
import { ProfileEmailCard } from '../../features/profile/components/ProfileEmailCard'
import { ProfileNotificationsCard } from '../../features/profile/components/ProfileNotificationsCard'
import { ProfileMfaCard } from '../../features/profile/components/ProfileMfaCard'
import { ProfileOverviewCard } from '../../features/profile/components/ProfileOverviewCard'
import { ProfilePasswordCard } from '../../features/profile/components/ProfilePasswordCard'
import { ProfileTrustedDevicesCard } from '../../features/profile/components/ProfileTrustedDevicesCard'
import { ProfilePhoneCard } from '../../features/profile/components/ProfilePhoneCard'
import { ProfileUsernameCard } from '../../features/profile/components/ProfileUsernameCard'
import type { PortalProfile } from '../../features/profile/types'
import { PortalAlert } from '../../shared/components/PortalAlert'

export function ProfileSettingsPage() {
  const { t } = useTranslation('common')
  const [profile, setProfile] = useState<PortalProfile | null>(null)
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    setLoadError(null)
    try {
      const data = await fetchPortalProfile()
      setProfile(data)
    } catch (err) {
      setLoadError(readApiErrorMessage(err))
      setProfile(null)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void load()
  }, [load])

  useEffect(() => {
    document.title = t('profileSettings.titleDoc')
  }, [t])

  return (
    <main className="profile-settings-page">
      <header className="profile-settings-hero">
        <h1 className="profile-settings-title">{t('profileSettings.title')}</h1>
        <p className="profile-settings-lead">{t('profileSettings.pageLead')}</p>
      </header>

      {loading ? <p className="profile-settings-page-status">{t('loading')}</p> : null}

      {!loading && loadError ? (
        <div className="profile-settings-load-error">
          <PortalAlert variant="error" title={t('profileSettings.alertErrorTitle')}>
            {loadError}
          </PortalAlert>
          <button type="button" className="profile-settings-btn-secondary" onClick={() => void load()}>
            {t('profileSettings.retry')}
          </button>
        </div>
      ) : null}

      {!loading && !loadError && profile ? (
        <div className="profile-settings-shell">
          <div className="profile-overview-row">
            <ProfileOverviewCard profile={profile} onProfileUpdated={setProfile} />
            <ProfileMfaCard
              totpEnabled={profile.totpEnabled}
              onTotpEnabledChange={(enabled) => setProfile((p) => (p ? { ...p, totpEnabled: enabled } : p))}
            />
          </div>
          <div className="profile-settings-stack">
            <ProfileUsernameCard profile={profile} onUsernameChanged={setProfile} />
            <ProfileEmailCard profile={profile} onEmailChanged={setProfile} />
            <ProfilePhoneCard profile={profile} onUpdated={setProfile} />
            <ProfilePasswordCard accountEmail={profile.email} />
            <ProfileTrustedDevicesCard totpEnabled={profile.totpEnabled} />
            <ProfileNotificationsCard profile={profile} onUpdated={setProfile} />
          </div>
        </div>
      ) : null}
    </main>
  )
}
