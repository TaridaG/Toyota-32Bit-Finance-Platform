import { useEffect, useMemo, useState } from 'react'
import { createPortal } from 'react-dom'
import { useTranslation } from 'react-i18next'
import { PORTAL_PAGES } from '../../../data/portalPages'
import { getPortalPageElementsByIds } from '../../../data/portalPageElements'
import {
  DEFAULT_LOCALE,
  LANGUAGE_LABELS,
  SUPPORTED_LOCALES,
  normalizeLocale,
  type SupportedLocale,
} from '../../../shared/i18n'
import {
  completeInfoCardWithAi,
  translateInfoCardWithAi,
} from '../../../features/info-cards/api/adminInfoCardAiApi'
import type { InfoCard, InfoCardInput } from '../../../types/infoCards'
import {
  INFO_CARD_CATEGORIES,
  INFO_CARD_DIFFICULTIES,
  INFO_CARD_TYPES,
} from '../constants'
import { termsFromElementIds } from '../utils/searchPortalPageElements'
import {
  EMPTY_LOCALE_FORM,
  buildInfoCardInput,
  cardToLocaleForms,
  isLocaleFormComplete,
  localeLabel,
  slugifyInfoCardTitle,
  splitCsv,
  wizardLocaleOrder,
  type InfoCardLocaleFormState,
} from '../utils/infoCardTranslations'
import {
  canCompleteInfoCardWithAi,
  isLocaleAiSourceReady,
  localeFormToAiSourceContent,
  resolveAiErrorMessage,
  resolveAiSourceLocale,
} from '../utils/infoCardAiHelpers'

const EMPTY_FORM: InfoCardInput = {
  title: '',
  targetTerms: [],
  targetElementIds: [],
  targetInstrumentSymbols: [],
  pages: ['MARKETS'],
  category: 'MARKET_DATA',
  type: 'TERM',
  difficulty: 'BEGINNER',
  status: 'ACTIVE',
  shortDescription: '',
  detailedDescription: '',
  howToInterpret: '',
  commonMistake: '',
  example: '',
  relatedTerms: [],
}

export type InfoCardEditorPickPrefill = {
  title?: string
  targetTerms?: string[]
  targetElementIds?: string[]
  targetInstrumentSymbols?: string[]
  pages?: InfoCard['pages']
  pickLabel?: string
  localeTitles?: Record<SupportedLocale, string>
}

type InfoCardEditorDrawerProps = {
  open: boolean
  initial: InfoCard | null
  defaultPage: InfoCard['pages'][number]
  /** Finansal okuryazarlık: yalnızca bu sayfada listelenen genel kart */
  mode?: 'default' | 'literacy'
  pickPrefill?: InfoCardEditorPickPrefill | null
  onClose: () => void
  onSave: (input: InfoCardInput, id?: string) => void | Promise<void>
}

type WizardStep = number | 'preview'

function emptyLocaleForms(): Record<SupportedLocale, InfoCardLocaleFormState> {
  return {
    tr: { ...EMPTY_LOCALE_FORM },
    en: { ...EMPTY_LOCALE_FORM },
    de: { ...EMPTY_LOCALE_FORM },
  }
}

