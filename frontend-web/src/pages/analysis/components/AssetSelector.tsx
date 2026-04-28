import { useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import type { AssetDefinition, AssetType } from '../types'

type AssetSelectorProps = {
  assets: AssetDefinition[]
  selectedAssetId: string
  selectedAssetType: AssetType | 'all'
  onAssetChange: (assetId: string) => void
  onAssetTypeChange: (type: AssetType | 'all') => void
}

export function AssetSelector({
  assets,
  selectedAssetId,
  selectedAssetType,
  onAssetChange,
  onAssetTypeChange,
}: AssetSelectorProps) {
  const { t } = useTranslation('analysis')
  const [query, setQuery] = useState('')

  const filteredAssets = useMemo(() => {
    const lowered = query.trim().toLowerCase()
    return assets.filter((asset) => {
      const matchesType = selectedAssetType === 'all' || asset.type === selectedAssetType
      const matchesQuery =
        lowered.length === 0 ||
        asset.symbol.toLowerCase().includes(lowered) ||
        asset.name.toLowerCase().includes(lowered)
      return matchesType && matchesQuery
    })
  }, [assets, query, selectedAssetType])

  return (
    <section className="fi-asset-selector card">
      <input
        type="search"
        value={query}
        onChange={(event) => setQuery(event.target.value)}
        placeholder={t('assetSelector.searchPlaceholder')}
      />
      <select value={selectedAssetType} onChange={(event) => onAssetTypeChange(event.target.value as AssetType | 'all')}>
        <option value="all">{t('assetSelector.types.all')}</option>
        <option value="stock">{t('assetSelector.types.stock')}</option>
        <option value="crypto">{t('assetSelector.types.crypto')}</option>
        <option value="fx">{t('assetSelector.types.fx')}</option>
        <option value="commodity">{t('assetSelector.types.commodity')}</option>
        <option value="index">{t('assetSelector.types.index')}</option>
      </select>
      <select value={selectedAssetId} onChange={(event) => onAssetChange(event.target.value)}>
        {filteredAssets.map((asset) => (
          <option key={asset.id} value={asset.id}>
            {asset.symbol} - {asset.name}
          </option>
        ))}
      </select>
    </section>
  )
}
