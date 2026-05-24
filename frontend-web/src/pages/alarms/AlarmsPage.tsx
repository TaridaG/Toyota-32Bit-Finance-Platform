import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import {
  deactivateAlarm,
  fetchActiveAlarms,
  notifyAlarmsChanged,
  type AlarmItem,
} from '../../features/alarms/api/alarmApi'
import { sortAlarmsNewestFirst } from '../../features/alarms/lib/alarmListHelpers'
import { formatAlarmCondition, formatAlarmPricePair } from '../../features/alarms/lib/alarmUi'
import { fetchMarketPricesSummary } from '../../features/markets/api/marketService'
import { useDocumentTitle } from '../../shared/hooks/useDocumentTitle'

const PAGE_SIZE = 15

export function AlarmsPage() {
  const { t } = useTranslation('alarmsPage')
  const { t: tMarkets } = useTranslation('markets')
  useDocumentTitle(t('titleDoc'))

  const [page, setPage] = useState(0)
  const [allAlarms, setAllAlarms] = useState<AlarmItem[]>([])
  const [pricesBySymbol, setPricesBySymbol] = useState<Record<string, number>>({})
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [expandedId, setExpandedId] = useState<number | null>(null)
  const [pendingRemoveIds, setPendingRemoveIds] = useState<number[]>([])

  const loadAlarms = useCallback(async () => {
    setLoading(true)
    setError(null)
    try {
      const items = sortAlarmsNewestFirst(await fetchActiveAlarms())
      setAllAlarms(items)
      const symbols = [...new Set(items.map((a) => a.instrumentSymbol).filter(Boolean))]
      if (symbols.length === 0) {
        setPricesBySymbol({})
        return
      }
      const summary = await fetchMarketPricesSummary(symbols)
      const prices: Record<string, number> = {}
      for (const symbol of symbols) {
        const p = summary[symbol]?.price
        if (p != null && Number.isFinite(p)) {
          prices[symbol] = p
        }
      }
      setPricesBySymbol(prices)
    } catch {
      setError(t('loadError'))
      setAllAlarms([])
      setPricesBySymbol({})
    } finally {
      setLoading(false)
    }
  }, [t])

  useEffect(() => {
    void loadAlarms()
  }, [loadAlarms])

  const totalElements = allAlarms.length
  const totalPages = Math.max(1, Math.ceil(totalElements / PAGE_SIZE))

  const pageItems = useMemo(() => {
    const start = page * PAGE_SIZE
    return allAlarms.slice(start, start + PAGE_SIZE)
  }, [allAlarms, page])

  useEffect(() => {
    if (page > 0 && page >= totalPages) {
      setPage(Math.max(0, totalPages - 1))
    }
  }, [page, totalPages])

  const handleToggle = (id: number) => {
    setExpandedId((prev) => (prev === id ? null : id))
  }

  const handleRemove = async (alarmId: number) => {
    if (pendingRemoveIds.includes(alarmId)) {
      return
    }
    setPendingRemoveIds((prev) => [...prev, alarmId])
    try {
      await deactivateAlarm(alarmId)
      setAllAlarms((prev) => prev.filter((a) => a.id !== alarmId))
      if (expandedId === alarmId) {
        setExpandedId(null)
      }
      notifyAlarmsChanged()
    } catch {
      setError(t('removeError'))
    } finally {
      setPendingRemoveIds((prev) => prev.filter((id) => id !== alarmId))
    }
  }

  const tCommon = (key: string, opts?: Record<string, unknown>) =>
    t(key, { ns: 'common', ...opts })

  return (
    <div className="portal-page portal-notifications-page">
      <header className="portal-notifications-page-header">
        <div>
          <h1 className="portal-page-title">{t('title')}</h1>
          <p className="portal-page-lead">{t('lead')}</p>
        </div>
      </header>

      {loading ? (
        <p className="portal-notifications-page-muted">{t('loading')}</p>
      ) : error ? (
        <p className="portal-notifications-page-error">{error}</p>
      ) : allAlarms.length === 0 ? (
        <div className="portal-notifications-page-muted">
          <p>{t('empty')}</p>
          <p className="portal-alarms-page-hint">
            {t('createOnMarkets')}{' '}
            <Link to="/app/markets" className="portal-notifications-footer-link">
              {t('marketsLink')}
            </Link>
          </p>
        </div>
      ) : (
        <>
          <ul className="portal-notifications-inbox">
            {pageItems.map((alarm) => {
              const expanded = expandedId === alarm.id
              const live = pricesBySymbol[alarm.instrumentSymbol]
              const prices = formatAlarmPricePair(
                alarm.condition,
                alarm.threshold,
                live,
                (key, opts) => tMarkets(key, opts),
              )
              return (
                <li key={alarm.id} className="portal-notifications-inbox-item portal-alarms-inbox-item">
                  <button
                    type="button"
                    className="portal-notifications-inbox-main"
                    onClick={() => handleToggle(alarm.id)}
                  >
                    <div className="portal-notifications-inbox-left">
                      <span className="portal-notifications-inbox-symbol">{alarm.instrumentSymbol}</span>
                      <span className="portal-notifications-inbox-type">
                        {formatAlarmCondition(alarm.condition, alarm.threshold, (key, opts) =>
                          tMarkets(key, opts),
                        )}
                      </span>
                    </div>
                    <div className="portal-notifications-inbox-right">
                      <time dateTime={alarm.createdAt}>
                        {new Date(alarm.createdAt).toLocaleString()}
                      </time>
                    </div>
                  </button>
                  {expanded ? (
                    <div className="portal-notifications-inbox-detail portal-alarms-inbox-detail">
                      <p>
                        {tCommon('header.alarms.now')}: <strong>{prices.current}</strong>
                      </p>
                      <p>
                        {tCommon('header.alarms.target')}: <strong>{prices.target}</strong>
                      </p>
                    </div>
                  ) : null}
                  <button
                    type="button"
                    className="portal-notifications-inbox-delete"
                    aria-label={t('removeAria')}
                    disabled={pendingRemoveIds.includes(alarm.id)}
                    onClick={() => void handleRemove(alarm.id)}
                  >
                    {t('remove')}
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
                  totalPages,
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
