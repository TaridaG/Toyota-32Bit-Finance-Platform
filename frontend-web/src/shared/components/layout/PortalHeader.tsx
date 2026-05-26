import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import type { MouseEvent as ReactMouseEvent } from 'react'
import { Link, NavLink, useLocation } from 'react-router-dom'
import siteLogo from '../../../assets/site-logo.png'
import { useTheme } from '../../theme/ThemeProvider'
import { useTranslation } from 'react-i18next'
import { LANGUAGE_LABELS, SUPPORTED_LOCALES, normalizeLocale } from '../../i18n'
import { SUPPORTED_CURRENCIES } from '../../preferences/preferences'
import { useAppPreferences } from '../../preferences/useAppPreferences'
import {
  getAuthClaims,
  getProfileDisplayLabel,
  getProfileInitials,
  isAdminUser,
} from '../../auth/session'
import { fetchPortalProfile, fetchPortalProfileBootstrap } from '../../../features/profile/api/portalProfileApi'
import { usePortalAvatarObjectUrl } from '../../../features/profile/hooks/usePortalAvatarObjectUrl'
import { updatePortalPreferences } from '../../../features/profile/api/portalProfileApi'
import {
  fetchNotificationPreview,
  type PortalNotification,
} from '../../../features/notifications/api/notificationApi'
import { formatNotificationType } from '../../../features/notifications/lib/notificationUi'
import {
  deactivateAlarm,
  fetchActiveAlarms,
  type AlarmItem,
} from '../../../features/alarms/api/alarmApi'
import { fetchMarketPricesSummary } from '../../../features/markets/api/marketService'
import { formatAlarmCondition, formatAlarmPricePair } from '../../../features/alarms/lib/alarmUi'
import { notifyAlarmsChanged } from '../../../features/alarms/api/alarmApi'
import {
  ALARM_HEADER_PREVIEW,
  formatExtraAlarmCount,
  sortAlarmsNewestFirst,
} from '../../../features/alarms/lib/alarmListHelpers'
import { useAlarmUi } from '../../../features/alarms/AlarmUiContext'
import { useLiteracyHelpMode } from '../../../features/literacy-help/LiteracyHelpModeContext'
import { IconHelp } from '../../../features/literacy-help/IconHelp'
import { useAdminInfoCardPick } from '../../../features/admin-info-card-pick/AdminInfoCardPickContext'
import { IconHelpAdd } from '../../../features/admin-info-card-pick/IconHelpAdd'
import { isAdminPickRouteAllowed } from '../../../features/admin-info-card-pick/pickTargetUtils'
import {
  PUBLIC_BANK_RATES_ROUTE,
  PUBLIC_FINANCIAL_LITERACY_ROUTE,
  PUBLIC_MARKETS_ROUTE,
} from '../../../app/routes/publicCatalogRoutes'
import { scheduleIdleWork } from '../../browser/scheduleIdleWork'

type PortalHeaderProps = {
  isAuthenticated: boolean
  onLogout?: () => void
}

type AppNavItem = {
  to: string
  labelKey: string
  end?: boolean
}

type PublicNavItem = {
  labelKey: string
  to?: string
  href?: string
}

const appNavPortfolioItem: AppNavItem = {
  to: '/app/my-portfolio',
  labelKey: 'header.navApp.myPortfolio',
}

const appNavItemsWithoutPortfolio: AppNavItem[] = [
  { to: PUBLIC_MARKETS_ROUTE, labelKey: 'header.navPublic.markets' },
  { to: '/app/faiz-vadeli', labelKey: 'header.navApp.faizVadeli' },
  { to: '/app/analysis', labelKey: 'header.navApp.analysis' },
  { to: '/app/news', labelKey: 'header.navPublic.news' },
  { to: PUBLIC_BANK_RATES_ROUTE, labelKey: 'header.navPublic.bankRates' },
  { to: PUBLIC_FINANCIAL_LITERACY_ROUTE, labelKey: 'header.navPublic.finansalOkuryazarlik' },
]

const appNavBilgiKartlariItem: AppNavItem = {
  to: '/app/bilgi-kartlari',
  labelKey: 'header.navApp.bilgiKartlari',
}
const appNavAdminItem: AppNavItem = { to: '/admin', labelKey: 'header.navApp.admin' }

const publicNavItems: PublicNavItem[] = [
  { labelKey: 'header.navPublic.markets', to: PUBLIC_MARKETS_ROUTE },
  { labelKey: 'header.navPublic.analysis', to: '/analysis' },
  { labelKey: 'header.navPublic.news', to: '/news' },
  { labelKey: 'header.navPublic.bankRates', to: PUBLIC_BANK_RATES_ROUTE },
  { labelKey: 'header.navPublic.finansalOkuryazarlik', to: PUBLIC_FINANCIAL_LITERACY_ROUTE },
]

const mobileNavItems: PublicNavItem[] = publicNavItems

