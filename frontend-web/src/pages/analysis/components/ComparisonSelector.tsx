import type { AssetDefinition } from '../types'
import { useTranslation } from 'react-i18next'

type ComparisonSelectorProps = {
  assets: AssetDefinition[]
  selected: string[]
  onToggle: (assetId: string) => void
}

export function ComparisonSelector({ assets, selected, onToggle }: ComparisonSelectorProps) {
  const { t } = useTranslation('analysis')
  return (
    <section className="fi-comparison-selector card">
      <h3>{t('compareTitle')}</h3>
      <div>
        {assets.map((asset) => (
          <button
            key={asset.id}
            type="button"
            className={`fi-compare-chip${selected.includes(asset.id) ? ' fi-compare-chip-active' : ''}`}
            onClick={() => onToggle(asset.id)}
          >
            {asset.symbol}
          </button>
        ))}
      </div>
    </section>
  )
}
