import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useDocumentTitle } from '../../shared/hooks/useDocumentTitle'

const featuredInstruments = [
  { symbol: 'BTCUSDT', name: 'Bitcoin', price: '69,240.10', change: '+1.83%' },
  { symbol: 'ETHUSDT', name: 'Ethereum', price: '3,540.65', change: '+1.21%' },
  { symbol: 'XAUUSD', name: 'Gold Ounce', price: '2,420.80', change: '-0.34%' },
  { symbol: 'NASDAQ', name: 'Nasdaq 100', price: '19,845.12', change: '+0.92%' },
]

const highlightKeyPairs = [
  { titleKey: 'landing.highlights.realtimeTitle', descriptionKey: 'landing.highlights.realtimeDesc' },
  { titleKey: 'landing.highlights.reportTitle', descriptionKey: 'landing.highlights.reportDesc' },
  { titleKey: 'landing.highlights.riskTitle', descriptionKey: 'landing.highlights.riskDesc' },
]

export function LandingPage() {
  const { t } = useTranslation()
  useDocumentTitle(t('landing.titleDoc'))

  return (
    <div className="landing-page">
      <header className="landing-hero">
        <div>
          <p className="landing-badge">{t('landing.kicker')}</p>
          <h1>{t('landing.heroTitle')}</h1>
          <p className="landing-subtitle">{t('landing.heroSubtitle')}</p>
          <div className="landing-cta-group">
            <Link to="/login" className="landing-cta-primary">
              {t('landing.ctaLogin')}
            </Link>
            <Link to="/register" className="landing-cta-secondary">
              {t('landing.ctaRegister')}
            </Link>
          </div>
        </div>
      </header>

      <section className="landing-section">
        <div className="landing-section-head">
          <h2>{t('landing.instrumentsTitle')}</h2>
          <p>{t('landing.instrumentsSubtitle')}</p>
        </div>
        <div className="instrument-grid">
          {featuredInstruments.map((instrument) => (
            <article key={instrument.symbol} className="instrument-card">
              <div>
                <h3>{instrument.symbol}</h3>
                <p>{instrument.name}</p>
              </div>
              <div>
                <strong>{instrument.price}</strong>
                <span
                  className={
                    instrument.change.startsWith('-')
                      ? 'instrument-change instrument-change-down'
                      : 'instrument-change instrument-change-up'
                  }
                >
                  {instrument.change}
                </span>
              </div>
            </article>
          ))}
        </div>
      </section>

      <section className="landing-section">
        <div className="landing-section-head">
          <h2>{t('landing.whyTitle')}</h2>
        </div>
        <div className="highlight-grid">
          {highlightKeyPairs.map((item) => (
            <article key={item.titleKey} className="highlight-card">
              <h3>{t(item.titleKey)}</h3>
              <p>{t(item.descriptionKey)}</p>
            </article>
          ))}
        </div>
      </section>
    </div>
  )
}

