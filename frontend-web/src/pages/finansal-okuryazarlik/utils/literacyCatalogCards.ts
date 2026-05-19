import type { InfoCard } from '../../../types/infoCards'

/** Cards listed on Finansal Okuryazarlık (not tied to a portal button pick). */
export function isLiteracyCatalogCard(card: InfoCard): boolean {
  return card.pages.includes('FINANCIAL_LITERACY')
}
