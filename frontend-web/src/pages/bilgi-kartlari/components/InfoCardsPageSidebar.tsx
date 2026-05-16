import { useTranslation } from 'react-i18next'
import { USER_PORTAL_PAGES, countCardsForPortalPage } from '../../../data/portalPages'
import type { InfoCard, PortalPageKey } from '../../../types/infoCards'

type InfoCardsPageSidebarProps = {
  cards: InfoCard[]
  selectedPage: PortalPageKey
  onSelect: (page: PortalPageKey) => void
}

export function InfoCardsPageSidebar({ cards, selectedPage, onSelect }: InfoCardsPageSidebarProps) {
  const { t } = useTranslation('common')

  return (
    <aside className="ic-sidebar card">
      <section className="ic-sidebar-group">
        <h4>{t('bilgiKartlariPage.sidebar.pages')}</h4>
        <ul>
          {USER_PORTAL_PAGES.map((page) => (
            <li key={page.key}>
              <button
                type="button"
                className={`ic-sidebar-item${selectedPage === page.key ? ' ic-sidebar-item-active' : ''}`}
                onClick={() => onSelect(page.key)}
              >
                <span>{t(page.labelKey)}</span>
                <span className="ic-sidebar-count">{countCardsForPortalPage(cards, page.key)}</span>
              </button>
            </li>
          ))}
        </ul>
      </section>
    </aside>
  )
}
