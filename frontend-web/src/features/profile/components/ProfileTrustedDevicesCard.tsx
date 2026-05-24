import { useCallback, useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import {
  fetchPortalTrustedDevices,
  revokeAllPortalTrustedDevices,
  revokePortalTrustedDevice,
  type PortalTrustedDeviceRow,
} from '../api/portalTrustedDevicesApi'
import { readApiErrorMessage } from '../api/portalProfileApi'
import { ProfileSettingsPanel } from './ProfileSettingsPanel'

type Props = {
  totpEnabled: boolean
}

function formatDeviceInstant(iso: string | null | undefined, locale: string): string {
  if (!iso) {
    return '—'
  }
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) {
    return '—'
  }
  return new Intl.DateTimeFormat(locale, {
    dateStyle: 'medium',
    timeStyle: 'short',
  }).format(d)
}

function deviceShortId(id: string): string {
  const compact = id.replace(/-/g, '')
  return compact.length >= 8 ? compact.slice(-8).toUpperCase() : id.slice(0, 8)
}

export function ProfileTrustedDevicesCard({ totpEnabled }: Props) {
  const { t, i18n } = useTranslation('common')
  const locale = i18n.language
  const [expanded, setExpanded] = useState(false)
  const [featureEnabled, setFeatureEnabled] = useState(true)
  const [devices, setDevices] = useState<PortalTrustedDeviceRow[]>([])
  const [loading, setLoading] = useState(true)
  const [busyId, setBusyId] = useState<string | null>(null)
  const [revokingAll, setRevokingAll] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const data = await fetchPortalTrustedDevices()
      setFeatureEnabled(data.featureEnabled)
      setDevices(data.devices)
    } catch (err) {
      setError(readApiErrorMessage(err))
      setDevices([])
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void load()
  }, [load])

  const summary = useMemo(() => {
    if (!featureEnabled) {
      return t('profileSettings.trustedDevices.summaryDisabled')
    }
    if (devices.length === 0) {
      return t('profileSettings.trustedDevices.summaryEmpty')
    }
    return t('profileSettings.trustedDevices.summaryCount', { count: devices.length })
  }, [devices.length, featureEnabled, t])

  const handleRevoke = async (device: PortalTrustedDeviceRow) => {
    setError(null)
    setSuccess(null)
    setBusyId(device.id)
    try {
      await revokePortalTrustedDevice(device.id)
      setDevices((prev) => prev.filter((d) => d.id !== device.id))
      setSuccess(
        device.currentDevice
          ? t('profileSettings.trustedDevices.revokedCurrent')
          : t('profileSettings.trustedDevices.revokedOne'),
      )
      window.setTimeout(() => setSuccess(null), 4000)
    } catch (err) {
      setError(readApiErrorMessage(err))
    } finally {
      setBusyId(null)
    }
  }

  const handleRevokeAll = async () => {
    if (devices.length === 0) {
      return
    }
    const ok = window.confirm(t('profileSettings.trustedDevices.revokeAllConfirm'))
    if (!ok) {
      return
    }
    setError(null)
    setSuccess(null)
    setRevokingAll(true)
    try {
      await revokeAllPortalTrustedDevices()
      setDevices([])
      setSuccess(t('profileSettings.trustedDevices.revokedAll'))
      window.setTimeout(() => setSuccess(null), 4000)
    } catch (err) {
      setError(readApiErrorMessage(err))
    } finally {
      setRevokingAll(false)
    }
  }

  const showList = featureEnabled && !loading

  return (
    <ProfileSettingsPanel
      kicker={t('profileSettings.kickerTrustedDevices')}
      summary={summary}
      expanded={expanded}
      onToggle={setExpanded}
    >
      <p className="profile-settings-panel-hint">{t('profileSettings.trustedDevices.lead')}</p>
      {!totpEnabled ? (
        <p className="profile-settings-hint profile-settings-hint-warning">{t('profileSettings.trustedDevices.mfaRequired')}</p>
      ) : null}

      {loading ? <p className="profile-settings-hint">{t('loading')}</p> : null}

      {!featureEnabled && !loading ? (
        <p className="profile-settings-hint">{t('profileSettings.trustedDevices.featureDisabled')}</p>
      ) : null}

      {showList && devices.length === 0 ? (
        <p className="profile-settings-hint">{t('profileSettings.trustedDevices.empty')}</p>
      ) : null}

      {showList && devices.length > 0 ? (
        <ul className="profile-trusted-devices-list" aria-label={t('profileSettings.trustedDevices.listAria')}>
          {devices.map((device, index) => (
            <li key={device.id} className="profile-trusted-device-row">
              <div className="profile-trusted-device-row-main">
                <p className="profile-trusted-device-row-title">
                  {device.currentDevice
                    ? t('profileSettings.trustedDevices.thisBrowser')
                    : t('profileSettings.trustedDevices.browserLabel', {
                        index: devices.length - index,
                        id: deviceShortId(device.id),
                      })}
                  {device.currentDevice ? (
                    <span className="profile-trusted-device-badge">{t('profileSettings.trustedDevices.currentBadge')}</span>
                  ) : null}
                </p>
                <dl className="profile-trusted-device-meta">
                  <div>
                    <dt>{t('profileSettings.trustedDevices.added')}</dt>
                    <dd>{formatDeviceInstant(device.createdAt, locale)}</dd>
                  </div>
                  <div>
                    <dt>{t('profileSettings.trustedDevices.lastUsed')}</dt>
                    <dd>{formatDeviceInstant(device.lastUsedAt, locale)}</dd>
                  </div>
                  <div>
                    <dt>{t('profileSettings.trustedDevices.expires')}</dt>
                    <dd>{formatDeviceInstant(device.expiresAt, locale)}</dd>
                  </div>
                </dl>
              </div>
              <button
                type="button"
                className="profile-settings-btn-secondary profile-trusted-device-revoke"
                disabled={busyId != null || revokingAll}
                onClick={() => void handleRevoke(device)}
              >
                {busyId === device.id
                  ? t('profileSettings.saving')
                  : t('profileSettings.trustedDevices.revoke')}
              </button>
            </li>
          ))}
        </ul>
      ) : null}

      {error ? <p className="profile-settings-error">{error}</p> : null}
      {success ? <p className="profile-settings-saved">{success}</p> : null}

      {showList && devices.length > 1 ? (
        <div className="profile-settings-inline-actions">
          <button
            type="button"
            className="profile-settings-btn-secondary"
            disabled={revokingAll || busyId != null}
            onClick={() => void handleRevokeAll()}
          >
            {revokingAll
              ? t('profileSettings.saving')
              : t('profileSettings.trustedDevices.revokeAll')}
          </button>
        </div>
      ) : null}
    </ProfileSettingsPanel>
  )
}
