import { apiClient } from '../api/client'

function parseLocalePayload(payload: unknown): string | null {
  if (!payload) {
    return null
  }

  if (typeof payload === 'string') {
    return payload
  }

  if (typeof payload !== 'object') {
    return null
  }

  const record = payload as Record<string, unknown>

  const direct = [record.language, record.locale, record.preferredLanguage]
  for (const candidate of direct) {
    if (typeof candidate === 'string' && candidate.trim()) {
      return candidate
    }
  }

  const preferences = record.preferences
  if (preferences && typeof preferences === 'object') {
    const preferencesRecord = preferences as Record<string, unknown>
    const nested = [preferencesRecord.language, preferencesRecord.locale]
    for (const candidate of nested) {
      if (typeof candidate === 'string' && candidate.trim()) {
        return candidate
      }
    }
  }

  return null
}

const localeEndpoints = [
  '/users/me/preferences',
  '/users/me',
  '/auth/me',
] as const

export async function fetchLocaleFromBackend(): Promise<string | null> {
  for (const endpoint of localeEndpoints) {
    try {
      const response = await apiClient.get(endpoint)
      const locale = parseLocalePayload(response.data)
      if (locale) {
        return locale
      }
    } catch {
      continue
    }
  }

  return null
}

