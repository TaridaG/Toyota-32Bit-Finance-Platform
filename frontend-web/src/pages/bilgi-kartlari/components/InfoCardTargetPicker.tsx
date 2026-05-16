import { useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { PORTAL_PAGES } from '../../../data/portalPages'
import { getPortalPageElementsByIds } from '../../../data/portalPageElements'
import type { PortalPageKey } from '../../../types/infoCards'
import {
  matchKeysFromElementIds,
  searchPortalPageElementGroups,
} from '../utils/searchPortalPageElements'

type InfoCardTargetPickerProps = {
  selectedPages: PortalPageKey[]
  selectedElementIds: string[]
  onChange: (ids: string[]) => void
}

export function InfoCardTargetPicker({
  selectedPages,
  selectedElementIds,
  onChange,
}: InfoCardTargetPickerProps) {
  const { t } = useTranslation('common')
  const [query, setQuery] = useState('')

  const selectedMatchKeys = useMemo(() => matchKeysFromElementIds(selectedElementIds), [selectedElementIds])

  const groups = useMemo(
    () => searchPortalPageElementGroups(query, selectedPages),
    [query, selectedPages],
  )

  const selectedElements = useMemo(
    () => getPortalPageElementsByIds(selectedElementIds),
    [selectedElementIds],
  )

  const toggleGroup = (matchKey: string) => {
    const group = groups.find((g) => g.matchKey === matchKey)
    if (!group) {
      return
    }
    const isSelected = selectedMatchKeys.includes(matchKey)
    if (isSelected) {
      const removeIds = new Set(group.elements.map((e) => e.id))
      onChange(selectedElementIds.filter((id) => !removeIds.has(id)))
      return
    }
    const next = new Set(selectedElementIds)
    for (const element of group.elements) {
      next.add(element.id)
    }
    onChange([...next])
  }

  const removeMatchKey = (matchKey: string) => {
    const removeIds = new Set(
      selectedElements.filter((e) => e.matchKey === matchKey).map((e) => e.id),
    )
    onChange(selectedElementIds.filter((id) => !removeIds.has(id)))
  }

  if (selectedPages.length === 0) {
    return (
      <p className="ic-target-picker-hint">{t('bilgiKartlariPage.editor.selectPagesFirst')}</p>
    )
  }

  return (
    <fieldset className="ic-field ic-target-picker">
      <legend>{t('bilgiKartlariPage.editor.targetElements')}</legend>
      <p className="ic-target-picker-lead">{t('bilgiKartlariPage.editor.targetElementsLead')}</p>

      <input
        type="search"
        className="ic-target-search"
        value={query}
        onChange={(e) => setQuery(e.target.value)}
        placeholder={t('bilgiKartlariPage.editor.targetElementsSearch')}
      />

      <div className="ic-target-results" role="listbox" aria-label={t('bilgiKartlariPage.editor.targetElementsResults')}>
        {groups.length === 0 ? (
          <p className="ic-target-empty">{t('bilgiKartlariPage.editor.targetElementsEmpty')}</p>
        ) : (
          groups.map((group) => {
            const selected = selectedMatchKeys.includes(group.matchKey)
            return (
              <button
                key={group.matchKey}
                type="button"
                role="option"
                aria-selected={selected}
                className={`ic-target-result${selected ? ' ic-target-result-selected' : ''}`}
                onClick={() => toggleGroup(group.matchKey)}
              >
                <span className="ic-target-result-main">
                  <span className="ic-target-result-label">{group.label}</span>
                  <span className="ic-target-result-meta">
                    {t(`bilgiKartlariPage.editor.elementKinds.${group.kind}`)}
                    {group.section ? ` · ${group.section}` : ''}
                  </span>
                </span>
                <span className="ic-target-result-badges">
                  {group.sharedAcrossSelectedPages ? (
                    <span className="ic-target-badge ic-target-badge-shared">
                      {t('bilgiKartlariPage.editor.sharedAcrossPages', { count: selectedPages.length })}
                    </span>
                  ) : null}
                  {group.elements.map((element) => (
                    <span key={element.id} className="ic-target-badge">
                      {t(PORTAL_PAGES.find((p) => p.key === element.pageKey)?.labelKey ?? '')}
                    </span>
                  ))}
                </span>
              </button>
            )
          })
        )}
      </div>

      {selectedMatchKeys.length > 0 ? (
        <div className="ic-target-selected">
          <p className="ic-target-selected-title">{t('bilgiKartlariPage.editor.selectedElements')}</p>
          <ul className="ic-target-selected-list">
            {selectedMatchKeys.map((matchKey) => {
              const items = selectedElements.filter((e) => e.matchKey === matchKey)
              const primary = items[0]
              if (!primary) {
                return null
              }
              return (
                <li key={matchKey} className="ic-target-selected-item">
                  <span>
                    <strong>{primary.label}</strong>
                    <span className="ic-target-selected-pages">
                      {items.map((e) => t(PORTAL_PAGES.find((p) => p.key === e.pageKey)?.labelKey ?? '')).join(' · ')}
                    </span>
                  </span>
                  <button
                    type="button"
                    className="ic-target-remove"
                    aria-label={t('bilgiKartlariPage.editor.removeElement', { label: primary.label })}
                    onClick={() => removeMatchKey(matchKey)}
                  >
                    ×
                  </button>
                </li>
              )
            })}
          </ul>
        </div>
      ) : null}
    </fieldset>
  )
}
