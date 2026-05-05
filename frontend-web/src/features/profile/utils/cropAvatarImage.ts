import type { Area } from 'react-easy-crop'

/** Max longer edge for the cropped JPEG sent to the API (server still normalizes to ~512px JPEG). */
export const AVATAR_CROP_EXPORT_MAX_EDGE = 2048

/**
 * Renders the cropped region from the source image at up to {@link AVATAR_CROP_EXPORT_MAX_EDGE}
 * on the longest edge, then encodes as JPEG (browser). Server-side processing remains canonical.
 */
export function renderCroppedAvatarJpeg(
  image: HTMLImageElement,
  pixelCrop: Area,
  maxEdge = AVATAR_CROP_EXPORT_MAX_EDGE,
  quality = 0.92,
): Promise<Blob> {
  const scaleX = image.naturalWidth / image.width
  const scaleY = image.naturalHeight / image.height
  const sourceCropW = pixelCrop.width * scaleX
  const sourceCropH = pixelCrop.height * scaleY
  const sx = pixelCrop.x * scaleX
  const sy = pixelCrop.y * scaleY

  let outW = sourceCropW
  let outH = sourceCropH
  const longest = Math.max(outW, outH)
  if (longest > maxEdge) {
    const s = maxEdge / longest
    outW *= s
    outH *= s
  }

  const canvas = document.createElement('canvas')
  canvas.width = Math.max(1, Math.round(outW))
  canvas.height = Math.max(1, Math.round(outH))
  const ctx = canvas.getContext('2d')
  if (!ctx) {
    return Promise.reject(new Error('Canvas unsupported'))
  }
  ctx.drawImage(image, sx, sy, sourceCropW, sourceCropH, 0, 0, canvas.width, canvas.height)

  return new Promise((resolve, reject) => {
    canvas.toBlob(
      (blob) => {
        if (blob) {
          resolve(blob)
        } else {
          reject(new Error('JPEG encoding failed'))
        }
      },
      'image/jpeg',
      quality,
    )
  })
}

export function blobToFile(blob: Blob, name: string): File {
  return new File([blob], name, { type: blob.type || 'image/jpeg', lastModified: Date.now() })
}
