import { lazy, Suspense, useMemo } from 'react'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { motion, useReducedMotion } from 'framer-motion'
import { useDocumentTitle } from '../../shared/hooks/useDocumentTitle'
import { useNews } from '../../features/news/hooks/useNews'
import { formatNumber } from '../../shared/format/number'
import { useTheme } from '../../shared/theme/ThemeProvider'
import type { NewsApiItem } from '../../features/news/api/newsService'
import { ChartBackground } from './components/ChartBackground'
import { AnalysisPreview } from './components/AnalysisPreview'
import { GlobalMarketsSection } from './components/global-markets/GlobalMarketsSection'
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
      const sentiment = item.sentiment ?? 'neutral'
      acc[sentiment] += 1
      return acc
    }, base)
  }, [newsFeed.data])

  const topReactionItem = useMemo(() => {
    const withReaction = newsFeed.data.filter((item) => item.reactionPercent1h != null)
    return withReaction.sort((a, b) => Math.abs(b.reactionPercent1h ?? 0) - Math.abs(a.reactionPercent1h ?? 0))[0] ?? null
  }, [newsFeed.data])

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

      <GlobalMarketsSection />

      <motion.section id="analysis" className="landing-section landing-screen-section section-analysis" {...motionProps}>
        <div className="container analysis-showcase">
          <div className="landing-copy analysis-copy">
            <h2 className="analysis-title">{t('analysis.title')}</h2>
            <p className="analysis-subtitle">{t('analysis.subtitle')}</p>
            <ul className="analysis-points">
              <li>{t('analysis.points.point1')}</li>
              <li>{t('analysis.points.point2')}</li>
              <li>{t('analysis.points.point3')}</li>
            </ul>
            <div className="landing-cta-row analysis-cta-row">
              <Link to="/analysis" className="landing-cta-primary">
                {t('analysis.cta')}
              </Link>
            </div>
          </div>
          <div className="analysis-visual-wrap">
            <AnalysisPreview />
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
