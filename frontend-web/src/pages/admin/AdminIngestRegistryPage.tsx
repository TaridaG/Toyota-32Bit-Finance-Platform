import { useEffect, useState } from 'react'
import {
  deleteFromIngest,
  disableIngest,
  enableIngest,
  fetchIngestCatalog,
  triggerHistoryPull,
  triggerLivePull,
  type IngestCatalogItem,
} from '../../features/admin/api/adminIngestRegistryApi'
import { PortalAlert } from '../../shared/components/PortalAlert'

export function AdminIngestRegistryPage() {
  const [rows, setRows] = useState<IngestCatalogItem[]>([])
  const [loading, setLoading] = useState(false)
  const [actionError, setActionError] = useState<string | null>(null)
  const [actionInfo, setActionInfo] = useState<string | null>(null)
  const [busyRowKey, setBusyRowKey] = useState<string | null>(null)

  const load = async () => {
    setLoading(true)
    try {
      const data = await fetchIngestCatalog()
      setRows(data)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void load()
  }, [])

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
      setActionError(e instanceof Error ? e.message : 'Islem basarisiz.')
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
      setActionInfo(`${row.symbol} icin tarihsel veri arka planda cekiliyor. Birkac dakika sonra yenileyin.`)
    } catch (e) {
      setActionError(e instanceof Error ? e.message : 'Tarihsel veri cekme basarisiz.')
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
      setActionError(e instanceof Error ? e.message : 'Canli veri cekme basarisiz.')
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
      setActionError(e instanceof Error ? e.message : 'Silme islemi basarisiz.')
    } finally {
      setBusyRowKey(null)
    }
  }

  return (
    <div className="fi-admin-page">
      <header className="fi-admin-page-head">
        <div>
          <h1 className="fi-admin-h1">Ingest registry</h1>
          <p className="fi-admin-lead">
            Hangi enstrümanlar için canlı ve geçmiş veri çekildiğini yönetin.
          </p>
        </div>
        <div>
          <button type="button" className="fi-admin-dash-refresh" disabled={loading} onClick={() => void load()}>
            Yenile
          </button>
        </div>
      </header>
      {actionInfo ? <PortalAlert variant="info">{actionInfo}</PortalAlert> : null}
      {actionError ? <PortalAlert variant="error">{actionError}</PortalAlert> : null}

      <section className="fi-admin-section">
        <table className="fi-admin-dir-table">
          <thead>
            <tr>
              <th>Instrument ID</th>
              <th>Symbol</th>
              <th>Segment</th>
              <th>Enabled</th>
              <th>Total days</th>
              <th>365d</th>
              <th>30d</th>
              <th>Last error</th>
              <th>Aksiyonlar</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((row) => (
              <tr key={`${row.instrumentId}-${row.segment}`}>
                <td>{row.instrumentId}</td>
                <td>{row.symbol}</td>
                <td>{row.segment}</td>
                <td>
                  {busyRowKey === `${row.instrumentId}-${row.segment}` ? (
                    <span>...</span>
                  ) : null}
                  <input
                    type="checkbox"
                    checked={row.enabled}
                    disabled={busyRowKey === `${row.instrumentId}-${row.segment}`}
                    onChange={() => void onToggle(row)}
                  />
                </td>
                <td>{row.totalDays ?? '-'}</td>
                <td>{row.recentDays ?? '-'}</td>
                <td>{row.recent30Days ?? '-'}</td>
                <td>{row.lastError ? row.lastError : '-'}</td>
                <td>
                  <div style={{ display: 'flex', gap: '0.5rem', justifyContent: 'flex-end' }}>
                    <button
                      type="button"
                      className="fi-admin-dir-btn"
                      disabled={busyRowKey === `${row.instrumentId}-${row.segment}`}
                      onClick={() => void runHistoryPull(row)}
                    >
                      Tarihsel veri cek
                    </button>
                    <button
                      type="button"
                      className="fi-admin-dir-btn"
                      disabled={busyRowKey === `${row.instrumentId}-${row.segment}`}
                      onClick={() => void runLivePull(row)}
                    >
                      Canli veri cek
                    </button>
                    <button
                      type="button"
                      className="fi-admin-dir-btn fi-admin-dir-action-btn--danger"
                      disabled={busyRowKey === `${row.instrumentId}-${row.segment}`}
                      onClick={() => void runDelete(row)}
                    >
                      Sil
                    </button>
                  </div>
                </td>
              </tr>
            ))}
            {rows.length === 0 && !loading ? (
              <tr>
                <td colSpan={9}>Henüz ingest konfigürasyonu bulunmuyor.</td>
              </tr>
            ) : null}
          </tbody>
        </table>
      </section>
    </div>
  )
}

