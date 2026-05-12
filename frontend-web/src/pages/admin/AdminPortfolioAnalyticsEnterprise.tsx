import { Fragment, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import type { PortfolioAnalyticsPresetParam, PortfolioAnalyticsQuery } from '../../features/admin/api/adminPortfolioAnalyticsApi'
import type { AdminPortfolioAnalyticsState } from '../../features/admin/hooks/useAdminPortfolioAnalytics'
import { formatAdminDecimal, formatAdminInteger, formatWowPercent } from '../../features/admin/formatAdminNumbers'
import { AdminDailyComboChart } from './AdminDailyComboChart'

type Props = {
  query: PortfolioAnalyticsQuery
  onSelectPreset: (p: PortfolioAnalyticsPresetParam) => void
  onSelectCustomDefault: () => void
  customFrom: string
  customTo: string
  onCustomFromChange: (v: string) => void
  onCustomToChange: (v: string) => void
  onApplyCustomRange: () => void
  rangeError: string | null
  analytics: AdminPortfolioAnalyticsState
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

function formatCreatedAtCell(raw: string | null, locale: string): string {
  if (raw == null || raw.length === 0) return '—'
  const hasZone = raw.endsWith('Z') || /[+-]\d{2}:?\d{2}$/.test(raw)
  const iso = hasZone ? raw : `${raw.replace(' ', 'T')}Z`
  const t = Date.parse(iso)
  if (Number.isNaN(t)) return raw
  return new Date(t).toLocaleString(locale, { dateStyle: 'medium', timeStyle: 'short', timeZone: 'UTC' })
}

export function AdminPortfolioAnalyticsEnterprise({
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

  const presets: { id: PortfolioAnalyticsPresetParam; labelKey: string }[] = [
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

  const chartLabels = useMemo(
    () => ({
      empty: t('totalPortfolioPage.analytics.chartEmpty'),
      aria: t('totalPortfolioPage.analytics.chartAria'),
      tipPrimary: t('totalPortfolioPage.analytics.tipCreated'),
      tipAvg: t('totalPortfolioPage.analytics.tipAvg7'),
      tipDelta: t('totalPortfolioPage.analytics.tipDelta'),
    }),
    [t],
  )

  const comboRows =
    analytics.status === 'ok'
      ? analytics.data.dailyCreations.map((r) => ({
          date: r.date,
          barValue: r.portfoliosCreated,
          rollingAverage7d: r.rollingAverage7d,
          deltaVsPreviousDay: r.deltaVsPreviousDay,
        }))
      : []

  return (
    <Fragment>
      <header className="fi-admin-page-head">
        <div>
          <h1 className="fi-admin-h1">{t('dashboard.kpi.activePortfolios')}</h1>
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
          <div className="fi-admin-ua-skel fi-admin-ua-skel--table" />
        </div>
      ) : null}

      {analytics.status === 'error' ? (
        <section className="fi-admin-card fi-admin-ua-error">
          <p>{analytics.message}</p>
          <button type="button" className="fi-admin-dash-refresh" onClick={() => void onRefresh()}>
            {t('totalPortfolioPage.retry')}
          </button>
        </section>
      ) : null}

      {analytics.status === 'ok' ? (
        <div className="fi-admin-ua-root">
          <div className="fi-admin-ua-kpi-row">
            <section className="fi-admin-card fi-admin-ua-kpi fi-admin-ua-kpi--total">
              <div className="fi-admin-ua-kpi-head">
                <span className="fi-admin-ua-hero-kicker">{t('totalPortfolioPage.analytics.kpiTotal')}</span>
              </div>
              <div className="fi-admin-ua-kpi-value-inline">
                <span className="fi-admin-ua-kpi-value fi-admin-ua-kpi-value--total">
                  {formatAdminInteger(analytics.data.summary.totalPortfolios, locale)}
                </span>
                <span
                  className={`fi-admin-ua-delta fi-admin-ua-delta--adjacent ${wowToneClass(analytics.data.summary.totalPortfoliosVsPriorDayPercentApprox)}`}
                >
                  {formatWowPercent(analytics.data.summary.totalPortfoliosVsPriorDayPercentApprox, locale)}
                </span>
              </div>
            </section>
            <section className="fi-admin-card fi-admin-ua-kpi">
              <div className="fi-admin-ua-kpi-head">
                <span className="fi-admin-ua-kpi-title">{t('totalPortfolioPage.analytics.kpiLastWeek')}</span>
              </div>
              <p className="fi-admin-ua-kpi-value">
                {formatAdminInteger(analytics.data.summary.portfoliosCreatedPreviousIsoWeekUtc, locale)}
              </p>
            </section>
            <section className="fi-admin-card fi-admin-ua-kpi">
              <div className="fi-admin-ua-kpi-head">
                <span className="fi-admin-ua-kpi-title">{t('totalPortfolioPage.analytics.kpiAvgLots')}</span>
              </div>
              <p className="fi-admin-ua-kpi-value">
                {formatAdminDecimal(analytics.data.summary.averageOpenLotsPerPortfolio, locale, 2)}
              </p>
            </section>
            <section className="fi-admin-card fi-admin-ua-kpi">
              <div className="fi-admin-ua-kpi-head">
                <span className="fi-admin-ua-kpi-title">{t('totalPortfolioPage.analytics.kpiPerUser')}</span>
              </div>
              <p className="fi-admin-ua-kpi-value">
                {formatAdminDecimal(analytics.data.summary.portfoliosPerRosterUser, locale, 2)}
              </p>
            </section>
            <section className="fi-admin-card fi-admin-ua-kpi fi-admin-ua-kpi--placeholder">
              <div className="fi-admin-ua-kpi-head">
                <span className="fi-admin-ua-kpi-title">{t('totalPortfolioPage.analytics.kpiShared')}</span>
              </div>
              <p className="fi-admin-ua-kpi-value fi-admin-ua-kpi-value--muted">—</p>
            </section>
          </div>

          <section className="fi-admin-card fi-admin-ua-chart-card">
            <h2 className="fi-admin-h2 fi-admin-ua-block-title">{t('totalPortfolioPage.analytics.chartTitle')}</h2>
            <div className="fi-admin-ua-chart-toolbar">
              <div className="fi-admin-ua-range" role="group" aria-label={t('totalPortfolioPage.analytics.rangeAria')}>
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
              <span className="fi-admin-ua-leg fi-admin-ua-leg--bars">{t('totalPortfolioPage.analytics.legendDaily')}</span>
              <span className="fi-admin-ua-leg fi-admin-ua-leg--line">{t('totalPortfolioPage.analytics.legendAvg')}</span>
            </div>
            <AdminDailyComboChart rows={comboRows} locale={locale} labels={chartLabels} />
          </section>

          <section className="fi-admin-card fi-admin-ua-recent-card">
            <h2 className="fi-admin-h2 fi-admin-ua-block-title">{t('totalPortfolioPage.recent.title')}</h2>
            <div className="fi-admin-ua-table-wrap">
              <table className="fi-admin-ua-table">
                <thead>
                  <tr>
                    <th>{t('totalPortfolioPage.recent.colName')}</th>
                    <th>{t('totalPortfolioPage.recent.colOwner')}</th>
                    <th>{t('totalPortfolioPage.recent.colCurrency')}</th>
                    <th>{t('totalPortfolioPage.recent.colCreated')}</th>
                  </tr>
                </thead>
                <tbody>
                  {analytics.data.recentPortfolios.map((r) => (
                    <tr key={r.id}>
                      <td className="fi-admin-ua-recent-title">{r.name}</td>
                      <td className="fi-admin-ua-mono">{r.ownerUsername}</td>
                      <td>{r.baseCurrency}</td>
                      <td className="fi-admin-ua-mono">{formatCreatedAtCell(r.createdAt, locale)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>
        </div>
      ) : null}
    </Fragment>
  )
}
