import type {
  LiteracyCategory,
  LiteracyContentType,
  LiteracyDifficulty,
  LiteracyEntry,
  LiteracyPortalPage,
} from '../types/financialLiteracy'

export type LiteracyFilters = {
  query: string
  category: LiteracyCategory | 'ALL'
  difficulties: LiteracyDifficulty[]
  contentTypes: LiteracyContentType[]
  portalPages: LiteracyPortalPage[]
  showAdminTerms: boolean
}

export function filterLiteracyEntries(
  entries: LiteracyEntry[],
  filters: LiteracyFilters,
): LiteracyEntry[] {
  const q = filters.query.trim().toLowerCase()

  return entries.filter((entry) => {
    if (entry.adminOnly && !filters.showAdminTerms) {
      return false
    }

    if (filters.category !== 'ALL' && entry.category !== filters.category) {
      return false
    }

    if (filters.difficulties.length > 0 && !filters.difficulties.includes(entry.difficulty)) {
      return false
    }

    if (filters.contentTypes.length > 0 && !filters.contentTypes.includes(entry.type)) {
      return false
    }

    if (filters.portalPages.length > 0) {
      const pages = entry.usedInPortal.map((u) => u.page)
      if (!filters.portalPages.some((p) => pages.includes(p))) {
        return false
      }
    }

    if (!q) {
      return true
    }

    const haystack = [
      entry.title,
      entry.shortDefinition,
      entry.financialMeaning,
      entry.howToInterpret,
      entry.commonMistake ?? '',
      entry.example ?? '',
      ...entry.tags,
      ...entry.relatedTerms,
    ]
      .join(' ')
      .toLowerCase()

    return haystack.includes(q)
  })
}

export function countByType(entries: LiteracyEntry[]) {
  return {
    terms: entries.filter((e) => e.type === 'TERM').length,
    charts: entries.filter((e) => e.type === 'CHART').length,
    analysisTools: entries.filter((e) => e.type === 'ANALYSIS_TOOL').length,
    macro: entries.filter((e) => e.type === 'MACRO_INDICATOR').length,
    system: entries.filter((e) => e.type === 'SYSTEM_TERM').length,
    total: entries.length,
  }
}

export function countByCategory(entries: LiteracyEntry[], category: LiteracyCategory) {
  return entries.filter((e) => e.category === category).length
}
