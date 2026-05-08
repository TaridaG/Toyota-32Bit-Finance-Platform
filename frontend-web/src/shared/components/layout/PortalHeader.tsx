import { useEffect, useMemo, useRef, useState } from 'react'
import type { MouseEvent as ReactMouseEvent } from 'react'
import { Link, NavLink } from 'react-router-dom'
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
} from '../../auth/session'
import { fetchPortalProfile } from '../../../features/profile/api/portalProfileApi'
import { usePortalAvatarObjectUrl } from '../../../features/profile/hooks/usePortalAvatarObjectUrl'
import { updatePortalPreferences } from '../../../features/profile/api/portalProfileApi'
import { fetchMyNotifications, type NotificationItem } from '../../../features/notifications/api/notificationApi'

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

const appNavItems: AppNavItem[] = [
  { to: '/app/markets', labelKey: 'header.navApp.markets' },
  { to: '/app/my-portfolio', labelKey: 'header.navApp.myPortfolio' },
  { to: '/app/analysis', labelKey: 'header.navApp.analysis' },
  { to: '/app/news', labelKey: 'header.navPublic.news' },
]

const publicNavItems: PublicNavItem[] = [
  { labelKey: 'header.navPublic.markets', to: '/markets' },
  { labelKey: 'header.navPublic.myPortfolio', to: '/my-portfolio' },
  { labelKey: 'header.navPublic.analysis', to: '/analysis' },
  { labelKey: 'header.navPublic.news', to: '/news' },
]

const mobileNavItems: PublicNavItem[] = [
  { labelKey: 'header.navPublic.markets', to: '/markets' },
  { labelKey: 'header.navPublic.myPortfolio', to: '/my-portfolio' },
  { labelKey: 'header.navPublic.analysis', to: '/analysis' },
  { labelKey: 'header.navPublic.news', to: '/news' },
]

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

