import { lazy, Suspense, useMemo } from 'react'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { motion, useReducedMotion } from 'framer-motion'
import { useDocumentTitle } from '../../shared/hooks/useDocumentTitle'
import { useMarkets } from '../../features/markets/hooks/useMarkets'
import { useNews } from '../../features/news/hooks/useNews'
import { formatNumber, formatPrice } from '../../shared/format/number'
import { useAppPreferences } from '../../shared/preferences/useAppPreferences'
import { useTheme } from '../../shared/theme/ThemeProvider'
import type { NewsApiItem } from '../../features/news/api/newsService'
import { ChartBackground } from './components/ChartBackground'
import './landing.css'

const SectionDots = lazy(() =>
  import('../../shared/components/navigation/SectionDots').then((module) => ({ default: module.SectionDots })),
)

const SECTION_IDS = [
  'hero',
  'markets',
  'analysis',
  'ai-insights',
  'news',
  'portfolio',
  'reporting',
  'security',
  'final-cta',
] as const

function sectionMotion(reduceMotion: boolean) {
  if (reduceMotion) {
    return {
      initial: false,
      whileInView: {},
      viewport: { once: true },
      transition: {},
    }
  }

  return {
    initial: { opacity: 0, y: 60 },
    whileInView: { opacity: 1, y: 0 },
    viewport: { once: true, amount: 0.4 },
    transition: { duration: 0.6 },
  }
}

