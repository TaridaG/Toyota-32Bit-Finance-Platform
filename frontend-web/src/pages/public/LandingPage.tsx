import { lazy, Suspense, useMemo } from 'react'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { motion, useReducedMotion } from 'framer-motion'
import { useDocumentTitle } from '../../shared/hooks/useDocumentTitle'
import { useNews } from '../../features/news/hooks/useNews'
import { useTheme } from '../../shared/theme/ThemeProvider'
import { ChartBackground } from './components/ChartBackground'
import { AnalysisPreview } from './components/AnalysisPreview'
import { BankRatesPreview } from './components/BankRatesPreview'
import { LiteracyInfoPreview } from './components/LiteracyInfoPreview'
import { NewsPreview } from './components/NewsPreview'
import { PortfolioShowcasePreview } from './components/PortfolioShowcasePreview'
import { GlobalMarketsSection } from './components/global-markets/GlobalMarketsSection'
import './landing.css'

const SectionDots = lazy(() =>
  import('../../shared/components/navigation/SectionDots').then((module) => ({ default: module.SectionDots })),
)

const SECTION_IDS = [
  'hero',
  'markets',
  'analysis',
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
  const { t } = useTranslation('landing')
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

      <motion.section id="news" className="landing-section landing-screen-section section-news" {...motionProps}>
        <div className="container news-showcase">
          <div className="landing-copy news-copy analysis-copy">
            <h2 className="analysis-title">{t('news.title')}</h2>
            <p className="analysis-subtitle">{t('news.subtitle')}</p>
            <ul className="analysis-points">
              <li>{t('news.points.point1')}</li>
              <li>{t('news.points.point2')}</li>
              <li>{t('news.points.point3')}</li>
            </ul>
            <div className="landing-cta-row analysis-cta-row">
              <Link to="/news" className="landing-cta-primary">
                {t('news.cta')}
              </Link>
            </div>
          </div>
          <div className="news-visual-wrap">
            <NewsPreview items={newsFeed.data} loading={newsFeed.loading} error={Boolean(newsFeed.error)} />
          </div>
        </div>
      </motion.section>

      <motion.section id="portfolio" className="landing-section landing-screen-section section-bank-rates" {...motionProps}>
        <div className="container bank-rates-showcase">
          <div className="landing-copy bank-rates-copy analysis-copy">
            <h2 className="analysis-title">{t('portfolio.title')}</h2>
            <p className="analysis-subtitle">{t('portfolio.subtitle')}</p>
            <Link to="/bank-rates" className="landing-inline-link">
              {t('portfolio.cta')}
            </Link>
          </div>
          <div className="bank-rates-visual-wrap">
            <BankRatesPreview />
          </div>
        </div>
      </motion.section>

      <motion.section id="security" className="landing-section landing-screen-section section-security" {...motionProps}>
        <div className="container literacy-showcase">
          <div className="landing-copy literacy-copy analysis-copy">
            <h2 className="analysis-title">{t('security.title')}</h2>
            <p className="analysis-subtitle">{t('security.subtitle')}</p>
            <Link to="/finansal-okuryazarlik" className="landing-inline-link">
              {t('security.cta')}
            </Link>
          </div>
          <div className="literacy-visual-wrap">
            <LiteracyInfoPreview />
          </div>
        </div>
      </motion.section>

      <motion.section id="final-cta" className="landing-section landing-screen-section landing-final-cta" {...motionProps}>
        <div className="container final-cta-showcase">
          <div className="landing-copy final-cta-copy analysis-copy">
            <h2 className="analysis-title">{t('finalCta.title')}</h2>
            <p className="analysis-subtitle">{t('finalCta.subtitle')}</p>
            <div className="landing-cta-row analysis-cta-row">
              <Link to="/login" className="landing-cta-primary">
                {t('finalCta.login')}
              </Link>
              <Link to="/register" className="landing-cta-secondary">
                {t('finalCta.register')}
              </Link>
            </div>
          </div>
          <div className="final-cta-visual-wrap">
            <PortfolioShowcasePreview />
          </div>
        </div>
      </motion.section>
    </div>
  )
}
