import { useTranslation } from 'react-i18next'

export function InsightBox({ text }: { text: string }) {
  const { t } = useTranslation('newsPage')
  return (
    <article className="card fi-panel-card fi-insight-box">
      <h3>{t('aiInsightTitle')}</h3>
      <p>{text}</p>
    </article>
  )
}
