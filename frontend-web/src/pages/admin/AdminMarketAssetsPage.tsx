import { useCallback, useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import {
  fetchMarketAssetDashboard,
  recomputeMarketAssets,
  readMarketAssetApiError,
  type MarketAssetDashboard,
} from '../../features/admin/api/adminMarketAssetsApi'
import { PortalAlert } from '../../shared/components/PortalAlert'

const PAGE_SIZE = 10
const POLL_MS = 3000

export function AdminMarketAssetsPage() {
  const { t, i18n } = useTranslation('admin')
  const [page, setPage] = useState(0)
  const [data, setData] = useState<MarketAssetDashboard | null>(null)
  const [loadState, setLoadState] = useState<'loading' | 'ok' | 'error'>('loading')
  const [loadError, setLoadError] = useState<string | null>(null)
  const [recomputeBusy, setRecomputeBusy] = useState(false)
  const [actionError, setActionError] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoadState('loading')
    setLoadError(null)
    try {
      const dash = await fetchMarketAssetDashboard(page, PAGE_SIZE)
      setData(dash)
      setLoadState('ok')
    } catch (e) {
      setLoadError(readMarketAssetApiError(e))
      setLoadState('error')
    }
  }, [page])

  useEffect(() => {
    void load()
  }, [load])

  useEffect(() => {
    if (data?.status !== 'RUNNING') return
    const id = window.setInterval(() => void load(), POLL_MS)
    return () => window.clearInterval(id)
  }, [data?.status, load])

  const onRecompute = async () => {
    setActionError(null)
    setRecomputeBusy(true)
    try {
      const res = await recomputeMarketAssets()
      if (!res.started) {
        setActionError(t('marketAssetsPage.alreadyRunning'))
      }
      await load()
    } catch (e) {
      setActionError(readMarketAssetApiError(e))
    } finally {
      setRecomputeBusy(false)
    }
  }

  const fmtNum = (v: number | null, digits = 2) =>
    v == null
      ? '—'
      : new Intl.NumberFormat(i18n.language, { maximumFractionDigits: digits, minimumFractionDigits: 0 }).format(v)

  const fmtPct = (v: number | null) => (v == null ? '—' : `${fmtNum(v, 2)}%`)

  const fmtTime = (iso: string | null) =>
    iso
      ? new Date(iso).toLocaleString(i18n.language, { dateStyle: 'medium', timeStyle: 'short' })
      : '—'

  const statusLabel =
    data?.status === 'RUNNING'
      ? t('marketAssetsPage.statusRunning')
      : data?.status === 'READY'
        ? t('marketAssetsPage.statusReady')
        : data?.status === 'FAILED'
          ? t('marketAssetsPage.statusFailed')
          : t('marketAssetsPage.statusIdle')

  return (
    <div className="fi-admin-page fi-admin-market-assets fi-admin-total-users--enterprise">
      <header className="fi-admin-page-head fi-admin-market-assets-head">
        <div>
          <h1 className="fi-admin-h1">{t('dashboard.kpi.marketStreams')}</h1>
          <p className="fi-admin-lead">{t('marketAssetsPage.lead')}</p>
          {data ? (
            <p className="fi-admin-market-assets-meta">
              {t('marketAssetsPage.meta', {
                status: statusLabel,
                computedAt: fmtTime(data.computedAt),
                rows: data.instrumentRowCount,
              })}
            </p>
          ) : null}
        </div>
        <div className="fi-admin-market-assets-actions">
          <button
            type="button"
            className="fi-admin-dir-btn fi-admin-dir-btn--primary"
            disabled={recomputeBusy || data?.status === 'RUNNING'}
            onClick={() => void onRecompute()}
          >
            {data?.status === 'RUNNING' || recomputeBusy
              ? t('marketAssetsPage.computing')
              : t('marketAssetsPage.compute')}
          </button>
          <button type="button" className="fi-admin-dash-refresh" onClick={() => void load()} disabled={loadState === 'loading'}>
            {t('dashboard.refresh')}
          </button>
        </div>
      </header>

      {actionError ? (
        <PortalAlert variant="error" className="fi-admin-market-assets-alert">
          {actionError}
        </PortalAlert>
      ) : null}

      {data?.status === 'FAILED' && data.errorMessage ? (
        <PortalAlert variant="warning" title={t('marketAssetsPage.statusFailed')}>
          {data.errorMessage}
        </PortalAlert>
      ) : null}

      {loadState === 'ok' && data ? (
        <section className="fi-admin-market-assets-kpis" aria-label={t('marketAssetsPage.kpiAria')}>
          <article className="fi-admin-market-assets-kpi">
            <h2 className="fi-admin-market-assets-kpi-label">{t('marketAssetsPage.kpiWatchlist')}</h2>
            <p className="fi-admin-market-assets-kpi-value">{fmtNum(data.avgWatchlistInstrumentsPerUser, 2)}</p>
            <p className="fi-admin-market-assets-kpi-hint">{t('marketAssetsPage.kpiWatchlistHint')}</p>
          </article>
          <article className="fi-admin-market-assets-kpi">
            <h2 className="fi-admin-market-assets-kpi-label">{t('marketAssetsPage.kpiPortfolioAssets')}</h2>
            <p className="fi-admin-market-assets-kpi-value">{fmtNum(data.avgInstrumentsPerPortfolio, 2)}</p>
            <p className="fi-admin-market-assets-kpi-hint">{t('marketAssetsPage.kpiPortfolioAssetsHint')}</p>
          </article>
          <article className="fi-admin-market-assets-kpi">
            <h2 className="fi-admin-market-assets-kpi-label">{t('marketAssetsPage.kpiDistribution')}</h2>
            <p className="fi-admin-market-assets-kpi-value">{fmtPct(data.avgPortfolioWeightPercent)}</p>
            <p className="fi-admin-market-assets-kpi-hint">{t('marketAssetsPage.kpiDistributionHint')}</p>
          </article>
        </section>
      ) : null}

      {loadState === 'loading' && (
        <div className="fi-admin-card fi-admin-market-assets-loading" aria-busy>
          <p>{t('marketAssetsPage.loading')}</p>
        </div>
      )}

      {loadState === 'error' && (
        <section className="fi-admin-card">
          <p>{loadError}</p>
          <button type="button" className="fi-admin-dash-refresh" onClick={() => void load()}>
            {t('totalUsersPage.retry')}
          </button>
        </section>
      )}

      {loadState === 'ok' && data ? (
        <section className="fi-admin-card fi-admin-market-assets-table-section">
          <h2 className="fi-admin-h2">{t('marketAssetsPage.tableTitle')}</h2>
          <p className="fi-admin-lead fi-admin-market-assets-table-lead">{t('marketAssetsPage.tableLead')}</p>
          {data.table.content.length === 0 ? (
            <p className="fi-admin-market-assets-empty-inline">{t('marketAssetsPage.empty')}</p>
          ) : (
            <>
              <div className="fi-admin-dir-table-wrap">
                <table className="fi-admin-dir-table fi-admin-market-assets-table">
                  <thead>
                    <tr>
                      <th scope="col">#</th>
                      <th scope="col">{t('marketAssetsPage.colSymbol')}</th>
                      <th scope="col">{t('marketAssetsPage.colName')}</th>
                      <th scope="col" className="fi-admin-dir-th--num">
                        {t('marketAssetsPage.colPortfolios')}
                      </th>
                      <th scope="col" className="fi-admin-dir-th--num">
                        {t('marketAssetsPage.colUsers')}
                      </th>
                      <th scope="col" className="fi-admin-dir-th--num">
                        {t('marketAssetsPage.colAvgWeight')}
                      </th>
                    </tr>
                  </thead>
                  <tbody>
                    {data.table.content.map((row) => (
                      <tr key={row.instrumentId}>
                        <td className="fi-admin-dir-td fi-admin-dir-td--muted">{row.rank}</td>
                        <td className="fi-admin-dir-td fi-admin-dir-td--mono">{row.symbol}</td>
                        <td className="fi-admin-dir-td">{row.instrumentName}</td>
                        <td className="fi-admin-dir-td fi-admin-dir-td--num">{row.portfolioCount}</td>
                        <td className="fi-admin-dir-td fi-admin-dir-td--num">{row.userCount}</td>
                        <td className="fi-admin-dir-td fi-admin-dir-td--num">{fmtPct(row.avgWeightPercent)}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <nav className="fi-admin-dir-pagination" aria-label={t('marketAssetsPage.paginationAria')}>
                <span className="fi-admin-dir-page-meta">
                  {t('marketAssetsPage.pageMeta', {
                    page: data.table.page + 1,
                    totalPages: Math.max(1, data.table.totalPages),
                    total: data.table.totalElements,
                  })}
                </span>
                <div className="fi-admin-dir-page-actions">
                  <button
                    type="button"
                    className="fi-admin-dir-btn"
                    disabled={page <= 0 || data.status === 'RUNNING'}
                    onClick={() => setPage((p) => Math.max(0, p - 1))}
                  >
                    {t('latencyPage.prev')}
                  </button>
                  <button
                    type="button"
                    className="fi-admin-dir-btn"
                    disabled={page + 1 >= data.table.totalPages || data.status === 'RUNNING'}
                    onClick={() => setPage((p) => p + 1)}
                  >
                    {t('latencyPage.next')}
                  </button>
                </div>
              </nav>
            </>
          )}
        </section>
      ) : null}
    </div>
  )
}
