import type { AssetDefinition } from '../types'

type ComparisonSelectorProps = {
  assets: AssetDefinition[]
  selected: string[]
  onToggle: (assetId: string) => void
}

export function ComparisonSelector({ assets, selected, onToggle }: ComparisonSelectorProps) {
  return (
    <section className="fi-comparison-selector card">
      <h3>Compare</h3>
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
