import {
  createAdminInfoCard,
  deleteAdminInfoCard,
  fetchInfoCardBySlug,
  fetchPortalInfoCards,
  lookupPortalInfoCard,
  toggleAdminInfoCardStatus,
  updateAdminInfoCard,
} from '../features/info-cards/api/infoCardsHttpApi'
import type { InfoCard, InfoCardInput, InfoCardsDashboard, PortalPageKey } from '../types/infoCards'
import { computeDashboard, getInfoCardsSnapshot } from './infoCardsStore'

let portalCache: InfoCard[] = []
let portalLoaded = false

function setPortalCache(cards: InfoCard[]) {
  portalCache = cards
  portalLoaded = true
}

/** API facade — portal reads + admin mutations via finance-api. */
export const infoCardsApi = {
  getPortalCache(): InfoCard[] {
    return portalCache
  },

  isPortalLoaded(): boolean {
    return portalLoaded
  },

  async loadPortalCards(includeAdminOnly = false): Promise<InfoCard[]> {
    const cards = await fetchPortalInfoCards(undefined, includeAdminOnly)
    setPortalCache(cards)
    return cards
  },

  getAll(): InfoCard[] {
    return portalLoaded ? portalCache : getInfoCardsSnapshot()
  },

  getByPage(pageKey: PortalPageKey): InfoCard[] {
    return infoCardsApi.getAll().filter((c) => c.pages.includes(pageKey))
  },

  async getBySlug(slug: string): Promise<InfoCard | undefined> {
    const local = infoCardsApi.getAll().find((c) => c.slug === slug)
    if (local) {
      return local
    }
    try {
      return await fetchInfoCardBySlug(slug)
    } catch {
      return undefined
    }
  },

  getById(id: string): InfoCard | undefined {
    return infoCardsApi.getAll().find((c) => c.id === id)
  },

  findForTerm(term: string, pageKey: PortalPageKey): InfoCard | undefined {
    return infoCardsApi.findForHelpTarget(pageKey, { term })
  },

  findForElement(elementId: string, pageKey: PortalPageKey): InfoCard | undefined {
    return infoCardsApi.findForHelpTarget(pageKey, { elementId })
  },

  findForHelpTarget(
    pageKey: PortalPageKey,
    options: { term?: string; elementId?: string; instrumentSymbol?: string },
  ): InfoCard | undefined {
    const normalizedTerm = options.term?.trim().toLowerCase() ?? ''
    const normalizedInstrument = options.instrumentSymbol?.trim().toUpperCase() ?? ''
    return infoCardsApi.getAll().find(
      (c) =>
        c.status === 'ACTIVE' &&
        c.pages.includes(pageKey) &&
        ((options.elementId && c.targetElementIds?.includes(options.elementId)) ||
          (normalizedTerm &&
            c.targetTerms.some((t) => t.trim().toLowerCase() === normalizedTerm)) ||
          (normalizedInstrument &&
            (c.targetInstrumentSymbols ?? []).some(
              (s) => s.trim().toUpperCase() === normalizedInstrument,
            ))),
    )
  },

  async lookupHelpTargetRemote(
    pageKey: PortalPageKey,
    options: { term?: string; elementId?: string; instrumentSymbol?: string },
  ): Promise<InfoCard | undefined> {
    const local = infoCardsApi.findForHelpTarget(pageKey, options)
    if (local) {
      return local
    }
    try {
      return await lookupPortalInfoCard(pageKey, options)
    } catch {
      return undefined
    }
  },

  getDashboard(): InfoCardsDashboard {
    return computeDashboard(infoCardsApi.getAll())
  },

  async create(input: InfoCardInput): Promise<InfoCard> {
    const card = await createAdminInfoCard(input)
    await infoCardsApi.loadPortalCards(true)
    return card
  },

  async update(id: string, patch: Partial<InfoCardInput>): Promise<InfoCard> {
    const current = infoCardsApi.getAll().find((c) => c.id === id)
    const merged: InfoCardInput = {
      title: patch.title ?? current?.title ?? '',
      targetTerms: patch.targetTerms ?? current?.targetTerms ?? [],
      targetElementIds: patch.targetElementIds ?? current?.targetElementIds ?? [],
      targetInstrumentSymbols:
        patch.targetInstrumentSymbols ?? current?.targetInstrumentSymbols ?? [],
      pages: patch.pages ?? current?.pages ?? ['MARKETS'],
      category: patch.category ?? current?.category ?? 'MARKET_DATA',
      type: patch.type ?? current?.type ?? 'TERM',
      difficulty: patch.difficulty ?? current?.difficulty ?? 'BEGINNER',
      status: patch.status ?? current?.status ?? 'ACTIVE',
      shortDescription: patch.shortDescription ?? current?.shortDescription ?? '',
      detailedDescription: patch.detailedDescription ?? current?.detailedDescription ?? '',
      howToInterpret: patch.howToInterpret ?? current?.howToInterpret,
      commonMistake: patch.commonMistake ?? current?.commonMistake,
      example: patch.example ?? current?.example,
      relatedTerms: patch.relatedTerms ?? current?.relatedTerms ?? [],
      adminOnly: patch.adminOnly ?? current?.adminOnly,
    }
    const card = await updateAdminInfoCard(id, merged)
    await infoCardsApi.loadPortalCards(true)
    return card
  },

  async delete(id: string): Promise<boolean> {
    await deleteAdminInfoCard(id)
    await infoCardsApi.loadPortalCards(true)
    return true
  },

  async toggleStatus(id: string): Promise<InfoCard | null> {
    const card = await toggleAdminInfoCardStatus(id)
    await infoCardsApi.loadPortalCards(true)
    return card
  },
}
