import type { SupportedLocale } from '../i18n'

export const SUPPORTED_CURRENCIES = ['USD', 'EUR', 'TRY', 'GBP', 'JPY', 'AED'] as const
export type SupportedCurrency = (typeof SUPPORTED_CURRENCIES)[number]

export type AppPreferencesContextValue = {
  language: SupportedLocale
  currency: SupportedCurrency
  setLanguage: (language: SupportedLocale) => Promise<void>
  setCurrency: (currency: SupportedCurrency) => void
}
