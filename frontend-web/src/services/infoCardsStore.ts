import { buildDefaultInfoCards } from '../data/literacyToInfoCards'
import type { InfoCard, InfoCardInput, InfoCardsDashboard, PortalPageKey } from '../types/infoCards'

const STORAGE_KEY = 'finance.infoCards.v1'

type Listener = () => void

let cache: InfoCard[] | null = null
const listeners = new Set<Listener>()

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

function notify() {
  listeners.forEach((l) => l())
}

function loadFromStorage(): InfoCard[] {
  if (typeof window === 'undefined') {
    return buildDefaultInfoCards()
  }
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY)
    if (!raw) {
      return buildDefaultInfoCards()
    }
    const parsed = JSON.parse(raw) as InfoCard[]
    if (!Array.isArray(parsed) || parsed.length === 0) {
      return buildDefaultInfoCards()
    }
    return parsed
  } catch {
    return buildDefaultInfoCards()
  }
}

function persist(cards: InfoCard[]) {
  cache = cards
  if (typeof window !== 'undefined') {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(cards))
  }
  notify()
}

export function getInfoCardsSnapshot(): InfoCard[] {
  if (!cache) {
    cache = loadFromStorage()
  }
  return cache
}

export function subscribeInfoCards(listener: Listener): () => void {
  listeners.add(listener)
  return () => listeners.delete(listener)
}

export function resetInfoCardsToDefaults(): InfoCard[] {
  const cards = buildDefaultInfoCards()
  persist(cards)
  return cards
}

export function seedInfoCardsIfEmpty(): InfoCard[] {
  const current = getInfoCardsSnapshot()
  if (current.length === 0) {
    return resetInfoCardsToDefaults()
  }
  return current
}

function wordCount(text: string): number {
  return text.trim().split(/\s+/).filter(Boolean).length
}

export function computeDashboard(cards: InfoCard[]): InfoCardsDashboard {
  const active = cards.filter((c) => c.status === 'ACTIVE')
  const passive = cards.filter((c) => c.status === 'PASSIVE')
  const pageCounts = new Map<PortalPageKey, number>()
  for (const card of cards) {
    for (const page of card.pages) {
      pageCounts.set(page, (pageCounts.get(page) ?? 0) + 1)
    }
  }
  let mostCoveredPage: PortalPageKey | null = null
  let maxCount = 0
  for (const [page, count] of pageCounts) {
    if (count > maxCount) {
      maxCount = count
      mostCoveredPage = page
    }
  }
  const sortedByUpdate = [...cards].sort(
    (a, b) => Date.parse(b.updatedAt) - Date.parse(a.updatedAt),
  )
  const avgWords =
    cards.length === 0
      ? 0
      : Math.round(
          cards.reduce((sum, c) => sum + wordCount(c.shortDescription), 0) / cards.length,
        )

  return {
    activeCards: active.length,
    passiveCards: passive.length,
    averageWordCount: avgWords,
    coveredPages: pageCounts.size,
    mostCoveredPage,
    beginnerCount: cards.filter((c) => c.difficulty === 'BEGINNER').length,
    intermediateCount: cards.filter((c) => c.difficulty === 'INTERMEDIATE').length,
    advancedCount: cards.filter((c) => c.difficulty === 'ADVANCED').length,
    lastUpdatedCardTitle: sortedByUpdate[0]?.title ?? null,
  }
}

export function findInfoCardBySlug(slug: string): InfoCard | undefined {
  return getInfoCardsSnapshot().find((c) => c.slug === slug)
}

export function findInfoCardById(id: string): InfoCard | undefined {
  return getInfoCardsSnapshot().find((c) => c.id === id)
}

export function findInfoCardForElement(elementId: string, pageKey: PortalPageKey): InfoCard | undefined {
  return getInfoCardsSnapshot().find(
    (c) =>
      c.status === 'ACTIVE' &&
      c.pages.includes(pageKey) &&
      c.targetElementIds?.includes(elementId),
  )
}

export function findInfoCardForTerm(term: string, pageKey: PortalPageKey): InfoCard | undefined {
  const normalized = term.trim().toLowerCase()
  return getInfoCardsSnapshot().find(
    (c) =>
      c.status === 'ACTIVE' &&
      c.pages.includes(pageKey) &&
      c.targetTerms.some((t) => t.trim().toLowerCase() === normalized),
  )
}

export function findInfoCardForHelpTarget(
  pageKey: PortalPageKey,
  options: { term?: string; elementId?: string },
): InfoCard | undefined {
  if (options.elementId) {
    const byElement = findInfoCardForElement(options.elementId, pageKey)
    if (byElement) {
      return byElement
    }
  }
  if (options.term) {
    return findInfoCardForTerm(options.term, pageKey)
  }
  return undefined
}

export function getInfoCardsByPage(pageKey: PortalPageKey): InfoCard[] {
  return getInfoCardsSnapshot().filter((c) => c.pages.includes(pageKey))
}

export function createInfoCard(input: InfoCardInput): InfoCard {
  const now = new Date().toISOString()
  const id = input.id ?? `card-${Date.now()}`
  const card: InfoCard = {
    ...input,
    id,
    slug: input.slug ?? slugify(input.title),
    createdAt: now,
    updatedAt: now,
  }
  persist([...getInfoCardsSnapshot(), card])
  return card
}

export function updateInfoCard(id: string, patch: Partial<InfoCardInput>): InfoCard | null {
  const cards = getInfoCardsSnapshot()
  const idx = cards.findIndex((c) => c.id === id)
  if (idx < 0) {
    return null
  }
  const prev = cards[idx]
  const updated: InfoCard = {
    ...prev,
    ...patch,
    id: prev.id,
    slug: patch.slug ?? (patch.title ? slugify(patch.title) : prev.slug),
    updatedAt: new Date().toISOString(),
  }
  const next = [...cards]
  next[idx] = updated
  persist(next)
  return updated
}

export function deleteInfoCard(id: string): boolean {
  const next = getInfoCardsSnapshot().filter((c) => c.id !== id)
  if (next.length === getInfoCardsSnapshot().length) {
    return false
  }
  persist(next)
  return true
}

export function toggleInfoCardStatus(id: string): InfoCard | null {
  const card = findInfoCardById(id)
  if (!card) {
    return null
  }
  return updateInfoCard(id, { status: card.status === 'ACTIVE' ? 'PASSIVE' : 'ACTIVE' })
}
