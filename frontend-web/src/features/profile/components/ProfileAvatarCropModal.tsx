import { useCallback, useEffect, useState } from 'react'
import Cropper, { type Area } from 'react-easy-crop'
import { useTranslation } from 'react-i18next'
import {
  AVATAR_CROP_EXPORT_MAX_EDGE,
  blobToFile,
  renderCroppedAvatarJpeg,
} from '../utils/cropAvatarImage'

type Props = {
  open: boolean
  imageSrc: string | null
  onClose: () => void
  onConfirm: (file: File) => void
}

export function ProfileAvatarCropModal({ open, imageSrc, onClose, onConfirm }: Props) {
  const { t } = useTranslation('common')
  const [crop, setCrop] = useState({ x: 0, y: 0 })
  const [zoom, setZoom] = useState(1)
  const [croppedAreaPixels, setCroppedAreaPixels] = useState<Area | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (open) {
      setCrop({ x: 0, y: 0 })
      setZoom(1)
      setCroppedAreaPixels(null)
      setError(null)
      setBusy(false)
    }
  }, [open, imageSrc])

  const onCropComplete = useCallback((_area: Area, areaPx: Area) => {
    setCroppedAreaPixels(areaPx)
  }, [])

  /** Fires on init and every move — {@link onCropComplete} alone can miss the first paint, leaving confirm disabled. */
  const onCropAreaChange = useCallback((_area: Area, areaPx: Area) => {
    setCroppedAreaPixels(areaPx)
  }, [])

  const handleConfirm = async () => {
    if (!imageSrc || !croppedAreaPixels) {
      return
    }
    setBusy(true)
    setError(null)
    try {
      const img = new Image()
      await new Promise<void>((resolve, reject) => {
        img.onload = () => resolve()
        img.onerror = () => reject(new Error('load'))
        img.src = imageSrc
      })
      const blob = await renderCroppedAvatarJpeg(img, croppedAreaPixels, AVATAR_CROP_EXPORT_MAX_EDGE)
      const file = blobToFile(blob, 'profile-avatar.jpg')
      onConfirm(file)
    } catch {
      setError(t('profileSettings.cropError'))
    } finally {
      setBusy(false)
    }
  }

  if (!open || !imageSrc) {
    return null
  }

  return (
    <div className="profile-avatar-crop-backdrop" role="presentation" onClick={() => !busy && onClose()}>
      <div
        className="profile-avatar-crop-dialog"
        role="dialog"
        aria-modal="true"
        aria-labelledby="profile-avatar-crop-title"
        onClick={(e) => e.stopPropagation()}
        onKeyDown={(e) => {
          if (e.key === 'Escape' && !busy) {
            onClose()
          }
        }}
      >
        <h2 id="profile-avatar-crop-title" className="profile-avatar-crop-title">
          {t('profileSettings.cropTitle')}
        </h2>
        <p className="profile-avatar-crop-hint">{t('profileSettings.cropHint')}</p>
        <div className="profile-avatar-crop-stage">
          <Cropper
            image={imageSrc}
            crop={crop}
            zoom={zoom}
            aspect={1}
            cropShape="round"
            showGrid={false}
            onCropChange={setCrop}
            onCropComplete={onCropComplete}
            onCropAreaChange={onCropAreaChange}
            onZoomChange={setZoom}
          />
        </div>
        <label className="profile-avatar-crop-zoom">
          <span>{t('profileSettings.cropZoom')}</span>
          <input
            type="range"
            min={1}
            max={3}
            step={0.01}
            value={zoom}
            onChange={(e) => setZoom(Number(e.target.value))}
            disabled={busy}
          />
        </label>
        {error ? <p className="profile-settings-error">{error}</p> : null}
        <div className="profile-avatar-crop-actions">
          <button type="button" className="profile-settings-btn-secondary" disabled={busy} onClick={onClose}>
            {t('profileSettings.cropCancel')}
          </button>
          <button
            type="button"
            className="profile-settings-btn-primary"
            disabled={busy || !croppedAreaPixels}
            onClick={() => void handleConfirm()}
          >
            {busy ? t('profileSettings.saving') : t('profileSettings.cropConfirm')}
          </button>
        </div>
      </div>
    </div>
  )
}
