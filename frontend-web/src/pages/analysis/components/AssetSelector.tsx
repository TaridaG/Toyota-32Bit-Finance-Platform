import { useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import type { MarketCategory } from '../../../shared/types/market'
import { ANALYSIS_MARKET_CATEGORIES } from '../utils/analysisCatalog'
import type { AssetDefinition } from '../types'

type AssetSelectorProps = {
  assets: AssetDefinition[]
  selectedAssetId: string
  instrumentCategory: MarketCategory
  onInstrumentCategoryChange: (category: MarketCategory) => void
  onAssetChange: (assetId: string) => void
  catalogLoading?: boolean
  variant?: 'default' | 'deck' | 'popover'
}

export function AssetSelector({
  assets,
  selectedAssetId,
  instrumentCategory,
  onInstrumentCategoryChange,
  onAssetChange,
  catalogLoading = false,
  variant = 'default',
}: AssetSelectorProps) {
  const { t } = useTranslation(['analysis', 'marketsPage'])
  const [query, setQuery] = useState('')

  const instrumentOptions = useMemo(() => {
    const lowered = query.trim().toLowerCase()
    let base = [...assets].sort((a, b) => a.symbol.localeCompare(b.symbol))
    if (lowered.length > 0) {
      base = base.filter(
        (asset) =>
          asset.symbol.toLowerCase().includes(lowered) || asset.name.toLowerCase().includes(lowered),
      )
    }
    if (selectedAssetId && !base.some((a) => a.id === selectedAssetId)) {
      const current = assets.find((a) => a.id === selectedAssetId)
      if (current) {
        base = [current, ...base]
      }
    }
    return base
  }, [assets, query, selectedAssetId])

  const categoryOptions = ANALYSIS_MARKET_CATEGORIES.filter((c) => c !== 'eurobond')

  const isDeck = variant === 'deck'
  const isPopover = variant === 'popover'

  const categorySelect = (
    <select
      className={isPopover ? 'fi-asset-selector-popover-select' : undefined}
      value={instrumentCategory}
      onChange={(event) => onInstrumentCategoryChange(event.target.value as MarketCategory)}
      disabled={catalogLoading}
    >
      {categoryOptions.map((cat) => (
        <option key={cat} value={cat}>
          {t(`marketsPage:categories.${cat}`)}
        </option>
      ))}
    </select>
  )

  const instrumentSelect = (
    <select
      className={isPopover ? 'fi-asset-selector-popover-select' : undefined}
      value={selectedAssetId}
      onChange={(event) => onAssetChange(event.target.value)}
      disabled={catalogLoading || instrumentOptions.length === 0}
    >
      {catalogLoading ? (
        <option value="">{t('common:loading')}</option>
      ) : instrumentOptions.length === 0 ? (
        <option value="">{t('assetSelector.noMatches')}</option>
      ) : (
        instrumentOptions.map((asset) => (
          <option key={asset.id} value={asset.id}>
            {asset.symbol} — {asset.name}
          </option>
        ))
      )}
    </select>
  )

  if (isPopover) {
    return (
      <section className="fi-asset-selector fi-asset-selector--popover fi-asset-selector--popover-bar">
        <div className="fi-asset-selector-popover-toolbar" role="group" aria-label={t('assetSelector.popoverToolbarAria')}>
          <div className="fi-asset-selector-popover-field fi-asset-selector-popover-field--search">
            <label className="fi-asset-selector-label" htmlFor="fi-analysis-instrument-search-pop">
              {t('assetSelector.deckSymbolLabel')}
            </label>
            <div className="fi-asset-search-wrap">
              <span className="fi-asset-search-ico" aria-hidden="true">
                <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <circle cx="11" cy="11" r="8" />
                  <line x1="21" y1="21" x2="16.65" y2="16.65" />
                </svg>
              </span>
              <input
                id="fi-analysis-instrument-search-pop"
                type="search"
                className="fi-asset-search-input fi-asset-search-input--popover-bar"
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                placeholder={t('assetSelector.deckSearchPlaceholder')}
                autoComplete="off"
              />
            </div>
          </div>
          <div className="fi-asset-selector-popover-field fi-asset-selector-popover-field--category">
            <label className="fi-asset-selector-label" htmlFor="fi-analysis-instrument-category-pop">
              {t('assetSelector.deckCategoryLabel')}
            </label>
            <select
              id="fi-analysis-instrument-category-pop"
              className="fi-asset-selector-popover-select"
              value={instrumentCategory}
              onChange={(event) => onInstrumentCategoryChange(event.target.value as MarketCategory)}
              disabled={catalogLoading}
            >
              {categoryOptions.map((cat) => (
                <option key={cat} value={cat}>
                  {t(`marketsPage:categories.${cat}`)}
                </option>
              ))}
            </select>
          </div>
          <div className="fi-asset-selector-popover-field fi-asset-selector-popover-field--symbol">
            <label className="fi-asset-selector-label" htmlFor="fi-analysis-instrument-select-pop">
              {t('assetSelector.deckChartSymbol')}
            </label>
            <select
              id="fi-analysis-instrument-select-pop"
              className="fi-asset-selector-popover-select"
              value={selectedAssetId}
              onChange={(event) => onAssetChange(event.target.value)}
              disabled={catalogLoading || instrumentOptions.length === 0}
            >
              {catalogLoading ? (
                <option value="">{t('common:loading')}</option>
              ) : instrumentOptions.length === 0 ? (
                <option value="">{t('assetSelector.noMatches')}</option>
              ) : (
                instrumentOptions.map((asset) => (
                  <option key={asset.id} value={asset.id}>
                    {asset.symbol} — {asset.name}
                  </option>
                ))
              )}
            </select>
          </div>
        </div>
        <p className="fi-asset-selector-popover-meta" aria-live="polite">
          {catalogLoading
            ? t('common:loading')
            : t('assetSelector.deckTotalCount', { count: instrumentOptions.length })}
        </p>
      </section>
    )
  }

  if (!isDeck) {
    return (
      <section className="fi-asset-selector fi-toolbar-panel">
        <div className="fi-asset-selector-row">
          <label className="fi-asset-selector-label" htmlFor="fi-analysis-instrument-search">
            {t('assetSelector.instrumentLabel')}
          </label>
          <input
            id="fi-analysis-instrument-search"
            type="search"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder={t('assetSelector.searchPlaceholder')}
            autoComplete="off"
          />
          <span className="fi-asset-selector-count" aria-live="polite">
            {t('assetSelector.instrumentCount', { count: instrumentOptions.length })}
          </span>
        </div>
        <div className="fi-asset-selector-row">
          <label className="fi-asset-selector-label" htmlFor="fi-analysis-instrument-category">
            {t('assetSelector.deckCategoryLabel')}
          </label>
          <select
            id="fi-analysis-instrument-category"
            value={instrumentCategory}
            onChange={(event) => onInstrumentCategoryChange(event.target.value as MarketCategory)}
            disabled={catalogLoading}
          >
            {categoryOptions.map((cat) => (
              <option key={cat} value={cat}>
                {t(`marketsPage:categories.${cat}`)}
              </option>
            ))}
          </select>
        </div>
        <div className="fi-asset-selector-row">
          <label className="fi-asset-selector-label" htmlFor="fi-analysis-instrument-select">
            {t('assetSelector.activeInstrument')}
          </label>
          <select
            id="fi-analysis-instrument-select"
            value={selectedAssetId}
            onChange={(event) => onAssetChange(event.target.value)}
            disabled={catalogLoading || instrumentOptions.length === 0}
          >
            {catalogLoading ? (
              <option value="">{t('common:loading')}</option>
            ) : instrumentOptions.length === 0 ? (
              <option value="">{t('assetSelector.noMatches')}</option>
            ) : (
              instrumentOptions.map((asset) => (
                <option key={asset.id} value={asset.id}>
                  {asset.symbol} — {asset.name}
                </option>
              ))
            )}
          </select>
        </div>
      </section>
    )
  }

  return (
    <section className="fi-asset-selector fi-asset-selector--deck fi-toolbar-panel">
      <div className="fi-analysis-step-head">
        <span className="fi-analysis-step-num">1</span>
        <h3 className="fi-analysis-step-title">{t('deck.instrumentTitle')}</h3>
      </div>
      <div className="fi-asset-selector-row fi-asset-selector-row--search">
        <label className="fi-asset-selector-label" htmlFor="fi-analysis-instrument-search-deck">
          {t('assetSelector.deckSymbolLabel')}
        </label>
        <div className="fi-asset-search-wrap">
          <span className="fi-asset-search-ico" aria-hidden="true">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <circle cx="11" cy="11" r="8" />
              <line x1="21" y1="21" x2="16.65" y2="16.65" />
            </svg>
          </span>
          <input
            id="fi-analysis-instrument-search-deck"
            type="search"
            className="fi-asset-search-input"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder={t('assetSelector.deckSearchPlaceholder')}
            autoComplete="off"
          />
        </div>
        <span className="fi-asset-selector-count fi-asset-selector-count--inline" aria-live="polite">
          {catalogLoading
            ? t('common:loading')
            : t('assetSelector.deckTotalCount', { count: instrumentOptions.length })}
        </span>
      </div>
      <div className="fi-asset-selector-row">
        <label className="fi-asset-selector-label" htmlFor="fi-analysis-instrument-category-deck">
          {t('assetSelector.deckCategoryLabel')}
        </label>
        {categorySelect}
      </div>
      <div className="fi-asset-selector-row">
        <label className="fi-asset-selector-label" htmlFor="fi-analysis-instrument-select-deck">
          {t('assetSelector.deckChartSymbol')}
        </label>
        {instrumentSelect}
      </div>
    </section>
  )
}
