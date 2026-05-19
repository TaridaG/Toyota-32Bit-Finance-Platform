import type { SupportedLocale } from '../shared/i18n'

export type SupportedLanguage = SupportedLocale

export interface InfoCardAiContent {
  title?: string
  shortDescription?: string
  detailedDescription?: string
  howToInterpret?: string
  commonMistake?: string
  example?: string
  relatedTerms?: string[]
}

export interface CompleteInfoCardAiRequest {
  language: SupportedLanguage
  title: string
  shortDescription: string
  category?: string
  type?: string
  difficulty?: string
  fieldsToGenerate?: string[]
}

export interface TranslateInfoCardAiRequest {
  sourceLanguage: SupportedLanguage
  targetLanguage: SupportedLanguage
  sourceContent: {
    title: string
    shortDescription: string
    detailedDescription?: string
    howToInterpret?: string
    commonMistake?: string
    example?: string
    relatedTerms?: string[]
  }
}
