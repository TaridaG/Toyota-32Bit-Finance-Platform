import type {
  LiteracyCategory,
  LiteracyContentType,
  LiteracyDifficulty,
  LiteracyEntry,
  LiteracyPortalPage,
  LiteracyPortalUsage,
} from '../types/financialLiteracy'
import { LITERACY_PORTAL_ROUTES } from '../types/financialLiteracy'

function slugify(title: string): string {
  return title
    .toLowerCase()
    .replace(/ğ/g, 'g')
    .replace(/ü/g, 'u')
    .replace(/ş/g, 's')
    .replace(/ı/g, 'i')
    .replace(/ö/g, 'o')
    .replace(/ç/g, 'c')
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-|-$/g, '')
}

function portal(
  page: LiteracyPortalPage,
  description: string,
): LiteracyPortalUsage {
  return { page, description, route: LITERACY_PORTAL_ROUTES[page] }
}

type EntryInput = {
  id: string
  title: string
  type?: LiteracyContentType
  category: LiteracyCategory
  difficulty: LiteracyDifficulty
  shortDefinition: string
  financialMeaning?: string
  usedInPortal: LiteracyPortalUsage[]
  howToInterpret: string
  commonMistake?: string
  example?: string
  relatedTerms?: string[]
  tags?: string[]
  adminOnly?: boolean
}

export function literacyEntry(input: EntryInput): LiteracyEntry {
  return {
    id: input.id,
    title: input.title,
    slug: slugify(input.title),
    type: input.type ?? 'TERM',
    category: input.category,
    difficulty: input.difficulty,
    shortDefinition: input.shortDefinition,
    financialMeaning:
      input.financialMeaning ??
      input.shortDefinition +
        ' Bu kavram, portalda veri okuma ve karar destek ekranlarında yatırım performansını yorumlarken kullanılır.',
    usedInPortal: input.usedInPortal,
    howToInterpret: input.howToInterpret,
    commonMistake: input.commonMistake,
    example: input.example,
    relatedTerms: input.relatedTerms ?? [],
    tags: input.tags ?? [],
    adminOnly: input.adminOnly,
  }
}

export { portal, slugify }
