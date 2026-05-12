import { Fragment, useCallback, useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import {
  fetchAdminUserAvatarBlob,
  fetchAdminUserPortfolioTree,
  type AdminUserDirectoryQuery,
  type AdminUserListItem,
  type AdminUserPortfolioTree,
} from '../../features/admin/api/adminUserDirectoryApi'
import { useAdminUserDirectory } from '../../features/admin/hooks/useAdminUserDirectory'

type SortColumn = 'createdAt' | 'username' | 'email' | 'emailVerified' | 'portfolioCount'

function parseSort(sort: string): { col: SortColumn; dir: 'asc' | 'desc' } {
  const [c, d] = sort.split(',')
  const dir = d === 'asc' ? 'asc' : 'desc'
  const col = (['createdAt', 'username', 'email', 'emailVerified', 'portfolioCount'] as const).includes(
    c as SortColumn,
  )
    ? (c as SortColumn)
    : 'createdAt'
  return { col, dir }
}

function nextSort(current: string, column: SortColumn): string {
  const { col, dir } = parseSort(current)
  if (col === column) {
    return `${column},${dir === 'asc' ? 'desc' : 'asc'}`
  }
  const defaultDir: 'asc' | 'desc' =
    column === 'username' || column === 'email' ? 'asc' : column === 'emailVerified' ? 'desc' : 'desc'
  return `${column},${defaultDir}`
}

function utcDayStartIso(dateYmd: string): string {
  return `${dateYmd}T00:00:00.000Z`
}

/** Exclusive upper bound: the instant after the last included UTC calendar day. */
function utcDayAfterLastInclusiveIso(dateYmd: string): string {
  const d = new Date(`${dateYmd}T00:00:00.000Z`)
  d.setUTCDate(d.getUTCDate() + 1)
  return d.toISOString()
}

function AdminDirectoryAvatar({
  userId,
  hasAvatar,
  initials,
}: {
  userId: string
  hasAvatar: boolean
  initials: string
}) {
  const [objectUrl, setObjectUrl] = useState<string | null>(null)

  useEffect(() => {
    if (!hasAvatar) {
      setObjectUrl(null)
      return
    }
    let url: string | null = null
    let cancelled = false
    void (async () => {
      const blob = await fetchAdminUserAvatarBlob(userId)
      if (cancelled || !blob) return
      url = URL.createObjectURL(blob)
      setObjectUrl(url)
    })()
    return () => {
      cancelled = true
      if (url) URL.revokeObjectURL(url)
    }
  }, [userId, hasAvatar])

  if (objectUrl) {
    return <img src={objectUrl} alt="" className="fi-admin-dir-avatar-img" width={40} height={40} />
  }
  return (
    <span className="fi-admin-dir-avatar-fallback" aria-hidden>
      {initials.slice(0, 2).toUpperCase()}
    </span>
  )
}

function SortCaret({ active, dir }: { active: boolean; dir: 'asc' | 'desc' }) {
  if (!active) return <span className="fi-admin-dir-th-caret fi-admin-dir-th-caret--muted" aria-hidden />
  return (
    <span className="fi-admin-dir-th-caret" aria-hidden>
      {dir === 'asc' ? '▲' : '▼'}
    </span>
  )
}

export function AdminUserDirectorySection() {
  const { t, i18n } = useTranslation('admin')
  const [page, setPage] = useState(0)
  const [size, setSize] = useState<10 | 20 | 50>(10)
  const [sort, setSort] = useState('createdAt,desc')

  const [draftFrom, setDraftFrom] = useState('')
  const [draftTo, setDraftTo] = useState('')
  const [draftEmail, setDraftEmail] = useState<'all' | 'yes' | 'no'>('all')
  const [draftPortfolio, setDraftPortfolio] = useState<'all' | '1' | '2' | '3' | '4' | '5'>('all')

  const [appliedFrom, setAppliedFrom] = useState<string | undefined>()
  const [appliedToExclusive, setAppliedToExclusive] = useState<string | undefined>()
  const [appliedEmail, setAppliedEmail] = useState<'all' | 'yes' | 'no'>('all')
  const [appliedPortfolio, setAppliedPortfolio] = useState<'all' | '1' | '2' | '3' | '4' | '5'>('all')
  const [filterError, setFilterError] = useState<string | null>(null)
  const [filtersOpen, setFiltersOpen] = useState(false)
  const [expandedUserId, setExpandedUserId] = useState<string | null>(null)
  const [portfolioTrees, setPortfolioTrees] = useState<Record<string, AdminUserPortfolioTree>>({})
  const [portfolioLoadingId, setPortfolioLoadingId] = useState<string | null>(null)
  const [portfolioError, setPortfolioError] = useState<Record<string, string>>({})

  const openOrClosePortfolios = useCallback((userId: string) => {
    void (async () => {
      if (expandedUserId === userId) {
        setExpandedUserId(null)
        return
      }
      setExpandedUserId(userId)
      if (portfolioTrees[userId]) return
      setPortfolioLoadingId(userId)
      setPortfolioError((m) => {
        const n = { ...m }
        delete n[userId]
        return n
      })
      try {
        const tree = await fetchAdminUserPortfolioTree(userId)
        setPortfolioTrees((m) => ({ ...m, [userId]: tree }))
      } catch (e) {
        const msg = e instanceof Error ? e.message : String(e)
        setPortfolioError((m) => ({ ...m, [userId]: msg }))
      } finally {
        setPortfolioLoadingId(null)
      }
    })()
  }, [expandedUserId, portfolioTrees])

  const refetchPortfolios = useCallback((userId: string) => {
    void (async () => {
      setPortfolioLoadingId(userId)
      setPortfolioError((m) => {
        const n = { ...m }
        delete n[userId]
        return n
      })
      try {
        const tree = await fetchAdminUserPortfolioTree(userId)
        setPortfolioTrees((m) => ({ ...m, [userId]: tree }))
      } catch (e) {
        const msg = e instanceof Error ? e.message : String(e)
        setPortfolioError((m) => ({ ...m, [userId]: msg }))
      } finally {
        setPortfolioLoadingId(null)
      }
    })()
  }, [])

  const query = useMemo((): AdminUserDirectoryQuery => {
    const { col, dir } = parseSort(sort)
    const q: AdminUserDirectoryQuery = {
      page,
      size,
      sortField: col,
      sortDir: dir,
    }
    if (appliedFrom) q.registeredFrom = appliedFrom
    if (appliedToExclusive) q.registeredToExclusive = appliedToExclusive
    if (appliedEmail === 'yes') q.emailVerified = true
    if (appliedEmail === 'no') q.emailVerified = false
    if (appliedPortfolio !== 'all') q.portfolioCount = Number(appliedPortfolio)
    return q
  }, [page, size, sort, appliedFrom, appliedToExclusive, appliedEmail, appliedPortfolio])

  const { state, refetch } = useAdminUserDirectory(query)
  const { col: sortCol, dir: sortDir } = parseSort(sort)

  const applyFilters = useCallback(() => {
    setFilterError(null)
    let fromIso: string | undefined
    let toEx: string | undefined
    if (draftFrom) fromIso = utcDayStartIso(draftFrom)
    if (draftTo) toEx = utcDayAfterLastInclusiveIso(draftTo)
    if (fromIso && toEx && new Date(toEx) <= new Date(fromIso)) {
      setFilterError(t('totalUsersPage.directory.dateRangeInvalid'))
      return
    }
    setAppliedFrom(fromIso)
    setAppliedToExclusive(toEx)
    setAppliedEmail(draftEmail)
    setAppliedPortfolio(draftPortfolio)
    setPage(0)
  }, [draftFrom, draftTo, draftEmail, draftPortfolio, t])

  const clearFilters = useCallback(() => {
    setFilterError(null)
    setDraftFrom('')
    setDraftTo('')
    setDraftEmail('all')
    setDraftPortfolio('all')
    setAppliedFrom(undefined)
    setAppliedToExclusive(undefined)
    setAppliedEmail('all')
    setAppliedPortfolio('all')
    setPage(0)
  }, [])

  const onSort = (column: SortColumn) => {
    setSort((s) => nextSort(s, column))
    setPage(0)
  }

  useEffect(() => {
    if (state.status !== 'ok') return
    const tp = state.data.totalPages
    if (tp > 0 && page >= tp) {
      setPage(tp - 1)
    }
  }, [state, page])

  const fmtRegistered = useCallback(
    (iso: string) =>
      new Date(iso).toLocaleString(i18n.language, {
        dateStyle: 'medium',
        timeStyle: 'short',
      }),
    [i18n.language],
  )

  const fmtPortfolioOpened = useCallback(
    (localIso: string) => {
      const d = new Date(localIso.includes('T') ? localIso : localIso.replace(' ', 'T'))
      if (Number.isNaN(d.getTime())) return localIso
      return d.toLocaleString(i18n.language, { dateStyle: 'medium', timeStyle: 'short' })
    },
    [i18n.language],
  )

  const fmtWeight = useCallback(
    (pct: number) =>
      new Intl.NumberFormat(i18n.language, { maximumFractionDigits: 1, minimumFractionDigits: 0 }).format(pct) + '%',
    [i18n.language],
  )

  const fmtCompact = useCallback(
    (n: number) =>
      new Intl.NumberFormat(i18n.language, { notation: 'compact', maximumFractionDigits: 1 }).format(n),
    [i18n.language],
  )

  const headerButton = (column: SortColumn, label: string) => {
    const active = sortCol === column
    return (
      <button type="button" className="fi-admin-dir-th-btn" onClick={() => onSort(column)}>
        <span>{label}</span>
        <SortCaret active={active} dir={sortDir} />
      </button>
    )
  }

  const rows: AdminUserListItem[] = state.status === 'ok' ? state.data.content : []

  return (
    <section className="fi-admin-dir" aria-labelledby="fi-admin-dir-title">
      <div className="fi-admin-dir-head">
        <div>
          <h2 id="fi-admin-dir-title" className="fi-admin-dir-title">
            {t('totalUsersPage.directory.title')}
          </h2>
          <p className="fi-admin-dir-lead">{t('totalUsersPage.directory.lead')}</p>
          {state.status === 'ok' && (
            <p className="fi-admin-dir-roster-total">
              {t('totalUsersPage.directory.rosterTotal', { count: state.data.totalElements })}
            </p>
          )}
        </div>
        <div className="fi-admin-dir-toolbar">
          <label className="fi-admin-dir-page-size">
            <span className="fi-admin-dir-page-size-label">{t('totalUsersPage.directory.pageSize')}</span>
            <select
              value={size}
              onChange={(e) => {
                setSize(Number(e.target.value) as 10 | 20 | 50)
                setPage(0)
              }}
            >
              <option value={10}>10</option>
              <option value={20}>20</option>
              <option value={50}>50</option>
            </select>
          </label>
          <div className="fi-admin-dir-toolbar-actions">
            <button
              type="button"
              className="fi-admin-dash-refresh"
              aria-expanded={filtersOpen}
              aria-controls="fi-admin-dir-filters-panel"
              onClick={() => setFiltersOpen((o) => !o)}
            >
              {t('totalUsersPage.directory.filtersToggle')}
            </button>
            <button type="button" className="fi-admin-dash-refresh" onClick={() => void refetch()}>
              {t('dashboard.refresh')}
            </button>
          </div>
        </div>
      </div>

      {filtersOpen && (
        <div id="fi-admin-dir-filters-panel" className="fi-admin-dir-filters">
          <div className="fi-admin-dir-filters-body">
            <div className="fi-admin-dir-filter-grid">
            <label className="fi-admin-dir-field">
              <span>{t('totalUsersPage.directory.dateFrom')}</span>
              <input type="date" value={draftFrom} onChange={(e) => setDraftFrom(e.target.value)} />
            </label>
            <label className="fi-admin-dir-field">
              <span>{t('totalUsersPage.directory.dateTo')}</span>
              <input type="date" value={draftTo} onChange={(e) => setDraftTo(e.target.value)} />
            </label>
            <label className="fi-admin-dir-field">
              <span>{t('totalUsersPage.directory.emailVerified')}</span>
              <select
                value={draftEmail}
                onChange={(e) => setDraftEmail(e.target.value as 'all' | 'yes' | 'no')}
              >
                <option value="all">{t('totalUsersPage.directory.emailAll')}</option>
                <option value="yes">{t('totalUsersPage.directory.emailYes')}</option>
                <option value="no">{t('totalUsersPage.directory.emailNo')}</option>
              </select>
            </label>
            <label className="fi-admin-dir-field">
              <span>{t('totalUsersPage.directory.portfolioCount')}</span>
              <select
                value={draftPortfolio}
                onChange={(e) =>
                  setDraftPortfolio(e.target.value as 'all' | '1' | '2' | '3' | '4' | '5')
                }
              >
                <option value="all">{t('totalUsersPage.directory.portfolioAny')}</option>
                <option value="1">1</option>
                <option value="2">2</option>
                <option value="3">3</option>
                <option value="4">4</option>
                <option value="5">5</option>
              </select>
            </label>
            </div>
            <p className="fi-admin-dir-filters-hint">{t('totalUsersPage.directory.utcHint')}</p>
            {filterError && <p className="fi-admin-dir-filter-error">{filterError}</p>}
            <div className="fi-admin-dir-filters-actions">
              <button type="button" className="fi-admin-dir-btn fi-admin-dir-btn--primary" onClick={applyFilters}>
                {t('totalUsersPage.directory.apply')}
              </button>
              <button type="button" className="fi-admin-dir-btn" onClick={clearFilters}>
                {t('totalUsersPage.directory.clear')}
              </button>
            </div>
          </div>
        </div>
      )}

      {state.status === 'loading' && (
        <div className="fi-admin-dir-loading" aria-busy>
          {t('totalUsersPage.directory.loading')}
        </div>
      )}

      {state.status === 'error' && (
        <div className="fi-admin-dir-error">
          <p>{state.message}</p>
          <button type="button" className="fi-admin-dash-refresh" onClick={() => void refetch()}>
            {t('totalUsersPage.retry')}
          </button>
        </div>
      )}

      {state.status === 'ok' && (
        <>
          <div className="fi-admin-dir-table-wrap">
            <table className="fi-admin-dir-table">
              <thead>
                <tr>
                  <th className="fi-admin-dir-th fi-admin-dir-th--expand" scope="col">
                    <span className="fi-admin-dir-th-sr">{t('totalUsersPage.directory.colExpand')}</span>
                  </th>
                  <th className="fi-admin-dir-th fi-admin-dir-th--avatar" scope="col">
                    {t('totalUsersPage.directory.colAvatar')}
                  </th>
                  <th className="fi-admin-dir-th" scope="col">
                    {headerButton('username', t('totalUsersPage.directory.colUsername'))}
                  </th>
                  <th className="fi-admin-dir-th" scope="col">
                    {headerButton('email', t('totalUsersPage.directory.colEmail'))}
                  </th>
                  <th className="fi-admin-dir-th" scope="col">
                    {headerButton('emailVerified', t('totalUsersPage.directory.colVerified'))}
                  </th>
                  <th className="fi-admin-dir-th" scope="col">
                    {headerButton('createdAt', t('totalUsersPage.directory.colRegistered'))}
                  </th>
                  <th className="fi-admin-dir-th fi-admin-dir-th--num" scope="col">
                    {headerButton('portfolioCount', t('totalUsersPage.directory.colPortfolios'))}
                  </th>
                </tr>
              </thead>
              <tbody>
                {rows.length === 0 ? (
                  <tr>
                    <td colSpan={7} className="fi-admin-dir-empty">
                      {t('totalUsersPage.directory.empty')}
                    </td>
                  </tr>
                ) : (
                  rows.map((u) => {
                    const expanded = expandedUserId === u.id
                    const tree = portfolioTrees[u.id]
                    const pErr = portfolioError[u.id]
                    const pLoading = portfolioLoadingId === u.id && !tree
                    return (
                      <Fragment key={u.id}>
                        <tr>
                          <td className="fi-admin-dir-td fi-admin-dir-td--expand">
                            <button
                              type="button"
                              className={`fi-admin-dir-expand ${expanded ? 'fi-admin-dir-expand--open' : ''}`}
                              aria-expanded={expanded}
                              aria-controls={`fi-admin-dir-detail-${u.id}`}
                              onClick={() => openOrClosePortfolios(u.id)}
                              title={
                                expanded
                                  ? t('totalUsersPage.directory.collapsePortfolios')
                                  : t('totalUsersPage.directory.expandPortfolios')
                              }
                            >
                              <span aria-hidden>{expanded ? '▼' : '▶'}</span>
                            </button>
                          </td>
                          <td className="fi-admin-dir-td fi-admin-dir-td--avatar">
                            <AdminDirectoryAvatar
                              userId={u.id}
                              hasAvatar={u.hasProfileAvatar}
                              initials={u.username || u.email || '?'}
                            />
                          </td>
                          <td className="fi-admin-dir-td">{u.username}</td>
                          <td className="fi-admin-dir-td fi-admin-dir-td--mono">{u.email}</td>
                          <td className="fi-admin-dir-td">
                            <span
                              className={
                                u.emailVerified
                                  ? 'fi-admin-dir-pill fi-admin-dir-pill--ok'
                                  : 'fi-admin-dir-pill fi-admin-dir-pill--no'
                              }
                            >
                              {u.emailVerified
                                ? t('totalUsersPage.directory.verifiedYes')
                                : t('totalUsersPage.directory.verifiedNo')}
                            </span>
                          </td>
                          <td className="fi-admin-dir-td fi-admin-dir-td--muted">{fmtRegistered(u.createdAt)}</td>
                          <td className="fi-admin-dir-td fi-admin-dir-td--num">{u.portfolioCount}</td>
                        </tr>
                        {expanded && (
                          <tr className="fi-admin-dir-detail-tr" id={`fi-admin-dir-detail-${u.id}`}>
                            <td colSpan={7} className="fi-admin-dir-detail-td">
                              {pLoading && (
                                <div className="fi-admin-dir-detail-loading">
                                  {t('totalUsersPage.directory.portfoliosLoading')}
                                </div>
                              )}
                              {!pLoading && pErr && (
                                <div className="fi-admin-dir-detail-error">
                                  <p>{pErr}</p>
                                  <button
                                    type="button"
                                    className="fi-admin-dir-btn"
                                    onClick={() => refetchPortfolios(u.id)}
                                  >
                                    {t('totalUsersPage.directory.portfoliosRetry')}
                                  </button>
                                </div>
                              )}
                              {!pLoading && !pErr && tree && (
                                <div className="fi-admin-dir-nested">
                                  {tree.portfolios.length === 0 ? (
                                    <p className="fi-admin-dir-nested-empty">
                                      {t('totalUsersPage.directory.noPortfolios')}
                                    </p>
                                  ) : (
                                    tree.portfolios.map((p) => (
                                      <div key={p.portfolioId} className="fi-admin-dir-nested-card">
                                        <div className="fi-admin-dir-nested-head">
                                          <h3 className="fi-admin-dir-nested-title">{p.name}</h3>
                                          <p className="fi-admin-dir-nested-meta">
                                            {t('totalUsersPage.directory.portfolioOpened')}:{' '}
                                            {fmtPortfolioOpened(p.createdAt)} · {p.baseCurrency}
                                          </p>
                                        </div>
                                        {p.holdings.length === 0 ? (
                                          <p className="fi-admin-dir-nested-empty">
                                            {t('totalUsersPage.directory.noHoldings')}
                                          </p>
                                        ) : (
                                          <table className="fi-admin-dir-nested-table">
                                            <thead>
                                              <tr>
                                                <th>{t('totalUsersPage.directory.hColSymbol')}</th>
                                                <th>{t('totalUsersPage.directory.hColName')}</th>
                                                <th className="fi-admin-dir-nested-num">
                                                  {t('totalUsersPage.directory.hColWeight')}
                                                </th>
                                                <th className="fi-admin-dir-nested-num">
                                                  {t('totalUsersPage.directory.hColValue')}
                                                </th>
                                              </tr>
                                            </thead>
                                            <tbody>
                                              {p.holdings.map((h) => (
                                                <tr key={`${p.portfolioId}-${h.symbol}`}>
                                                  <td className="fi-admin-dir-nested-mono">{h.symbol}</td>
                                                  <td>{h.instrumentName}</td>
                                                  <td className="fi-admin-dir-nested-num">{fmtWeight(h.weightPercent)}</td>
                                                  <td className="fi-admin-dir-nested-num">{fmtCompact(h.marketValue)}</td>
                                                </tr>
                                              ))}
                                            </tbody>
                                          </table>
                                        )}
                                      </div>
                                    ))
                                  )}
                                </div>
                              )}
                            </td>
                          </tr>
                        )}
                      </Fragment>
                    )
                  })
                )}
              </tbody>
            </table>
          </div>

          <nav className="fi-admin-dir-pagination" aria-label={t('totalUsersPage.directory.paginationAria')}>
            <span className="fi-admin-dir-page-meta">
              {state.data.totalElements === 0
                ? t('totalUsersPage.directory.emptyMeta')
                : t('totalUsersPage.directory.pageOf', {
                    page: state.data.page + 1,
                    totalPages: state.data.totalPages,
                    totalElements: state.data.totalElements,
                  })}
            </span>
            <div className="fi-admin-dir-page-actions">
              <button
                type="button"
                className="fi-admin-dir-btn"
                disabled={page <= 0 || state.data.totalPages === 0}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
              >
                {t('latencyPage.prev')}
              </button>
              <button
                type="button"
                className="fi-admin-dir-btn"
                disabled={state.data.totalPages <= 1 || page >= state.data.totalPages - 1}
                onClick={() => setPage((p) => p + 1)}
              >
                {t('latencyPage.next')}
              </button>
            </div>
          </nav>
        </>
      )}
    </section>
  )
}
