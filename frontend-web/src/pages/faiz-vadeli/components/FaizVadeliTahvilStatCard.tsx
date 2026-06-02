import { useTranslation } from 'react-i18next'
import type { StatCardCopy } from '../faizVadeliDashboardCopy'
import { FaizVadeliStatCard } from './FaizVadeliStatCard'

export function FaizVadeliTahvilStatCard({
  template,
  onShowHistory,
}: {
  template: StatCardCopy
  onShowHistory: () => void
}) {
  const { t } = useTranslation('common')
  const stat: StatCardCopy = {
    ...template,
    value: t('faizVadeliPage.tahvil.cardValue'),
    sub1: t('faizVadeliPage.tahvil.cardHint'),
    sub2: undefined,
    badge: undefined,
    delta: undefined,
    deltaTone: undefined,
  }

  return (
    <FaizVadeliStatCard
      stat={stat}
      interactive
      onActivate={onShowHistory}
      interactiveAriaLabel={t('faizVadeliPage.tahvil.detailOpenHint')}
      interactiveTitle={t('faizVadeliPage.tahvil.detailOpenHint')}
    />
  )
}
