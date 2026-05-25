import { lazy, memo, Suspense, useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { motion, useReducedMotion, type Variants } from 'framer-motion'
import { useTheme } from '../../../../shared/theme/ThemeProvider'
import { StarfieldBackground } from './StarfieldBackground'
import { useGlobePointer } from './useGlobePointer'
import './global-markets.css'

const GlobeCanvas = lazy(() => import('./GlobeCanvas').then((module) => ({ default: module.GlobeCanvas })))

const FEATURE_KEYS = ['feature1', 'feature2', 'feature3', 'feature4'] as const

function useMediaQuery(query: string): boolean {
  const [matches, setMatches] = useState(() =>
    typeof window !== 'undefined' ? window.matchMedia(query).matches : false,
  )

  useEffect(() => {
    const media = window.matchMedia(query)
    const onChange = () => setMatches(media.matches)
    onChange()
    media.addEventListener('change', onChange)
    return () => media.removeEventListener('change', onChange)
  }, [query])

  return matches
}

export const GlobalMarketsSection = memo(function GlobalMarketsSection() {
  const { t } = useTranslation('landing')
  const { theme } = useTheme()
  const isDark = theme === 'dark'
  const reduceMotion = useReducedMotion() ?? false
  const isMobile = useMediaQuery('(max-width: 900px)')
  const sectionRef = useRef<HTMLElement>(null)
  const globeHostRef = useRef<HTMLDivElement>(null)
  const [isVisible, setIsVisible] = useState(false)

  const tiltRef = useGlobePointer(globeHostRef, !reduceMotion && !isMobile)

  useEffect(() => {
    const node = sectionRef.current
    if (!node) {
      return undefined
    }
    const observer = new IntersectionObserver(
      ([entry]) => {
        setIsVisible(entry.isIntersecting)
      },
      { rootMargin: '120px 0px', threshold: 0.08 },
    )
    observer.observe(node)
    return () => observer.disconnect()
  }, [])

  const reveal = reduceMotion
    ? { initial: false }
    : {
        initial: { opacity: 0 },
        whileInView: { opacity: 1 },
        viewport: { once: true, amount: 0.15 },
        transition: { duration: 0.6, ease: [0.22, 1, 0.36, 1] as const },
      }

  const childVariants: Variants = reduceMotion
    ? {}
    : {
        hidden: { opacity: 0, y: 14 },
        visible: { opacity: 1, y: 0, transition: { duration: 0.5 } },
      }

  const stagger = reduceMotion
    ? {}
    : {
        variants: {
          hidden: {},
          visible: { transition: { staggerChildren: 0.07, delayChildren: 0.05 } },
        },
        initial: 'hidden' as const,
        whileInView: 'visible' as const,
        viewport: { once: true, amount: 0.15 },
      }

  return (
    <motion.section
      id="markets"
      ref={sectionRef}
      className="landing-section section-markets global-markets-section"
      {...reveal}
    >
      <StarfieldBackground />

      <div className="global-markets-stage">
        <motion.div className="global-markets-split" {...stagger}>
          <motion.div className="global-markets-left" variants={childVariants}>
            <div className="global-markets-copy">
              <span className="gm-kicker">
                <span className="gm-kicker-dot" aria-hidden="true" />
                {t('globalMarkets.kicker')}
              </span>
              <h2 className="gm-title">
                {t('globalMarkets.titleLine1')}
                <br />
                <span className="gm-title-accent">{t('globalMarkets.titleAccent')}</span>{' '}
                {t('globalMarkets.titleLine2')}
              </h2>
              <p className="gm-subtitle">{t('globalMarkets.subtitle')}</p>
            </div>

            <div className="global-markets-footer">
              <ul className="gm-features">
                {FEATURE_KEYS.map((key) => (
                  <li key={key}>
                    <span className="gm-feature-check" aria-hidden="true">
                      <svg viewBox="0 0 16 16">
                        <path d="M3.5 8.2 6.4 11l6.1-6.4" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" />
                      </svg>
                    </span>
                    {t(`globalMarkets.features.${key}`)}
                  </li>
                ))}
              </ul>
              <Link to="/markets" className="gm-cta">
                {t('globalMarkets.cta')}
                <span className="gm-cta-arrow" aria-hidden="true">
                  →
                </span>
              </Link>
            </div>
          </motion.div>

          <motion.div className="global-markets-globe-col" variants={childVariants}>
            <div className="gm-ambient gm-ambient-globe" aria-hidden="true" />
            <div ref={globeHostRef} className="global-markets-globe-wrap">
              <Suspense fallback={<div className="gm-globe-fallback" aria-hidden="true" />}>
                <GlobeCanvas
                  tiltRef={tiltRef}
                  reduceMotion={reduceMotion}
                  isMobile={isMobile}
                  isVisible={isVisible}
                  isDark={isDark}
                />
              </Suspense>
            </div>
          </motion.div>
        </motion.div>
      </div>
    </motion.section>
  )
})
