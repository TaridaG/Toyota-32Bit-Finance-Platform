import { useTranslation } from 'react-i18next'
import { useDocumentTitle } from '../../shared/hooks/useDocumentTitle'

export function TurkiyeEkonomisiPage() {
  const { t } = useTranslation('common')
  useDocumentTitle(t('turkiyeEkonomisiPage.titleDoc'))

  return (
    <section className="turkiye-ekonomisi-page" aria-labelledby="turkiye-ekonomisi-heading">
      <header className="section-header">
        <h2 id="turkiye-ekonomisi-heading">{t('turkiyeEkonomisiPage.title')}</h2>
        <p className="turkiye-ekonomisi-page-lead">{t('turkiyeEkonomisiPage.lead')}</p>
      </header>
      <div className="fi-faiz-tab-placeholder turkiye-ekonomisi-page-placeholder" role="status">
        <p>{t('turkiyeEkonomisiPage.placeholder')}</p>
      </div>
    </section>
  )
}