export function LandingPage() {
  const { t, i18n } = useTranslation('landing')
  const reduceMotion = useReducedMotion() ?? false
  const { theme } = useTheme()
  const isDark = theme === 'dark'
  const { currency } = useAppPreferences()
  const marketOverview = useMarkets({ page: 0, size: 4, category: 'all', searchTerm: '', displayCurrency: currency })
  const newsFeed = useNews(0, 8)
  useDocumentTitle(t('titleDoc'))

  const sectionList = useMemo(
    () =>
      SECTION_IDS.map((id) => ({
        id,
        label: t(`sections.${id}.dotLabel`),
      })),
    [t],
  )

  const sentimentStats = useMemo(() => {
    const base = { positive: 0, negative: 0, neutral: 0 }
    return newsFeed.data.reduce((acc, item) => {
      acc[item.sentiment] += 1
      return acc
    }, base)
  }, [newsFeed.data])

  const topReactionItem = useMemo(() => {
    const withReaction = newsFeed.data.filter((item) => item.reactionPercent1h != null)
    return withReaction.sort((a, b) => Math.abs(b.reactionPercent1h ?? 0) - Math.abs(a.reactionPercent1h ?? 0))[0] ?? null
  }, [newsFeed.data])

  const marketHealth = useMemo(() => {
    const total = marketOverview.rows.length
    const live = marketOverview.rows.filter((item) => item.freshness === 'LIVE').length
    const delayed = Math.max(0, total - live)
    return { total, live, delayed }
  }, [marketOverview.rows])

  const leadingSentiment = useMemo(() => {
    const entries: Array<{ key: 'positive' | 'negative' | 'neutral'; value: number }> = [
      { key: 'positive', value: sentimentStats.positive },
      { key: 'negative', value: sentimentStats.negative },
      { key: 'neutral', value: sentimentStats.neutral },
    ]
    entries.sort((a, b) => b.value - a.value)
    return entries[0]?.key ?? 'neutral'
  }, [sentimentStats])

  const motionProps = sectionMotion(reduceMotion)

  return (
    <div className="landing-page landing-root scroll-container">
      <Suspense fallback={null}>
        <SectionDots sections={sectionList} ariaLabel={t('sectionsNavAria')} />
      </Suspense>

      <motion.section id="hero" className="landing-section landing-screen-section landing-hero" {...motionProps}>
        <ChartBackground isReducedMotion={reduceMotion} theme={theme} />
        <div className="landing-hero-content hero-card">
          <p className="landing-kicker">{t('hero.kicker')}</p>
          <h1 className={`hero-title${isDark ? ' hero-title-dark' : ' hero-title-light'}`}>{t('hero.title')}</h1>
          <p className={`landing-subtitle${isDark ? ' landing-subtitle-dark' : ' landing-subtitle-light'}`}>{t('hero.subtitle')}</p>
          <div className="landing-cta-row">
            <Link to="/login" className="landing-cta-primary">
              {t('hero.ctaLogin')}
            </Link>
            <Link to="/register" className="landing-cta-secondary">
              {t('hero.ctaRegister')}
            </Link>
          </div>
        </div>
        <div className="hero-fade" />
      </motion.section>

      <motion.section id="markets" className="landing-section landing-screen-section section-markets" {...motionProps}>
        <div className="container">
          <header className="landing-section-head landing-section-inner">
            <div className="landing-markets-head-title">
              <h2 className="section-title">{t('markets.title')}</h2>
              <span className={`landing-live-pill${marketHealth.delayed > 0 ? ' landing-live-pill-delayed' : ''}`}>
                {marketHealth.delayed > 0 ? 'DELAYED' : 'LIVE'}
              </span>
            </div>
            <p>{t('markets.subtitle')}</p>
            {marketHealth.total > 0 ? (
              <p className="landing-markets-health">
                {marketHealth.live}/{marketHealth.total} instruments live
              </p>
            ) : null}
          </header>
          {marketOverview.loading ? (
            <p className="landing-state">{t('states.loading')}</p>
          ) : marketOverview.error ? (
            <div className="landing-state-row">
              <p className="landing-state">{t('states.error')}</p>
              <button type="button" onClick={() => void marketOverview.refetch()}>
                {t('states.retry')}
              </button>
            </div>
          ) : (
            <motion.div
              className="landing-card-grid grid landing-section-inner"
              initial={reduceMotion ? false : { opacity: 0 }}
              whileInView={reduceMotion ? {} : { opacity: 1 }}
              viewport={{ once: true, amount: 0.35 }}
              transition={{ staggerChildren: 0.08 }}
            >
              {marketOverview.rows.map((instrument) => (
                <motion.article
                  key={instrument.symbol}
                  className="landing-feature-card landing-card"
                  initial={reduceMotion ? false : { opacity: 0, y: 16 }}
                  whileInView={reduceMotion ? {} : { opacity: 1, y: 0 }}
                  viewport={{ once: true, amount: 0.35 }}
                  transition={{ duration: 0.4 }}
                >
                  <div>
                    <strong className="landing-feature-card-symbol">
                      {instrument.symbol}
                      <span
                        className={`landing-feature-status-dot${
                          instrument.freshness === 'STALE' ? ' landing-feature-status-dot-stale' : ''
                        }`}
                        aria-hidden
                      />
                    </strong>
                    <p>{instrument.name}</p>
                  </div>
                  <div className="landing-feature-card-right">
                    <span className="landing-feature-price">{formatPrice(instrument.price, i18n.language, currency)}</span>
                    <small className={`landing-change-chip ${(instrument.change24h ?? 0) >= 0 ? 'landing-up' : 'landing-down'}`}>
                      {(instrument.change24h ?? 0) >= 0 ? '▲ ' : '▼ '}
                      {formatNumber(instrument.change24h, i18n.language, 2)}%
                    </small>
                  </div>
                </motion.article>
              ))}
            </motion.div>
          )}
        </div>
      </motion.section>

      <motion.section id="analysis" className="landing-section landing-screen-section section-analysis" {...motionProps}>
        <div className="container analysis-grid">
          <div className="landing-copy">
            <h2 className="section-title">{t('analysis.title')}</h2>
            <p>{t('analysis.subtitle')}</p>
            <ul>
              <li>{t('analysis.points.point1')}</li>
              <li>{t('analysis.points.point2')}</li>
              <li>{t('analysis.points.point3')}</li>
            </ul>
          </div>
          <div className="landing-visual-box landing-card">
            <h3>{t('analysis.visualTitle')}</h3>
            <p>{t('analysis.visualBody')}</p>
          </div>
        </div>
      </motion.section>

      <motion.section id="ai-insights" className="landing-section landing-screen-section section-ai" {...motionProps}>
        <div className="container landing-split">
          <div className="landing-copy">
            <h2 className="section-title">{t('ai.title')}</h2>
            <p>{t('ai.subtitle')}</p>
            <p>{t(`ai.leadingSentiment.${leadingSentiment}`)}</p>
          </div>
          <div className="landing-stats-box landing-card">
            <p>{t('ai.stats.positive', { count: sentimentStats.positive })}</p>
            <p>{t('ai.stats.negative', { count: sentimentStats.negative })}</p>
            <p>{t('ai.stats.neutral', { count: sentimentStats.neutral })}</p>
            <p>
              {topReactionItem
                ? t('ai.stats.reaction', {
                    symbol: topReactionSymbol(topReactionItem),
                    value: formatNumber(topReactionItem.reactionPercent1h, i18n.language, 2),
                  })
                : t('ai.stats.reactionFallback')}
            </p>
          </div>
        </div>
      </motion.section>

      <motion.section id="news" className="landing-section landing-screen-section" {...motionProps}>
        <div className="container landing-split">
          <div className="landing-copy">
            <h2 className="section-title">{t('news.title')}</h2>
            <p>{t('news.subtitle')}</p>
          </div>
          <div className="landing-news-list landing-card">
            {newsFeed.loading ? (
              <p className="landing-state">{t('states.loading')}</p>
            ) : newsFeed.error ? (
              <div className="landing-state-row">
                <p className="landing-state">{t('states.error')}</p>
                <button type="button" onClick={() => void newsFeed.refetch()}>
                  {t('states.retry')}
                </button>
              </div>
            ) : (
              newsFeed.data.slice(0, 3).map((item) => (
                <article key={item.id} className="landing-news-card landing-card">
                  <strong>{item.title}</strong>
                  <p>{item.summary ?? t('states.notAvailable')}</p>
                </article>
              ))
            )}
          </div>
        </div>
      </motion.section>

      <motion.section id="portfolio" className="landing-section landing-screen-section section-portfolio" {...motionProps}>
        <div className="container landing-split">
          <div className="landing-copy">
            <h2 className="section-title">{t('portfolio.title')}</h2>
            <p>{t('portfolio.subtitle')}</p>
            <Link to="/my-portfolio" className="landing-inline-link">
              {t('portfolio.cta')}
            </Link>
          </div>
          <div className="landing-visual-box landing-card">
            <h3>{t('portfolio.visualTitle')}</h3>
            <p>{t('portfolio.visualBody')}</p>
          </div>
        </div>
      </motion.section>

      <motion.section id="reporting" className="landing-section landing-screen-section" {...motionProps}>
        <div className="container landing-split">
          <div className="landing-copy">
            <h2 className="section-title">{t('reporting.title')}</h2>
            <p>{t('reporting.subtitle')}</p>
          </div>
          <div className="landing-visual-box landing-card">
            <h3>{t('reporting.visualTitle')}</h3>
            <p>{t('reporting.visualBody')}</p>
          </div>
        </div>
      </motion.section>

      <motion.section id="security" className="landing-section landing-screen-section section-security" {...motionProps}>
        <div className="container landing-split">
          <div className="landing-copy">
            <h2 className="section-title">{t('security.title')}</h2>
            <p>{t('security.subtitle')}</p>
          </div>
          <div className="landing-visual-box landing-card">
            <h3>{t('security.visualTitle')}</h3>
            <p>{t('security.visualBody')}</p>
          </div>
        </div>
      </motion.section>

      <motion.section id="final-cta" className="landing-section landing-screen-section landing-final-cta" {...motionProps}>
        <h2>{t('finalCta.title')}</h2>
        <p>{t('finalCta.subtitle')}</p>
        <div className="landing-cta-row">
          <Link to="/login" className="landing-cta-primary">
            {t('finalCta.login')}
          </Link>
          <Link to="/register" className="landing-cta-secondary">
            {t('finalCta.register')}
          </Link>
        </div>
      </motion.section>
    </div>
  )
}

function topReactionSymbol(item: NewsApiItem): string {
  return item.relatedSymbols[0] ?? item.sourceName
}

