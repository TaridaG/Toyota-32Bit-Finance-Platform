import { PORTAL_PAGE_ELEMENTS } from '../../data/portalPageElements'
import type { PortalPageKey } from '../../types/infoCards'

function normalize(value: string): string {
  return value.trim().toLowerCase()
}

/** Map a live-page pick to catalog element ids when possible. */
export function resolveElementIdsForPick(
  pageKey: PortalPageKey,
  label: string,
  term: string,
  domElementId?: string,
): string[] {
  if (domElementId?.trim()) {
    return [domElementId.trim()]
  }
  const normalizedLabel = normalize(label)
  const normalizedTerm = normalize(term)
  const match = PORTAL_PAGE_ELEMENTS.find(
    (element) =>
      element.pageKey === pageKey &&
      (normalize(element.label) === normalizedLabel ||
        normalize(element.term) === normalizedTerm ||
        normalize(element.label) === normalizedTerm),
  )
  return match ? [match.id] : []
}
