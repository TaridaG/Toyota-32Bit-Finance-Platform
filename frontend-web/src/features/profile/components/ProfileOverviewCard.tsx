import { useEffect, useId, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { notifyProfileAvatarChanged } from '../../../shared/auth/session'
import {
  deletePortalAvatar,
  readApiErrorMessage,
  uploadPortalAvatar,
} from '../api/portalProfileApi'
import { usePortalAvatarObjectUrl } from '../hooks/usePortalAvatarObjectUrl'
import { ProfileAvatarCropModal } from './ProfileAvatarCropModal'
import type { PortalProfile } from '../types'

type Props = {
  profile: PortalProfile
  onProfileUpdated: (next: PortalProfile) => void
}

export function ProfileOverviewCard({ profile, onProfileUpdated }: Props) {
  const { t } = useTranslation('common')
  const inputId = useId()
  const fileRef = useRef<HTMLInputElement>(null)
  const serverAvatarUrl = usePortalAvatarObjectUrl(profile.avatarUpdatedAt)
  const [pendingFile, setPendingFile] = useState<File | null>(null)
  const [pendingPreviewUrl, setPendingPreviewUrl] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [savedHint, setSavedHint] = useState(false)
  const [cropOpen, setCropOpen] = useState(false)
  const [cropImageSrc, setCropImageSrc] = useState<string | null>(null)

  useEffect(() => {
    if (!pendingFile) {
      setPendingPreviewUrl(null)
      return undefined
    }
    const url = URL.createObjectURL(pendingFile)
    setPendingPreviewUrl(url)
    return () => URL.revokeObjectURL(url)
  }, [pendingFile])

  useEffect(() => {
    return () => {
      if (cropImageSrc) {
        URL.revokeObjectURL(cropImageSrc)
      }
    }
  }, [cropImageSrc])

  const displaySrc = pendingPreviewUrl ?? serverAvatarUrl
  const phoneDisplay = profile.phone?.trim() ? profile.phone : t('profileSettings.phoneNotSet')

  const handleFile = (file: File | undefined) => {
    setError(null)
    setSavedHint(false)
    if (!file) return
    if (!file.type.startsWith('image/')) {
      setError(t('profileSettings.errorFile'))
      return
    }
    if (cropImageSrc) {
      URL.revokeObjectURL(cropImageSrc)
    }
    setCropImageSrc(URL.createObjectURL(file))
    setCropOpen(true)
  }

  const handleCropClose = () => {
    setCropOpen(false)
    if (cropImageSrc) {
      URL.revokeObjectURL(cropImageSrc)
      setCropImageSrc(null)
    }
    if (fileRef.current) {
      fileRef.current.value = ''
    }
  }

  const handleCropConfirm = (croppedFile: File) => {
    setPendingFile(croppedFile)
    setCropOpen(false)
    if (cropImageSrc) {
      URL.revokeObjectURL(cropImageSrc)
      setCropImageSrc(null)
    }
  }

  const handleSavePhoto = async () => {
    if (!pendingFile) return
    setBusy(true)
    setError(null)
    try {
      const next = await uploadPortalAvatar(pendingFile)
      onProfileUpdated(next)
      notifyProfileAvatarChanged({ avatarUpdatedAt: next.avatarUpdatedAt ?? null })
      setPendingFile(null)
      setSavedHint(true)
      window.setTimeout(() => setSavedHint(false), 2000)
      if (fileRef.current) fileRef.current.value = ''
    } catch (err) {
      setError(readApiErrorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  const handleRemovePhoto = async () => {
    if (pendingFile) {
      setPendingFile(null)
      setError(null)
      if (fileRef.current) fileRef.current.value = ''
      return
    }
    setError(null)
    setSavedHint(false)
    setBusy(true)
    try {
      const next = await deletePortalAvatar()
      onProfileUpdated(next)
      notifyProfileAvatarChanged({ avatarUpdatedAt: next.avatarUpdatedAt ?? null })
      if (fileRef.current) fileRef.current.value = ''
    } catch (err) {
      setError(readApiErrorMessage(err))
    } finally {
      setBusy(false)
    }
  }

  const hasServerAvatar = Boolean(profile.avatarUpdatedAt)
  const showRemove = hasServerAvatar || pendingFile != null

  return (
    <section className="profile-overview" aria-label={t('profileSettings.overviewAria')}>
      <ProfileAvatarCropModal
        open={cropOpen}
        imageSrc={cropImageSrc}
        onClose={handleCropClose}
        onConfirm={handleCropConfirm}
      />
      <div className="profile-overview-visual">
        <div className="profile-overview-avatar-wrap">
          {displaySrc ? (
            <img src={displaySrc} alt="" className="profile-overview-avatar" />
          ) : (
            <div className="profile-overview-avatar profile-overview-avatar-placeholder" aria-hidden>
              {profile.username.slice(0, 1).toUpperCase()}
            </div>
          )}
        </div>
        <input
          ref={fileRef}
          id={inputId}
          type="file"
          accept="image/png,image/jpeg,image/webp"
          className="profile-overview-file-input"
          disabled={busy}
          onChange={(e) => handleFile(e.target.files?.[0])}
        />
        <div className="profile-overview-photo-actions">
          <button
            type="button"
            className="profile-settings-btn-edit"
            disabled={busy}
            onClick={() => fileRef.current?.click()}
          >
            {t('profileSettings.changePhoto')}
          </button>
          {pendingFile ? (
            <button
              type="button"
              className="profile-settings-btn-primary profile-overview-btn-compact"
              disabled={busy}
              onClick={() => void handleSavePhoto()}
            >
              {busy ? t('profileSettings.saving') : t('profileSettings.save')}
            </button>
          ) : null}
          {showRemove ? (
            <button
              type="button"
              className="profile-settings-btn-secondary profile-overview-btn-compact"
              disabled={busy}
              onClick={() => void handleRemovePhoto()}
            >
              {t('profileSettings.remove')}
            </button>
          ) : null}
        </div>
        <p className="profile-overview-photo-note">{t('profileSettings.lead')}</p>
        {error ? <p className="profile-settings-error">{error}</p> : null}
        {savedHint ? <p className="profile-settings-saved">{t('profileSettings.saved')}</p> : null}
      </div>

      <div className="profile-overview-facts">
        <div className="profile-overview-fact">
          <span className="profile-overview-fact-label">{t('profileSettings.accountEmail')}</span>
          <span className="profile-overview-fact-value profile-overview-fact-mono">{profile.email}</span>
          <span className="profile-overview-fact-hint">{t('profileSettings.emailReadonlyHint')}</span>
        </div>
        <div className="profile-overview-fact">
          <span className="profile-overview-fact-label">{t('profileSettings.accountUsername')}</span>
          <span className="profile-overview-fact-value">@{profile.username}</span>
        </div>
        <div className="profile-overview-fact">
          <span className="profile-overview-fact-label">{t('profileSettings.accountPhone')}</span>
          <span className="profile-overview-fact-value">{phoneDisplay}</span>
        </div>
      </div>
    </section>
  )
}
