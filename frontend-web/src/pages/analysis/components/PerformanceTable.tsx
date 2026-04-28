import type { AssetDefinition, TimeRange } from '../types'
import { useTranslation } from 'react-i18next'

type PerformanceRow = {
  assetId: string
  daily: number
  weekly: number
  monthly: number
  yearly: number
}

type PerformanceTableProps = {
  titleRange: TimeRange
  assets: AssetDefinition[]
  rows: PerformanceRow[]
}

export function PerformanceTable({ titleRange, assets, rows }: PerformanceTableProps) {
  const { t } = useTranslation('analysis')
  const byId = new Map(assets.map((asset) => [asset.id, asset]))

  return (
    <article className="card fi-performance-table">
      <div className="fi-panel-head">
        <h3>{t('performanceTableTitle')}</h3>
        <small>{t('syncedWithRange', { range: titleRange })}</small>
      </div>
      <table>
        <thead>
          <tr>
            <th>{t('columns.asset')}</th>
            <th>{t('columns.daily')}</th>
            <th>{t('columns.weekly')}</th>
            <th>{t('columns.monthly')}</th>
            <th>{t('columns.yearly')}</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row) => (
            <tr key={row.assetId}>
              <td>{byId.get(row.assetId)?.symbol ?? row.assetId}</td>
              <td className={row.daily >= 0 ? 'fi-up' : 'fi-down'}>{row.daily.toFixed(2)}%</td>
              <td className={row.weekly >= 0 ? 'fi-up' : 'fi-down'}>{row.weekly.toFixed(2)}%</td>
              <td className={row.monthly >= 0 ? 'fi-up' : 'fi-down'}>{row.monthly.toFixed(2)}%</td>
              <td className={row.yearly >= 0 ? 'fi-up' : 'fi-down'}>{row.yearly.toFixed(2)}%</td>
            </tr>
          ))}
        </tbody>
      </table>
    </article>
  )
}
