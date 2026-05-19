import { SUPPORTED_LOCALES, type SupportedLocale } from '../../../shared/i18n'
import { splitCsv, type InfoCardLocaleFormState } from './infoCardTranslations'

export function canCompleteInfoCardWithAi(form: InfoCardLocaleFormState): boolean {
  return Boolean(form.title.trim() && form.shortDescription.trim())
}

export function isLocaleAiSourceReady(form: InfoCardLocaleFormState): boolean {
  return Boolean(
    form.title.trim() &&
      form.shortDescription.trim() &&
      form.detailedDescription.trim() &&
      form.howToInterpret.trim() &&
      form.commonMistake.trim() &&
      form.example.trim() &&
      splitCsv(form.relatedTermsText).length > 0,
  )
}

export function resolveAiSourceLocale(
  localeForms: Record<SupportedLocale, InfoCardLocaleFormState>,
): SupportedLocale | null {
  if (isLocaleAiSourceReady(localeForms.tr)) {
    return 'tr'
  }
  for (const locale of SUPPORTED_LOCALES) {
    if (isLocaleAiSourceReady(localeForms[locale])) {
      return locale
    }
  }
  return null
}

export function localeFormToAiSourceContent(form: InfoCardLocaleFormState) {
  return {
    title: form.title.trim(),
    shortDescription: form.shortDescription.trim(),
    detailedDescription: form.detailedDescription.trim(),
    howToInterpret: form.howToInterpret.trim(),
    commonMistake: form.commonMistake.trim(),
    example: form.example.trim(),
    relatedTerms: splitCsv(form.relatedTermsText),
  }
}

export function resolveAiErrorMessage(
  error: unknown,
  disabledLabel: string,
  failedLabel: string,
  quotaLabel: string,
  timeoutLabel: string,
): string {
  if (error && typeof error === 'object') {
    const axiosLike = error as {
      code?: string
      message?: string
      response?: { status?: number; data?: { error?: { code?: string; message?: string } } }
    }
    const status = axiosLike.response?.status
    const apiError = axiosLike.response?.data?.error
    const apiCode = apiError?.code ?? axiosLike.code
    if (apiCode === 'OPENAI_QUOTA_EXCEEDED') {
      return quotaLabel
    }
    if (apiCode === 'AI_DISABLED' || apiCode === 'AI_CONFIGURATION' || (status === 503 && !apiCode)) {
      return disabledLabel
    }
    if (status === 504 || status === 408) {
      return timeoutLabel
    }
    if (status === 502 || apiCode === 'AI_OPENAI_ERROR') {
      return failedLabel
    }
  }
  return failedLabel
}