export function InfoCardEditorDrawer({
  open,
  initial,
  defaultPage,
  mode = 'default',
  pickPrefill,
  onClose,
  onSave,
}: InfoCardEditorDrawerProps) {
  const { t, i18n } = useTranslation('common')
  const adminLocale = normalizeLocale(i18n.language) ?? DEFAULT_LOCALE
  const localeOrder = useMemo(() => wizardLocaleOrder(adminLocale), [adminLocale])

  const [form, setForm] = useState<InfoCardInput>(EMPTY_FORM)
  const [localeForms, setLocaleForms] = useState(emptyLocaleForms)
  const [wizardStep, setWizardStep] = useState<WizardStep>(0)
  const [targetElementIds, setTargetElementIds] = useState<string[]>([])
  const [instrumentSymbolsText, setInstrumentSymbolsText] = useState('')
  const [stepError, setStepError] = useState<string | null>(null)
  const [saveError, setSaveError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)
  const [aiLoading, setAiLoading] = useState(false)
  const [aiError, setAiError] = useState<string | null>(null)

  useEffect(() => {
    if (!open) {
      return
    }
    setSaveError(null)
    setStepError(null)
    setSaving(false)
    setAiLoading(false)
    setAiError(null)
    setWizardStep(0)

    if (initial) {
      setForm({
        title: initial.title,
        targetTerms: initial.targetTerms,
        pages: initial.pages,
        category: initial.category,
        type: initial.type,
        difficulty: initial.difficulty,
        status: initial.status,
        shortDescription: initial.shortDescription,
        detailedDescription: initial.detailedDescription,
        howToInterpret: initial.howToInterpret ?? '',
        commonMistake: initial.commonMistake ?? '',
        example: initial.example ?? '',
        relatedTerms: initial.relatedTerms,
        adminOnly: initial.adminOnly,
        targetElementIds: initial.targetElementIds ?? [],
        targetInstrumentSymbols: initial.targetInstrumentSymbols ?? [],
      })
      setLocaleForms(cardToLocaleForms(initial))
      setTargetElementIds(initial.targetElementIds ?? [])
      setInstrumentSymbolsText((initial.targetInstrumentSymbols ?? []).join(', '))
    } else if (pickPrefill) {
      const pages = pickPrefill.pages?.length ? pickPrefill.pages : [defaultPage]
      const terms = pickPrefill.targetTerms?.length ? pickPrefill.targetTerms : []
      const instruments = pickPrefill.targetInstrumentSymbols?.length ? pickPrefill.targetInstrumentSymbols : []
      const elementIds = pickPrefill.targetElementIds ?? []
      const localeTitles = pickPrefill.localeTitles
      const seedTitle = pickPrefill.title ?? terms[0] ?? ''
      const forms = emptyLocaleForms()
      if (localeTitles) {
        for (const locale of SUPPORTED_LOCALES) {
          forms[locale] = {
            ...EMPTY_LOCALE_FORM,
            title: localeTitles[locale] || seedTitle,
          }
        }
      } else {
        forms[adminLocale] = { ...EMPTY_LOCALE_FORM, title: seedTitle }
      }
      setLocaleForms(forms)
      setForm({
        ...EMPTY_FORM,
        pages,
        title: seedTitle,
        targetTerms: terms,
        targetInstrumentSymbols: instruments,
        slug: instruments[0] ? `asset-${instruments[0].toLowerCase()}` : undefined,
        type: instruments.length > 0 ? 'ASSET' : 'TERM',
        category: 'MARKET_DATA',
      })
      setTargetElementIds(elementIds)
      setInstrumentSymbolsText(instruments.join(', '))
    } else if (mode === 'literacy') {
      setForm({
        ...EMPTY_FORM,
        pages: ['FINANCIAL_LITERACY'],
        category: 'BASIC_FINANCE',
      })
      setLocaleForms(emptyLocaleForms())
      setTargetElementIds([])
      setInstrumentSymbolsText('')
    } else {
      setForm({ ...EMPTY_FORM, pages: [defaultPage] })
      setLocaleForms(emptyLocaleForms())
      setTargetElementIds([])
      setInstrumentSymbolsText('')
    }
  }, [open, initial, defaultPage, pickPrefill, adminLocale, mode])

  useEffect(() => {
    if (!open) {
      return
    }
    const onKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        onClose()
      }
    }
    document.addEventListener('keydown', onKeyDown)
    return () => document.removeEventListener('keydown', onKeyDown)
  }, [open, onClose])

  const aiSourceLocale = useMemo(() => resolveAiSourceLocale(localeForms), [localeForms])

  if (!open) {
    return null
  }

  const isPickCreate = Boolean(pickPrefill && !initial)
  const isLiteracyCreate = mode === 'literacy' && !initial && !pickPrefill
  const isContentOnly = isPickCreate || isLiteracyCreate || Boolean(initial)
  const activeLocale = wizardStep === 'preview' ? null : localeOrder[wizardStep as number]
  const activeLocaleForm = activeLocale ? localeForms[activeLocale] : null
  const canAiComplete = Boolean(activeLocaleForm && canCompleteInfoCardWithAi(activeLocaleForm))
  const canAiTranslate = Boolean(
    activeLocale &&
      aiSourceLocale &&
      activeLocale !== aiSourceLocale &&
      isLocaleAiSourceReady(localeForms[aiSourceLocale]),
  )

  const bindingPageLabels = form.pages
    .map((key) => t(PORTAL_PAGES.find((p) => p.key === key)?.labelKey ?? ''))
    .filter(Boolean)
    .join(', ')

  const bindingTargetLabel = (() => {
    const instruments = (form.targetInstrumentSymbols ?? []).length
      ? (form.targetInstrumentSymbols ?? []).join(', ')
      : null
    if (instruments) {
      return instruments
    }
    const elements = getPortalPageElementsByIds(targetElementIds)
    if (elements.length > 0) {
      return [...new Set(elements.map((e) => e.label))].join(', ')
    }
    return form.targetTerms.join(', ') || pickPrefill?.pickLabel || '—'
  })()

  const updateLocaleForm = (locale: SupportedLocale, patch: Partial<InfoCardLocaleFormState>) => {
    setLocaleForms((prev) => ({
      ...prev,
      [locale]: { ...prev[locale], ...patch },
    }))
  }

  const handleAiComplete = async () => {
    if (!activeLocale || !activeLocaleForm) {
      return
    }
    setAiLoading(true)
    setAiError(null)
    try {
      const result = await completeInfoCardWithAi({
        language: activeLocale,
        title: activeLocaleForm.title.trim(),
        shortDescription: activeLocaleForm.shortDescription.trim(),
        category: form.category,
        type: form.type,
        difficulty: form.difficulty,
      })
      updateLocaleForm(activeLocale, {
        detailedDescription: result.detailedDescription ?? activeLocaleForm.detailedDescription,
        howToInterpret: result.howToInterpret ?? activeLocaleForm.howToInterpret,
        commonMistake: result.commonMistake ?? activeLocaleForm.commonMistake,
        example: result.example ?? activeLocaleForm.example,
        relatedTermsText:
          result.relatedTerms && result.relatedTerms.length > 0
            ? result.relatedTerms.join(', ')
            : activeLocaleForm.relatedTermsText,
      })
    } catch (error) {
      setAiError(
        resolveAiErrorMessage(
          error,
          t('bilgiKartlariPage.editor.wizard.aiDisabled'),
          t('bilgiKartlariPage.editor.wizard.aiFailed'),
          t('bilgiKartlariPage.editor.wizard.aiQuotaExceeded'),
          t('bilgiKartlariPage.editor.wizard.aiTimeout'),
        ),
      )
    } finally {
      setAiLoading(false)
    }
  }

  const handleAiTranslate = async () => {
    if (!activeLocale || !aiSourceLocale) {
      return
    }
    setAiLoading(true)
    setAiError(null)
    try {
      const result = await translateInfoCardWithAi({
        sourceLanguage: aiSourceLocale,
        targetLanguage: activeLocale,
        sourceContent: localeFormToAiSourceContent(localeForms[aiSourceLocale]),
      })
      updateLocaleForm(activeLocale, {
        title: result.title ?? localeForms[activeLocale].title,
        shortDescription: result.shortDescription ?? localeForms[activeLocale].shortDescription,
        detailedDescription: result.detailedDescription ?? localeForms[activeLocale].detailedDescription,
        howToInterpret: result.howToInterpret ?? localeForms[activeLocale].howToInterpret,
        commonMistake: result.commonMistake ?? localeForms[activeLocale].commonMistake,
        example: result.example ?? localeForms[activeLocale].example,
        relatedTermsText:
          result.relatedTerms && result.relatedTerms.length > 0
            ? result.relatedTerms.join(', ')
            : localeForms[activeLocale].relatedTermsText,
      })
    } catch (error) {
      setAiError(
        resolveAiErrorMessage(
          error,
          t('bilgiKartlariPage.editor.wizard.aiDisabled'),
          t('bilgiKartlariPage.editor.wizard.aiFailed'),
          t('bilgiKartlariPage.editor.wizard.aiQuotaExceeded'),
          t('bilgiKartlariPage.editor.wizard.aiTimeout'),
        ),
      )
    } finally {
      setAiLoading(false)
    }
  }

  const validateCurrentStep = (): boolean => {
    if (wizardStep === 'preview') {
      return true
    }
    const locale = localeOrder[wizardStep as number]
    if (!isLocaleFormComplete(localeForms[locale])) {
      setStepError(t('bilgiKartlariPage.editor.wizard.localeRequired'))
      return false
    }
    setStepError(null)
    return true
  }

  const goNext = () => {
    if (!validateCurrentStep()) {
      return
    }
    if (wizardStep === 'preview') {
      return
    }
    if (wizardStep >= localeOrder.length - 1) {
      setWizardStep('preview')
      return
    }
    setWizardStep((wizardStep as number) + 1)
  }

  const goBack = () => {
    setStepError(null)
    if (wizardStep === 'preview') {
      setWizardStep(localeOrder.length - 1)
      return
    }
    if (typeof wizardStep === 'number' && wizardStep > 0) {
      setWizardStep(wizardStep - 1)
    }
  }

  const handleSubmit = async () => {
    for (const locale of SUPPORTED_LOCALES) {
      if (!isLocaleFormComplete(localeForms[locale])) {
        setStepError(t('bilgiKartlariPage.editor.wizard.allLocalesRequired'))
        setWizardStep(localeOrder.indexOf(locale))
        return
      }
    }

    const primaryTitle = localeForms[adminLocale].title.trim()
    const literacySlug = slugifyInfoCardTitle(primaryTitle)

    const termsFromElements = termsFromElementIds(targetElementIds)
    const targetInstrumentSymbols = isLiteracyCreate
      ? []
      : isContentOnly
        ? (form.targetInstrumentSymbols ?? []).map((s) => s.toUpperCase())
        : splitCsv(instrumentSymbolsText).map((s) => s.toUpperCase())
    const targetElementIdsFinal = isLiteracyCreate ? [] : targetElementIds
    const targetTerms = isLiteracyCreate
      ? [literacySlug, primaryTitle]
      : termsFromElements.length > 0
        ? termsFromElements
        : targetInstrumentSymbols.length > 0
          ? targetInstrumentSymbols
          : [primaryTitle]
    const slug =
      form.slug ??
      (isLiteracyCreate
        ? literacySlug
        : targetInstrumentSymbols[0]
          ? `asset-${targetInstrumentSymbols[0].toLowerCase()}`
          : undefined)

    const payload = buildInfoCardInput(
      {
        ...form,
        pages: isLiteracyCreate ? ['FINANCIAL_LITERACY'] : form.pages,
        slug,
        targetElementIds: targetElementIdsFinal,
        targetInstrumentSymbols,
        targetTerms,
        type: targetInstrumentSymbols.length > 0 && form.type === 'TERM' ? 'ASSET' : form.type,
      },
      localeForms,
      adminLocale,
    )

    setSaveError(null)
    setSaving(true)
    try {
      await onSave(payload, initial?.id)
    } catch (error) {
      const message =
        error instanceof Error && 'response' in error
          ? String((error as { response?: { data?: { message?: string } } }).response?.data?.message ?? error.message)
          : error instanceof Error
            ? error.message
            : t('bilgiKartlariPage.editor.saveError')
      setSaveError(message || t('bilgiKartlariPage.editor.saveError'))
    } finally {
      setSaving(false)
    }
  }

  const modalTitle = initial
    ? t('bilgiKartlariPage.editor.editTitle')
    : isLiteracyCreate
      ? t('finansalOkuryazarlikPage.admin.editorTitle')
      : isPickCreate
        ? t('bilgiKartlariPage.editor.pickTitle')
        : t('bilgiKartlariPage.editor.newTitle')

  return createPortal(
    <div className="ic-modal-overlay" role="presentation">
      <button type="button" className="ic-modal-backdrop" aria-label={t('bilgiKartlariPage.editor.close')} onClick={onClose} />
      <div
        className="ic-modal card ic-modal--wizard"
        role="dialog"
        aria-modal="true"
        aria-labelledby="ic-editor-modal-title"
        onClick={(e) => e.stopPropagation()}
      >
        <header className="ic-modal-head">
          <div className="ic-wizard-head-main">
            <h2 id="ic-editor-modal-title">{modalTitle}</h2>
            {isPickCreate && pickPrefill?.pickLabel ? (
              <p className="ic-editor-pick-label">
                {t('bilgiKartlariPage.editor.pickedTarget', { label: pickPrefill.pickLabel })}
              </p>
            ) : null}
            <div className="ic-wizard-locale-rail" role="tablist" aria-label={t('bilgiKartlariPage.editor.wizard.localeRailAria')}>
              {localeOrder.map((locale, index) => {
                const complete = isLocaleFormComplete(localeForms[locale])
                const active = wizardStep === index
                return (
                  <button
                    key={locale}
                    type="button"
                    role="tab"
                    aria-selected={active}
                    className={`ic-wizard-locale-chip${active ? ' ic-wizard-locale-chip--active' : ''}${complete ? ' ic-wizard-locale-chip--done' : ''}`}
                    onClick={() => {
                      setStepError(null)
                      setWizardStep(index)
                    }}
                  >
                    {localeLabel(locale)}
                  </button>
                )
              })}
              <button
                type="button"
                role="tab"
                aria-selected={wizardStep === 'preview'}
                className={`ic-wizard-locale-chip ic-wizard-locale-chip--preview${wizardStep === 'preview' ? ' ic-wizard-locale-chip--active' : ''}`}
                onClick={() => {
                  setStepError(null)
                  setWizardStep('preview')
                }}
              >
                {t('bilgiKartlariPage.editor.wizard.preview')}
              </button>
            </div>
          </div>
          <button type="button" className="ic-modal-close" onClick={onClose} aria-label={t('bilgiKartlariPage.editor.close')}>
            ×
          </button>
        </header>

        <div className="ic-modal-body">
          {wizardStep === 'preview' ? (
            <div className="ic-wizard-preview">
              {SUPPORTED_LOCALES.map((locale) => {
                const content = localeForms[locale]
                return (
                  <section key={locale} className="ic-wizard-preview-panel card">
                    <h3 className="ic-wizard-preview-locale">{LANGUAGE_LABELS[locale]}</h3>
                    <p className="ic-wizard-preview-title">{content.title || '—'}</p>
                    <p className="ic-wizard-preview-short">{content.shortDescription || '—'}</p>
                    {content.detailedDescription ? (
                      <p className="ic-wizard-preview-detail">{content.detailedDescription}</p>
                    ) : null}
                    {content.howToInterpret ? (
                      <p className="ic-wizard-preview-hint">
                        <strong>{t('bilgiKartlariPage.editor.howToInterpret')}:</strong> {content.howToInterpret}
                      </p>
                    ) : null}
                    {content.commonMistake ? (
                      <p className="ic-wizard-preview-hint">
                        <strong>{t('bilgiKartlariPage.editor.commonMistake')}:</strong> {content.commonMistake}
                      </p>
                    ) : null}
                  </section>
                )
              })}
              <div className="ic-wizard-preview-meta card">
                <p>
                  <strong>{t('bilgiKartlariPage.editor.type')}:</strong>{' '}
                  {t(`finansalOkuryazarlikPage.contentTypes.${form.type}`)}
                </p>
                <p>
                  <strong>{t('bilgiKartlariPage.editor.difficulty')}:</strong>{' '}
                  {t(`finansalOkuryazarlikPage.difficulties.${form.difficulty}`)}
                </p>
                <p>
                  <strong>{t('bilgiKartlariPage.editor.category')}:</strong>{' '}
                  {t(`finansalOkuryazarlikPage.categories.${form.category}`)}
                </p>
                <p>
                  <strong>{t('bilgiKartlariPage.editor.pages')}:</strong> {bindingPageLabels}
                </p>
                <p>
                  <strong>{t('bilgiKartlariPage.editor.bindingTarget')}:</strong> {bindingTargetLabel}
                </p>
              </div>
            </div>
          ) : activeLocale ? (
            <>
              <p className="ic-wizard-step-lead">
                {t('bilgiKartlariPage.editor.wizard.stepLead', { locale: localeLabel(activeLocale) })}
              </p>
              {isContentOnly ? (
                <div className="ic-editor-binding-summary" aria-live="polite">
                  <p>
                    <strong>{t('bilgiKartlariPage.editor.pages')}:</strong> {bindingPageLabels}
                  </p>
                  {isLiteracyCreate ? (
                    <p className="ic-target-picker-lead">{t('finansalOkuryazarlikPage.admin.editorBindingHint')}</p>
                  ) : (
                    <>
                      <p>
                        <strong>{t('bilgiKartlariPage.editor.bindingTarget')}:</strong> {bindingTargetLabel}
                      </p>
                      {isPickCreate && wizardStep === 0 ? (
                        <p className="ic-target-picker-lead">{t('bilgiKartlariPage.editor.pickBindingHint')}</p>
                      ) : null}
                    </>
                  )}
                </div>
              ) : null}

              {wizardStep === 0 ? (
                <>
                  <label className="ic-field">
                    <span>{t('bilgiKartlariPage.editor.type')}</span>
                    <select
                      value={form.type}
                      onChange={(e) => setForm((p) => ({ ...p, type: e.target.value as InfoCardInput['type'] }))}
                    >
                      {INFO_CARD_TYPES.map((type) => (
                        <option key={type} value={type}>
                          {t(`finansalOkuryazarlikPage.contentTypes.${type}`)}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label className="ic-field">
                    <span>{t('bilgiKartlariPage.editor.difficulty')}</span>
                    <select
                      value={form.difficulty}
                      onChange={(e) =>
                        setForm((p) => ({ ...p, difficulty: e.target.value as InfoCardInput['difficulty'] }))
                      }
                    >
                      {INFO_CARD_DIFFICULTIES.map((d) => (
                        <option key={d} value={d}>
                          {t(`finansalOkuryazarlikPage.difficulties.${d}`)}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label className="ic-field">
                    <span>{t('bilgiKartlariPage.editor.category')}</span>
                    <select
                      value={form.category}
                      onChange={(e) =>
                        setForm((p) => ({ ...p, category: e.target.value as InfoCardInput['category'] }))
                      }
                    >
                      {INFO_CARD_CATEGORIES.map((c) => (
                        <option key={c} value={c}>
                          {t(`finansalOkuryazarlikPage.categories.${c}`)}
                        </option>
                      ))}
                    </select>
                  </label>
                </>
              ) : null}

              <fieldset className="ic-field ic-literacy-fields">
                <legend>{t('bilgiKartlariPage.editor.literacySection')}</legend>
                <p className="ic-target-picker-lead">{t('bilgiKartlariPage.editor.literacySectionLead')}</p>
              </fieldset>

              <label className="ic-field">
                <span>{t('bilgiKartlariPage.editor.title')}</span>
                <input
                  value={localeForms[activeLocale].title}
                  onChange={(e) => updateLocaleForm(activeLocale, { title: e.target.value })}
                />
              </label>
              <label className="ic-field">
                <span>{t('bilgiKartlariPage.editor.shortDescription')}</span>
                <textarea
                  rows={3}
                  value={localeForms[activeLocale].shortDescription}
                  onChange={(e) => updateLocaleForm(activeLocale, { shortDescription: e.target.value })}
                />
              </label>

              <div className="ic-editor-ai-toolbar">
                {canAiTranslate ? (
                  <button
                    type="button"
                    className="lit-btn-secondary"
                    disabled={aiLoading || saving}
                    onClick={() => void handleAiTranslate()}
                  >
                    {aiLoading
                      ? t('bilgiKartlariPage.editor.wizard.aiTranslateLoading')
                      : t('bilgiKartlariPage.editor.wizard.aiTranslate', {
                          source: localeLabel(aiSourceLocale!),
                          target: localeLabel(activeLocale),
                        })}
                  </button>
                ) : (
                  <button
                    type="button"
                    className="lit-btn-secondary"
                    disabled={!canAiComplete || aiLoading || saving}
                    onClick={() => void handleAiComplete()}
                  >
                    {aiLoading
                      ? t('bilgiKartlariPage.editor.wizard.aiCompleteLoading')
                      : t('bilgiKartlariPage.editor.wizard.aiComplete')}
                  </button>
                )}
              </div>

              <label className="ic-field">
                <span>{t('bilgiKartlariPage.editor.detailedDescription')}</span>
                <textarea
                  rows={4}
                  value={localeForms[activeLocale].detailedDescription}
                  onChange={(e) => updateLocaleForm(activeLocale, { detailedDescription: e.target.value })}
                />
              </label>
              <label className="ic-field">
                <span>{t('bilgiKartlariPage.editor.howToInterpret')}</span>
                <textarea
                  rows={3}
                  value={localeForms[activeLocale].howToInterpret}
                  onChange={(e) => updateLocaleForm(activeLocale, { howToInterpret: e.target.value })}
                />
              </label>
              <label className="ic-field">
                <span>{t('bilgiKartlariPage.editor.commonMistake')}</span>
                <textarea
                  rows={2}
                  value={localeForms[activeLocale].commonMistake}
                  onChange={(e) => updateLocaleForm(activeLocale, { commonMistake: e.target.value })}
                />
              </label>
              <label className="ic-field">
                <span>{t('bilgiKartlariPage.editor.example')}</span>
                <textarea
                  rows={2}
                  value={localeForms[activeLocale].example}
                  onChange={(e) => updateLocaleForm(activeLocale, { example: e.target.value })}
                />
              </label>
              <label className="ic-field">
                <span>{t('bilgiKartlariPage.editor.relatedTerms')}</span>
                <input
                  value={localeForms[activeLocale].relatedTermsText}
                  onChange={(e) => updateLocaleForm(activeLocale, { relatedTermsText: e.target.value })}
                />
              </label>

              {aiError ? <p className="ic-editor-save-error">{aiError}</p> : null}

              {wizardStep === 0 ? (
                <label className="ic-check">
                  <input
                    type="checkbox"
                    checked={form.status === 'ACTIVE'}
                    onChange={(e) => setForm((p) => ({ ...p, status: e.target.checked ? 'ACTIVE' : 'PASSIVE' }))}
                  />
                  <span>{t('bilgiKartlariPage.editor.active')}</span>
                </label>
              ) : null}
            </>
          ) : null}

          {stepError ? <p className="ic-editor-save-error">{stepError}</p> : null}
        </div>

        {saveError ? <p className="ic-editor-save-error ic-editor-save-error--foot">{saveError}</p> : null}

        <footer className="ic-modal-foot ic-modal-foot--wizard">
          <button type="button" className="lit-btn-secondary" onClick={onClose} disabled={saving}>
            {t('bilgiKartlariPage.editor.cancel')}
          </button>
          <div className="ic-wizard-foot-actions">
            <button
              type="button"
              className="lit-btn-secondary"
              onClick={goBack}
              disabled={saving || wizardStep === 0}
            >
              {t('bilgiKartlariPage.editor.wizard.back')}
            </button>
            {wizardStep === 'preview' ? (
              <button type="button" className="lit-btn-primary" onClick={() => void handleSubmit()} disabled={saving}>
                {saving ? t('bilgiKartlariPage.editor.saving') : t('bilgiKartlariPage.editor.wizard.saveAll')}
              </button>
            ) : (
              <button type="button" className="lit-btn-primary" onClick={goNext} disabled={saving}>
                {wizardStep >= localeOrder.length - 1
                  ? t('bilgiKartlariPage.editor.wizard.toPreview')
                  : t('bilgiKartlariPage.editor.wizard.next')}
              </button>
            )}
          </div>
        </footer>
      </div>
    </div>,
    document.body,
  )
}
