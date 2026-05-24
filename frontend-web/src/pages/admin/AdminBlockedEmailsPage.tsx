import { useCallback, useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import {
  fetchBlockedEmails,
  readBlockedEmailsApiError,
  unblockBlockedEmail,
  type BlockedEmailRow,
  type BlockedEmailsPage,
} from '../../features/admin/api/adminBlockedEmailsApi'
import { PortalAlert } from '../../shared/components/PortalAlert'

const PAGE_SIZE = 10

export function AdminBlockedEmailsPage() {
  const { t, i18n } = useTranslation('admin')
  const [page, setPage] = useState(0)
  const [data, setData] = useState<BlockedEmailsPage | null>(null)
  const [loadState, setLoadState] = useState<'loading' | 'ok' | 'error'>('loading')
  const [loadError, setLoadError] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)
  const [modalRow, setModalRow] = useState<BlockedEmailRow | null>(null)
  const [unblockBusy, setUnblockBusy] = useState(false)

  const load = useCallback(async () => {
    setLoadState('loading')
    setLoadError(null)
    try {
      const pageData = await fetchBlockedEmails(page, PAGE_SIZE)
      setData(pageData)
      setLoadState('ok')
    } catch (e) {
      setLoadError(readBlockedEmailsApiError(e))
      setLoadState('error')
    }
  }, [page])

  useEffect(() => {
    void load()
  }, [load])

  useEffect(() => {
    if (loadState !== 'ok' || !data) return
    if (data.totalPages > 0 && page >= data.totalPages) {
      setPage(data.totalPages - 1)
    }
  }, [loadState, data, page])

  const fmtTime = (iso: string) =>
    new Date(iso).toLocaleString(i18n.language, { dateStyle: 'medium', timeStyle: 'short' })

  const confirmUnblock = async () => {
    if (!modalRow) return
    setActionError(null)
    setUnblockBusy(true)
    try {
      await unblockBlockedEmail(modalRow.id)
      setModalRow(null)
      await load()
    } catch (e) {
      setActionError(readBlockedEmailsApiError(e))
    } finally {
      setUnblockBusy(false)
    }
  }

  const rows = data?.content ?? []

  return (
    <div className="fi-admin-page">
      <header className="fi-admin-page-head">
        <h1 className="fi-admin-page-title">{t('blockedEmailsPage.title')}</h1>
        <p className="fi-admin-page-lead">{t('blockedEmailsPage.lead')}</p>
      </header>

      {actionError ? <PortalAlert variant="error">{actionError}</PortalAlert> : null}

      {loadState === 'loading' && (
        <div className="fi-admin-dir-loading" aria-busy>
          {t('blockedEmailsPage.loading')}
        </div>
      )}

      {loadState === 'error' && (
        <div className="fi-admin-dir-error">
          <PortalAlert variant="error">{loadError}</PortalAlert>
          <button type="button" className="fi-admin-dash-refresh" onClick={() => void load()}>
            {t('totalUsersPage.retry')}
          </button>
        </div>
      )}

      {loadState === 'ok' && data && (
        <section className="fi-admin-card fi-admin-blocked-emails-card" aria-labelledby="fi-admin-blocked-list-title">
          <div className="fi-admin-blocked-emails-card-head">
            <h2 id="fi-admin-blocked-list-title" className="fi-admin-h2">
              {t('blockedEmailsPage.listTitle')}
            </h2>
            {data.totalElements > 0 ? (
              <span className="fi-admin-blocked-emails-badge" aria-label={t('blockedEmailsPage.totalBadge', { count: data.totalElements })}>
                {data.totalElements}
              </span>
            ) : null}
          </div>

          {rows.length === 0 ? (
            <div className="fi-admin-blocked-empty">
              <BlockedEmptyIcon />
              <p>{t('blockedEmailsPage.empty')}</p>
            </div>
          ) : (
            <div className="fi-admin-dir-table-wrap fi-admin-blocked-table-wrap">
              <table className="fi-admin-dir-table fi-admin-blocked-table">
                <colgroup>
                  <col className="fi-admin-blocked-col fi-admin-blocked-col--email" />
                  <col className="fi-admin-blocked-col fi-admin-blocked-col--time" />
                  <col className="fi-admin-blocked-col fi-admin-blocked-col--actions" />
                </colgroup>
                <thead>
                  <tr>
                    <th className="fi-admin-dir-th" scope="col">
                      {t('blockedEmailsPage.colEmail')}
                    </th>
                    <th className="fi-admin-dir-th" scope="col">
                      {t('blockedEmailsPage.colBlockedAt')}
                    </th>
                    <th className="fi-admin-dir-th fi-admin-dir-th--actions" scope="col">
                      {t('blockedEmailsPage.colActions')}
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {rows.map((row) => (
                    <tr key={row.id} className="fi-admin-blocked-row">
                      <td className="fi-admin-dir-td">
                        <div className="fi-admin-blocked-email-cell">
                          <span className="fi-admin-blocked-email-icon" aria-hidden>
                            <MailBlockedIcon />
                          </span>
                          <span className="fi-admin-blocked-email-text">{row.email}</span>
                        </div>
                      </td>
                      <td className="fi-admin-dir-td fi-admin-dir-td--muted">
                        <time className="fi-admin-blocked-time" dateTime={row.blockedAt}>
                          <ClockIcon />
                          {fmtTime(row.blockedAt)}
                        </time>
                      </td>
                      <td className="fi-admin-dir-td fi-admin-dir-td--actions">
                        <div className="fi-admin-dir-row-actions">
                          <button
                            type="button"
                            className="fi-admin-dir-action-btn fi-admin-dir-action-btn--success fi-admin-blocked-unblock-btn"
                            onClick={() => {
                              setActionError(null)
                              setModalRow(row)
                            }}
                          >
                            <UnlockIcon />
                            {t('blockedEmailsPage.actionUnblock')}
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          <nav className="fi-admin-dir-pagination fi-admin-blocked-pagination" aria-label={t('blockedEmailsPage.paginationAria')}>
            <span className="fi-admin-dir-page-meta">
              {data.totalElements === 0
                ? t('blockedEmailsPage.emptyMeta')
                : t('blockedEmailsPage.pageMeta', {
                    page: data.page + 1,
                    totalPages: data.totalPages,
                    total: data.totalElements,
                  })}
            </span>
            {data.totalElements > 0 ? (
              <div className="fi-admin-dir-page-actions">
                <button
                  type="button"
                  className="fi-admin-dir-btn"
                  disabled={page <= 0 || data.totalPages === 0}
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                >
                  {t('latencyPage.prev')}
                </button>
                <button
                  type="button"
                  className="fi-admin-dir-btn"
                  disabled={data.totalPages <= 1 || page >= data.totalPages - 1}
                  onClick={() => setPage((p) => p + 1)}
                >
                  {t('latencyPage.next')}
                </button>
              </div>
            ) : null}
          </nav>
        </section>
      )}

      {modalRow ? (
        <div className="fi-admin-modal-backdrop" role="presentation" onClick={() => setModalRow(null)}>
          <div
            className="fi-admin-modal fi-admin-modal--unblock"
            role="dialog"
            aria-modal="true"
            aria-labelledby="fi-admin-unblock-modal-title"
            onClick={(e) => e.stopPropagation()}
          >
            <h3 id="fi-admin-unblock-modal-title" className="fi-admin-modal-title">
              {t('blockedEmailsPage.modalUnblockTitle')}
            </h3>
            <p className="fi-admin-modal-lead">
              {t('blockedEmailsPage.modalUnblockLead', { email: modalRow.email })}
            </p>
            <p className="fi-admin-modal-email">{modalRow.email}</p>
            <p className="fi-admin-modal-note">{t('blockedEmailsPage.modalUnblockNotify')}</p>
            <div className="fi-admin-modal-actions">
              <button type="button" className="fi-admin-dir-btn" onClick={() => setModalRow(null)} disabled={unblockBusy}>
                {t('totalUsersPage.directory.modalCancel')}
              </button>
              <button
                type="button"
                className="fi-admin-dir-btn fi-admin-dir-btn--primary"
                disabled={unblockBusy}
                onClick={() => void confirmUnblock()}
              >
                {unblockBusy
                  ? t('blockedEmailsPage.modalUnblocking')
                  : t('blockedEmailsPage.actionUnblockConfirm')}
              </button>
            </div>
          </div>
        </div>
      ) : null}
    </div>
  )
}

function MailBlockedIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" aria-hidden>
      <path d="M4 6h16v12H4z" strokeLinejoin="round" />
      <path d="m4 7 8 6 8-6" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M3 3l18 18" strokeLinecap="round" />
    </svg>
  )
}

function ClockIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" aria-hidden>
      <circle cx="12" cy="12" r="9" />
      <path d="M12 7v5l3 2" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  )
}

function UnlockIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" aria-hidden>
      <path d="M7 11V8a5 5 0 0 1 9.9-1" strokeLinecap="round" />
      <rect x="5" y="11" width="14" height="10" rx="2" />
      <path d="M12 15v2" strokeLinecap="round" />
    </svg>
  )
}

function BlockedEmptyIcon() {
  return (
    <span className="fi-admin-blocked-empty-icon" aria-hidden>
      <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.25">
        <path d="M4 6h16v12H4z" strokeLinejoin="round" />
        <path d="m4 7 8 6 8-6" strokeLinecap="round" strokeLinejoin="round" />
        <path d="M3 3l18 18" strokeLinecap="round" />
      </svg>
    </span>
  )
}
