import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import i18n, { DEFAULT_LOCALE, normalizeLocale, setAppLocale, type SupportedLocale } from '../i18n'
import { apiClient } from '../api/client'
import { SUPPORTED_CURRENCIES, type AppPreferencesContextValue, type SupportedCurrency } from './preferences'
import { AppPreferencesContext } from './AppPreferencesStore'
const CURRENCY_STORAGE_KEY = 'finance.currency'
const DEFAULT_CURRENCY: SupportedCurrency = 'USD'

function getStoredCurrency(): SupportedCurrency {
  if (typeof window === 'undefined') {
    return DEFAULT_CURRENCY
  }
  const value = window.localStorage.getItem(CURRENCY_STORAGE_KEY)
  if (value && (SUPPORTED_CURRENCIES as readonly string[]).includes(value)) {
    return value as SupportedCurrency
  }
  return DEFAULT_CURRENCY
}

function writeHeaderDefaults(language: SupportedLocale, currency: SupportedCurrency) {
  apiClient.defaults.headers.common['X-Language'] = language
  apiClient.defaults.headers.common['X-Currency'] = currency
}

export function AppPreferencesProvider({ children }: { children: ReactNode }) {
  const [language, setLanguageState] = useState<SupportedLocale>(
    normalizeLocale(i18n.resolvedLanguage ?? i18n.language) ?? DEFAULT_LOCALE,
  )
  const [currency, setCurrencyState] = useState<SupportedCurrency>(getStoredCurrency)

  useEffect(() => {
    writeHeaderDefaults(language, currency)
  }, [language, currency])

  useEffect(() => {
    const onLanguageChanged = (lng: string) => {
      const normalized = normalizeLocale(lng) ?? DEFAULT_LOCALE
      setLanguageState(normalized)
    }
    i18n.on('languageChanged', onLanguageChanged)
    return () => {
      i18n.off('languageChanged', onLanguageChanged)
    }
  }, [])

  const setLanguage = useCallback(async (nextLanguage: SupportedLocale) => {
    await setAppLocale(nextLanguage)
    setLanguageState(nextLanguage)
  }, [])

  const setCurrency = useCallback((nextCurrency: SupportedCurrency) => {
    setCurrencyState((current) => (current === nextCurrency ? current : nextCurrency))
    if (typeof window !== 'undefined') {
      window.localStorage.setItem(CURRENCY_STORAGE_KEY, nextCurrency)
    }
  }, [])

  const value = useMemo<AppPreferencesContextValue>(
    () => ({
      language,
      currency,
      setLanguage,
      setCurrency,
    }),
    [currency, language, setCurrency, setLanguage],
  )

  return <AppPreferencesContext.Provider value={value}>{children}</AppPreferencesContext.Provider>
}

