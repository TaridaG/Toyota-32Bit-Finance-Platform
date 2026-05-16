import { useTranslation } from 'react-i18next'
import type { InfoCardsDashboard as DashboardData } from '../../../types/infoCards'
import { PORTAL_PAGES } from '../../../data/portalPages'

type InfoCardsDashboardProps = {
  data: DashboardData
}

export function InfoCardsDashboard({ data }: InfoCardsDashboardProps) {
  const { t } = useTranslation('common')
  const mostPageLabel = data.mostCoveredPage
    ? t(PORTAL_PAGES.find((p) => p.key === data.mostCoveredPage)?.labelKey ?? '')
    : '—'

  const tiles = [
    { label: t('bilgiKartlariPage.dashboard.active'), value: data.activeCards },
    { label: t('bilgiKartlariPage.dashboard.passive'), value: data.passiveCards },
    { label: t('bilgiKartlariPage.dashboard.avgWords'), value: data.averageWordCount },
    { label: t('bilgiKartlariPage.dashboard.coveredPages'), value: data.coveredPages },
    { label: t('bilgiKartlariPage.dashboard.mostPage'), value: mostPageLabel },
    { label: t('bilgiKartlariPage.dashboard.beginner'), value: data.beginnerCount },
    { label: t('bilgiKartlariPage.dashboard.intermediate'), value: data.intermediateCount },
    { label: t('bilgiKartlariPage.dashboard.advanced'), value: data.advancedCount },
  ]

  return (
    <div className="ic-dashboard-grid">
      {tiles.map((tile) => (
        <article key={tile.label} className="ic-dashboard-tile card">
          <span className="ic-dashboard-tile-label">{tile.label}</span>
          <strong className="ic-dashboard-tile-value">{tile.value}</strong>
        </article>
      ))}
    </div>
  )
}
