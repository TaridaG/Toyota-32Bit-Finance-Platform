import { useCallback, useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import {
  fetchAdminLatencyRunsPage,
  fetchLatencyRunSamples,
  type LatencyProbeSampleItem,
  type LatencyRunRow,
} from '../../features/admin/api/adminLatencyMetrics'

const PAGE_SIZE = 10

export function AdminAvgLatencyPage() {
  const { t, i18n } = useTranslation('admin')
  const [page, setPage] = useState(0)
  const [rows, setRows] = useState<LatencyRunRow[]>([])
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [loadState, setLoadState] = useState<'loading' | 'ok' | 'error'>('loading')
  const [listError, setListError] = useState<string | null>(null)
  const [samplesByRun, setSamplesByRun] = useState<
    Record<number, { status: 'idle' | 'loading' | 'ok' | 'error'; items?: LatencyProbeSampleItem[]; message?: string }>
  >({})

  const fmtTime = (iso: string) =>
    new Date(iso).toLocaleString(i18n.language, { dateStyle: 'medium', timeStyle: 'medium' })

  const loadPage = useCallback(async () => {
    setLoadState('loading')
    setListError(null)
    try {
      const data = await fetchAdminLatencyRunsPage(page, PAGE_SIZE)
      setRows(data.content)
      setTotalPages(data.totalPages)
      setTotalElements(data.totalElements)
      setLoadState('ok')
    } catch (e) {
      setListError(e instanceof Error ? e.message : 'error')
      setLoadState('error')
    }
  }, [page])

  useEffect(() => {
    void loadPage()
  }, [loadPage])

  const pathLabel = useCallback(
    (path: string) => {
      const key = PROBE_PATH_LABEL_KEYS[path]
      return key ? t(key) : path
    },
    [t],
  )

  const ensureSamples = useCallback(
    async (runId: number) => {
      setSamplesByRun((prev) => {
        const cur = prev[runId]
        if (cur?.status === 'ok' || cur?.status === 'loading') return prev
        return { ...prev, [runId]: { status: 'loading' } }
      })
      try {
        const items = await fetchLatencyRunSamples(runId)
        setSamplesByRun((prev) => ({ ...prev, [runId]: { status: 'ok', items } }))
      } catch (e) {
        const msg = e instanceof Error ? e.message : 'error'
        setSamplesByRun((prev) => ({ ...prev, [runId]: { status: 'error', message: msg } }))
      }
    },
    [],
  )

  return (
    <div className="fi-admin-page fi-admin-latency-history">
      <header className="fi-admin-page-head">
        <div>
          <h1 className="fi-admin-h1">{t('latencyPage.title')}</h1>
          <p className="fi-admin-lead">{t('latencyPage.lead')}</p>
        </div>
        <button type="button" className="fi-admin-dash-refresh" onClick={() => void loadPage()}>
          {t('dashboard.refresh')}
        </button>
      </header>

      {loadState === 'loading' && (
        <div className="fi-admin-card fi-admin-latency-history--loading" aria-busy>
          <p>{t('latencyPage.loading')}</p>
        </div>
      )}

      {loadState === 'error' && (
        <section className="fi-admin-card fi-admin-latency-history-error">
          <p>{listError}</p>
          <button type="button" className="fi-admin-dash-refresh" onClick={() => void loadPage()}>
            {t('totalUsersPage.retry')}
          </button>
        </section>
      )}

      {loadState === 'ok' && rows.length === 0 && (
        <section className="fi-admin-card">
          <p>{t('latencyPage.noRuns')}</p>
        </section>
      )}

      {loadState === 'ok' && rows.length > 0 && (
        <>
          <section className="fi-admin-card fi-admin-latency-history-table-wrap">
            <table className="fi-admin-latency-history-table">
              <thead>
                <tr>
                  <th>{t('latencyPage.colId')}</th>
                  <th>{t('latencyPage.colAvg')}</th>
                  <th>{t('latencyPage.colWhen')}</th>
                  <th>{t('latencyPage.colDetail')}</th>
                </tr>
              </thead>
              <tbody>
                {rows.map((row) => (
                  <tr key={row.id}>
                    <td>{row.id}</td>
                    <td>
                      <strong>{row.averageLatencySec.toFixed(3)}</strong> s
                    </td>
                    <td>{fmtTime(row.measuredAt)}</td>
                    <td>
                      {row.hasSamples ? (
                        <details
                          className="fi-admin-latency-details"
                          onToggle={(ev) => {
                            if (ev.currentTarget.open) void ensureSamples(row.id)
                          }}
                        >
                          <summary className="fi-admin-latency-details-summary">{t('latencyPage.expandSamples')}</summary>
                          <div className="fi-admin-latency-details-body">
                            {(() => {
                              const st = samplesByRun[row.id]
                              if (!st || st.status === 'idle' || st.status === 'loading') {
                                return <p className="fi-admin-latency-details-muted">{t('latencyPage.samplesLoading')}</p>
                              }
                              if (st.status === 'error') {
                                return <p className="fi-admin-latency-details-err">{st.message}</p>
                              }
                              return (
                                <ul className="fi-admin-latency-sample-list">
                                  {st.items?.map((s) => (
                                    <li key={s.path}>
                                      <span className="fi-admin-latency-sample-path">{pathLabel(s.path)}</span>
                                      <span className="fi-admin-latency-sample-ms">
                                        {s.durationMs.toFixed(1)} {t('latencyPage.ms')}
                                      </span>
                                    </li>
                                  ))}
                                </ul>
                              )
                            })()}
                          </div>
                        </details>
                      ) : (
                        <span className="fi-admin-latency-details-muted">{t('latencyPage.samplesPurged')}</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </section>

          {totalPages > 1 && (
            <nav className="fi-admin-latency-pager" aria-label={t('latencyPage.paginationAria')}>
              <button
                type="button"
                className="fi-admin-latency-pager-btn"
                disabled={page <= 0}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
              >
                {t('latencyPage.prev')}
              </button>
              <span className="fi-admin-latency-pager-meta">
                {t('latencyPage.pageOf', { page: page + 1, totalPages, totalElements })}
              </span>
              <button
                type="button"
                className="fi-admin-latency-pager-btn"
                disabled={page >= totalPages - 1}
                onClick={() => setPage((p) => p + 1)}
              >
                {t('latencyPage.next')}
              </button>
            </nav>
          )}
        </>
      )}
    </div>
  )
}

const PROBE_PATH_LABEL_KEYS: Record<string, string> = {
  '/api/v1/admin/metrics/portal-users': 'latencyPage.path.portalUsers',
  '/api/v1/admin/metrics/portal-portfolios': 'latencyPage.path.portalPortfolios',
}