export function PortalHeader({ isAuthenticated, onLogout }: PortalHeaderProps) {
  const [menuOpen, setMenuOpen] = useState(false)
  const [downloadOpen, setDownloadOpen] = useState(false)
  const [localeOpen, setLocaleOpen] = useState(false)
  const [profileOpen, setProfileOpen] = useState(false)
  const [notificationsOpen, setNotificationsOpen] = useState(false)
  const [notificationsLoading, setNotificationsLoading] = useState(false)
  const [notifications, setNotifications] = useState<NotificationItem[]>([])
  const [notificationsError, setNotificationsError] = useState<string | null>(null)
  const [serverAvatarUpdatedAt, setServerAvatarUpdatedAt] = useState<string | null>(null)
  const [profileImgBroken, setProfileImgBroken] = useState(false)
  const profileAnchorRef = useRef<HTMLDivElement>(null)
  const { theme, setTheme, toggleTheme } = useTheme()
  const { t, i18n } = useTranslation()
  const { currency, setLanguage, setCurrency } = useAppPreferences()
  const currentLocale = normalizeLocale(i18n.resolvedLanguage ?? i18n.language) ?? 'en'

  const claims = useMemo(() => (isAuthenticated ? getAuthClaims() : null), [isAuthenticated])
  const displayLabel = useMemo(() => {
    const label = getProfileDisplayLabel(claims)
    return label.length > 0 ? label : '…'
  }, [claims])
  const initials = useMemo(() => getProfileInitials(displayLabel), [displayLabel])
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
      return
    }
    void fetchPortalProfile()
      .then((p) => setServerAvatarUpdatedAt(p.avatarUpdatedAt ?? null))
      .catch(() => setServerAvatarUpdatedAt(null))
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

  useEffect(() => {
    if (!notificationsOpen || !isAuthenticated) {
      return
    }
    setNotificationsLoading(true)
    setNotificationsError(null)
    void fetchMyNotifications()
      .then((items) => {
        setNotifications(
          [...items].sort(
            (a, b) => Date.parse(b.triggeredAt ?? '') - Date.parse(a.triggeredAt ?? ''),
          ),
        )
      })
      .catch(() => {
        setNotificationsError(t('header.notifications.loadError'))
      })
      .finally(() => {
        setNotificationsLoading(false)
      })
  }, [isAuthenticated, notificationsOpen, t])

  const closeMenu = () => setMenuOpen(false)
  const closeDesktopPanels = () => {
    setDownloadOpen(false)
    setLocaleOpen(false)
    setProfileOpen(false)
    setNotificationsOpen(false)
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

  return (
    <header className="portal-header">
      <div className="portal-header-inner">
        <Link to={isAuthenticated ? '/app' : '/'} className="portal-logo">
          <img src={siteLogo} className="portal-logo-mark" alt="32Bit logo" />
          <span>{t('appName')}</span>
        </Link>

        <nav className="portal-nav portal-nav-desktop" aria-label="Ana navigasyon">
          {isAuthenticated
            ? appNavItems.map((item) => (
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
                  <Link
                    key={item.labelKey}
                    to={item.to}
                    className="portal-nav-link"
                    onClick={() => {
                      closeMenu()
                      closeDesktopPanels()
                    }}
                  >
                    {t(item.labelKey)}
                  </Link>
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

          <div className="portal-popover-anchor">
            <button
              type="button"
              className={`portal-icon-button${notificationsOpen ? ' portal-icon-button-active' : ''}`}
              aria-label={t('header.notifications.aria')}
              aria-expanded={notificationsOpen}
              onClick={() => {
                setNotificationsOpen((prev) => !prev)
                setDownloadOpen(false)
                setLocaleOpen(false)
                setProfileOpen(false)
              }}
            >
              <IconBell />
            </button>
            {notificationsOpen ? (
              <div className="portal-popover portal-notifications-popover">
                <div className="portal-notifications-header">{t('header.notifications.title')}</div>
                {!isAuthenticated ? (
                  <div className="portal-notifications-empty">
                    {t('header.notifications.loginRequired')}
                  </div>
                ) : notificationsLoading ? (
                  <div className="portal-notifications-empty">{t('common.loading')}</div>
                ) : notificationsError ? (
                  <div className="portal-notifications-empty">{notificationsError}</div>
                ) : notifications.length === 0 ? (
                  <div className="portal-notifications-empty">{t('header.notifications.empty')}</div>
                ) : (
                  <ul className="portal-notifications-list">
                    {notifications.map((item, idx) => (
                      <li
                        key={`${item.instrumentSymbol}-${item.triggeredAt}-${idx}`}
                        className="portal-notifications-item"
                      >
                        <div className="portal-notifications-symbol">{item.instrumentSymbol}</div>
                        <div className="portal-notifications-meta">
                          <span>{item.condition}</span>
                          <span>{new Date(item.triggeredAt).toLocaleString()}</span>
                        </div>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            ) : null}
          </div>

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
                  <h4>{t('common.language')}</h4>
                  <input type="text" placeholder={t('common.search')} />
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
                  <h4>{t('common.currency')}</h4>
                  <input type="text" placeholder={t('common.search')} />
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
          <aside className="portal-mobile-drawer" aria-label="Mobil menü">
            <button className="portal-mobile-close" aria-label={t('header.mobile.close')} onClick={closeMenu}>
              x
            </button>

            {isAuthenticated ? (
              <div className="portal-mobile-user">
                <div className="portal-mobile-user-avatar" aria-hidden>
                  {avatarUrl && !profileImgBroken ? (
                    <img
                      src={avatarUrl}
                      alt=""
                      className="portal-mobile-user-avatar-img"
                      onError={() => setProfileImgBroken(true)}
                    />
                  ) : (
                    <span className="portal-mobile-user-initials">{initials}</span>
                  )}
                </div>
                <div className="portal-mobile-user-text">
                  <span className="portal-mobile-user-name">{displayLabel}</span>
                  <Link to="/app/profile" className="portal-mobile-user-settings" onClick={closeMenu}>
                    {t('header.profileMenu.settings')}
                  </Link>
                </div>
              </div>
            ) : null}

            {!isAuthenticated ? (
              <div className="portal-mobile-auth-row">
                <Link to="/login" className="portal-mobile-auth-secondary" onClick={closeMenu}>
                  {t('header.actions.login')}
                </Link>
                <Link to="/register" className="portal-mobile-auth-primary" onClick={closeMenu}>
                  {t('header.actions.register')}
                </Link>
              </div>
            ) : null}

            <div className="portal-mobile-search">
              <span>{t('common.search')}</span>
              <input type="text" placeholder={t('common.search')} />
            </div>

            <nav className="portal-mobile-list" aria-label="Mobil navigasyon">
              {isAuthenticated
                ? appNavItems.map((item) => (
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
                      <span>{t(item.labelKey)}</span>
                      <span className="portal-mobile-item-arrow">›</span>
                    </NavLink>
                  ))
                : mobileNavItems.map((item) => (
                    item.to ? (
                      <Link key={item.labelKey} to={item.to} onClick={closeMenu} className="portal-mobile-item">
                        <span className="portal-mobile-item-icon" />
                        <span>{t(item.labelKey)}</span>
                        <span className="portal-mobile-item-arrow">›</span>
                      </Link>
                    ) : (
                      <a key={item.labelKey} href={item.href ?? '#'} onClick={closeMenu} className="portal-mobile-item">
                        <span className="portal-mobile-item-icon" />
                        <span>{t(item.labelKey)}</span>
                        <span className="portal-mobile-item-arrow">›</span>
                      </a>
                    )
                  ))}
            </nav>

            <div className="portal-mobile-footer">
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
              <p>{t('common.support247')}</p>
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

