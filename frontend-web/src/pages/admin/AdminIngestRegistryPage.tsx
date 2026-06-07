import { useCallback, useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import {
  deleteFromIngest,
  disableIngest,
  enableIngest,
  fetchIngestCatalog,
  triggerHistoryPull,
  triggerLivePull,
  type IngestCatalogItem,
  type IngestCatalogPage,
} from '../../features/admin/api/adminIngestRegistryApi'
import { PortalAlert } from '../../shared/components/PortalAlert'

const PAGE_SIZE = 10

export function AdminIngestRegistryPage() {
  const { t } = useTranslation('admin')
  const [page, setPage] = useState(0)
  const [catalogPage, setCatalogPage] = useState<IngestCatalogPage | null>(null)
  const [loading, setLoading] = useState(false)
  const [actionError, setActionError] = useState<string | null>(null)
  const [actionInfo, setActionInfo] = useState<string | null>(null)
  const [busyRowKey, setBusyRowKey] = useState<string | null>(null)

  const rows = catalogPage?.content ?? []

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const data = await fetchIngestCatalog(page, PAGE_SIZE)
      setCatalogPage(data)
    } finally {
      setLoading(false)
    }
  }, [page])

  useEffect(() => {
    void load()
  }, [load])

  const segmentLabel = (segment: string) => {
    const key = `ingestRegistryPage.segment.${segment}` as const
    const translated = t(key)
    return translated === key ? segment : translated
  }

  const onToggle = async (row: IngestCatalogItem) => {
    setActionError(null)
    const key = `${row.instrumentId}-${row.segment}`
    setBusyRowKey(key)
    try {
      if (row.enabled) {
        await disableIngest(row.instrumentId, row.segment)
      } else {
        await enableIngest(row.instrumentId, row.segment)
      }
      await load()
    } catch (e) {
      setActionError(e instanceof Error ? e.message : t('ingestRegistryPage.errorGeneric'))
    } finally {
      setBusyRowKey(null)
    }
  }

  const runHistoryPull = async (row: IngestCatalogItem) => {
    setActionError(null)
    setActionInfo(null)
    const key = `${row.instrumentId}-${row.segment}`
    setBusyRowKey(key)
    try {
      await triggerHistoryPull(row.instrumentId, row.segment)
      setActionInfo(t('ingestRegistryPage.historyStarted', { symbol: row.symbol }))
    } catch (e) {
      setActionError(e instanceof Error ? e.message : t('ingestRegistryPage.errorHistory'))
    } finally {
      setBusyRowKey(null)
    }
  }

  const runLivePull = async (row: IngestCatalogItem) => {
    setActionError(null)
    const key = `${row.instrumentId}-${row.segment}`
    setBusyRowKey(key)
    try {
      await triggerLivePull(row.instrumentId, row.segment)
      await load()
    } catch (e) {
      setActionError(e instanceof Error ? e.message : t('ingestRegistryPage.errorLive'))
    } finally {
      setBusyRowKey(null)
    }
  }

  const runDelete = async (row: IngestCatalogItem) => {
    setActionError(null)
    const key = `${row.instrumentId}-${row.segment}`
    setBusyRowKey(key)
    try {
      await deleteFromIngest(row.instrumentId, row.segment)
      await load()
    } catch (e) {
      setActionError(e instanceof Error ? e.message : t('ingestRegistryPage.errorDelete'))
    } finally {
      setBusyRowKey(null)
    }
  }

  const totalPages = Math.max(1, catalogPage?.totalPages ?? 1)

  return (
    <div className="fi-admin-page">
      <header className="fi-admin-page-head">
        <div>
          <h1 className="fi-admin-h1">{t('ingestRegistryPage.title')}</h1>
          <p className="fi-admin-lead">{t('ingestRegistryPage.lead')}</p>
        </div>
        <div>
          <button type="button" className="fi-admin-dash-refresh" disabled={loading} onClick={() => void load()}>
            {t('ingestRegistryPage.refresh')}
          </button>
        </div>
      </header>
      {actionInfo ? <PortalAlert variant="info">{actionInfo}</PortalAlert> : null}
      {actionError ? <PortalAlert variant="error">{actionError}</PortalAlert> : null}

      <section className="fi-admin-card fi-admin-ingest-registry-section">
        {loading && rows.length === 0 ? (
          <p className="fi-admin-ingest-empty">{t('ingestRegistryPage.loading')}</p>
        ) : null}
        {!loading && rows.length === 0 ? (
          <p className="fi-admin-ingest-empty">{t('ingestRegistryPage.empty')}</p>
        ) : null}
        {rows.length > 0 ? (
          <>
            <div className="fi-admin-dir-table-wrap">
              <table className="fi-admin-dir-table">
                <thead>
                  <tr>
                    <th scope="col">{t('ingestRegistryPage.colInstrumentId')}</th>
                    <th scope="col">{t('ingestRegistryPage.colSymbol')}</th>
                    <th scope="col">{t('ingestRegistryPage.colSegment')}</th>
                    <th scope="col">{t('ingestRegistryPage.colEnabled')}</th>
                    <th scope="col">{t('ingestRegistryPage.colTotalDays')}</th>
                    <th scope="col">{t('ingestRegistryPage.colRecent365')}</th>
                    <th scope="col">{t('ingestRegistryPage.colRecent30')}</th>
                    <th scope="col">{t('ingestRegistryPage.colLastError')}</th>
                    <th scope="col">{t('ingestRegistryPage.colActions')}</th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((row) => (
                    <tr key={`${row.instrumentId}-${row.segment}`}>
                      <td>{row.instrumentId}</td>
                      <td className="fi-admin-dir-td--mono">{row.symbol}</td>
                      <td>{segmentLabel(row.segment)}</td>
                      <td>
                        {busyRowKey === `${row.instrumentId}-${row.segment}` ? <span>…</span> : null}
                        <input
                          type="checkbox"
                          checked={row.enabled}
                          disabled={busyRowKey === `${row.instrumentId}-${row.segment}`}
                          onChange={() => void onToggle(row)}
                          aria-label={t('ingestRegistryPage.colEnabled')}
                        />
                      </td>
                      <td>{row.totalDays ?? '—'}</td>
                      <td>{row.recentDays ?? '—'}</td>
                      <td>{row.recent30Days ?? '—'}</td>
                      <td>{row.lastError ? row.lastError : '—'}</td>
                      <td>
                        <div className="fi-admin-ingest-actions">
                          <button
                            type="button"
                            className="fi-admin-dir-btn"
                            disabled={busyRowKey === `${row.instrumentId}-${row.segment}`}
                            onClick={() => void runHistoryPull(row)}
                          >
                            {t('ingestRegistryPage.actionHistory')}
                          </button>
                          <button
                            type="button"
                            className="fi-admin-dir-btn"
                            disabled={busyRowKey === `${row.instrumentId}-${row.segment}`}
                            onClick={() => void runLivePull(row)}
                          >
                            {t('ingestRegistryPage.actionLive')}
                          </button>
                          <button
                            type="button"
                            className="fi-admin-dir-btn fi-admin-dir-action-btn--danger"
                            disabled={busyRowKey === `${row.instrumentId}-${row.segment}`}
                            onClick={() => void runDelete(row)}
                          >
                            {t('ingestRegistryPage.actionDelete')}
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <nav className="fi-admin-dir-pagination" aria-label={t('ingestRegistryPage.paginationAria')}>
              <span className="fi-admin-dir-page-meta">
                {t('ingestRegistryPage.pageMeta', {
                  page: (catalogPage?.page ?? 0) + 1,
                  totalPages,
                  total: catalogPage?.totalElements ?? 0,
                })}
              </span>
              <div className="fi-admin-dir-page-actions">
                <button
                  type="button"
                  className="fi-admin-dir-btn"
                  disabled={page <= 0 || loading}
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                >
                  {t('ingestRegistryPage.prev')}
                </button>
                <button
                  type="button"
                  className="fi-admin-dir-btn"
                  disabled={page + 1 >= totalPages || loading}
                  onClick={() => setPage((p) => p + 1)}
                >
                  {t('ingestRegistryPage.next')}
                </button>
              </div>
            </nav>
          </>
        ) : null}
      </section>
    </div>
  )
}