const CURRENCY_LABELS: Record<(typeof SUPPORTED_CURRENCIES)[number], string> = {
  USD: 'USD - $',
  EUR: 'EUR - €',
  TRY: 'TRY - ₺',
  GBP: 'GBP - £',
  JPY: 'JPY - ¥',
  AED: 'AED - د.إ',
}

function IconSearch() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <circle cx="11" cy="11" r="7" />
      <path d="m20 20-4.2-4.2" />
    </svg>
  )
}

function IconDownload() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M12 3v10" />
      <path d="m8 10 4 4 4-4" />
      <path d="M4 18v3h16v-3" />
    </svg>
  )
}

function IconLanguage() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <circle cx="12" cy="12" r="9" />
      <path d="M3 12h18" />
      <path d="M12 3c3 3 3 15 0 18" />
      <path d="M12 3c-3 3-3 15 0 18" />
    </svg>
  )
}

function IconBell() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M15 18H9" />
      <path d="M6 18h12" />
      <path d="M7.5 18V11a4.5 4.5 0 1 1 9 0v7" />
      <path d="M12 3.5v1" />
    </svg>
  )
}

function IconAlarmClock() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M5.5 4.5 3 2" />
      <path d="M18.5 4.5 21 2" />
      <path d="M9 2.5h6" />
      <circle cx="12" cy="13" r="7" />
      <path d="M12 10v3.5l2.5 1.5" />
    </svg>
  )
}

function IconSun() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <circle cx="12" cy="12" r="4" />
      <path d="M12 2v3" />
      <path d="M12 19v3" />
      <path d="M2 12h3" />
      <path d="M19 12h3" />
      <path d="m5 5 2.2 2.2" />
      <path d="m16.8 16.8 2.2 2.2" />
      <path d="m19 5-2.2 2.2" />
      <path d="m7.2 16.8-2.2 2.2" />
    </svg>
  )
}

function IconMoon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path d="M21 12.8A9 9 0 1 1 11.2 3a7.2 7.2 0 1 0 9.8 9.8Z" />
    </svg>
  )
}

const NOTIFICATION_HEADER_PREVIEW = 3

function formatExtraNotificationCount(total: number): string | null {
  const extra = total - NOTIFICATION_HEADER_PREVIEW
  if (extra <= 0) {
    return null
  }
  return extra > 99 ? '99+' : String(extra)
}

