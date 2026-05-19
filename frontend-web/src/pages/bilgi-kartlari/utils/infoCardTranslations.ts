import {
  DEFAULT_LOCALE,
  LANGUAGE_LABELS,
  SUPPORTED_LOCALES,
  type SupportedLocale,
} from '../../../shared/i18n'
import type { InfoCard, InfoCardInput, InfoCardLocaleContent, InfoCardTranslations } from '../../../types/infoCards'

export type InfoCardLocaleFormState = {
  title: string
  shortDescription: string
  detailedDescription: string
  howToInterpret: string
  commonMistake: string
  example: string
  relatedTermsText: string
}

export const EMPTY_LOCALE_FORM: InfoCardLocaleFormState = {
  title: '',
  shortDescription: '',
  detailedDescription: '',
  howToInterpret: '',
  commonMistake: '',
  example: '',
  relatedTermsText: '',
}

export function wizardLocaleOrder(primary: SupportedLocale): SupportedLocale[] {
  const rest = SUPPORTED_LOCALES.filter((locale) => locale !== primary)
  return [primary, ...rest]
}

export function localeLabel(locale: SupportedLocale): string {
  return LANGUAGE_LABELS[locale]
}

export function slugifyInfoCardTitle(title: string): string {
  const normalized = title
    .toLowerCase()
    .replace(/ğ/g, 'g')
    .replace(/ü/g, 'u')
    .replace(/ş/g, 's')
    .replace(/ı/g, 'i')
    .replace(/ö/g, 'o')
    .replace(/ç/g, 'c')
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-|-$/g, '')
  return normalized || 'card'
}

export function splitCsv(value: string): string[] {
  return value
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)
}

export function localeFormFromContent(content: InfoCardLocaleContent): InfoCardLocaleFormState {
  return {
    title: content.title,
    shortDescription: content.shortDescription,
    detailedDescription: content.detailedDescription,
    howToInterpret: content.howToInterpret ?? '',
    commonMistake: content.commonMistake ?? '',
    example: content.example ?? '',
    relatedTermsText: content.relatedTerms.join(', '),
  }
}

export function contentFromLocaleForm(form: InfoCardLocaleFormState): InfoCardLocaleContent {
  return {
    title: form.title.trim(),
    shortDescription: form.shortDescription.trim(),
    detailedDescription: form.detailedDescription.trim(),
    howToInterpret: form.howToInterpret.trim() || undefined,
    commonMistake: form.commonMistake.trim() || undefined,
    example: form.example.trim() || undefined,
    relatedTerms: splitCsv(form.relatedTermsText),
  }
}

export function isLocaleFormComplete(form: InfoCardLocaleFormState): boolean {
  return Boolean(form.title.trim() && form.shortDescription.trim())
}

export function cardToLocaleForms(card: InfoCard): Record<SupportedLocale, InfoCardLocaleFormState> {
  const fallback: InfoCardLocaleContent = {
    title: card.title,
    shortDescription: card.shortDescription,
    detailedDescription: card.detailedDescription,
    howToInterpret: card.howToInterpret,
    commonMistake: card.commonMistake,
    example: card.example,
    relatedTerms: card.relatedTerms ?? [],
  }
  const forms = {} as Record<SupportedLocale, InfoCardLocaleFormState>
  for (const locale of SUPPORTED_LOCALES) {
    const content = card.translations?.[locale] ?? fallback
    forms[locale] = localeFormFromContent(content)
  }
  return forms
}

export function buildTranslationsPayload(
  localeForms: Record<SupportedLocale, InfoCardLocaleFormState>,
): InfoCardTranslations {
  const payload = {} as InfoCardTranslations
  for (const locale of SUPPORTED_LOCALES) {
    payload[locale] = contentFromLocaleForm(localeForms[locale])
  }
  return payload
}

export function buildInfoCardInput(
  meta: Omit<
    InfoCardInput,
    | 'title'
    | 'shortDescription'
    | 'detailedDescription'
    | 'howToInterpret'
    | 'commonMistake'
    | 'example'
    | 'relatedTerms'
    | 'translations'
  >,
  localeForms: Record<SupportedLocale, InfoCardLocaleFormState>,
  primaryLocale: SupportedLocale = DEFAULT_LOCALE,
): InfoCardInput {
  const translations = buildTranslationsPayload(localeForms)
  const primary = translations[primaryLocale] ?? translations.tr!
  return {
    ...meta,
    title: primary.title,
    shortDescription: primary.shortDescription,
    detailedDescription: primary.detailedDescription,
    howToInterpret: primary.howToInterpret,
    commonMistake: primary.commonMistake,
    example: primary.example,
    relatedTerms: primary.relatedTerms,
    translations,
  }
}

export function resolveInfoCardForLocale(card: InfoCard, locale: SupportedLocale): InfoCard {
  const content = card.translations?.[locale]
  if (!content) {
    return card
  }
  return {
    ...card,
    title: content.title,
    shortDescription: content.shortDescription,
    detailedDescription: content.detailedDescription,
    howToInterpret: content.howToInterpret,
    commonMistake: content.commonMistake,
    example: content.example,
    relatedTerms: content.relatedTerms,
  }
}
