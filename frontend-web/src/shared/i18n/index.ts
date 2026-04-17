import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'
import { apiClient } from '../api/client'
import { fetchLocaleFromBackend } from './localeApi'
import { translations, type TranslationSchema } from './translations'

export const SUPPORTED_LOCALES = ['en', 'tr', 'de', 'fr'] as const
export type SupportedLocale = (typeof SUPPORTED_LOCALES)[number]

export const LANGUAGE_LABELS: Record<SupportedLocale, string> = {
  en: 'English',
  tr: 'Türkçe',
  de: 'Deutsch',
  fr: 'Français',
}

const LOCALE_STORAGE_KEY = 'finance.locale'
const DEFAULT_LOCALE: SupportedLocale = 'en'

function isSupportedLocale(value: string): value is SupportedLocale {
  return (SUPPORTED_LOCALES as readonly string[]).includes(value)
}

export function normalizeLocale(value: string | null | undefined): SupportedLocale | null {
  if (!value) {
    return null
  }

  const normalized = value.toLowerCase().replace('_', '-')
  const [language] = normalized.split('-')

  if (isSupportedLocale(normalized)) {
    return normalized
  }

  if (isSupportedLocale(language)) {
    return language
  }

  return null
}

function getStoredLocale(): SupportedLocale | null {
  if (typeof window === 'undefined') {
    return null
  }

  return normalizeLocale(window.localStorage.getItem(LOCALE_STORAGE_KEY))
}

function setApiLocaleHeader(locale: SupportedLocale) {
  apiClient.defaults.headers.common['Accept-Language'] = locale
}

export async function setAppLocale(
  locale: string,
  options?: { persist?: boolean },
): Promise<SupportedLocale> {
  const normalizedLocale = normalizeLocale(locale) ?? DEFAULT_LOCALE

  await i18n.changeLanguage(normalizedLocale)
  setApiLocaleHeader(normalizedLocale)

  if (options?.persist !== false && typeof window !== 'undefined') {
    window.localStorage.setItem(LOCALE_STORAGE_KEY, normalizedLocale)
  }

  return normalizedLocale
}

export async function syncLocaleFromBackend() {
  const backendLocale = await fetchLocaleFromBackend()
  if (!backendLocale) {
    return
  }

  await setAppLocale(backendLocale)
}

const initialLocale = getStoredLocale() ?? DEFAULT_LOCALE
setApiLocaleHeader(initialLocale)

const resources = Object.fromEntries(
  SUPPORTED_LOCALES.map((locale) => [locale, { translation: translations[locale] }]),
) as Record<SupportedLocale, { translation: TranslationSchema }>

void i18n.use(initReactI18next).init({
  resources,
  lng: initialLocale,
  fallbackLng: DEFAULT_LOCALE,
  interpolation: {
    escapeValue: false,
  },
})

export default i18n

