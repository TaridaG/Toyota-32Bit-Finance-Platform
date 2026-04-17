import { useDocumentTitle } from '../../shared/hooks/useDocumentTitle'
import { useTranslation } from 'react-i18next'

export function DashboardPage() {
  const { t } = useTranslation()
  useDocumentTitle(t('dashboard.titleDoc'))

  return (
    <section>
      <h2>{t('dashboard.title')}</h2>
      <p>{t('dashboard.lead')}</p>
    </section>
  )
}

