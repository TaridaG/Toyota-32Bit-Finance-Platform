import { useCallback, useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useDocumentTitle } from '../../shared/hooks/useDocumentTitle'
import { useAppPreferences } from '../../shared/preferences/useAppPreferences'
import { isAdminUser } from '../../shared/auth/session'
import { infoCardsApi } from '../../services/infoCardsApi'
import { useInfoCards } from '../../features/info-cards/InfoCardsProvider'
import {
  fetchLiteracyCatalogPage,
  type LiteracyCatalogStats,
} from '../../features/info-cards/api/infoCardsHttpApi'
import { translatePortalPageKey } from '../../data/portalPages'
import { infoCardToLiteracyEntry } from './utils/infoCardAdapter'
import type {
  LiteracyCategory,
  LiteracyContentType,
  LiteracyDifficulty,
  LiteracyEntry,
  LiteracyPortalPage,
} from './types/financialLiteracy'
import { LiteracyHero } from './components/LiteracyHero'
import { LiteracySidebarFilters } from './components/LiteracySidebarFilters'
import { LiteracyTermCard } from './components/LiteracyTermCard'
import { LiteracyDetailDrawer } from './components/LiteracyDetailDrawer'
import { LiteracyEmptyState } from './components/LiteracyEmptyState'
import { InfoCardEditorDrawer } from '../bilgi-kartlari/components/InfoCardEditorDrawer'
import { InfoCardsPagination } from '../bilgi-kartlari/components/InfoCardsPagination'
import type { InfoCardInput } from '../../types/infoCards'

const EMPTY_STATS: LiteracyCatalogStats = {
  total: 0,
  terms: 0,
  charts: 0,
  analysisTools: 0,
  macro: 0,
}

