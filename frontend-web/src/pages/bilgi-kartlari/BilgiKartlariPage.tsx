import { useCallback, useEffect, useState } from 'react'
import { createPortal } from 'react-dom'
import { useTranslation } from 'react-i18next'
import { useDocumentTitle } from '../../shared/hooks/useDocumentTitle'
import { useInfoCards } from '../../features/info-cards/InfoCardsProvider'
import { fetchAdminInfoCardsPage } from '../../features/info-cards/api/infoCardsHttpApi'
import { PORTAL_PAGES } from '../../data/portalPages'
import { getPortalPageElementsByIds } from '../../data/portalPageElements'
import type { InfoCard, InfoCardInput, PortalPageKey } from '../../types/infoCards'
import { InfoCardsDashboard } from './components/InfoCardsDashboard'
import { InfoCardsPageSidebar } from './components/InfoCardsPageSidebar'
import { InfoCardEditorDrawer } from './components/InfoCardEditorDrawer'
import { HelpCardPopover } from '../../components/help/HelpCardPopover'
import { InfoCardsPagination } from './components/InfoCardsPagination'
import {
  InfoCardsStatusSegment,
  type InfoCardListStatusFilter,
} from './components/InfoCardsStatusSegment'

const LIST_PAGE_SIZE = 10

