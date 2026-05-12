import { Fragment } from 'react'
import { useTranslation } from 'react-i18next'
import type { UserAnalyticsPresetParam, UserAnalyticsQuery } from '../../features/admin/api/adminUserAnalyticsApi'
import type { AdminUserAnalyticsState } from '../../features/admin/hooks/useAdminUserAnalytics'
import { formatAdminInteger, formatWowPercent } from '../../features/admin/formatAdminNumbers'
import { AdminRegistrationsComboChart } from './AdminRegistrationsComboChart'

type Props = {
  query: UserAnalyticsQuery
  onSelectPreset: (p: UserAnalyticsPresetParam) => void
  onSelectCustomDefault: () => void
  customFrom: string
  customTo: string
  onCustomFromChange: (v: string) => void
  onCustomToChange: (v: string) => void
  onApplyCustomRange: () => void
  rangeError: string | null
  analytics: AdminUserAnalyticsState
  onRefresh: () => void
}

function wowToneClass(pct: number): string {
  if (pct > 0.0001) return 'fi-admin-ua-delta--up'
  if (pct < -0.0001) return 'fi-admin-ua-delta--down'
  return 'fi-admin-ua-delta--flat'
}

function formatUtcRangeLabel(fromIso: string, toExclusiveIso: string, locale: string): string {
  const start = new Date(fromIso)
  const endEx = new Date(toExclusiveIso)
  const end = new Date(endEx.getTime() - 1)
  const o: Intl.DateTimeFormatOptions = { dateStyle: 'medium', timeZone: 'UTC' }
  return `${start.toLocaleDateString(locale, o)} \u2013 ${end.toLocaleDateString(locale, o)} (UTC)`
}

function formatWhen(iso: string, locale: string): string {
  return new Date(iso).toLocaleString(locale, { dateStyle: 'medium', timeStyle: 'medium', timeZone: 'UTC' })
}

