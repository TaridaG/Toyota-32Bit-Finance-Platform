import { useDocumentTitle } from '../../shared/hooks/useDocumentTitle'
import { useTranslation } from 'react-i18next'

export function SimulationPage() {
  const { t } = useTranslation('common')
  useDocumentTitle(t('simulation.titleDoc'))

  return (
    <section>
      <h2>{t('simulation.title')}</h2>
      <p>{t('simulation.lead')}</p>
    </section>
  )
}

