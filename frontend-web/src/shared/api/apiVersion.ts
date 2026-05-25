/** Canonical external REST API version (gateway rewrites to downstream /api/**). */
export const API_VERSION = 'v1' as const

export const API_VERSION_PREFIX = `/api/${API_VERSION}`

/**
 * Rewrites {@code /api/...} to {@code /api/v1/...}. Already-versioned and non-API paths are unchanged.
 */
export function withApiVersion(url: string | undefined): string | undefined {
  if (url == null || url === '') {
    return url
  }
  if (!url.startsWith('/api/') || url.startsWith('/api/v')) {
    return url
  }
  return url.replace(/^\/api\//, `${API_VERSION_PREFIX}/`)
}
