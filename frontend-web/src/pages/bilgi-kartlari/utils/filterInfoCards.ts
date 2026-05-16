import type {
  InfoCard,
  InfoCardDifficulty,
  InfoCardStatus,
  InfoCardType,
  PortalPageKey,
} from '../../../types/infoCards'

export type InfoCardFilters = {
  query: string
  pageKey: PortalPageKey | 'ALL'
  status: InfoCardStatus | 'ALL'
  difficulties: InfoCardDifficulty[]
  types: InfoCardType[]
}

export function filterInfoCards(cards: InfoCard[], filters: InfoCardFilters): InfoCard[] {
  const q = filters.query.trim().toLowerCase()

  return cards.filter((card) => {
    if (filters.status !== 'ALL' && card.status !== filters.status) {
      return false
    }
    if (filters.pageKey !== 'ALL' && !card.pages.includes(filters.pageKey)) {
      return false
    }
    if (filters.difficulties.length > 0 && !filters.difficulties.includes(card.difficulty)) {
      return false
    }
    if (filters.types.length > 0 && !filters.types.includes(card.type)) {
      return false
    }
    if (!q) {
      return true
    }
    const haystack = [
      card.title,
      card.shortDescription,
      card.detailedDescription,
      ...card.targetTerms,
      ...(card.targetInstrumentSymbols ?? []),
      ...card.relatedTerms,
    ]
      .join(' ')
      .toLowerCase()
    return haystack.includes(q)
  })
}

