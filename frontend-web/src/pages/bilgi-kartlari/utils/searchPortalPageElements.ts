import type { PortalPageElement } from '../../../data/portalPageElements'
import { PORTAL_PAGE_ELEMENTS } from '../../../data/portalPageElements'
import type { PortalPageKey } from '../../../types/infoCards'

export type PortalElementSearchGroup = {
  matchKey: string
  label: string
  term: string
  kind: PortalPageElement['kind']
  section?: string
  elements: PortalPageElement[]
  /** Present on all currently selected pages */
  sharedAcrossSelectedPages: boolean
}

function normalize(value: string): string {
  return value.trim().toLowerCase()
}

export function searchPortalPageElementGroups(
  query: string,
  selectedPages: PortalPageKey[],
): PortalElementSearchGroup[] {
  if (selectedPages.length === 0) {
    return []
  }

  const q = normalize(query)
  const pageSet = new Set(selectedPages)
  const filtered = PORTAL_PAGE_ELEMENTS.filter((e) => pageSet.has(e.pageKey))

  const byMatchKey = new Map<string, PortalPageElement[]>()
  for (const element of filtered) {
    if (q) {
      const haystack = [element.label, element.term, element.section ?? '', element.kind].join(' ')
      if (!normalize(haystack).includes(q)) {
        continue
      }
    }
    const list = byMatchKey.get(element.matchKey) ?? []
    list.push(element)
    byMatchKey.set(element.matchKey, list)
  }

  const groups: PortalElementSearchGroup[] = []
  for (const [matchKey, elements] of byMatchKey) {
    const sorted = [...elements].sort((a, b) => a.pageKey.localeCompare(b.pageKey))
    const pagesInGroup = new Set(sorted.map((e) => e.pageKey))
    const sharedAcrossSelectedPages = selectedPages.every((p) => pagesInGroup.has(p))
    const primary = sorted[0]
    groups.push({
      matchKey,
      label: primary.label,
      term: primary.term,
      kind: primary.kind,
      section: primary.section,
      elements: sorted,
      sharedAcrossSelectedPages,
    })
  }

  return groups.sort((a, b) => {
    if (a.sharedAcrossSelectedPages !== b.sharedAcrossSelectedPages) {
      return a.sharedAcrossSelectedPages ? -1 : 1
    }
    return a.label.localeCompare(b.label, 'tr')
  })
}

export function elementIdsFromGroups(groups: PortalElementSearchGroup[], matchKeys: string[]): string[] {
  const keys = new Set(matchKeys)
  const ids: string[] = []
  for (const group of groups) {
    if (!keys.has(group.matchKey)) {
      continue
    }
    for (const element of group.elements) {
      ids.push(element.id)
    }
  }
  return [...new Set(ids)]
}

export function matchKeysFromElementIds(ids: string[]): string[] {
  const keys = new Set<string>()
  for (const id of ids) {
    const element = PORTAL_PAGE_ELEMENTS.find((e) => e.id === id)
    if (element) {
      keys.add(element.matchKey)
    }
  }
  return [...keys]
}

export function pruneElementIdsForPages(ids: string[], pages: PortalPageKey[]): string[] {
  const pageSet = new Set(pages)
  return ids.filter((id) => {
    const element = PORTAL_PAGE_ELEMENTS.find((e) => e.id === id)
    return element && pageSet.has(element.pageKey)
  })
}

export function termsFromElementIds(ids: string[]): string[] {
  const terms = new Set<string>()
  for (const id of ids) {
    const element = PORTAL_PAGE_ELEMENTS.find((e) => e.id === id)
    if (element?.term.trim()) {
      terms.add(element.term.trim())
    }
  }
  return [...terms]
}