export function FinansalOkuryazarlikPage() {
  const { t } = useTranslation('common')
  const { language } = useAppPreferences()
  const [searchParams, setSearchParams] = useSearchParams()
  useDocumentTitle(t('finansalOkuryazarlikPage.titleDoc'))

  const { createCard } = useInfoCards()
  const showAdminTerms = isAdminUser()
  const [editorOpen, setEditorOpen] = useState(false)

  const [query, setQuery] = useState('')
  const [category, setCategory] = useState<LiteracyCategory | 'ALL'>('ALL')
  const [difficulties, setDifficulties] = useState<LiteracyDifficulty[]>([])
  const [contentTypes, setContentTypes] = useState<LiteracyContentType[]>([])
  const [portalPages, setPortalPages] = useState<LiteracyPortalPage[]>([])
  const [selectedEntry, setSelectedEntry] = useState<LiteracyEntry | null>(null)

  const [listPage, setListPage] = useState(0)
  const [entries, setEntries] = useState<LiteracyEntry[]>([])
  const [stats, setStats] = useState<LiteracyCatalogStats>(EMPTY_STATS)
  const [totalElements, setTotalElements] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)
  const [listLoading, setListLoading] = useState(false)

  const resetListPage = () => setListPage(0)

  const loadCatalog = useCallback(async () => {
    setListLoading(true)
    try {
      const page = await fetchLiteracyCatalogPage({
        page: listPage,
        query,
        category,
        difficulties,
        types: contentTypes,
        portalPages,
        includeAdminOnly: showAdminTerms,
      })
      setEntries(page.content.map(infoCardToLiteracyEntry))
      setStats(page.stats)
      setTotalElements(page.totalElements)
      setTotalPages(page.totalPages)
    } finally {
      setListLoading(false)
      setLoading(false)
    }
  }, [listPage, query, category, difficulties, contentTypes, portalPages, showAdminTerms, language])

  useEffect(() => {
    void loadCatalog()
  }, [loadCatalog])

  useEffect(() => {
    setSelectedEntry((current) => {
      if (!current) {
        return null
      }
      return entries.find((entry) => entry.id === current.id) ?? current
    })
  }, [entries, language])

  useEffect(() => {
    const termSlug = searchParams.get('term')
    if (!termSlug) {
      return
    }
    let cancelled = false
    void (async () => {
      const card = await infoCardsApi.getBySlug(termSlug)
      if (!cancelled && card) {
        const entry = infoCardToLiteracyEntry(card)
        setSelectedEntry(entry)
        setQuery(entry.title)
        resetListPage()
      }
    })()
    setSearchParams({}, { replace: true })
    return () => {
      cancelled = true
    }
  }, [searchParams, setSearchParams])

  const entriesByTitle = useMemo(() => {
    const map = new Map<string, LiteracyEntry>()
    for (const entry of entries) {
      map.set(entry.title, entry)
    }
    return map
  }, [entries])

  const toggle = <T,>(list: T[], value: T): T[] =>
    list.includes(value) ? list.filter((v) => v !== value) : [...list, value]

  const labelCategory = (c: LiteracyCategory) => t(`finansalOkuryazarlikPage.categories.${c}`)
  const labelDifficulty = (d: LiteracyDifficulty) => t(`finansalOkuryazarlikPage.difficulties.${d}`)
  const labelType = (type: LiteracyContentType) => t(`finansalOkuryazarlikPage.contentTypes.${type}`)
  const labelPortal = (p: LiteracyPortalPage | string) => translatePortalPageKey(t, p)

  if (loading) {
    return (
      <section className="finansal-okuryazarlik-page lit-page" aria-labelledby="finansal-okuryazarlik-heading">
        <p className="lit-empty">{t('loading')}</p>
      </section>
    )
  }

  return (
    <section className="finansal-okuryazarlik-page lit-page" aria-labelledby="finansal-okuryazarlik-heading">
      <LiteracyHero
        title={t('finansalOkuryazarlikPage.title')}
        lead={t('finansalOkuryazarlikPage.lead')}
        searchValue={query}
        onSearchChange={(value) => {
          setQuery(value)
          resetListPage()
        }}
        searchPlaceholder={t('finansalOkuryazarlikPage.searchPlaceholder')}
        searchAriaLabel={t('finansalOkuryazarlikPage.searchAria')}
        stats={
          <>
            <span>
              <strong>{stats.total}</strong> {t('finansalOkuryazarlikPage.stats.terms')}
            </span>
            <span>
              <strong>{stats.charts}</strong> {t('finansalOkuryazarlikPage.stats.charts')}
            </span>
            <span>
              <strong>{stats.analysisTools}</strong> {t('finansalOkuryazarlikPage.stats.analysisTools')}
            </span>
            <span>
              <strong>{stats.macro}</strong> {t('finansalOkuryazarlikPage.stats.macro')}
            </span>
          </>
        }
        adminActions={
          showAdminTerms ? (
            <button type="button" className="lit-btn-primary" onClick={() => setEditorOpen(true)}>
              {t('finansalOkuryazarlikPage.admin.addCard')}
            </button>
          ) : null
        }
      />

      <InfoCardEditorDrawer
        open={editorOpen}
        initial={null}
        defaultPage="FINANCIAL_LITERACY"
        mode="literacy"
        onClose={() => setEditorOpen(false)}
        onSave={async (input: InfoCardInput) => {
          await createCard(input)
          setEditorOpen(false)
          setListPage(0)
          await loadCatalog()
        }}
      />

      <div className="lit-main-layout">
        <LiteracySidebarFilters
          category={category}
          onCategoryChange={(value) => {
            setCategory(value)
            resetListPage()
          }}
          difficulties={difficulties}
          onDifficultyToggle={(d) => {
            setDifficulties((prev) => toggle(prev, d))
            resetListPage()
          }}
          contentTypes={contentTypes}
          onContentTypeToggle={(type) => {
            setContentTypes((prev) => toggle(prev, type))
            resetListPage()
          }}
          portalPages={portalPages}
          onPortalPageToggle={(p) => {
            setPortalPages((prev) => toggle(prev, p))
            resetListPage()
          }}
          showSystemCategory={showAdminTerms}
          labels={{
            filtersTitle: t('finansalOkuryazarlikPage.filtersTitle'),
            categoryTitle: t('finansalOkuryazarlikPage.categoryTitle'),
            allCategories: t('finansalOkuryazarlikPage.allCategories'),
            difficultyTitle: t('finansalOkuryazarlikPage.difficultyTitle'),
            contentTypeTitle: t('finansalOkuryazarlikPage.contentTypeTitle'),
            portalPageTitle: t('finansalOkuryazarlikPage.portalPageTitle'),
            categoryLabel: labelCategory,
            difficultyLabel: labelDifficulty,
            contentTypeLabel: labelType,
            portalPageLabel: labelPortal,
          }}
        />

        <div className="lit-content-column">
          {listLoading ? (
            <p className="lit-empty">{t('loading')}</p>
          ) : entries.length === 0 ? (
            <LiteracyEmptyState
              title={t('finansalOkuryazarlikPage.emptyTitle')}
              message={t('finansalOkuryazarlikPage.emptyMessage')}
            />
          ) : (
            <>
              <div className="lit-term-grid">
                {entries.map((entry) => (
                  <LiteracyTermCard
                    key={entry.id}
                    entry={entry}
                    typeLabel={labelType(entry.type)}
                    difficultyLabel={labelDifficulty(entry.difficulty)}
                    usedInLabel={t('finansalOkuryazarlikPage.usedIn')}
                    detailsLabel={t('finansalOkuryazarlikPage.details')}
                    portalPageLabel={labelPortal}
                    onOpenDetails={setSelectedEntry}
                  />
                ))}
              </div>
              <InfoCardsPagination
                page={listPage}
                totalPages={totalPages}
                totalElements={totalElements}
                onPageChange={setListPage}
              />
            </>
          )}
        </div>
      </div>

      <LiteracyDetailDrawer
        entry={selectedEntry}
        onClose={() => setSelectedEntry(null)}
        typeLabel={selectedEntry ? labelType(selectedEntry.type) : ''}
        difficultyLabel={selectedEntry ? labelDifficulty(selectedEntry.difficulty) : ''}
        entriesByTitle={entriesByTitle}
        onSelectRelated={setSelectedEntry}
        sectionLabels={{
          shortDefinition: t('finansalOkuryazarlikPage.sections.shortDefinition'),
          financialMeaning: t('finansalOkuryazarlikPage.sections.financialMeaning'),
          usedInPortal: t('finansalOkuryazarlikPage.sections.usedInPortal'),
          howToInterpret: t('finansalOkuryazarlikPage.sections.howToInterpret'),
          commonMistake: t('finansalOkuryazarlikPage.sections.commonMistake'),
          example: t('finansalOkuryazarlikPage.sections.example'),
          relatedTerms: t('finansalOkuryazarlikPage.sections.relatedTerms'),
          goToPage: t('finansalOkuryazarlikPage.goToPage'),
          close: t('finansalOkuryazarlikPage.close'),
        }}
      />
    </section>
  )
}
