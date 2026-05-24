import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'
import { apiClient } from '../api/client'
import { fetchLocaleFromBackend } from './localeApi'
import en from './locales/en.json'
import tr from './locales/tr.json'
import de from './locales/de.json'

export const SUPPORTED_LOCALES = ['en', 'tr', 'de'] as const
export type SupportedLocale = (typeof SUPPORTED_LOCALES)[number]

export const LANGUAGE_LABELS: Record<SupportedLocale, string> = {
  en: 'English',
  tr: 'Türkçe',
  de: 'Deutsch',
}

const LOCALE_STORAGE_KEY = 'finance.locale'
export const DEFAULT_LOCALE: SupportedLocale = 'en'

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
  apiClient.defaults.headers.common['X-Language'] = locale
}

export async function setAppLocale(
  locale: string,
  options?: { persist?: boolean },
): Promise<SupportedLocale> {
  const normalizedLocale = normalizeLocale(locale) ?? DEFAULT_LOCALE

  if (options?.persist !== false && typeof window !== 'undefined') {
    window.localStorage.setItem(LOCALE_STORAGE_KEY, normalizedLocale)
  }
  setApiLocaleHeader(normalizedLocale)

  await i18n.changeLanguage(normalizedLocale)

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

const resources: Record<
  SupportedLocale,
  {
    common: object
    landing: object
    auth: object
    markets: object
    newsPage: object
    notificationsPage: object
    alarmsPage: object
    myNewsPage: object
    myAnalysisPage: object
    analysis: object
    portfolio: object
    admin: object
  }
> = {
  en: {
    common: (en as { common?: object }).common ?? {},
    landing: (en as { landing?: object }).landing ?? {},
    auth: (en as { auth?: object }).auth ?? {},
    markets: (en as { markets?: object }).markets ?? {},
    newsPage: (en as { newsPage?: object }).newsPage ?? {},
    notificationsPage: (en as { notificationsPage?: object }).notificationsPage ?? {},
    alarmsPage: (en as { alarmsPage?: object }).alarmsPage ?? {},
    myNewsPage: (en as { myNewsPage?: object }).myNewsPage ?? {},
    myAnalysisPage: (en as { myAnalysisPage?: object }).myAnalysisPage ?? {},
    analysis: (en as { analysis?: object }).analysis ?? {},
    portfolio: (en as { portfolio?: object }).portfolio ?? {},
    admin: (en as { admin?: object }).admin ?? {},
  },
  tr: {
    common: (tr as { common?: object }).common ?? {},
    landing: (tr as { landing?: object }).landing ?? {},
    auth: (tr as { auth?: object }).auth ?? {},
    markets: (tr as { markets?: object }).markets ?? {},
    newsPage: (tr as { newsPage?: object }).newsPage ?? {},
    notificationsPage: (tr as { notificationsPage?: object }).notificationsPage ?? {},
    alarmsPage: (tr as { alarmsPage?: object }).alarmsPage ?? {},
    myNewsPage: (tr as { myNewsPage?: object }).myNewsPage ?? {},
    myAnalysisPage: (tr as { myAnalysisPage?: object }).myAnalysisPage ?? {},
    analysis: (tr as { analysis?: object }).analysis ?? {},
    portfolio: (tr as { portfolio?: object }).portfolio ?? {},
    admin: (tr as { admin?: object }).admin ?? {},
  },
  de: {
    common: (de as { common?: object }).common ?? {},
    landing: (de as { landing?: object }).landing ?? {},
    auth: (de as { auth?: object }).auth ?? {},
    markets: (de as { markets?: object }).markets ?? {},
    newsPage: (de as { newsPage?: object }).newsPage ?? {},
    notificationsPage: (de as { notificationsPage?: object }).notificationsPage ?? {},
    alarmsPage: (de as { alarmsPage?: object }).alarmsPage ?? {},
    myNewsPage: (de as { myNewsPage?: object }).myNewsPage ?? {},
    myAnalysisPage: (de as { myAnalysisPage?: object }).myAnalysisPage ?? {},
    analysis: (de as { analysis?: object }).analysis ?? {},
    portfolio: (de as { portfolio?: object }).portfolio ?? {},
    admin: (de as { admin?: object }).admin ?? {},
  },
}

void i18n.use(initReactI18next).init({
  resources,
  lng: initialLocale,
  defaultNS: 'common',
  ns: ['common', 'landing', 'auth', 'markets', 'newsPage', 'notificationsPage', 'alarmsPage', 'myNewsPage', 'myAnalysisPage', 'analysis', 'portfolio', 'admin'],
  fallbackNS: 'common',
  fallbackLng: DEFAULT_LOCALE,
  interpolation: {
    escapeValue: false,
  },
})

export default i18n

