import { useEffect, useRef, useState } from 'react'
import { fetchPortalAvatarBlob } from '../api/portalProfileApi'

/**
 * Fetches the authenticated user's profile avatar as a blob URL (JPEG).
 * Revokes the URL on cleanup or when {@link avatarUpdatedAt} changes.
 */
export function usePortalAvatarObjectUrl(avatarUpdatedAt: string | null | undefined) {
  const [url, setUrl] = useState<string | null>(null)
  const objectUrlRef = useRef<string | null>(null)

  useEffect(() => {
    if (!avatarUpdatedAt) {
      if (objectUrlRef.current) {
        URL.revokeObjectURL(objectUrlRef.current)
        objectUrlRef.current = null
      }
      setUrl(null)
      return undefined
    }

    let cancelled = false

    void (async () => {
      const blob = await fetchPortalAvatarBlob()
      if (cancelled) {
        return
      }
      if (objectUrlRef.current) {
        URL.revokeObjectURL(objectUrlRef.current)
        objectUrlRef.current = null
      }
      if (!blob || blob.size === 0) {
        if (!cancelled) {
          setUrl(null)
        }
        return
      }
      const u = URL.createObjectURL(blob)
      objectUrlRef.current = u
      if (!cancelled) {
        setUrl(u)
      } else {
        URL.revokeObjectURL(u)
      }
    })()

    return () => {
      cancelled = true
      if (objectUrlRef.current) {
        URL.revokeObjectURL(objectUrlRef.current)
        objectUrlRef.current = null
      }
    }
  }, [avatarUpdatedAt])

  return url
}
