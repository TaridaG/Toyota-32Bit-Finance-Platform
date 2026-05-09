import { useTranslation } from 'react-i18next'
import { usePortalUserMetrics } from '../../features/admin/hooks/usePortalUserMetrics'
import { formatAdminInteger, formatWowPercent, sparklineFromDailyCounts } from '../../features/admin/formatAdminNumbers'
import { KpiSparkline } from './AdminOverviewKpiSparkline'

function wowDeltaToneClass(pct: number): string {
  if (pct > 0.0001) return 'fi-admin-total-users-stat-value--up'
  if (pct < -0.0001) return 'fi-admin-total-users-stat-value--down'
  return 'fi-admin-total-users-stat-value--flat'
}

export function AdminTotalUsersPage() {
  const { t, i18n } = useTranslation('admin')
  const { state, refetch } = usePortalUserMetrics()

  const fmtTime = (iso: string) =>
    new Date(iso).toLocaleString(i18n.language, { dateStyle: 'medium', timeStyle: 'medium' })

  return (
    <div className="fi-admin-page fi-admin-total-users">
      <header className="fi-admin-page-head">
        <div>
          <h1 className="fi-admin-h1">{t('dashboard.kpi.totalUsers')}</h1>
          <p className="fi-admin-lead">{t('sectionPages.kpiTotalUsers')}</p>
        </div>
        <div className="fi-admin-total-users-head-actions">
          {state.status === 'ok' && (
            <span className="fi-admin-total-users-meta">
              {t('totalUsersPage.refreshed', { time: fmtTime(state.data.generatedAt) })}
            </span>
          )}
          <button type="button" className="fi-admin-dash-refresh" onClick={() => void refetch()}>
            {t('dashboard.refresh')}
          </button>
        </div>
      </header>

      {state.status === 'loading' && (
        <div className="fi-admin-total-users-panel fi-admin-total-users-panel--loading" aria-busy>
          <div className="fi-admin-total-users-skel fi-admin-total-users-skel--hero" />
          <div className="fi-admin-total-users-grid">
            <div className="fi-admin-total-users-skel" />
            <div className="fi-admin-total-users-skel" />
            <div className="fi-admin-total-users-skel" />
          </div>
        </div>
      )}

      {state.status === 'error' && (
        <section className="fi-admin-card fi-admin-total-users-error">
          <p>{t('totalUsersPage.loadError')}</p>
          <button type="button" className="fi-admin-dash-refresh" onClick={() => void refetch()}>
            {t('totalUsersPage.retry')}
          </button>
        </section>
      )}

      {state.status === 'ok' && (
        <>
          <section className="fi-admin-total-users-hero">
            <div className="fi-admin-total-users-hero-value">
              {formatAdminInteger(state.data.totalUsers, i18n.language)}
            </div>
            <p className="fi-admin-total-users-hero-hint">{t('totalUsersPage.rosterDefinition')}</p>
          </section>

          <div className="fi-admin-total-users-grid">
            <section className="fi-admin-total-users-stat">
              <h2 className="fi-admin-total-users-stat-label">{t('totalUsersPage.newLast7')}</h2>
              <p className="fi-admin-total-users-stat-value">
                {formatAdminInteger(state.data.newUsersLast7Days, i18n.language)}
              </p>
            </section>
            <section className="fi-admin-total-users-stat">
              <h2 className="fi-admin-total-users-stat-label">{t('totalUsersPage.newPrev7')}</h2>
              <p className="fi-admin-total-users-stat-value">
                {formatAdminInteger(state.data.newUsersPrevious7Days, i18n.language)}
              </p>
            </section>
            <section className="fi-admin-total-users-stat">
              <h2 className="fi-admin-total-users-stat-label">{t('totalUsersPage.wowMomentum')}</h2>
              <p
                className={`fi-admin-total-users-stat-value fi-admin-total-users-stat-value--delta ${wowDeltaToneClass(
                  state.data.newUsersWeekOverWeekPercent,
                )}`}
              >
                {formatWowPercent(state.data.newUsersWeekOverWeekPercent, i18n.language)}
              </p>
            </section>
          </div>

          <section className="fi-admin-total-users-chart-card">
            <h2 className="fi-admin-total-users-chart-title">{t('totalUsersPage.dailySignupsTitle')}</h2>
            <p className="fi-admin-total-users-chart-sub">{t('totalUsersPage.dailySignupsSub')}</p>
            <div className="fi-admin-total-users-chart-spark">
              <KpiSparkline
                values={sparklineFromDailyCounts(state.data.newRegistrationsDailyLast7Utc)}
                accent="blue"
              />
            </div>
          </section>
        </>
      )}
    </div>
  )
}
