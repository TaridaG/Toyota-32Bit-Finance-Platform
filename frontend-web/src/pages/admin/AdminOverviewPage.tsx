import { useCallback, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { adminKpiSectionPath } from '../../features/admin/adminSectionRoutes'
import { getAdminOverviewMock } from '../../features/admin/mock/adminMockData'
import type { AdminDashboardKpi } from '../../features/admin/types'
import { type AdminLatencyKpi, useAdminLatencyKpi } from '../../features/admin/hooks/useAdminLatencyKpi'
import {
  type StreamsKpiLive,
  type UsersKpiLive,
  usePortalAdminDashboardKpis,
} from '../../features/admin/hooks/usePortalUserMetrics'
import { useAdminNewsKpi, type NewsKpiLive } from '../../features/admin/hooks/useAdminNewsKpi'
import { latencyHealthSegmentColor, latencyHealthStatusKey } from '../../features/admin/adminLatencyHealth'
import { KpiSparkline } from './AdminOverviewKpiSparkline'

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
  const { usersLive, portfoliosLive, instrumentsStreamsLive, refetchPortalDashboard } =
    usePortalAdminDashboardKpis(i18n.language)
  const { newsLive, refetchNews } = useAdminNewsKpi(i18n.language)
  const latencyKpi = useAdminLatencyKpi()

  const onRefresh = useCallback(() => {
    setRefreshedAt(new Date())
    void refetchPortalDashboard()
    void refetchNews()
    void latencyKpi.reloadSnapshot()
  }, [latencyKpi, refetchNews, refetchPortalDashboard])

  return (
    <div className="fi-admin-page fi-admin-dash">
      <header className="fi-admin-dash-head">
        <div>
          <h1 className="fi-admin-dash-title">{t('dashboard.title')}</h1>
          <p className="fi-admin-dash-lead">{t('dashboard.lead')}</p>
        </div>
        <div className="fi-admin-dash-head-actions">
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
          <DashboardKpiCard
            key={kpi.id}
            kpi={kpi}
            t={t}
            language={i18n.language}
            usersLive={kpi.id === 'users' ? usersLive : undefined}
            portfoliosLive={kpi.id === 'portfolios' ? portfoliosLive : undefined}
            streamsLive={kpi.id === 'marketStreams' ? instrumentsStreamsLive : undefined}
            newsLive={kpi.id === 'news' ? newsLive : undefined}
            latencyKpi={kpi.id === 'latency' || kpi.id === 'system' ? latencyKpi : undefined}
          />
        ))}
      </section>
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
  language,
  usersLive,
  portfoliosLive,
  streamsLive,
  newsLive,
  latencyKpi,
}: {
  kpi: AdminDashboardKpi
  t: (k: string, o?: Record<string, string | number>) => string
  language: string
  usersLive?: UsersKpiLive
  portfoliosLive?: UsersKpiLive
  streamsLive?: StreamsKpiLive
  newsLive?: NewsKpiLive
  latencyKpi?: AdminLatencyKpi
}) {
  switch (kpi.variant) {
    case 'trend': {
      const iconKind = kpi.id === 'portfolios' ? 'portfolio' : 'users'
      const trendLive = kpi.id === 'users' ? usersLive : kpi.id === 'portfolios' ? portfoliosLive : undefined
      const loadingTrend = trendLive !== undefined && trendLive.kind === 'loading'
      const errorTrend = trendLive !== undefined && trendLive.kind === 'error'
      const live = trendLive !== undefined && trendLive.kind === 'live' ? trendLive : null
      const valueNode = loadingTrend ? (
        <strong className="fi-admin-dash-kpi-value fi-admin-dash-kpi-value--skeleton" aria-busy>
          &nbsp;
        </strong>
      ) : errorTrend ? (
        <strong
          className="fi-admin-dash-kpi-value fi-admin-dash-kpi-value--error"
          title={trendLive.kind === 'error' ? trendLive.message : undefined}
        >
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
        : errorTrend
          ? 'flat'
          : kpi.trend === 'up'
            ? 'up'
            : 'down'
      const footKey = errorTrend
        ? kpi.id === 'portfolios'
          ? 'dashboard.kpi.portfoliosMetricLoadError'
          : 'dashboard.kpi.usersMetricLoadError'
        : (live?.footCaptionKey ??
            (kpi.id === 'users'
              ? 'dashboard.kpi.userFlowSparkFoot'
              : kpi.id === 'portfolios'
                ? 'dashboard.kpi.portfolioFlowSparkFoot'
                : 'dashboard.kpi.comparedToLast7Days'))
      const sparkVals = errorTrend ? Array.from({ length: 7 }, () => 50) : (live?.sparkline ?? kpi.sparkline)
      const sparkSecondary =
        !errorTrend && live?.sparklineSecondary != null && live.sparklineSecondary.length === sparkVals.length
          ? live.sparklineSecondary
          : undefined
      return (
        <article className={`fi-admin-dash-kpi fi-admin-dash-kpi--rich fi-admin-dash-kpi--accent-${kpi.accent}`}>
          <div className="fi-admin-dash-kpi-top">
            <div className="fi-admin-dash-kpi-ico-wrap" aria-hidden>
              <KpiIcon kind={iconKind} />
            </div>
            <div className="fi-admin-dash-kpi-main">
              <KpiTitleLink kpiId={kpi.id} labelKey={kpi.labelKey} t={t} />
              {valueNode}
              {loadingTrend ? (
                <span className="fi-admin-dash-kpi-delta fi-admin-dash-kpi-delta--pending" aria-hidden>
                  ···
                </span>
              ) : (
                <span className={`fi-admin-dash-kpi-delta fi-admin-dash-kpi-delta--${deltaClass}`}>
                  {errorTrend ? '—' : live ? live.deltaFormatted : t(kpi.deltaPctKey)}
                </span>
              )}
            </div>
          </div>
          <div className="fi-admin-dash-kpi-foot">
            <span className="fi-admin-dash-kpi-foot-cap fi-admin-dash-kpi-foot-cap--loud">{t(footKey)}</span>
            <KpiSparkline values={sparkVals} accent={kpi.accent} secondaryValues={sparkSecondary} />
          </div>
        </article>
      )
    }
    case 'streams': {
      const sl = streamsLive
      const loadingStreams = sl !== undefined && sl.kind === 'loading'
      const errorStreams = sl !== undefined && sl.kind === 'error'
      const liveStreams = sl !== undefined && sl.kind === 'live' ? sl : null
      const useLive = sl !== undefined
      return (
        <article className={`fi-admin-dash-kpi fi-admin-dash-kpi--rich fi-admin-dash-kpi--accent-${kpi.accent}`}>
          <div className="fi-admin-dash-kpi-top">
            <div className="fi-admin-dash-kpi-ico-wrap" aria-hidden>
              <KpiIcon kind="streams" />
            </div>
            <div className="fi-admin-dash-kpi-main">
              <KpiTitleLink kpiId={kpi.id} labelKey={kpi.labelKey} t={t} />
              {loadingStreams ? (
                <strong className="fi-admin-dash-kpi-value fi-admin-dash-kpi-value--skeleton" aria-busy>
                  &nbsp;
                </strong>
              ) : errorStreams ? (
                <strong
                  className="fi-admin-dash-kpi-value fi-admin-dash-kpi-value--error"
                  title={sl.kind === 'error' ? sl.message : undefined}
                >
                  —
                </strong>
              ) : (
                <strong className="fi-admin-dash-kpi-value">
                  {useLive && liveStreams ? liveStreams.totalFormatted : kpi.total}
                </strong>
              )}
              <span className="fi-admin-dash-kpi-sub">
                {useLive ? (
                  errorStreams ? (
                    <span className="fi-admin-dash-kpi-muted">{sl.kind === 'error' ? sl.message : null}</span>
                  ) : loadingStreams ? (
                    <span className="fi-admin-dash-kpi-delta fi-admin-dash-kpi-delta--pending" aria-hidden>
                      ···
                    </span>
                  ) : (
                    <span className="fi-admin-dash-kpi-muted">{t('dashboard.kpi.instrumentsInCatalog')}</span>
                  )
                ) : (
                  <>
                    <span className="fi-admin-dash-kpi-ok">
                      {kpi.active} {t('dashboard.kpi.activeLabel')}
                    </span>
                    <span className="fi-admin-dash-kpi-muted">
                      {kpi.passive} {t('dashboard.kpi.passiveLabel')}
                    </span>
                  </>
                )}
              </span>
            </div>
          </div>
          <div className="fi-admin-dash-kpi-foot">
            <span className="fi-admin-dash-kpi-foot-cap fi-admin-dash-kpi-foot-cap--loud">
              {errorStreams ? '\u00a0' : t('dashboard.kpi.instrumentDataActivityFoot')}
            </span>
            <KpiSparkline
              values={
                loadingStreams
                  ? Array.from({ length: 7 }, () => 0)
                  : errorStreams
                    ? Array.from({ length: 7 }, () => 0)
                    : useLive && liveStreams
                      ? liveStreams.sparkline
                      : kpi.sparkline
              }
              accent={kpi.accent}
            />
          </div>
        </article>
      )
    }
    case 'news': {
      const nl = newsLive
      const loadingNews = nl !== undefined && nl.kind === 'loading'
      const errorNews = nl !== undefined && nl.kind === 'error'
      const liveNews = nl !== undefined && nl.kind === 'live' ? nl : null
      const useNl = nl !== undefined
      const sparkVals =
        loadingNews || errorNews
          ? Array.from({ length: 7 }, () => 0)
          : useNl && liveNews
            ? liveNews.sparkline
            : kpi.sparkline
      return (
        <article className={`fi-admin-dash-kpi fi-admin-dash-kpi--rich fi-admin-dash-kpi--accent-${kpi.accent}`}>
          <div className="fi-admin-dash-kpi-top">
            <div className="fi-admin-dash-kpi-ico-wrap" aria-hidden>
              <KpiIcon kind="news" />
            </div>
            <div className="fi-admin-dash-kpi-main">
              <KpiTitleLink kpiId={kpi.id} labelKey={kpi.labelKey} t={t} />
              {loadingNews ? (
                <strong className="fi-admin-dash-kpi-value fi-admin-dash-kpi-value--skeleton" aria-busy>
                  &nbsp;
                </strong>
              ) : errorNews ? (
                <strong
                  className="fi-admin-dash-kpi-value fi-admin-dash-kpi-value--error"
                  title={nl.kind === 'error' ? nl.message : undefined}
                >
                  —
                </strong>
              ) : (
                <strong className="fi-admin-dash-kpi-value">
                  {useNl && liveNews ? liveNews.totalArticlesFormatted : kpi.total}
                </strong>
              )}
              <span className="fi-admin-dash-kpi-sub">
                {loadingNews ? (
                  <span className="fi-admin-dash-kpi-delta fi-admin-dash-kpi-delta--pending" aria-hidden>
                    ···
                  </span>
                ) : errorNews ? (
                  <span className="fi-admin-dash-kpi-muted">{nl.kind === 'error' ? nl.message : null}</span>
                ) : (
                  <>
                    <span className="fi-admin-dash-kpi-ok">
                      {useNl && liveNews
                        ? t('dashboard.kpi.newsSubSources', { count: liveNews.distinctSourcesFormatted })
                        : t('dashboard.kpi.newsSubSources', { count: String(kpi.active) })}
                    </span>
                    {(useNl && liveNews ? liveNews.errors : kpi.errors) > 0 ? (
                      <span className="fi-admin-dash-kpi-bad">
                        {useNl && liveNews ? liveNews.errors : kpi.errors} {t('dashboard.kpi.errorsLabel')}
                      </span>
                    ) : null}
                  </>
                )}
              </span>
            </div>
          </div>
          <div className="fi-admin-dash-kpi-foot">
            <span className="fi-admin-dash-kpi-foot-cap fi-admin-dash-kpi-foot-cap--loud">
              {errorNews ? '\u00a0' : t('dashboard.kpi.newsActivitySparkFoot')}
            </span>
            <KpiSparkline values={sparkVals} accent={kpi.accent} />
          </div>
        </article>
      )
    }
    case 'system': {
      const lp = latencyKpi
      const secLive =
        lp?.enabled === true
          ? lp.phase === 'probing' && lp.runningAvgSec != null
            ? lp.runningAvgSec
            : lp.displaySec
          : null
      const useLatencyBar = lp?.enabled === true && secLive != null && Number.isFinite(secLive)
      const totalSeg = 7
      const statusKey = useLatencyBar ? latencyHealthStatusKey(secLive) : kpi.statusKey
      const detailText = useLatencyBar
        ? t('dashboard.kpi.latencyHealthDetail', {
            sec: new Intl.NumberFormat(language, { maximumFractionDigits: 2, minimumFractionDigits: 0 }).format(
              secLive,
            ),
          })
        : t(kpi.detailKey)
      const ringColor = useLatencyBar ? latencyHealthSegmentColor(secLive, 3, totalSeg) : '#22c55e'
      const ariaLabel = useLatencyBar
        ? t('dashboard.kpi.latencyHealthSegmentsAria', {
            sec: new Intl.NumberFormat(language, { maximumFractionDigits: 2, minimumFractionDigits: 0 }).format(
              secLive,
            ),
          })
        : t('dashboard.kpi.healthSegmentsAria', {
            lit: kpi.healthSegments.filter(Boolean).length,
            total: kpi.healthSegments.length,
          })
      return (
        <article className="fi-admin-dash-kpi fi-admin-dash-kpi--rich fi-admin-dash-kpi--system-rich">
          <div className="fi-admin-dash-kpi-top">
            <div
              className="fi-admin-dash-kpi-ico-wrap fi-admin-dash-kpi-ico-wrap--outline fi-admin-dash-kpi-ico-wrap--latency-ring"
              style={{ borderColor: ringColor, color: ringColor }}
              aria-hidden
            >
              <IconShield />
            </div>
            <div className="fi-admin-dash-kpi-main">
              <KpiTitleLink kpiId={kpi.id} labelKey={kpi.labelKey} t={t} />
              <strong className="fi-admin-dash-kpi-status-lg">{t(statusKey)}</strong>
              <p className="fi-admin-dash-kpi-detail-tight">{detailText}</p>
            </div>
          </div>
          <div className="fi-admin-dash-kpi-segments fi-admin-dash-kpi-segments--latency" role="img" aria-label={ariaLabel}>
            {useLatencyBar
              ? Array.from({ length: totalSeg }, (_, i) => (
                  <span
                    key={i}
                    className="fi-admin-dash-kpi-seg fi-admin-dash-kpi-seg--latency"
                    style={{ backgroundColor: latencyHealthSegmentColor(secLive, i, totalSeg) }}
                  />
                ))
              : kpi.healthSegments.map((on, i) => (
                  <span key={i} className={on ? 'fi-admin-dash-kpi-seg fi-admin-dash-kpi-seg--on' : 'fi-admin-dash-kpi-seg'} />
                ))}
          </div>
        </article>
      )
    }
    case 'latency': {
      const lp = latencyKpi
      if (!lp) {
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
      }
      const adminLatency = lp.enabled === true
      const mockSec = Number.parseFloat(kpi.valueSec)
      const secForDisplay =
        adminLatency && lp.phase === 'probing' && lp.runningAvgSec != null
          ? lp.runningAvgSec
          : adminLatency && lp.displaySec != null
            ? lp.displaySec
            : mockSec
      const loadingSnap = adminLatency && lp.phase === 'loading_snapshot'
      const probing = adminLatency && lp.phase === 'probing'
      const doneOk = adminLatency && lp.phase === 'done_ok'
      const err = adminLatency && lp.phase === 'error'
      const footCap = probing
        ? t('dashboard.kpi.latencyProbeProgressFoot')
        : doneOk
          ? t('dashboard.kpi.latencyProbeSavedFoot')
          : err
            ? t('dashboard.kpi.latencyProbeErrorFoot')
            : t('dashboard.kpi.comparedToPrevWindow')
      return (
        <article className={`fi-admin-dash-kpi fi-admin-dash-kpi--rich fi-admin-dash-kpi--accent-${kpi.accent}`}>
          <div className="fi-admin-dash-kpi-top">
            <div className="fi-admin-dash-kpi-ico-wrap" aria-hidden>
              <KpiIcon kind="latency" />
            </div>
            <div className="fi-admin-dash-kpi-main">
              <div className="fi-admin-dash-kpi-latency-head">
                <KpiTitleLink kpiId={kpi.id} labelKey={kpi.labelKey} t={t} />
                {adminLatency ? (
                  <button
                    type="button"
                    className="fi-admin-dash-kpi-probe-btn"
                    onClick={() => void lp.runProbe()}
                    disabled={loadingSnap || probing || doneOk}
                    aria-label={t('dashboard.kpi.latencyProbeRefreshAria')}
                    title={t('dashboard.kpi.latencyProbeRefreshAria')}
                  >
                    🔄
                  </button>
                ) : null}
              </div>
              {loadingSnap ? (
                <strong className="fi-admin-dash-kpi-value fi-admin-dash-kpi-value--skeleton" aria-busy>
                  &nbsp;
                </strong>
              ) : (
                <strong className="fi-admin-dash-kpi-value">
                  {Number.isFinite(secForDisplay) ? secForDisplay.toFixed(2) : kpi.valueSec}
                  <span className="fi-admin-dash-kpi-unit">s</span>
                </strong>
              )}
              {probing ? (
                <span className="fi-admin-dash-kpi-delta fi-admin-dash-kpi-delta--pending">
                  {t('dashboard.kpi.latencyProbeRunning', { current: lp.probeCurrent, total: lp.probeTotal })}
                </span>
              ) : doneOk ? (
                <span className="fi-admin-dash-kpi-delta fi-admin-dash-kpi-delta--good">{t('dashboard.kpi.latencyProbeDone')}</span>
              ) : err ? (
                <span className="fi-admin-dash-kpi-delta fi-admin-dash-kpi-delta--flat" title={lp.errorMessage ?? undefined}>
                  —
                </span>
              ) : (
                <span className={`fi-admin-dash-kpi-delta fi-admin-dash-kpi-delta--${kpi.trend === 'down' ? 'good' : 'bad'}`}>
                  {t(kpi.deltaShortKey)}
                </span>
              )}
              <span className="fi-admin-dash-kpi-target">{t('dashboard.kpi.latencyTarget', { value: kpi.targetSec })}</span>
              {err && lp.errorMessage ? <span className="fi-admin-dash-kpi-probe-err">{lp.errorMessage}</span> : null}
            </div>
          </div>
          <div className="fi-admin-dash-kpi-foot">
            <span className="fi-admin-dash-kpi-foot-cap">{footCap}</span>
            <KpiSparkline values={kpi.sparkline} accent={kpi.accent} />
          </div>
        </article>
      )
    }
    default:
      return null
  }
}
