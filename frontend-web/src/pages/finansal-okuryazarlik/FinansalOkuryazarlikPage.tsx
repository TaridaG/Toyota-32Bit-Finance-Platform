import { useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useDocumentTitle } from '../../shared/hooks/useDocumentTitle'
import { isAdminUser } from '../../shared/auth/session'
import { infoCardsApi } from '../../services/infoCardsApi'
import { useInfoCards } from '../../features/info-cards/InfoCardsProvider'
import { infoCardToLiteracyEntry } from './utils/infoCardAdapter'
import { countByCategory, countByType, filterLiteracyEntries } from './utils/filterLiteracyEntries'
import type {
  LiteracyCategory,
  LiteracyContentType,
  LiteracyDifficulty,
  LiteracyEntry,
  LiteracyPortalPage,
} from './types/financialLiteracy'
import { LiteracyHero } from './components/LiteracyHero'
import { LiteracyCategoryCards } from './components/LiteracyCategoryCards'
import { LiteracySidebarFilters } from './components/LiteracySidebarFilters'
import { LiteracyTermCard } from './components/LiteracyTermCard'
import { LiteracyDetailDrawer } from './components/LiteracyDetailDrawer'
import { LearningPathCard } from './components/LearningPathCard'
import { LiteracyEmptyState } from './components/LiteracyEmptyState'

const LEARNING_PATH: { order: number; labelKey: string; category: LiteracyCategory }[] = [
  { order: 1, labelKey: 'BASIC_FINANCE', category: 'BASIC_FINANCE' },
  { order: 2, labelKey: 'MARKET_DATA', category: 'MARKET_DATA' },
  { order: 3, labelKey: 'CHARTS', category: 'CHARTS' },
  { order: 4, labelKey: 'PORTFOLIO_ANALYSIS', category: 'PORTFOLIO_ANALYSIS' },
  { order: 5, labelKey: 'TURKEY_ECONOMY', category: 'TURKEY_ECONOMY' },
  { order: 6, labelKey: 'SIMULATION', category: 'SIMULATION' },
]

export function FinansalOkuryazarlikPage() {
  const { t } = useTranslation('common')
  const [searchParams, setSearchParams] = useSearchParams()
  useDocumentTitle(t('finansalOkuryazarlikPage.titleDoc'))

  const { cards, loading } = useInfoCards()
  const showAdminTerms = isAdminUser()
  const visibleEntries = useMemo(
    () =>
      cards
        .filter((c) => c.status === 'ACTIVE' && (!c.adminOnly || showAdminTerms))
        .map(infoCardToLiteracyEntry),
    [cards, showAdminTerms],
  )

  const [query, setQuery] = useState('')
  const [category, setCategory] = useState<LiteracyCategory | 'ALL'>('ALL')
  const [difficulties, setDifficulties] = useState<LiteracyDifficulty[]>([])
  const [contentTypes, setContentTypes] = useState<LiteracyContentType[]>([])
  const [portalPages, setPortalPages] = useState<LiteracyPortalPage[]>([])
  const [selectedEntry, setSelectedEntry] = useState<LiteracyEntry | null>(null)

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
      }
    })()
    setSearchParams({}, { replace: true })
    return () => {
      cancelled = true
    }
  }, [searchParams, setSearchParams])

  const entriesByTitle = useMemo(() => {
    const map = new Map<string, LiteracyEntry>()
    for (const entry of visibleEntries) {
      map.set(entry.title, entry)
    }
    return map
  }, [visibleEntries])

  const filtered = useMemo(
    () =>
      filterLiteracyEntries(visibleEntries, {
        query,
        category,
        difficulties,
        contentTypes,
        portalPages,
        showAdminTerms,
      }),
    [visibleEntries, query, category, difficulties, contentTypes, portalPages, showAdminTerms],
  )

  const stats = countByType(visibleEntries)

  const toggle = <T,>(list: T[], value: T): T[] =>
    list.includes(value) ? list.filter((v) => v !== value) : [...list, value]

  const labelCategory = (c: LiteracyCategory) => t(`finansalOkuryazarlikPage.categories.${c}`)
  const labelDifficulty = (d: LiteracyDifficulty) => t(`finansalOkuryazarlikPage.difficulties.${d}`)
  const labelType = (type: LiteracyContentType) => t(`finansalOkuryazarlikPage.contentTypes.${type}`)
  const labelPortal = (p: LiteracyPortalPage) => t(`finansalOkuryazarlikPage.portalPages.${p}`)

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
        onSearchChange={setQuery}
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
      />

      <LiteracyCategoryCards
        activeCategory={category}
        onSelect={setCategory}
        labelForCategory={labelCategory}
        descriptionForCategory={(c) => t(`finansalOkuryazarlikPage.categoryDescriptions.${c}`)}
        countForCategory={(c) => countByCategory(visibleEntries, c)}
        allLabel={t('finansalOkuryazarlikPage.allCategories')}
        allCount={visibleEntries.length}
        showSystemCategory={showAdminTerms}
      />

      <div className="lit-main-layout">
        <LiteracySidebarFilters
          category={category}
          onCategoryChange={setCategory}
          difficulties={difficulties}
          onDifficultyToggle={(d) => setDifficulties((prev) => toggle(prev, d))}
          contentTypes={contentTypes}
          onContentTypeToggle={(type) => setContentTypes((prev) => toggle(prev, type))}
          portalPages={portalPages}
          onPortalPageToggle={(p) => setPortalPages((prev) => toggle(prev, p))}
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
          <LearningPathCard
            title={t('finansalOkuryazarlikPage.learningPathTitle')}
            steps={LEARNING_PATH.map((step) => ({
              order: step.order,
              label: labelCategory(step.category),
              categoryKey: step.category,
            }))}
            onStepClick={(key) => setCategory(key as LiteracyCategory)}
          />

          {filtered.length === 0 ? (
            <LiteracyEmptyState
              title={t('finansalOkuryazarlikPage.emptyTitle')}
              message={t('finansalOkuryazarlikPage.emptyMessage')}
            />
          ) : (
            <div className="lit-term-grid">
              {filtered.map((entry) => (
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
