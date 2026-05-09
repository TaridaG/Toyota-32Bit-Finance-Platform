import { useCallback, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { ADMIN_DASH_SECTION_LINKS, adminKpiSectionPath } from '../../features/admin/adminSectionRoutes'
import { getAdminOverviewMock } from '../../features/admin/mock/adminMockData'
import type { AdminDashboardKpi, AdminDataFlowPoint } from '../../features/admin/types'
import { type UsersKpiLive, useAdminPortalUsersKpiLive } from '../../features/admin/hooks/usePortalUserMetrics'
import { KpiSparkline } from './AdminOverviewKpiSparkline'

function conicFromPercents(segments: { percent: number; color: string }[]): string {
  let acc = 0
  const stops = segments.map((s) => {
    const a = acc
    acc += s.percent
    return `${s.color} ${a}% ${acc}%`
  })
  return `conic-gradient(${stops.join(', ')})`
}

function buildFlowChartPoints(series: AdminDataFlowPoint[], key: 'success' | 'error' | 'warn'): string {
  if (series.length === 0) return ''
  const n = series.length
  const denom = Math.max(1, n - 1)
  return series
    .map((p, i) => {
      const x = (i / denom) * 100
      const v = p[key]
      const y = 42 - (v / 100) * 38
      return `${x},${y}`
    })
    .join(' ')
}

function IconRefresh() {
  return (
    <svg className="fi-admin-dash-refresh-ico" viewBox="0 0 24 24" fill="none" aria-hidden>
      <path
        d="M21 12a9 9 0 0 0-9-9 9.75 9.75 0 0 0-6.74 2.74L3 8"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <path d="M3 3v5h5" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
      <path
        d="M3 12a9 9 0 0 0 9 9 9.75 9.75 0 0 0 6.74-2.74L21 16"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <path d="M21 21v-5h-5" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

function IconShield() {
  return (
    <svg className="fi-admin-dash-shield" viewBox="0 0 24 24" aria-hidden>
      <path
        d="M12 3 5 6v6c0 5 3.5 9 7 10 3.5-1 7-5 7-10V6l-7-3Z"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.6"
        strokeLinejoin="round"
      />
    </svg>
  )
}

function KpiIcon({ kind }: { kind: 'users' | 'portfolio' | 'streams' | 'news' | 'latency' }) {
  const c = { fill: 'none', stroke: 'currentColor', strokeWidth: 1.65, strokeLinecap: 'round' as const, strokeLinejoin: 'round' as const }
  switch (kind) {
    case 'users':
      return (
        <svg viewBox="0 0 24 24" aria-hidden>
          <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2" {...c} />
          <circle cx="9" cy="7" r="4" {...c} />
          <path d="M22 21v-2a4 4 0 0 0-3-3.87M16 3.13a4 4 0 0 1 0 7.75" {...c} />
        </svg>
      )
    case 'portfolio':
      return (
        <svg viewBox="0 0 24 24" aria-hidden>
          <path d="M8 7V5a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" {...c} />
          <rect x="4" y="7" width="16" height="14" rx="2" {...c} />
          <path d="M9 12h6" {...c} />
        </svg>
      )
    case 'streams':
      return (
        <svg viewBox="0 0 24 24" aria-hidden>
          <path d="M4 14h2v5H4v-5Zm7-4h2v9h-2v-9Zm7-5h2v14h-2V5Z" fill="currentColor" stroke="none" />
        </svg>
      )
    case 'news':
      return (
        <svg viewBox="0 0 24 24" aria-hidden>
          <path d="M6 4h11a2 2 0 0 1 2 2v14H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2Z" {...c} />
          <path d="M8 8h8M8 12h5" {...c} />
        </svg>
      )
    default:
      return (
        <svg viewBox="0 0 24 24" aria-hidden>
          <circle cx="12" cy="13" r="7" {...c} />
          <path d="M12 9v5l3 2M9 3h6" {...c} />
        </svg>
      )
  }
}

export function AdminOverviewPage() {
  const { t, i18n } = useTranslation('admin')
  const [refreshedAt, setRefreshedAt] = useState(() => new Date())
  const data = useMemo(() => getAdminOverviewMock(), [])
  const { usersLive, refetchPortalUsers } = useAdminPortalUsersKpiLive(i18n.language)

  const onRefresh = useCallback(() => {
    setRefreshedAt(new Date())
    void refetchPortalUsers()
  }, [refetchPortalUsers])

  const donutStyle = useMemo(
    () => ({
      background: conicFromPercents(data.latencyBands.map((b) => ({ percent: b.percent, color: b.color }))),
    }),
    [data.latencyBands],
  )

  const flowPts = useMemo(
    () => ({
      success: buildFlowChartPoints(data.dataFlowSeries, 'success'),
      error: buildFlowChartPoints(data.dataFlowSeries, 'error'),
      warn: buildFlowChartPoints(data.dataFlowSeries, 'warn'),
    }),
    [data.dataFlowSeries],
  )

  const fmtTime = (iso: string) =>
    new Date(iso).toLocaleString(i18n.language, { dateStyle: 'short', timeStyle: 'medium' })

  return (
    <div className="fi-admin-page fi-admin-dash">
      <header className="fi-admin-dash-head">
        <div>
          <h1 className="fi-admin-dash-title">{t('dashboard.title')}</h1>
          <p className="fi-admin-dash-lead">{t('dashboard.lead')}</p>
        </div>
        <div className="fi-admin-dash-head-actions">
          <span className="fi-admin-dash-mock">{t('dashboard.mockTag')}</span>
          <time className="fi-admin-dash-clock" dateTime={refreshedAt.toISOString()}>
            {refreshedAt.toLocaleString(i18n.language, {
              day: 'numeric',
              month: 'long',
              year: 'numeric',
              hour: '2-digit',
              minute: '2-digit',
              second: '2-digit',
              timeZoneName: 'short',
            })}
          </time>
          <button type="button" className="fi-admin-dash-refresh" onClick={onRefresh}>
            <IconRefresh />
            {t('dashboard.refresh')}
          </button>
        </div>
      </header>

      <section className="fi-admin-dash-kpis" aria-label={t('dashboard.kpiAria')}>
        {data.dashboardKpis.map((kpi) => (
          <DashboardKpiCard key={kpi.id} kpi={kpi} t={t} usersLive={kpi.id === 'users' ? usersLive : undefined} />
        ))}
      </section>

      <div className="fi-admin-dash-main">
        <div className="fi-admin-dash-charts-col">
          <section className="fi-admin-dash-card fi-admin-dash-card--chart">
            <div className="fi-admin-dash-card-head">
              <h2 className="fi-admin-dash-h2">
                <Link to={ADMIN_DASH_SECTION_LINKS.dataFlow} className="fi-admin-dash-section-link">
                  {t('dashboard.dataFlow.title')}
                </Link>
              </h2>
              <p className="fi-admin-dash-card-desc">{t('dashboard.dataFlow.subtitle')}</p>
            </div>
            <div className="fi-admin-dash-legend">
              <span className="fi-admin-dash-legend-item fi-admin-dash-legend--ok">
                <i /> {t('dashboard.dataFlow.legendSuccess')} ({data.dataFlowSummary.successPct}%)
              </span>
              <span className="fi-admin-dash-legend-item fi-admin-dash-legend--err">
                <i /> {t('dashboard.dataFlow.legendError')} ({data.dataFlowSummary.errorPct}%)
              </span>
              <span className="fi-admin-dash-legend-item fi-admin-dash-legend--warn">
                <i /> {t('dashboard.dataFlow.legendWarn')} ({data.dataFlowSummary.warnPct}%)
              </span>
            </div>
            <div className="fi-admin-dash-chart-svg-wrap" role="img" aria-label={t('dashboard.dataFlow.title')}>
              <svg className="fi-admin-dash-chart-svg" viewBox="0 0 100 44" preserveAspectRatio="none">
                <line x1="0" y1="42" x2="100" y2="42" className="fi-admin-dash-chart-axis" />
                <polyline className="fi-admin-dash-line fi-admin-dash-line--success" points={flowPts.success} fill="none" />
                <polyline className="fi-admin-dash-line fi-admin-dash-line--error" points={flowPts.error} fill="none" />
                <polyline className="fi-admin-dash-line fi-admin-dash-line--warn" points={flowPts.warn} fill="none" />
              </svg>
              <div className="fi-admin-dash-chart-x">
                {data.dataFlowSeries.map((p) => (
                  <span key={p.label}>{p.label}</span>
                ))}
              </div>
            </div>
          </section>

          <section className="fi-admin-dash-card fi-admin-dash-card--donut">
            <div className="fi-admin-dash-card-head">
              <h2 className="fi-admin-dash-h2">
                <Link to={ADMIN_DASH_SECTION_LINKS.latencySummary} className="fi-admin-dash-section-link">
                  {t('dashboard.latency.title')}
                </Link>
              </h2>
              <p className="fi-admin-dash-card-desc">{t('dashboard.latency.subtitle')}</p>
            </div>
            <div className="fi-admin-dash-donut-row">
              <div className="fi-admin-dash-donut-visual">
                <div className="fi-admin-dash-donut-ring" style={donutStyle} />
                <div className="fi-admin-dash-donut-hole">
                  <strong>{data.latencyAverageSec.toFixed(2)}s</strong>
                  <span>{t('dashboard.latency.avgLabel')}</span>
                </div>
              </div>
              <ul className="fi-admin-dash-donut-legend">
                {data.latencyBands.map((b) => (
                  <li key={b.bandKey}>
                    <span className="fi-admin-dash-dot" style={{ background: b.color }} />
                    <span className="fi-admin-dash-donut-label">{t(`dashboard.latency.band.${b.bandKey}`)}</span>
                    <span className="fi-admin-dash-donut-meta">
                      {b.count} ({b.percent}%)
                    </span>
                  </li>
                ))}
              </ul>
            </div>
          </section>
        </div>

        <div className="fi-admin-dash-tables-col">
          <section className="fi-admin-dash-card">
            <h2 className="fi-admin-dash-h2">
              <Link to={ADMIN_DASH_SECTION_LINKS.dataStreams} className="fi-admin-dash-section-link">
                {t('dashboard.tables.streamsTitle')}
              </Link>
            </h2>
            <div className="fi-admin-table-wrap">
              <table className="fi-admin-table fi-admin-dash-table">
                <thead>
                  <tr>
                    <th>{t('dashboard.tables.colSource')}</th>
                    <th>{t('dashboard.tables.colType')}</th>
                    <th>{t('dashboard.tables.colStatus')}</th>
                    <th>{t('dashboard.tables.colLatency')}</th>
                    <th>{t('dashboard.tables.colLastUpdate')}</th>
                    <th>{t('dashboard.tables.colSuccess')}</th>
                  </tr>
                </thead>
                <tbody>
                  {data.streamRows.map((row) => (
                    <tr key={row.name}>
                      <td><strong>{row.name}</strong></td>
                      <td>{t(row.categoryKey)}</td>
                      <td>
                        <span className={`fi-admin-dash-pill fi-admin-dash-pill--${row.status}`}>
                          {t(`dashboard.status.${row.status}`)}
                        </span>
                      </td>
                      <td>{row.latencyMs} ms</td>
                      <td>{fmtTime(row.lastUpdate)}</td>
                      <td>
                        <div className="fi-admin-dash-barcell">
                          <span>{row.successRate.toFixed(2)}%</span>
                          <span className="fi-admin-dash-bartrack">
                            <span className="fi-admin-dash-barfill" style={{ width: `${Math.min(100, row.successRate)}%` }} />
                          </span>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <button type="button" className="fi-admin-dash-footer-link">
              {t('dashboard.tables.viewAllStreams')} →
            </button>
          </section>

          <section className="fi-admin-dash-card">
            <h2 className="fi-admin-dash-h2">{t('dashboard.tables.newsTitle')}</h2>
            <div className="fi-admin-table-wrap">
              <table className="fi-admin-table fi-admin-dash-table">
                <thead>
                  <tr>
                    <th>{t('dashboard.tables.colSource')}</th>
                    <th>{t('dashboard.tables.colStatus')}</th>
                    <th>{t('dashboard.tables.colLatency')}</th>
                    <th>{t('dashboard.tables.colLastNews')}</th>
                    <th>{t('dashboard.tables.colNewsCount')}</th>
                  </tr>
                </thead>
                <tbody>
                  {data.newsStreamRows.map((row) => (
                    <tr key={row.name}>
                      <td><strong>{row.name}</strong></td>
                      <td>
                        <span className={`fi-admin-dash-pill fi-admin-dash-pill--${row.status}`}>
                          {t(`dashboard.newsStatus.${row.status}`)}
                        </span>
                      </td>
                      <td>{row.latencyLabel}</td>
                      <td>{fmtTime(row.lastNews)}</td>
                      <td>{row.newsCount}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Link to={ADMIN_DASH_SECTION_LINKS.newsStreams} className="fi-admin-dash-footer-link">
              {t('dashboard.tables.viewAllNews')} →
            </Link>
          </section>
        </div>
      </div>
    </div>
  )
}

function KpiTitleLink({
  kpiId,
  labelKey,
  t,
}: {
  kpiId: string
  labelKey: string
  t: (k: string, o?: Record<string, string | number>) => string
}) {
  const to = adminKpiSectionPath(kpiId)
  return (
    <span className="fi-admin-dash-kpi-label">
      {to ? (
        <Link to={to} className="fi-admin-dash-kpi-label-link">
          {t(labelKey)}
        </Link>
      ) : (
        t(labelKey)
      )}
    </span>
  )
}

function DashboardKpiCard({
  kpi,
  t,
  usersLive,
}: {
  kpi: AdminDashboardKpi
  t: (k: string, o?: Record<string, string | number>) => string
  usersLive?: UsersKpiLive
}) {
  switch (kpi.variant) {
    case 'trend': {
      const iconKind = kpi.id === 'portfolios' ? 'portfolio' : 'users'
      const u = kpi.id === 'users' ? usersLive : undefined
      const loadingUsers = u !== undefined && u.kind === 'loading'
      const errorUsers = u !== undefined && u.kind === 'error'
      const live = u !== undefined && u.kind === 'live' ? u : null
      const valueNode = loadingUsers ? (
        <strong className="fi-admin-dash-kpi-value fi-admin-dash-kpi-value--skeleton" aria-busy>
          &nbsp;
        </strong>
      ) : errorUsers ? (
        <strong className="fi-admin-dash-kpi-value fi-admin-dash-kpi-value--error" title={u.message}>
          —
        </strong>
      ) : (
        <strong className="fi-admin-dash-kpi-value">{live ? live.valueFormatted : kpi.value}</strong>
      )
      const deltaClass = live
        ? live.trend === 'up'
          ? 'up'
          : live.trend === 'down'
            ? 'down'
            : 'flat'
        : errorUsers
          ? 'flat'
          : kpi.trend === 'up'
            ? 'up'
            : 'down'
      const footKey = errorUsers
        ? 'dashboard.kpi.usersMetricLoadError'
        : (live?.footCaptionKey ?? 'dashboard.kpi.comparedToLast7Days')
      const sparkVals = errorUsers ? Array.from({ length: 7 }, () => 50) : (live?.sparkline ?? kpi.sparkline)
      return (
        <article className={`fi-admin-dash-kpi fi-admin-dash-kpi--rich fi-admin-dash-kpi--accent-${kpi.accent}`}>
          <div className="fi-admin-dash-kpi-top">
            <div className="fi-admin-dash-kpi-ico-wrap" aria-hidden>
              <KpiIcon kind={iconKind} />
            </div>
            <div className="fi-admin-dash-kpi-main">
              <KpiTitleLink kpiId={kpi.id} labelKey={kpi.labelKey} t={t} />
              {valueNode}
              {loadingUsers ? (
                <span className="fi-admin-dash-kpi-delta fi-admin-dash-kpi-delta--pending" aria-hidden>
                  ···
                </span>
              ) : (
                <span className={`fi-admin-dash-kpi-delta fi-admin-dash-kpi-delta--${deltaClass}`}>
                  {errorUsers ? '—' : live ? live.deltaFormatted : t(kpi.deltaPctKey)}
                </span>
              )}
            </div>
          </div>
          <div className="fi-admin-dash-kpi-foot">
            <span className="fi-admin-dash-kpi-foot-cap fi-admin-dash-kpi-foot-cap--loud">{t(footKey)}</span>
            <KpiSparkline values={sparkVals} accent={kpi.accent} />
          </div>
        </article>
      )
    }
    case 'streams':
      return (
        <article className={`fi-admin-dash-kpi fi-admin-dash-kpi--rich fi-admin-dash-kpi--accent-${kpi.accent}`}>
          <div className="fi-admin-dash-kpi-top">
            <div className="fi-admin-dash-kpi-ico-wrap" aria-hidden>
              <KpiIcon kind="streams" />
            </div>
            <div className="fi-admin-dash-kpi-main">
              <KpiTitleLink kpiId={kpi.id} labelKey={kpi.labelKey} t={t} />
              <strong className="fi-admin-dash-kpi-value">{kpi.total}</strong>
              <span className="fi-admin-dash-kpi-sub">
                <span className="fi-admin-dash-kpi-ok">
                  {kpi.active} {t('dashboard.kpi.activeLabel')}
                </span>
                <span className="fi-admin-dash-kpi-muted">
                  {kpi.passive} {t('dashboard.kpi.passiveLabel')}
                </span>
              </span>
            </div>
          </div>
          <div className="fi-admin-dash-kpi-foot">
            <span className="fi-admin-dash-kpi-foot-cap fi-admin-dash-kpi-foot-cap--ghost">&nbsp;</span>
            <KpiSparkline values={kpi.sparkline} accent={kpi.accent} />
          </div>
        </article>
      )
    case 'news':
      return (
        <article className={`fi-admin-dash-kpi fi-admin-dash-kpi--rich fi-admin-dash-kpi--accent-${kpi.accent}`}>
          <div className="fi-admin-dash-kpi-top">
            <div className="fi-admin-dash-kpi-ico-wrap" aria-hidden>
              <KpiIcon kind="news" />
            </div>
            <div className="fi-admin-dash-kpi-main">
              <KpiTitleLink kpiId={kpi.id} labelKey={kpi.labelKey} t={t} />
              <strong className="fi-admin-dash-kpi-value">{kpi.total}</strong>
              <span className="fi-admin-dash-kpi-sub">
                <span className="fi-admin-dash-kpi-ok">
                  {kpi.active} {t('dashboard.kpi.activeLabel')}
                </span>
                <span className="fi-admin-dash-kpi-bad">
                  {kpi.errors} {t('dashboard.kpi.errorsLabel')}
                </span>
              </span>
            </div>
          </div>
          <div className="fi-admin-dash-kpi-foot">
            <span className="fi-admin-dash-kpi-foot-cap fi-admin-dash-kpi-foot-cap--ghost">&nbsp;</span>
            <KpiSparkline values={kpi.sparkline} accent={kpi.accent} />
          </div>
        </article>
      )
    case 'system': {
      const lit = kpi.healthSegments.filter(Boolean).length
      const total = kpi.healthSegments.length
      return (
        <article className="fi-admin-dash-kpi fi-admin-dash-kpi--rich fi-admin-dash-kpi--system-rich">
          <div className="fi-admin-dash-kpi-top">
            <div className="fi-admin-dash-kpi-ico-wrap fi-admin-dash-kpi-ico-wrap--outline" aria-hidden>
              <IconShield />
            </div>
            <div className="fi-admin-dash-kpi-main">
              <KpiTitleLink kpiId={kpi.id} labelKey={kpi.labelKey} t={t} />
              <strong className="fi-admin-dash-kpi-status-lg">{t(kpi.statusKey)}</strong>
              <p className="fi-admin-dash-kpi-detail-tight">{t(kpi.detailKey)}</p>
            </div>
          </div>
          <div
            className="fi-admin-dash-kpi-segments"
            role="img"
            aria-label={t('dashboard.kpi.healthSegmentsAria', { lit, total })}
          >
            {kpi.healthSegments.map((on, i) => (
              <span key={i} className={on ? 'fi-admin-dash-kpi-seg fi-admin-dash-kpi-seg--on' : 'fi-admin-dash-kpi-seg'} />
            ))}
          </div>
        </article>
      )
    }
    case 'latency':
      return (
        <article className={`fi-admin-dash-kpi fi-admin-dash-kpi--rich fi-admin-dash-kpi--accent-${kpi.accent}`}>
          <div className="fi-admin-dash-kpi-top">
            <div className="fi-admin-dash-kpi-ico-wrap" aria-hidden>
              <KpiIcon kind="latency" />
            </div>
            <div className="fi-admin-dash-kpi-main">
              <KpiTitleLink kpiId={kpi.id} labelKey={kpi.labelKey} t={t} />
              <strong className="fi-admin-dash-kpi-value">
                {kpi.valueSec}
                <span className="fi-admin-dash-kpi-unit">s</span>
              </strong>
              <span className={`fi-admin-dash-kpi-delta fi-admin-dash-kpi-delta--${kpi.trend === 'down' ? 'good' : 'bad'}`}>
                {t(kpi.deltaShortKey)}
              </span>
              <span className="fi-admin-dash-kpi-target">{t('dashboard.kpi.latencyTarget', { value: kpi.targetSec })}</span>
            </div>
          </div>
          <div className="fi-admin-dash-kpi-foot">
            <span className="fi-admin-dash-kpi-foot-cap">{t('dashboard.kpi.comparedToPrevWindow')}</span>
            <KpiSparkline values={kpi.sparkline} accent={kpi.accent} />
          </div>
        </article>
      )
    default:
      return null
  }
}
