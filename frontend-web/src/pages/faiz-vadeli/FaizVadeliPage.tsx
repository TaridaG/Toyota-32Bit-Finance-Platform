import { Link } from 'react-router-dom'
import { useTranslation } from 'react-i18next'
import { useDocumentTitle } from '../../shared/hooks/useDocumentTitle'
import { FaizVadeliDashboard } from './FaizVadeliDashboard'

export function FaizVadeliPage() {
  const { t } = useTranslation('common')
  useDocumentTitle(t('faizVadeliPage.titleDoc'))

  return (
    <section className="fi-faiz-vadeli-page" aria-labelledby="faiz-vadeli-heading">
      <header className="section-header fi-faiz-page-head">
        <div>
          <h2 id="faiz-vadeli-heading">{t('faizVadeliPage.title')}</h2>
          <p className="fi-faiz-vadeli-page-sub">{t('faizVadeliPage.lead')}</p>
        </div>
        <Link to="/app/markets" className="fi-faiz-vadeli-link">
          {t('faizVadeliPage.cardBondsCta')}
        </Link>
      </header>

      <FaizVadeliDashboard />
    </section>
  )
}
