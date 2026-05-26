import { useTranslation } from 'react-i18next'

export function LiteracyInfoPreview() {
  const { t } = useTranslation(['landing', 'common'])
  const stats = [
    { value: '120+', label: t('security.preview.stats.terms', { ns: 'landing' }) },
    { value: '24', label: t('security.preview.stats.charts', { ns: 'landing' }) },
    { value: '8', label: t('security.preview.stats.tools', { ns: 'landing' }) },
  ]

  const previewTerms = [
    {
      title: t('security.preview.cards.rsi.title', { ns: 'landing' }),
      badge: t('security.preview.cards.rsi.badge', { ns: 'landing' }),
      difficulty: t('security.preview.cards.rsi.difficulty', { ns: 'landing' }),
      description: t('security.preview.cards.rsi.description', { ns: 'landing' }),
    },
    {
      title: t('security.preview.cards.candles.title', { ns: 'landing' }),
      badge: t('security.preview.cards.candles.badge', { ns: 'landing' }),
      difficulty: t('security.preview.cards.candles.difficulty', { ns: 'landing' }),
      description: t('security.preview.cards.candles.description', { ns: 'landing' }),
    },
  ]

  return (
    <div className="literacy-preview" aria-hidden="true">
      <div className="literacy-preview-shell">
        <div className="literacy-preview-head">
          <div>
            <h3>{t('finansalOkuryazarlikPage.title', { ns: 'common' })}</h3>
            <p>{t('finansalOkuryazarlikPage.lead', { ns: 'common' })}</p>
          </div>
          <button type="button" className="literacy-preview-info-button">
            {t('security.preview.infoButton', { ns: 'landing' })}
          </button>
        </div>

        <div className="literacy-preview-search">
          <span>{t('finansalOkuryazarlikPage.searchPlaceholder', { ns: 'common' })}</span>
        </div>

        <div className="literacy-preview-stats">
          {stats.map((stat) => (
            <div key={stat.label} className="literacy-preview-stat">
              <strong>{stat.value}</strong>
              <span>{stat.label}</span>
            </div>
          ))}
        </div>

        <div className="literacy-preview-content">
          <aside className="literacy-preview-sidebar">
            <h4>{t('security.preview.learningAreas', { ns: 'landing' })}</h4>
            <ul>
              <li className="is-active">{t('security.preview.areas.markets', { ns: 'landing' })}</li>
              <li>{t('security.preview.areas.technical', { ns: 'landing' })}</li>
              <li>{t('security.preview.areas.macro', { ns: 'landing' })}</li>
              <li>{t('security.preview.areas.portfolio', { ns: 'landing' })}</li>
            </ul>
          </aside>

          <div className="literacy-preview-cards">
            {previewTerms.map((item) => (
              <article key={item.title} className="literacy-preview-card">
                <div className="literacy-preview-card-head">
                  <h4>{item.title}</h4>
                  <div className="literacy-preview-badges">
                    <span>{item.badge}</span>
                    <span>{item.difficulty}</span>
                  </div>
                </div>
                <p>{item.description}</p>
                <div className="literacy-preview-card-actions">
                  <span className="literacy-preview-page-pill">{t('security.preview.pagePill', { ns: 'landing' })}</span>
                  <span className="literacy-preview-help-pill">{t('security.preview.helpPill', { ns: 'landing' })}</span>
                </div>
              </article>
            ))}
          </div>
        </div>
      </div>
    </div>
  )
}
