import { useTranslation } from 'react-i18next'

type Props = {
  titleKey: string
  leadKey: string
}

export function AdminPlaceholderPage({ titleKey, leadKey }: Props) {
  const { t } = useTranslation('admin')

  return (
    <div className="fi-admin-page">
      <header className="fi-admin-page-head">
        <div>
          <h1 className="fi-admin-h1">{t(titleKey)}</h1>
          <p className="fi-admin-lead">{t(leadKey)}</p>
        </div>
        <span className="fi-admin-pill">{t('overview.mockData')}</span>
      </header>
      <section className="fi-admin-card fi-admin-placeholder">
        <p>{t('placeholder.body')}</p>
      </section>
    </div>
  )
}
