import type { LiteracyEntry, LiteracyPortalPage } from '../types/financialLiteracy'
import type { InfoCard } from '../../../types/infoCards'

export function infoCardToLiteracyEntry(card: InfoCard): LiteracyEntry {
  return {
    id: card.id,
    title: card.title,
    slug: card.slug,
    type: card.type,
    category: card.category,
    difficulty: card.difficulty,
    shortDefinition: card.shortDescription,
    financialMeaning: card.detailedDescription,
    usedInPortal: card.pages.map((page) => ({
      page: page as LiteracyPortalPage,
      description: '',
    })),
    howToInterpret: card.howToInterpret ?? '',
    commonMistake: card.commonMistake,
    example: card.example,
    relatedTerms: card.relatedTerms,
    tags: [...card.targetTerms, ...(card.targetInstrumentSymbols ?? [])],
    adminOnly: card.adminOnly,
  }
}
