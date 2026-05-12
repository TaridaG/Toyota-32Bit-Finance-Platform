import { Fragment, useMemo } from 'react'
import { useTranslation } from 'react-i18next'
import type { NewsAnalyticsPresetParam, NewsAnalyticsQuery } from '../../features/admin/api/adminNewsAnalyticsApi'
import type { AdminNewsAnalyticsState } from '../../features/admin/hooks/useAdminNewsAnalytics'
import { formatAdminInteger, formatWowPercent } from '../../features/admin/formatAdminNumbers'
import { AdminDailyComboChart } from './AdminDailyComboChart'

type Props = {
  query: NewsAnalyticsQuery
  onSelectPreset: (p: NewsAnalyticsPresetParam) => void
  onSelectCustomDefault: () => void
  customFrom: string
  customTo: string
  onCustomFromChange: (v: string) => void
  onCustomToChange: (v: string) => void
  onApplyCustomRange: () => void
  rangeError: string | null
  analytics: AdminNewsAnalyticsState
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

function formatPercentOneDecimal(value: number, language: string): string {
  const loc = language.toLowerCase().startsWith('tr') ? 'tr-TR' : language.toLowerCase().startsWith('de') ? 'de-DE' : 'en-US'
  const n = new Intl.NumberFormat(loc, { maximumFractionDigits: 1, minimumFractionDigits: 0 }).format(value)
  return `${n}%`
}

export function AdminNewsAnalyticsEnterprise({
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

  const presets: { id: NewsAnalyticsPresetParam; labelKey: string }[] = [
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
      empty: t('totalNewsPage.analytics.chartEmpty'),
      aria: t('totalNewsPage.analytics.chartAria'),
      tipPrimary: t('totalNewsPage.analytics.tipPublished'),
      tipAvg: t('totalNewsPage.analytics.tipAvg7'),
      tipDelta: t('totalNewsPage.analytics.tipDelta'),
    }),
    [t],
  )

  const comboRows =
    analytics.status === 'ok'
      ? analytics.data.dailyPublished.map((r) => ({
          date: r.date,
          barValue: r.articlesPublished,
          rollingAverage7d: r.rollingAverage7d,
          deltaVsPreviousDay: r.deltaVsPreviousDay,
        }))
      : []

  return (
    <Fragment>
      <header className="fi-admin-page-head">
        <div>
          <h1 className="fi-admin-h1">{t('dashboard.kpi.newsArticles')}</h1>
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
            {t('totalNewsPage.retry')}
          </button>
        </section>
      ) : null}

      {analytics.status === 'ok' ? (
        <div className="fi-admin-ua-root">
          <div className="fi-admin-ua-kpi-row">
            <section className="fi-admin-card fi-admin-ua-kpi fi-admin-ua-kpi--total">
              <div className="fi-admin-ua-kpi-head">
                <span className="fi-admin-ua-hero-kicker">{t('totalNewsPage.analytics.kpiTotal')}</span>
              </div>
              <div className="fi-admin-ua-kpi-value-inline">
                <span className="fi-admin-ua-kpi-value fi-admin-ua-kpi-value--total">
                  {formatAdminInteger(analytics.data.summary.totalArticles, locale)}
                </span>
                <span
                  className={`fi-admin-ua-delta fi-admin-ua-delta--adjacent ${wowToneClass(analytics.data.summary.totalArticlesVsPriorDayPercentApprox)}`}
                >
                  {formatWowPercent(analytics.data.summary.totalArticlesVsPriorDayPercentApprox, locale)}
                </span>
              </div>
            </section>
            <section className="fi-admin-card fi-admin-ua-kpi">
              <div className="fi-admin-ua-kpi-head">
                <span className="fi-admin-ua-kpi-title">{t('totalNewsPage.analytics.kpiLastWeek')}</span>
              </div>
              <p className="fi-admin-ua-kpi-value">
                {formatAdminInteger(analytics.data.summary.articlesPublishedPreviousIsoWeekUtc, locale)}
              </p>
            </section>
            <section className="fi-admin-card fi-admin-ua-kpi">
              <div className="fi-admin-ua-kpi-head">
                <span className="fi-admin-ua-kpi-title">{t('totalNewsPage.analytics.kpiLastMonth')}</span>
              </div>
              <p className="fi-admin-ua-kpi-value">
                {formatAdminInteger(analytics.data.summary.articlesPublishedPreviousCalendarMonthUtc, locale)}
              </p>
            </section>
            <section className="fi-admin-card fi-admin-ua-kpi">
              <div className="fi-admin-ua-kpi-head">
                <span className="fi-admin-ua-kpi-title">
                  {t('totalNewsPage.analytics.kpiTranslation', { lang: analytics.data.summary.translationMeasuredLanguage })}
                </span>
              </div>
              <p className="fi-admin-ua-kpi-value">
                {formatPercentOneDecimal(analytics.data.summary.translationCompletionPercent, locale)}
              </p>
            </section>
            <section className="fi-admin-card fi-admin-ua-kpi">
              <div className="fi-admin-ua-kpi-head">
                <span className="fi-admin-ua-kpi-title">{t('totalNewsPage.analytics.kpiSources')}</span>
              </div>
              <p className="fi-admin-ua-kpi-value">
                {formatAdminInteger(analytics.data.summary.distinctSourceCount, locale)}
              </p>
            </section>
          </div>

          <section className="fi-admin-card fi-admin-ua-chart-card">
            <h2 className="fi-admin-h2 fi-admin-ua-block-title">{t('totalNewsPage.analytics.chartTitle')}</h2>
            <div className="fi-admin-ua-chart-toolbar">
              <div className="fi-admin-ua-range" role="group" aria-label={t('totalNewsPage.analytics.rangeAria')}>
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
              <span className="fi-admin-ua-leg fi-admin-ua-leg--bars">{t('totalNewsPage.analytics.legendDaily')}</span>
              <span className="fi-admin-ua-leg fi-admin-ua-leg--line">{t('totalNewsPage.analytics.legendAvg')}</span>
            </div>
            <AdminDailyComboChart rows={comboRows} locale={locale} labels={chartLabels} />
          </section>

          <section className="fi-admin-card fi-admin-ua-recent-card">
            <h2 className="fi-admin-h2 fi-admin-ua-block-title">{t('totalNewsPage.recent.title')}</h2>
            <div className="fi-admin-ua-table-wrap">
              <table className="fi-admin-ua-table">
                <thead>
                  <tr>
                    <th>{t('totalNewsPage.recent.colTitle')}</th>
                    <th>{t('totalNewsPage.recent.colSource')}</th>
                    <th>{t('totalNewsPage.recent.colCategory')}</th>
                    <th>{t('totalNewsPage.recent.colPublished')}</th>
                  </tr>
                </thead>
                <tbody>
                  {analytics.data.recentArticles.map((r) => (
                    <tr key={r.id}>
                      <td className="fi-admin-ua-mono fi-admin-ua-recent-title">{r.title}</td>
                      <td>{r.sourceName}</td>
                      <td>{r.category}</td>
                      <td className="fi-admin-ua-mono">{formatWhen(r.publishedAt, locale)}</td>
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