export function BilgiKartlariPage() {
  const { t } = useTranslation('common')
  useDocumentTitle(t('bilgiKartlariPage.titleDoc'))
  const { cards, dashboard, loading, updateCard, deleteCard, toggleStatus, refresh } = useInfoCards()

  const [selectedPage, setSelectedPage] = useState<PortalPageKey>('MARKETS')
  const [query, setQuery] = useState('')
  const [statusFilter, setStatusFilter] = useState<InfoCardListStatusFilter>('ACTIVE')
  const [editorOpen, setEditorOpen] = useState(false)
  const [editing, setEditing] = useState<InfoCard | null>(null)
  const [preview, setPreview] = useState<InfoCard | null>(null)
  const [listPage, setListPage] = useState(0)
  const [listCards, setListCards] = useState<InfoCard[]>([])
  const [totalElements, setTotalElements] = useState(0)
  const [totalListPages, setTotalListPages] = useState(1)
  const [listLoading, setListLoading] = useState(false)

  const loadList = useCallback(async () => {
    setListLoading(true)
    try {
      const page = await fetchAdminInfoCardsPage({
        page: listPage,
        size: LIST_PAGE_SIZE,
        portalPage: selectedPage,
        query,
        status: statusFilter,
      })
      setListCards(page.content)
      setTotalElements(page.totalElements)
      setTotalListPages(Math.max(1, page.totalPages))
    } finally {
      setListLoading(false)
    }
  }, [listPage, selectedPage, query, statusFilter])

  useEffect(() => {
    setListPage(0)
  }, [selectedPage, query, statusFilter])

  useEffect(() => {
    void loadList()
  }, [loadList])

  useEffect(() => {
    if (!preview) {
      return
    }
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setPreview(null)
      }
    }
    document.addEventListener('keydown', onKeyDown)
    return () => document.removeEventListener('keydown', onKeyDown)
  }, [preview])

  const pageLabel = t(PORTAL_PAGES.find((p) => p.key === selectedPage)?.labelKey ?? '')

  const formatCardPages = (card: InfoCard) =>
    card.pages
      .map((key) => t(PORTAL_PAGES.find((p) => p.key === key)?.labelKey ?? key))
      .join(', ')

  const formatCardTargets = (card: InfoCard) => {
    const instruments = card.targetInstrumentSymbols ?? []
    if (instruments.length > 0) {
      return instruments.join(', ')
    }
    const elements = getPortalPageElementsByIds(card.targetElementIds ?? [])
    if (elements.length > 0) {
      const labels = [...new Set(elements.map((e) => e.label))]
      return labels.join(', ')
    }
    return card.targetTerms.join(', ')
  }

  const handleSave = async (input: InfoCardInput, id?: string) => {
    if (!id) {
      return
    }
    await updateCard(id, input)
    setEditorOpen(false)
    setEditing(null)
    await loadList()
    await refresh()
  }

  const handleDelete = async (card: InfoCard) => {
    if (window.confirm(t('bilgiKartlariPage.confirmDelete', { title: card.title }))) {
      await deleteCard(card.id)
      await loadList()
    }
  }

  const handleToggle = async (id: string) => {
    await toggleStatus(id)
    await loadList()
  }

  return (
    <section className="bilgi-kartlari-page ic-admin-page" aria-labelledby="bilgi-kartlari-heading">
      <header className="ic-admin-head">
        <div>
          <h2 id="bilgi-kartlari-heading">{t('bilgiKartlariPage.title')}</h2>
          <p className="bilgi-kartlari-page-lead">{t('bilgiKartlariPage.lead')}</p>
        </div>
      </header>

      {dashboard ? <InfoCardsDashboard data={dashboard} /> : null}

      <div className="ic-admin-layout">
        <InfoCardsPageSidebar cards={cards} selectedPage={selectedPage} onSelect={setSelectedPage} />

        <div className="ic-admin-main card">
          <header className="ic-admin-main-head">
            <h3>{t('bilgiKartlariPage.selectedPage', { page: pageLabel })}</h3>
            <div className="ic-admin-main-tools">
              <InfoCardsStatusSegment value={statusFilter} onChange={setStatusFilter} />
              <input
                type="search"
                className="ic-search"
                placeholder={t('bilgiKartlariPage.searchPlaceholder')}
                value={query}
                onChange={(e) => setQuery(e.target.value)}
              />
            </div>
          </header>

          {listLoading || loading ? (
            <p className="ic-empty">{t('bilgiKartlariPage.loading')}</p>
          ) : listCards.length === 0 ? (
            <p className="ic-empty">{t('bilgiKartlariPage.empty')}</p>
          ) : (
            <ul className="ic-card-list">
              {listCards.map((card) => (
                <li key={card.id} className="ic-card-list-item">
                  <div className="ic-card-list-top">
                    <h4>{card.title}</h4>
                    <div className="lit-term-card-badges">
                      <span className="lit-badge">{t(`finansalOkuryazarlikPage.contentTypes.${card.type}`)}</span>
                      <span className="lit-badge">{t(`finansalOkuryazarlikPage.difficulties.${card.difficulty}`)}</span>
                      {(card.targetInstrumentSymbols?.length ?? 0) > 0 ? (
                        <span className="lit-badge ic-badge-instrument">
                          {t('bilgiKartlariPage.binding.instrument')}
                        </span>
                      ) : (card.targetElementIds?.length ?? 0) > 0 ? (
                        <span className="lit-badge ic-badge-page-target">
                          {t('bilgiKartlariPage.binding.pageTarget')}
                        </span>
                      ) : (
                        <span className="lit-badge ic-badge-literacy">
                          {t('bilgiKartlariPage.binding.literacy')}
                        </span>
                      )}
                      <span className={`lit-badge${card.status === 'PASSIVE' ? ' ic-badge-passive' : ''}`}>
                        {card.status === 'ACTIVE'
                          ? t('bilgiKartlariPage.status.active')
                          : t('bilgiKartlariPage.status.passive')}
                      </span>
                    </div>
                  </div>
                  <p className="ic-card-target">
                    <strong>{t('bilgiKartlariPage.editor.pages')}: </strong>
                    {formatCardPages(card)}
                  </p>
                  <p className="ic-card-target">
                    <strong>{t('bilgiKartlariPage.editor.bindingTarget')}: </strong>
                    {formatCardTargets(card)}
                  </p>
                  <p className="ic-card-short">{card.shortDescription}</p>
                  {card.detailedDescription ? (
                    <p className="ic-card-literacy-excerpt">{card.detailedDescription.slice(0, 160)}…</p>
                  ) : null}
                  <p className="ic-card-meta">
                    {t('bilgiKartlariPage.updated')}: {new Date(card.updatedAt).toLocaleString()}
                  </p>
                  <div className="ic-card-actions">
                    <button type="button" className="lit-btn-secondary" onClick={() => setPreview(card)}>
                      {t('bilgiKartlariPage.actions.preview')}
                    </button>
                    <button
                      type="button"
                      className="lit-btn-secondary"
                      onClick={() => {
                        setEditing(card)
                        setEditorOpen(true)
                      }}
                    >
                      {t('bilgiKartlariPage.actions.edit')}
                    </button>
                    <button type="button" className="lit-btn-secondary" onClick={() => void handleToggle(card.id)}>
                      {card.status === 'ACTIVE'
                        ? t('bilgiKartlariPage.actions.deactivate')
                        : t('bilgiKartlariPage.actions.activate')}
                    </button>
                    <button type="button" className="lit-btn-secondary ic-btn-danger" onClick={() => void handleDelete(card)}>
                      {t('bilgiKartlariPage.actions.delete')}
                    </button>
                  </div>
                </li>
              ))}
            </ul>
          )}
          <InfoCardsPagination
            page={listPage}
            totalPages={totalListPages}
            totalElements={totalElements}
            onPageChange={setListPage}
          />
        </div>
      </div>

      <InfoCardEditorDrawer
        open={editorOpen && editing != null}
        initial={editing}
        defaultPage={selectedPage}
        onClose={() => {
          setEditorOpen(false)
          setEditing(null)
        }}
        onSave={handleSave}
      />

      {preview
        ? createPortal(
            <div className="ic-modal-overlay" role="presentation">
              <button
                type="button"
                className="ic-modal-backdrop"
                aria-label={t('bilgiKartlariPage.editor.close')}
                onClick={() => setPreview(null)}
              />
              <div className="ic-modal-preview" onClick={(e) => e.stopPropagation()}>
                <HelpCardPopover
                  card={preview}
                  anchor={new DOMRect(0, 0, 0, 0)}
                  centered
                  onClose={() => setPreview(null)}
                />
              </div>
            </div>,
            document.body,
          )
        : null}
    </section>
  )
}