export function PortalHeader({ isAuthenticated, onLogout }: PortalHeaderProps) {
  const [menuOpen, setMenuOpen] = useState(false)
  const [downloadOpen, setDownloadOpen] = useState(false)
  const [localeOpen, setLocaleOpen] = useState(false)
  const [profileOpen, setProfileOpen] = useState(false)
  const [notificationsOpen, setNotificationsOpen] = useState(false)
  const [notificationsLoading, setNotificationsLoading] = useState(false)
  const [notifications, setNotifications] = useState<PortalNotification[]>([])
  const [notificationUnreadCount, setNotificationUnreadCount] = useState(0)
  const [notificationTotalCount, setNotificationTotalCount] = useState(0)
  const [notificationsError, setNotificationsError] = useState<string | null>(null)
  const [alarmsOpen, setAlarmsOpen] = useState(false)
  const [alarmsLoading, setAlarmsLoading] = useState(false)
  const [activeAlarms, setActiveAlarms] = useState<AlarmItem[]>([])
  const [alarmTotalCount, setAlarmTotalCount] = useState(0)
  const [alarmsError, setAlarmsError] = useState<string | null>(null)
  const [alarmPendingIds, setAlarmPendingIds] = useState<number[]>([])
  const [alarmPricesBySymbol, setAlarmPricesBySymbol] = useState<Record<string, number>>({})
  const { refreshKey: alarmsRefreshKey, bumpAlarmsRefresh } = useAlarmUi()
  const [serverAvatarUpdatedAt, setServerAvatarUpdatedAt] = useState<string | null>(null)
  const [portalUsername, setPortalUsername] = useState<string | null>(null)
  const [profileImgBroken, setProfileImgBroken] = useState(false)
  const profileAnchorRef = useRef<HTMLDivElement>(null)
  const { theme, setTheme, toggleTheme } = useTheme()
  const { t, i18n } = useTranslation()
  const { t: tNotifications } = useTranslation('notificationsPage')
  const { currency, setLanguage, setCurrency } = useAppPreferences()
  const { pathname } = useLocation()
  const { active: literacyHelpActive, toggle: toggleLiteracyHelp, deactivate: deactivateLiteracyHelp } =
    useLiteracyHelpMode()
  const {
    pickModeActive,
    canPickOnRoute,
    togglePickMode,
    deactivatePickMode,
  } = useAdminInfoCardPick()
  const showAdminPick =
    isAuthenticated && isAdminUser() && canPickOnRoute && isAdminPickRouteAllowed(pathname)
  const currentLocale = normalizeLocale(i18n.resolvedLanguage ?? i18n.language) ?? 'en'

  const claims = useMemo(() => (isAuthenticated ? getAuthClaims() : null), [isAuthenticated])
  const displayLabel = useMemo(() => {
    const username = portalUsername?.trim()
    if (username) {
      return `@${username}`
    }
    const label = getProfileDisplayLabel(claims)
    return label.length > 0 ? label : '…'
  }, [portalUsername, claims])
  const initials = useMemo(() => {
    const username = portalUsername?.trim()
    if (username) {
      return username.slice(0, 1).toUpperCase()
    }
    return getProfileInitials(displayLabel.replace(/^@/, ''))
  }, [portalUsername, displayLabel])
  const serverAvatarBlobUrl = usePortalAvatarObjectUrl(serverAvatarUpdatedAt ?? undefined)
  const avatarUrl = useMemo(() => {
    if (!isAuthenticated) {
      return null
    }
    return serverAvatarBlobUrl ?? claims?.picture ?? null
  }, [isAuthenticated, serverAvatarBlobUrl, claims?.picture])

  useEffect(() => {
    setProfileImgBroken(false)
  }, [avatarUrl])

  useEffect(() => {
    if (!isAuthenticated) {
      setServerAvatarUpdatedAt(null)
      setPortalUsername(null)
      return
    }
    void fetchPortalProfileBootstrap()
      .then((p) => {
        setServerAvatarUpdatedAt(p.avatarUpdatedAt ?? null)
        setPortalUsername(p.username ?? null)
      })
      .catch(() => {
        setServerAvatarUpdatedAt(null)
        setPortalUsername(null)
      })
  }, [isAuthenticated])

  useEffect(() => {
    const onUsername = (e: Event) => {
      const detail = (e as CustomEvent<{ username?: string }>).detail
      if (typeof detail?.username === 'string' && detail.username.trim()) {
        setPortalUsername(detail.username.trim())
        return
      }
      if (!isAuthenticated) {
        return
      }
      void fetchPortalProfile()
        .then((p) => setPortalUsername(p.username ?? null))
        .catch(() => setPortalUsername(null))
    }
    window.addEventListener('finance-profile-username', onUsername)
    return () => window.removeEventListener('finance-profile-username', onUsername)
  }, [isAuthenticated])

  useEffect(() => {
    const onAvatar = (e: Event) => {
      const detail = (e as CustomEvent<{ avatarUpdatedAt?: string | null }>).detail
      if (detail && 'avatarUpdatedAt' in detail) {
        setServerAvatarUpdatedAt(detail.avatarUpdatedAt ?? null)
      } else if (isAuthenticated) {
        void fetchPortalProfile()
          .then((p) => setServerAvatarUpdatedAt(p.avatarUpdatedAt ?? null))
          .catch(() => setServerAvatarUpdatedAt(null))
      }
    }
    window.addEventListener('finance-profile-avatar', onAvatar)
    return () => window.removeEventListener('finance-profile-avatar', onAvatar)
  }, [isAuthenticated])

  useEffect(() => {
    if (!profileOpen) {
      return
    }
    const onDoc = (event: globalThis.MouseEvent) => {
      const el = profileAnchorRef.current
      if (el && !el.contains(event.target as Node)) {
        setProfileOpen(false)
      }
    }
    document.addEventListener('mousedown', onDoc)
    return () => document.removeEventListener('mousedown', onDoc)
  }, [profileOpen])

  const loadNotificationsPreview = useCallback(
    async (opts?: { showLoading?: boolean }) => {
      if (!isAuthenticated) {
        setNotifications([])
        setNotificationUnreadCount(0)
        setNotificationTotalCount(0)
        return
      }
      if (opts?.showLoading) {
        setNotificationsLoading(true)
      }
      setNotificationsError(null)
      try {
        const page = await fetchNotificationPreview(NOTIFICATION_HEADER_PREVIEW)
        setNotifications(page.content)
        setNotificationUnreadCount(page.unreadCount)
        setNotificationTotalCount(page.totalElements)
      } catch {
        setNotificationsError(t('header.notifications.loadError'))
      } finally {
        if (opts?.showLoading) {
          setNotificationsLoading(false)
        }
      }
    },
    [isAuthenticated, t],
  )

  useEffect(() => {
    if (!isAuthenticated) {
      setNotifications([])
      setNotificationUnreadCount(0)
      setNotificationTotalCount(0)
      return
    }
    return scheduleIdleWork(() => {
      void loadNotificationsPreview()
    }, 1_500)
  }, [isAuthenticated, loadNotificationsPreview])

  useEffect(() => {
    if (!notificationsOpen || !isAuthenticated) {
      return
    }
    void loadNotificationsPreview({ showLoading: true })
  }, [notificationsOpen, isAuthenticated, loadNotificationsPreview])

  useEffect(() => {
    const onChanged = () => {
      void loadNotificationsPreview()
    }
    window.addEventListener('finance-notifications-changed', onChanged)
    return () => window.removeEventListener('finance-notifications-changed', onChanged)
  }, [loadNotificationsPreview])

  const notificationMoreBadge = useMemo(
    () => formatExtraNotificationCount(notificationTotalCount),
    [notificationTotalCount],
  )

  const hasUnreadNotifications = notificationUnreadCount > 0

  const loadAlarmsPreview = useCallback(
    async (opts?: { showLoading?: boolean }) => {
      if (!isAuthenticated) {
        setActiveAlarms([])
        setAlarmTotalCount(0)
        setAlarmPricesBySymbol({})
        return
      }
      if (opts?.showLoading) {
        setAlarmsLoading(true)
      }
      setAlarmsError(null)
      try {
        const sorted = sortAlarmsNewestFirst(await fetchActiveAlarms())
        setAlarmTotalCount(sorted.length)
        const preview = sorted.slice(0, ALARM_HEADER_PREVIEW)
        setActiveAlarms(preview)
        const symbols = [...new Set(preview.map((a) => a.instrumentSymbol).filter(Boolean))]
        if (symbols.length === 0) {
          setAlarmPricesBySymbol({})
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
        setAlarmPricesBySymbol(prices)
      } catch {
        setAlarmsError(t('header.alarms.loadError'))
      } finally {
        if (opts?.showLoading) {
          setAlarmsLoading(false)
        }
      }
    },
    [isAuthenticated, t],
  )

  useEffect(() => {
    if (!isAuthenticated) {
      setActiveAlarms([])
      setAlarmTotalCount(0)
      setAlarmPricesBySymbol({})
      return
    }
    return scheduleIdleWork(() => {
      void loadAlarmsPreview()
    }, 2_000)
  }, [isAuthenticated, alarmsRefreshKey, loadAlarmsPreview])

  useEffect(() => {
    if (!alarmsOpen || !isAuthenticated) {
      return
    }
    void loadAlarmsPreview({ showLoading: true })
  }, [alarmsOpen, isAuthenticated, loadAlarmsPreview])

  useEffect(() => {
    const onChanged = () => {
      void loadAlarmsPreview()
    }
    window.addEventListener('finance-alarms-changed', onChanged)
    return () => window.removeEventListener('finance-alarms-changed', onChanged)
  }, [loadAlarmsPreview])

  const alarmMoreBadge = useMemo(() => formatExtraAlarmCount(alarmTotalCount), [alarmTotalCount])

  const closeMenu = () => setMenuOpen(false)
  const closeDesktopPanels = () => {
    setDownloadOpen(false)
    setLocaleOpen(false)
    setProfileOpen(false)
    setNotificationsOpen(false)
    setAlarmsOpen(false)
  }

  const handleRemoveAlarm = async (alarmId: number) => {
    if (alarmPendingIds.includes(alarmId)) {
      return
    }
    setAlarmPendingIds((prev) => [...prev, alarmId])
    try {
      await deactivateAlarm(alarmId)
      setActiveAlarms((prev) => prev.filter((a) => a.id !== alarmId))
      bumpAlarmsRefresh()
      notifyAlarmsChanged()
    } catch {
      setAlarmsError(t('header.alarms.loadError'))
    } finally {
      setAlarmPendingIds((prev) => prev.filter((id) => id !== alarmId))
    }
  }

  const handleThemeToggle = (event: ReactMouseEvent<HTMLButtonElement>) => {
    const rect = event.currentTarget.getBoundingClientRect()
    toggleTheme({
      x: rect.left + rect.width / 2,
      y: rect.top + rect.height / 2,
    })
  }

  const handleSelectLocale = async (locale: (typeof SUPPORTED_LOCALES)[number]) => {
    await setLanguage(locale)
    if (isAuthenticated) {
      try {
        await updatePortalPreferences(locale, currency)
      } catch {
        // keep local preference even if backend sync fails temporarily
      }
    }
    setLocaleOpen(false)
  }

  const appNavForSession = useMemo(() => {
    const core = [appNavPortfolioItem, ...appNavItemsWithoutPortfolio]
    if (isAuthenticated && isAdminUser()) {
      return [...core, appNavBilgiKartlariItem, appNavAdminItem]
    }
    return core
  }, [isAuthenticated])

  return (
    <header className="portal-header">
      <div className="portal-header-inner">
        <Link to={isAuthenticated ? '/markets' : '/'} className="portal-logo">
          <img src={siteLogo} className="portal-logo-mark" alt="32Bit logo" />
          <span>{t('appName')}</span>
        </Link>

        <nav className="portal-nav portal-nav-desktop" aria-label="Ana navigasyon">
          {isAuthenticated
            ? appNavForSession.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  end={item.end}
                  onClick={() => {
                    closeMenu()
                    closeDesktopPanels()
                  }}
                  className={({ isActive }) =>
                    `portal-nav-link${isActive ? ' portal-nav-link-active' : ''}`
                  }
                >
                  {t(item.labelKey)}
                </NavLink>
              ))
            : publicNavItems.map((item) => (
                item.to ? (
                  <NavLink
                    key={item.labelKey}
                    to={item.to}
                    onClick={() => {
                      closeMenu()
                      closeDesktopPanels()
                    }}
                    className={({ isActive }) =>
                      `portal-nav-link${isActive ? ' portal-nav-link-active' : ''}`
                    }
                  >
                    {t(item.labelKey)}
                  </NavLink>
                ) : (
                  <a
                    key={item.labelKey}
                    href={item.href ?? '#'}
                    className="portal-nav-link"
                    onClick={() => {
                      closeMenu()
                      closeDesktopPanels()
                    }}
                  >
                    {t(item.labelKey)}
                  </a>
                )
              ))}
        </nav>

        <div className="portal-actions portal-actions-desktop">
          <button type="button" className="portal-icon-button" aria-label={t('header.searchAria')}>
            <IconSearch />
          </button>

          {isAuthenticated ? (
            <div className="portal-literacy-help-group">
              <button
                type="button"
                data-literacy-help-control
                className={`portal-icon-button portal-literacy-help-button${literacyHelpActive ? ' portal-icon-button-active' : ''}`}
                aria-label={t('header.literacyHelp.aria')}
                aria-pressed={literacyHelpActive}
                title={t('header.literacyHelp.aria')}
                onClick={() => {
                  closeDesktopPanels()
                  if (pickModeActive) {
                    deactivatePickMode()
                  }
                  toggleLiteracyHelp()
                }}
              >
                <IconHelp />
              </button>
              {showAdminPick ? (
                <button
                  type="button"
                  data-admin-pick-control
                  className={`portal-icon-button portal-admin-pick-button${pickModeActive ? ' portal-icon-button-active' : ''}`}
                  aria-label={t('header.adminPick.aria')}
                  aria-pressed={pickModeActive}
                  title={t('header.adminPick.aria')}
                  onClick={() => {
                    closeDesktopPanels()
                    if (literacyHelpActive) {
                      deactivateLiteracyHelp()
                    }
                    togglePickMode()
                  }}
                >
                  <IconHelpAdd />
                </button>
              ) : null}
            </div>
          ) : null}

          <div className="portal-popover-anchor">
            <button
              type="button"
              className={`portal-icon-button portal-icon-button--bell${notificationsOpen ? ' portal-icon-button-active' : ''}${hasUnreadNotifications ? ' portal-icon-button--has-unread' : ''}`}
              aria-label={
                hasUnreadNotifications
                  ? `${t('header.notifications.aria')} (${notificationUnreadCount})`
                  : t('header.notifications.aria')
              }
              aria-expanded={notificationsOpen}
              onClick={() => {
                setNotificationsOpen((prev) => !prev)
                setDownloadOpen(false)
                setLocaleOpen(false)
                setProfileOpen(false)
              }}
            >
              <IconBell />
              {hasUnreadNotifications ? (
                <span className="portal-notification-unread-dot" aria-hidden />
              ) : null}
            </button>
            {notificationsOpen ? (
              <div className="portal-popover portal-notifications-popover">
                <div className="portal-notifications-header">
                  <Link
                    to="/app/notifications"
                    className="portal-notifications-header-link"
                    onClick={() => setNotificationsOpen(false)}
                  >
                    {t('header.notifications.title')}
                  </Link>
                </div>
                {!isAuthenticated ? (
                  <div className="portal-notifications-empty">
                    {t('header.notifications.loginRequired')}
                  </div>
                ) : notificationsLoading ? (
                  <div className="portal-notifications-empty">{t('loading')}</div>
                ) : notificationsError ? (
                  <div className="portal-notifications-empty">{notificationsError}</div>
                ) : notifications.length === 0 ? (
                  <div className="portal-notifications-empty">{t('header.notifications.empty')}</div>
                ) : (
                  <ul className="portal-notifications-list">
                    {notifications.map((item) => (
                      <li
                        key={item.id}
                        className={`portal-notifications-item${item.read ? ' portal-notifications-item-read' : ' portal-notifications-item-unread'}`}
                      >
                        <Link
                          to="/app/notifications"
                          className="portal-notifications-item-link"
                          onClick={() => setNotificationsOpen(false)}
                        >
                          <div className="portal-notifications-symbol">{item.instrumentSymbol}</div>
                          <div className="portal-notifications-meta">
                            <span className="portal-notifications-type">
                              {formatNotificationType(item.type, tNotifications)}
                            </span>
                            <time dateTime={item.triggeredAt}>
                              {new Date(item.triggeredAt).toLocaleString()}
                            </time>
                          </div>
                        </Link>
                      </li>
                    ))}
                  </ul>
                )}
                {isAuthenticated && notificationMoreBadge ? (
                  <div className="portal-notifications-more">
                    <Link
                      to="/app/notifications"
                      className="portal-notifications-more-link"
                      onClick={() => setNotificationsOpen(false)}
                    >
                      {t('header.notifications.moreNew', { count: `+${notificationMoreBadge}` })}
                    </Link>
                  </div>
                ) : null}
              </div>
            ) : null}
          </div>

          {isAuthenticated ? (
            <div className="portal-popover-anchor">
              <button
                type="button"
                className={`portal-icon-button${alarmsOpen ? ' portal-icon-button-active' : ''}`}
                aria-label={t('header.alarms.aria')}
                title={t('header.alarms.aria')}
                aria-expanded={alarmsOpen}
                onClick={() => {
                  setAlarmsOpen((prev) => !prev)
                  setNotificationsOpen(false)
                  setDownloadOpen(false)
                  setLocaleOpen(false)
                  setProfileOpen(false)
                }}
              >
                <IconAlarmClock />
              </button>
              {alarmsOpen ? (
                <div className="portal-popover portal-notifications-popover portal-alarms-popover">
                  <div className="portal-notifications-header">
                    <Link
                      to="/app/alarms"
                      className="portal-notifications-header-link"
                      onClick={() => setAlarmsOpen(false)}
                    >
                      {t('header.alarms.title')}
                    </Link>
                  </div>
                  {alarmsLoading ? (
                    <div className="portal-notifications-empty">{t('loading')}</div>
                  ) : alarmsError ? (
                    <div className="portal-notifications-empty">{alarmsError}</div>
                  ) : activeAlarms.length === 0 ? (
                    <div className="portal-notifications-empty">
                      <p>{t('header.alarms.empty')}</p>
                      <p className="portal-alarms-hint">{t('header.alarms.createOnMarkets')}</p>
                    </div>
                  ) : (
                    <ul className="portal-notifications-list">
                      {activeAlarms.map((alarm) => {
                        const live = alarmPricesBySymbol[alarm.instrumentSymbol]
                        const prices = formatAlarmPricePair(
                          alarm.condition,
                          alarm.threshold,
                          live,
                          (key, opts) => t(key, { ns: 'markets', ...opts }),
                        )
                        return (
                        <li key={alarm.id} className="portal-notifications-item portal-alarms-item">
                          <div className="portal-alarms-item-main">
                            <div className="portal-notifications-symbol">{alarm.instrumentSymbol}</div>
                            <div className="portal-notifications-meta">
                              <span>
                                {formatAlarmCondition(alarm.condition, alarm.threshold, (key, opts) =>
                                  t(key, { ns: 'markets', ...opts }),
                                )}
                              </span>
                            </div>
                            <div className="portal-alarms-prices">
                              <span>
                                {t('header.alarms.now', { ns: 'common' })}: <strong>{prices.current}</strong>
                              </span>
                              <span>
                                {t('header.alarms.target', { ns: 'common' })}: <strong>{prices.target}</strong>
                              </span>
                            </div>
                          </div>
                          <button
                            type="button"
                            className="portal-alarms-remove"
                            aria-label={t('header.alarms.remove')}
                            disabled={alarmPendingIds.includes(alarm.id)}
                            onClick={() => void handleRemoveAlarm(alarm.id)}
                          >
                            ×
                          </button>
                        </li>
                        )
                      })}
                    </ul>
                  )}
                  {alarmMoreBadge ? (
                    <div className="portal-notifications-more">
                      <Link
                        to="/app/alarms"
                        className="portal-notifications-more-link"
                        onClick={() => setAlarmsOpen(false)}
                      >
                        {t('header.alarms.moreNew', { count: `+${alarmMoreBadge}` })}
                      </Link>
                    </div>
                  ) : null}
                </div>
              ) : null}
            </div>
          ) : null}

          {isAuthenticated ? (
            <div className="portal-popover-anchor" ref={profileAnchorRef}>
              <button
                type="button"
                className={`portal-profile-trigger${profileOpen ? ' portal-profile-trigger-active' : ''}`}
                aria-label={t('header.profileMenu.openAria')}
                aria-expanded={profileOpen}
                aria-haspopup="menu"
                onClick={() => {
                  setProfileOpen((prev) => !prev)
                  setDownloadOpen(false)
                  setLocaleOpen(false)
                }}
              >
                {avatarUrl && !profileImgBroken ? (
                  <img
                    src={avatarUrl}
                    alt=""
                    className="portal-profile-trigger-img"
                    onError={() => setProfileImgBroken(true)}
                  />
                ) : (
                  <span className="portal-profile-trigger-initials" aria-hidden>
                    {initials}
                  </span>
                )}
              </button>
              {profileOpen ? (
                <div className="portal-popover portal-profile-menu" role="menu">
                  <div className="portal-profile-menu-user">
                    <div className="portal-profile-menu-avatar" aria-hidden>
                      {avatarUrl && !profileImgBroken ? (
                        <img
                          src={avatarUrl}
                          alt=""
                          className="portal-profile-menu-avatar-img"
                          onError={() => setProfileImgBroken(true)}
                        />
                      ) : (
                        <span className="portal-profile-menu-initials">{initials}</span>
                      )}
                    </div>
                    <span className="portal-profile-menu-name">{displayLabel}</span>
                  </div>
                  <Link
                    to="/app/profile"
                    role="menuitem"
                    className="portal-profile-menu-item"
                    onClick={() => {
                      setProfileOpen(false)
                      closeDesktopPanels()
                    }}
                  >
                    {t('header.profileMenu.settings')}
                  </Link>
                  <button
                    type="button"
                    role="menuitem"
                    className="portal-profile-menu-item portal-profile-menu-item-danger"
                    onClick={() => {
                      setProfileOpen(false)
                      onLogout?.()
                    }}
                  >
                    {t('header.profileMenu.logout')}
                  </button>
                </div>
              ) : null}
            </div>
          ) : (
            <>
              <Link to="/login" className="portal-action-secondary">
                {t('header.actions.login')}
              </Link>
              <Link to="/register" className="portal-action-primary">
                {t('header.actions.register')}
              </Link>
            </>
          )}

          <div className="portal-popover-anchor">
            <button
              type="button"
              className={`portal-icon-button${downloadOpen ? ' portal-icon-button-active' : ''}`}
              aria-label={t('header.download.aria')}
              aria-expanded={downloadOpen}
              onClick={() => {
                setDownloadOpen((prev) => !prev)
                setLocaleOpen(false)
                setProfileOpen(false)
              }}
            >
              <IconDownload />
            </button>
            {downloadOpen ? (
              <div className="portal-popover portal-download-popover">
                <div className="portal-qr-placeholder">QR</div>
                <p>{t('header.download.title')}</p>
                <small>{t('header.download.subtitle')}</small>
                <button type="button" className="portal-download-cta">
                  {t('header.download.moreOptions')}
                </button>
              </div>
            ) : null}
          </div>

          <div className="portal-popover-anchor">
            <button
              type="button"
              className={`portal-icon-button${localeOpen ? ' portal-icon-button-active' : ''}`}
              aria-label={t('header.locale.aria')}
              aria-expanded={localeOpen}
              onClick={() => {
                setLocaleOpen((prev) => !prev)
                setDownloadOpen(false)
                setProfileOpen(false)
              }}
            >
              <IconLanguage />
            </button>
            {localeOpen ? (
              <div className="portal-popover portal-locale-popover">
                <div className="portal-locale-col">
                  <h4>{t('language')}</h4>
                  <input type="text" placeholder={t('search')} />
                  <ul>
                    {SUPPORTED_LOCALES.map((locale) => (
                      <li key={locale}>
                        <button
                          type="button"
                          className={currentLocale === locale ? 'portal-locale-item-active' : undefined}
                          onClick={() => void handleSelectLocale(locale)}
                        >
                          {LANGUAGE_LABELS[locale]}
                        </button>
                      </li>
                    ))}
                  </ul>
                </div>
                <div className="portal-locale-col">
                  <h4>{t('currency')}</h4>
                  <input type="text" placeholder={t('search')} />
                  <ul>
                    {SUPPORTED_CURRENCIES.map((item) => (
                      <li key={item}>
                        <button
                          type="button"
                          className={currency === item ? 'portal-locale-item-active' : undefined}
                          onClick={() => {
                            setCurrency(item)
                            if (!isAuthenticated) {
                              return
                            }
                            void updatePortalPreferences(currentLocale, item).catch(() => {
                              // keep local preference even if backend sync fails temporarily
                            })
                          }}
                        >
                          {CURRENCY_LABELS[item]}
                        </button>
                      </li>
                    ))}
                  </ul>
                </div>
              </div>
            ) : null}
          </div>

          <button
            type="button"
            className="portal-icon-button portal-theme-toggle"
            aria-label={theme === 'dark' ? t('header.theme.switchToLight') : t('header.theme.switchToDark')}
            onClick={handleThemeToggle}
          >
            <span className="portal-theme-icon-stack" aria-hidden="true">
              <span
                className={`portal-theme-icon portal-theme-icon-sun${theme === 'light' ? ' portal-theme-icon-active' : ''}`}
              >
                <IconSun />
              </span>
              <span
                className={`portal-theme-icon portal-theme-icon-moon${theme === 'dark' ? ' portal-theme-icon-active' : ''}`}
              >
                <IconMoon />
              </span>
            </span>
          </button>
        </div>

        <button
          type="button"
          className="portal-menu-button"
          aria-label={menuOpen ? t('header.mobile.closeMenu') : t('header.mobile.openMenu')}
          aria-expanded={menuOpen}
          onClick={() => setMenuOpen((prev) => !prev)}
        >
          <span />
          <span />
          <span />
        </button>
      </div>

      {menuOpen ? (
        <div className="portal-mobile-drawer-wrap">
          <button
            className="portal-mobile-backdrop"
            aria-label={t('header.mobile.closeMenu')}
            onClick={closeMenu}
          />
          <aside
            className="portal-mobile-drawer"
            role="dialog"
            aria-modal="true"
            aria-labelledby="portal-mobile-menu-title"
          >
            <div className="portal-mobile-drawer-head">
              {isAuthenticated ? (
                <div className="portal-mobile-drawer-brand portal-mobile-drawer-brand--user">
                  <img src={siteLogo} className="portal-mobile-drawer-brand-mark" alt="" aria-hidden="true" />
                  <div className="portal-mobile-drawer-brand-copy">
                    <span className="portal-mobile-drawer-kicker">{t('appName')}</span>
                    <span id="portal-mobile-menu-title" className="portal-mobile-drawer-title">
                      {displayLabel}
                    </span>
                  </div>
                </div>
              ) : (
                <div className="portal-mobile-drawer-brand">
                  <img src={siteLogo} className="portal-mobile-drawer-brand-mark" alt="" aria-hidden="true" />
                  <div className="portal-mobile-drawer-brand-copy">
                    <span className="portal-mobile-drawer-kicker">{t('appName')}</span>
                    <span id="portal-mobile-menu-title" className="portal-mobile-drawer-title">
                      {t('header.mobile.openMenu')}
                    </span>
                  </div>
                </div>
              )}
              <div className="portal-mobile-drawer-head-actions">
                {isAuthenticated ? (
                  <div className="portal-mobile-drawer-avatar" aria-hidden>
                    {avatarUrl && !profileImgBroken ? (
                      <img
                        src={avatarUrl}
                        alt=""
                        className="portal-mobile-drawer-avatar-img"
                        onError={() => setProfileImgBroken(true)}
                      />
                    ) : (
                      <span className="portal-mobile-drawer-avatar-initials">{initials}</span>
                    )}
                  </div>
                ) : null}
                <button className="portal-mobile-close" aria-label={t('header.mobile.close')} onClick={closeMenu}>
                  <span aria-hidden="true">×</span>
                </button>
              </div>
            </div>

            {!isAuthenticated ? (
              <div className="portal-mobile-auth-row portal-mobile-section">
                <Link to="/login" className="portal-mobile-auth-secondary" onClick={closeMenu}>
                  {t('header.actions.login')}
                </Link>
                <Link to="/register" className="portal-mobile-auth-primary" onClick={closeMenu}>
                  {t('header.actions.register')}
                </Link>
              </div>
            ) : null}

            <section className="portal-mobile-section portal-mobile-nav-shell">
              <nav className="portal-mobile-list" aria-label="Mobil navigasyon">
                {isAuthenticated
                  ? appNavForSession.map((item) => (
                      <NavLink
                        key={item.to}
                        to={item.to}
                        end={item.end}
                        onClick={closeMenu}
                        className={({ isActive }) =>
                          `portal-mobile-item${isActive ? ' portal-mobile-item-active' : ''}`
                        }
                      >
                        <span className="portal-mobile-item-icon" />
                        <span className="portal-mobile-item-label">{t(item.labelKey)}</span>
                        <span className="portal-mobile-item-arrow">›</span>
                      </NavLink>
                    ))
                  : mobileNavItems.map((item) => (
                      item.to ? (
                        <NavLink
                          key={item.labelKey}
                          to={item.to}
                          onClick={closeMenu}
                          className={({ isActive }) =>
                            `portal-mobile-item${isActive ? ' portal-mobile-item-active' : ''}`
                          }
                        >
                          <span className="portal-mobile-item-icon" />
                          <span className="portal-mobile-item-label">{t(item.labelKey)}</span>
                          <span className="portal-mobile-item-arrow">›</span>
                        </NavLink>
                      ) : (
                        <a key={item.labelKey} href={item.href ?? '#'} onClick={closeMenu} className="portal-mobile-item">
                          <span className="portal-mobile-item-icon" />
                          <span className="portal-mobile-item-label">{t(item.labelKey)}</span>
                          <span className="portal-mobile-item-arrow">›</span>
                        </a>
                      )
                    ))}
              </nav>
            </section>

            <div className="portal-mobile-footer portal-mobile-footer-card">
              <div className="portal-mobile-theme">
                <span>{t('header.theme.label')}</span>
                <div>
                  <button
                    type="button"
                    className={theme === 'light' ? 'portal-theme-button-active' : undefined}
                    onClick={() => setTheme('light')}
                  >
                    {t('header.theme.light')}
                  </button>
                  <button
                    type="button"
                    className={theme === 'dark' ? 'portal-theme-button-active' : undefined}
                    onClick={() => setTheme('dark')}
                  >
                    {t('header.theme.dark')}
                  </button>
                </div>
              </div>
              {isAuthenticated ? (
                <Link to="/app/profile" className="portal-mobile-drawer-profile-link" onClick={closeMenu}>
                  {t('header.profileMenu.settings')}
                </Link>
              ) : null}
              <p className="portal-mobile-support">{t('support247')}</p>
              {isAuthenticated ? (
                <button
                  className="portal-action-secondary"
                  onClick={() => {
                    closeMenu()
                    onLogout?.()
                  }}
                >
                  {t('header.profileMenu.logout')}
                </button>
              ) : null}
            </div>
          </aside>
        </div>
      ) : null}
    </header>
  )
}

