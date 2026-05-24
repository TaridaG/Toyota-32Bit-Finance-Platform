import { useCallback, useEffect, useState, type MouseEvent } from 'react'
import { useTranslation } from 'react-i18next'
import {
  deleteNotification,
  fetchNotificationsPage,
  markAllNotificationsRead,
  markNotificationRead,
  notifyNotificationsChanged,
  type PortalNotification,
} from '../../features/notifications/api/notificationApi'
import {
  formatNotificationDetail,
  formatNotificationType,
} from '../../features/notifications/lib/notificationUi'
import { useDocumentTitle } from '../../shared/hooks/useDocumentTitle'

const PAGE_SIZE = 15

export function NotificationsPage() {
  const { t } = useTranslation('notificationsPage')
  const { t: tAlarms } = useTranslation('markets')
  useDocumentTitle(t('titleDoc'))

  const [page, setPage] = useState(0)
  const [items, setItems] = useState<PortalNotification[]>([])
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [unreadCount, setUnreadCount] = useState(0)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [expandedId, setExpandedId] = useState<number | null>(null)
  const [pendingDeleteIds, setPendingDeleteIds] = useState<number[]>([])
  const [markAllPending, setMarkAllPending] = useState(false)

  const loadPage = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const result = await fetchNotificationsPage(page, PAGE_SIZE)
      setItems(result.content)
      setTotalPages(result.totalPages)
      setTotalElements(result.totalElements)
      setUnreadCount(result.unreadCount)
    } catch {
      setError(t('loadError'))
      setItems([])
    } finally {
      setLoading(false)
    }
  }, [page, t])

  useEffect(() => {
    void loadPage()
  }, [loadPage])

  const handleOpen = async (item: PortalNotification) => {
    setExpandedId((prev) => (prev === item.id ? null : item.id))
    if (!item.read) {
      try {
        const updated = await markNotificationRead(item.id)
        if (updated) {
          setItems((prev) =>
            prev.map((n) => (n.id === item.id ? { ...n, read: true } : n)),
          )
          setUnreadCount((c) => Math.max(0, c - 1))
          notifyNotificationsChanged()
        }
      } catch {
        // keep UI usable even if mark-read fails
      }
    }
  }

  const handleDelete = async (id: number, event: MouseEvent) => {
    event.stopPropagation()
    if (pendingDeleteIds.includes(id)) {
      return
    }
    setPendingDeleteIds((prev) => [...prev, id])
    try {
      await deleteNotification(id)
      const wasUnread = items.find((n) => n.id === id)?.read === false
      setItems((prev) => prev.filter((n) => n.id !== id))
      setTotalElements((c) => Math.max(0, c - 1))
      if (wasUnread) {
        setUnreadCount((c) => Math.max(0, c - 1))
      }
      if (expandedId === id) {
        setExpandedId(null)
      }
      notifyNotificationsChanged()
    } catch {
      setError(t('deleteError'))
    } finally {
      setPendingDeleteIds((prev) => prev.filter((x) => x !== id))
    }
  }

  const handleMarkAllRead = async () => {
    if (markAllPending || unreadCount === 0) {
      return
    }
    setMarkAllPending(true)
    try {
      await markAllNotificationsRead()
      setItems((prev) => prev.map((n) => ({ ...n, read: true })))
      setUnreadCount(0)
      notifyNotificationsChanged()
    } catch {
      setError(t('markAllError'))
    } finally {
      setMarkAllPending(false)
    }
  }

  return (
    <div className="portal-page portal-notifications-page">
      <header className="portal-notifications-page-header">
        <div>
          <h1 className="portal-page-title">{t('title')}</h1>
          <p className="portal-page-lead">{t('lead')}</p>
        </div>
        {unreadCount > 0 ? (
          <button
            type="button"
            className="portal-button portal-button-secondary"
            disabled={markAllPending}
            onClick={() => void handleMarkAllRead()}
          >
            {t('markAllRead', { count: unreadCount })}
          </button>
        ) : null}
      </header>

      {loading ? (
        <p className="portal-notifications-page-muted">{t('loading')}</p>
      ) : error ? (
        <p className="portal-notifications-page-error">{error}</p>
      ) : items.length === 0 ? (
        <p className="portal-notifications-page-muted">{t('empty')}</p>
      ) : (
        <>
          <ul className="portal-notifications-inbox">
            {items.map((item) => {
              const expanded = expandedId === item.id
              return (
                <li
                  key={item.id}
                  className={`portal-notifications-inbox-item${item.read ? ' portal-notifications-inbox-item-read' : ''}`}
                >
                  <button
                    type="button"
                    className="portal-notifications-inbox-main"
                    onClick={() => void handleOpen(item)}
                  >
                    <div className="portal-notifications-inbox-left">
                      <span className="portal-notifications-inbox-symbol">
                        {item.instrumentSymbol}
                      </span>
                      <span className="portal-notifications-inbox-type">
                        {formatNotificationType(item.type, t)}
                      </span>
                    </div>
                    <div className="portal-notifications-inbox-right">
                      <time dateTime={item.triggeredAt}>
                        {new Date(item.triggeredAt).toLocaleString()}
                      </time>
                    </div>
                  </button>
                  {expanded ? (
                    <div className="portal-notifications-inbox-detail">
                      {formatNotificationDetail(item, t, tAlarms)}
                    </div>
                  ) : null}
                  <button
                    type="button"
                    className="portal-notifications-inbox-delete"
                    aria-label={t('deleteAria')}
                    disabled={pendingDeleteIds.includes(item.id)}
                    onClick={(e) => void handleDelete(item.id, e)}
                  >
                    {t('delete')}
                  </button>
                </li>
              )
            })}
          </ul>

          {totalPages > 1 ? (
            <div className="portal-notifications-pagination">
              <button
                type="button"
                className="portal-button portal-button-secondary"
                disabled={page <= 0}
                onClick={() => setPage((p) => Math.max(0, p - 1))}
              >
                {t('pagination.prev')}
              </button>
              <span className="portal-notifications-page-muted">
                {t('pagination.summary', {
                  page: page + 1,
                  totalPages: Math.max(totalPages, 1),
                  total: totalElements,
                })}
              </span>
              <button
                type="button"
                className="portal-button portal-button-secondary"
                disabled={page >= totalPages - 1}
                onClick={() => setPage((p) => p + 1)}
              >
                {t('pagination.next')}
              </button>
            </div>
          ) : null}
        </>
      )}
    </div>
  )
}
