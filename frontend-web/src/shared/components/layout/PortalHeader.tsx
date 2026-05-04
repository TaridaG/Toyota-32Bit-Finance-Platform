import { useState } from 'react'
import type { MouseEvent } from 'react'
import { Link, NavLink } from 'react-router-dom'
import { useTheme } from '../../theme/ThemeProvider'
import { useTranslation } from 'react-i18next'
import { LANGUAGE_LABELS, SUPPORTED_LOCALES, normalizeLocale } from '../../i18n'
import { SUPPORTED_CURRENCIES } from '../../preferences/preferences'
import { useAppPreferences } from '../../preferences/useAppPreferences'

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
  { to: '/app/portfolio', labelKey: 'header.navApp.portfolio' },
  { to: '/app/simulation', labelKey: 'header.navApp.simulation' },
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
  const { theme, setTheme, toggleTheme } = useTheme()
  const { t, i18n } = useTranslation()
  const { currency, setLanguage, setCurrency } = useAppPreferences()
  const currentLocale = normalizeLocale(i18n.resolvedLanguage ?? i18n.language) ?? 'en'

  const closeMenu = () => setMenuOpen(false)
  const closeDesktopPanels = () => {
    setDownloadOpen(false)
    setLocaleOpen(false)
  }

  const handleThemeToggle = (event: MouseEvent<HTMLButtonElement>) => {
    const rect = event.currentTarget.getBoundingClientRect()
    toggleTheme({
      x: rect.left + rect.width / 2,
      y: rect.top + rect.height / 2,
    })
  }

  const handleSelectLocale = async (locale: (typeof SUPPORTED_LOCALES)[number]) => {
    await setLanguage(locale)
    setLocaleOpen(false)
  }

  return (
    <header className="portal-header">
      <div className="portal-header-inner">
        <Link to={isAuthenticated ? '/app' : '/'} className="portal-logo">
          <span className="portal-logo-mark" />
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

          {isAuthenticated ? (
            <button className="portal-action-secondary" onClick={onLogout}>
              {t('header.actions.logout')}
            </button>
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
                          onClick={() => setCurrency(item)}
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
                  {t('header.actions.logout')}
                </button>
              ) : null}
            </div>
          </aside>
        </div>
      ) : null}
    </header>
  )
}

