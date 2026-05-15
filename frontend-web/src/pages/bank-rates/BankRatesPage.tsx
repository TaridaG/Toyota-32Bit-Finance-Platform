import { useTranslation } from 'react-i18next'
import { useDocumentTitle } from '../../shared/hooks/useDocumentTitle'

export function BankRatesPage() {
  const { t } = useTranslation('common')
  useDocumentTitle(t('bankRatesPage.titleDoc'))

  return (
    <section className="bank-rates-page" aria-labelledby="bank-rates-heading">
      <header className="section-header">
        <h2 id="bank-rates-heading">{t('bankRatesPage.title')}</h2>
        <p className="bank-rates-page-lead">{t('bankRatesPage.lead')}</p>
      </header>
      <div className="fi-faiz-tab-placeholder bank-rates-page-placeholder" role="status">
        <p>{t('bankRatesPage.placeholder')}</p>
      </div>
    </section>
  )
}