export function AdminUserAnalyticsEnterprise({
  query,
  onSelectPreset,
  onSelectCustomDefault,
  customFrom,
  customTo,
  onCustomFromChange,
  onCustomToChange,
  onApplyCustomRange,
  rangeError,
  analytics,
  onRefresh,
}: Props) {
  const { t, i18n } = useTranslation('admin')
  const locale = i18n.language
  const isCustom = query.kind === 'custom'

  const presets: { id: UserAnalyticsPresetParam; labelKey: string }[] = [
    { id: '7d', labelKey: 'totalUsersPage.analytics.range7' },
    { id: '14d', labelKey: 'totalUsersPage.analytics.range14' },
    { id: '30d', labelKey: 'totalUsersPage.analytics.range30' },
  ]

  const rangeLabel =
    analytics.status === 'ok'
      ? formatUtcRangeLabel(
          analytics.data.chartRangeStartUtcInclusive,
          analytics.data.chartRangeEndUtcExclusive,
          locale,
        )
      : ''

  return (
    <Fragment>
      <header className="fi-admin-page-head">
        <div>
          <h1 className="fi-admin-h1">{t('dashboard.kpi.totalUsers')}</h1>
        </div>
      </header>

      {analytics.status === 'loading' ? (
        <div className="fi-admin-ua-skeleton-root" aria-busy>
          <div className="fi-admin-ua-skel-grid5">
            <div className="fi-admin-ua-skel" />
            <div className="fi-admin-ua-skel" />
            <div className="fi-admin-ua-skel" />
            <div className="fi-admin-ua-skel" />
            <div className="fi-admin-ua-skel" />
          </div>
          <div className="fi-admin-ua-skel fi-admin-ua-skel--chart" />
        </div>
      ) : null}

      {analytics.status === 'error' ? (
        <section className="fi-admin-card fi-admin-ua-error">
          <p>{analytics.message}</p>
          <button type="button" className="fi-admin-dash-refresh" onClick={() => void onRefresh()}>
            {t('totalUsersPage.retry')}
          </button>
        </section>
      ) : null}

      {analytics.status === 'ok' ? (
        <div className="fi-admin-ua-root">
          <div className="fi-admin-ua-kpi-row">
            <section className="fi-admin-card fi-admin-ua-kpi fi-admin-ua-kpi--total">
              <div className="fi-admin-ua-kpi-head">
                <span className="fi-admin-ua-hero-kicker">{t('totalUsersPage.analytics.heroKicker')}</span>
              </div>
              <div className="fi-admin-ua-kpi-value-inline">
                <span className="fi-admin-ua-kpi-value fi-admin-ua-kpi-value--total">
                  {formatAdminInteger(analytics.data.summary.totalUsers, locale)}
                </span>
                <span
                  className={`fi-admin-ua-delta fi-admin-ua-delta--adjacent ${wowToneClass(analytics.data.summary.totalUsersVsPriorDayPercentApprox)}`}
                >
                  {formatWowPercent(analytics.data.summary.totalUsersVsPriorDayPercentApprox, locale)}
                </span>
              </div>
            </section>
            <section className="fi-admin-card fi-admin-ua-kpi">
              <div className="fi-admin-ua-kpi-head">
                <span className="fi-admin-ua-kpi-title">{t('totalUsersPage.analytics.kpiLastWeek')}</span>
              </div>
              <p className="fi-admin-ua-kpi-value">
                {formatAdminInteger(analytics.data.summary.newUsersPreviousIsoWeekUtc, locale)}
              </p>
            </section>
            <section className="fi-admin-card fi-admin-ua-kpi">
              <div className="fi-admin-ua-kpi-head">
                <span className="fi-admin-ua-kpi-title">{t('totalUsersPage.analytics.kpiLastMonth')}</span>
              </div>
              <p className="fi-admin-ua-kpi-value">
                {formatAdminInteger(analytics.data.summary.newUsersPreviousCalendarMonthUtc, locale)}
              </p>
            </section>
            <section className="fi-admin-card fi-admin-ua-kpi fi-admin-ua-kpi--placeholder">
              <div className="fi-admin-ua-kpi-head">
                <span className="fi-admin-ua-kpi-title">{t('totalUsersPage.analytics.kpiDeleted')}</span>
              </div>
              <p className="fi-admin-ua-kpi-value fi-admin-ua-kpi-value--muted">—</p>
            </section>
            <section className="fi-admin-card fi-admin-ua-kpi fi-admin-ua-kpi--placeholder">
              <div className="fi-admin-ua-kpi-head">
                <span className="fi-admin-ua-kpi-title">{t('totalUsersPage.analytics.kpiFrozen')}</span>
              </div>
              <p className="fi-admin-ua-kpi-value fi-admin-ua-kpi-value--muted">—</p>
            </section>
          </div>

          <section className="fi-admin-card fi-admin-ua-chart-card">
            <h2 className="fi-admin-h2 fi-admin-ua-block-title">{t('totalUsersPage.analytics.chartTitle')}</h2>
            <div className="fi-admin-ua-chart-toolbar">
              <div className="fi-admin-ua-range" role="group" aria-label={t('totalUsersPage.analytics.rangeAria')}>
                {presets.map((p) => (
                  <button
                    key={p.id}
                    type="button"
                    className={`fi-admin-ua-chip${query.kind === 'preset' && query.preset === p.id ? ' fi-admin-ua-chip--active' : ''}`}
                    onClick={() => onSelectPreset(p.id)}
                  >
                    {t(p.labelKey)}
                  </button>
                ))}
                <button
                  type="button"
                  className={`fi-admin-ua-chip${isCustom ? ' fi-admin-ua-chip--active' : ''}`}
                  onClick={() => onSelectCustomDefault()}
                >
                  {t('totalUsersPage.analytics.customRange')}
                </button>
              </div>
              <div className="fi-admin-ua-chart-meta">
                <span className="fi-admin-ua-meta-line" title={t('totalUsersPage.analytics.rangeUtcTitle')}>
                  {rangeLabel}
                </span>
                <span className="fi-admin-ua-meta-line">
                  {t('totalUsersPage.analytics.dataTime', { time: formatWhen(analytics.data.generatedAt, locale) })}
                </span>
              </div>
            </div>

            {isCustom ? (
              <div className="fi-admin-ua-custom-row">
                <label className="fi-admin-ua-date-field">
                  <span className="fi-admin-ua-date-label">{t('totalUsersPage.analytics.customFrom')}</span>
                  <input
                    type="date"
                    className="fi-admin-ua-date-input"
                    value={customFrom}
                    onChange={(e) => onCustomFromChange(e.target.value)}
                  />
                </label>
                <label className="fi-admin-ua-date-field">
                  <span className="fi-admin-ua-date-label">{t('totalUsersPage.analytics.customTo')}</span>
                  <input
                    type="date"
                    className="fi-admin-ua-date-input"
                    value={customTo}
                    onChange={(e) => onCustomToChange(e.target.value)}
                  />
                </label>
                <button type="button" className="fi-admin-ua-apply" onClick={() => onApplyCustomRange()}>
                  {t('totalUsersPage.analytics.customApply')}
                </button>
              </div>
            ) : null}
            {rangeError ? (
              <p className="fi-admin-ua-range-error" role="alert">
                {rangeError}
              </p>
            ) : null}

            <div className="fi-admin-ua-chart-legend">
              <span className="fi-admin-ua-leg fi-admin-ua-leg--bars">{t('totalUsersPage.analytics.legendNew')}</span>
              <span className="fi-admin-ua-leg fi-admin-ua-leg--line">{t('totalUsersPage.analytics.legendAvg')}</span>
            </div>
            <AdminRegistrationsComboChart rows={analytics.data.dailyRegistrations} locale={locale} />
          </section>
        </div>
      ) : null}
    </Fragment>
  )
}
