import { useEffect, useState } from 'react'
import { createPortal } from 'react-dom'
import { useTranslation } from 'react-i18next'
import { PORTAL_PAGES } from '../../../data/portalPages'
import { getPortalPageElementsByIds } from '../../../data/portalPageElements'
import type { InfoCard, InfoCardInput } from '../../../types/infoCards'
import {
  INFO_CARD_CATEGORIES,
  INFO_CARD_DIFFICULTIES,
  INFO_CARD_TYPES,
} from '../constants'
import { termsFromElementIds } from '../utils/searchPortalPageElements'

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
}

type InfoCardEditorDrawerProps = {
  open: boolean
  initial: InfoCard | null
  defaultPage: InfoCard['pages'][number]
  pickPrefill?: InfoCardEditorPickPrefill | null
  onClose: () => void
  onSave: (input: InfoCardInput, id?: string) => void | Promise<void>
}

function splitCsv(value: string): string[] {
  return value
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)
}

export function InfoCardEditorDrawer({
  open,
  initial,
  defaultPage,
  pickPrefill,
  onClose,
  onSave,
}: InfoCardEditorDrawerProps) {
  const { t } = useTranslation('common')
  const [form, setForm] = useState<InfoCardInput>(EMPTY_FORM)
  const [targetElementIds, setTargetElementIds] = useState<string[]>([])
  const [relatedTermsText, setRelatedTermsText] = useState('')
  const [instrumentSymbolsText, setInstrumentSymbolsText] = useState('')
  const [saveError, setSaveError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    if (!open) {
      return
    }
    setSaveError(null)
    setSaving(false)
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
      setTargetElementIds(initial.targetElementIds ?? [])
      setRelatedTermsText(initial.relatedTerms.join(', '))
      setInstrumentSymbolsText((initial.targetInstrumentSymbols ?? []).join(', '))
    } else if (pickPrefill) {
      const pages = pickPrefill.pages?.length ? pickPrefill.pages : [defaultPage]
      const terms = pickPrefill.targetTerms?.length ? pickPrefill.targetTerms : []
      const instruments = pickPrefill.targetInstrumentSymbols?.length
        ? pickPrefill.targetInstrumentSymbols
        : []
      const elementIds = pickPrefill.targetElementIds ?? []
      setForm({
        ...EMPTY_FORM,
        pages,
        title: pickPrefill.title ?? terms[0] ?? '',
        targetTerms: terms,
        targetInstrumentSymbols: instruments,
        slug: instruments[0] ? `asset-${instruments[0].toLowerCase()}` : undefined,
        type: instruments.length > 0 ? 'ASSET' : 'TERM',
        category: 'MARKET_DATA',
      })
      setTargetElementIds(elementIds)
      setRelatedTermsText('')
      setInstrumentSymbolsText(instruments.join(', '))
    } else {
      setForm({ ...EMPTY_FORM, pages: [defaultPage] })
      setTargetElementIds([])
      setRelatedTermsText('')
      setInstrumentSymbolsText('')
    }
  }, [open, initial, defaultPage, pickPrefill])

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

  if (!open) {
    return null
  }

  const isPickCreate = Boolean(pickPrefill && !initial)
  const isContentOnly = isPickCreate || Boolean(initial)

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

  const handleSubmit = async () => {
    if (!form.title.trim() || !form.shortDescription.trim()) {
      return
    }
    const termsFromElements = termsFromElementIds(targetElementIds)
    const targetInstrumentSymbols = isContentOnly
      ? (form.targetInstrumentSymbols ?? []).map((s) => s.toUpperCase())
      : splitCsv(instrumentSymbolsText).map((s) => s.toUpperCase())
    const targetTerms =
      termsFromElements.length > 0
        ? termsFromElements
        : targetInstrumentSymbols.length > 0
          ? targetInstrumentSymbols
          : [form.title.trim()]
    const slug =
      form.slug ??
      (targetInstrumentSymbols[0]
        ? `asset-${targetInstrumentSymbols[0].toLowerCase()}`
        : undefined)
    setSaveError(null)
    setSaving(true)
    try {
      await onSave(
        {
          ...form,
          slug,
          targetElementIds,
          targetInstrumentSymbols,
          targetTerms,
          type: targetInstrumentSymbols.length > 0 && form.type === 'TERM' ? 'ASSET' : form.type,
          relatedTerms: splitCsv(relatedTermsText),
        },
        initial?.id,
      )
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

  return createPortal(
    <div className="ic-modal-overlay" role="presentation">
      <button type="button" className="ic-modal-backdrop" aria-label={t('bilgiKartlariPage.editor.close')} onClick={onClose} />
      <div
        className="ic-modal card"
        role="dialog"
        aria-modal="true"
        aria-labelledby="ic-editor-modal-title"
        onClick={(e) => e.stopPropagation()}
      >
        <header className="ic-modal-head">
          <div>
            <h2 id="ic-editor-modal-title">
              {initial
                ? t('bilgiKartlariPage.editor.editTitle')
                : isPickCreate
                  ? t('bilgiKartlariPage.editor.pickTitle')
                  : t('bilgiKartlariPage.editor.newTitle')}
            </h2>
            {isPickCreate && pickPrefill?.pickLabel ? (
              <p className="ic-editor-pick-label">
                {t('bilgiKartlariPage.editor.pickedTarget', { label: pickPrefill.pickLabel })}
              </p>
            ) : null}
          </div>
          <button type="button" className="ic-modal-close" onClick={onClose} aria-label={t('bilgiKartlariPage.editor.close')}>
            ×
          </button>
        </header>
        <div className="ic-modal-body">
          <label className="ic-field">
            <span>{t('bilgiKartlariPage.editor.title')}</span>
            <input
              value={form.title}
              onChange={(e) => setForm((p) => ({ ...p, title: e.target.value }))}
            />
          </label>
          {isContentOnly ? (
            <div className="ic-editor-binding-summary" aria-live="polite">
              <p>
                <strong>{t('bilgiKartlariPage.editor.pages')}:</strong> {bindingPageLabels}
              </p>
              <p>
                <strong>{t('bilgiKartlariPage.editor.bindingTarget')}:</strong> {bindingTargetLabel}
              </p>
              {isPickCreate ? (
                <p className="ic-target-picker-lead">{t('bilgiKartlariPage.editor.pickBindingHint')}</p>
              ) : null}
            </div>
          ) : null}
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
          <fieldset className="ic-field ic-literacy-fields">
            <legend>{t('bilgiKartlariPage.editor.literacySection')}</legend>
            <p className="ic-target-picker-lead">{t('bilgiKartlariPage.editor.literacySectionLead')}</p>
          </fieldset>
          <label className="ic-field">
            <span>{t('bilgiKartlariPage.editor.shortDescription')}</span>
            <textarea
              rows={3}
              value={form.shortDescription}
              onChange={(e) => setForm((p) => ({ ...p, shortDescription: e.target.value }))}
            />
          </label>
          <label className="ic-field">
            <span>{t('bilgiKartlariPage.editor.detailedDescription')}</span>
            <textarea
              rows={4}
              value={form.detailedDescription}
              onChange={(e) => setForm((p) => ({ ...p, detailedDescription: e.target.value }))}
            />
          </label>
          <label className="ic-field">
            <span>{t('bilgiKartlariPage.editor.howToInterpret')}</span>
            <textarea
              rows={3}
              value={form.howToInterpret ?? ''}
              onChange={(e) => setForm((p) => ({ ...p, howToInterpret: e.target.value }))}
            />
          </label>
          <label className="ic-field">
            <span>{t('bilgiKartlariPage.editor.commonMistake')}</span>
            <textarea
              rows={2}
              value={form.commonMistake ?? ''}
              onChange={(e) => setForm((p) => ({ ...p, commonMistake: e.target.value }))}
            />
          </label>
          <label className="ic-field">
            <span>{t('bilgiKartlariPage.editor.relatedTerms')}</span>
            <input value={relatedTermsText} onChange={(e) => setRelatedTermsText(e.target.value)} />
          </label>
          <label className="ic-check">
            <input
              type="checkbox"
              checked={form.status === 'ACTIVE'}
              onChange={(e) => setForm((p) => ({ ...p, status: e.target.checked ? 'ACTIVE' : 'PASSIVE' }))}
            />
            <span>{t('bilgiKartlariPage.editor.active')}</span>
          </label>
        </div>
        {saveError ? <p className="ic-editor-save-error">{saveError}</p> : null}
        <footer className="ic-modal-foot">
          <button type="button" className="lit-btn-secondary" onClick={onClose} disabled={saving}>
            {t('bilgiKartlariPage.editor.cancel')}
          </button>
          <button type="button" className="lit-btn-primary" onClick={() => void handleSubmit()} disabled={saving}>
            {saving ? t('bilgiKartlariPage.editor.saving') : t('bilgiKartlariPage.editor.save')}
          </button>
        </footer>
      </div>
    </div>,
    document.body,
  )
}