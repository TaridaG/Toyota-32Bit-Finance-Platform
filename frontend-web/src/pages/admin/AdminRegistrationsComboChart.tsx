import { useTranslation } from 'react-i18next'
import type { AdminUserDailyRegistration } from '../../features/admin/api/adminUserAnalyticsApi'
import { AdminDailyComboChart } from './AdminDailyComboChart'

type Props = {
  rows: AdminUserDailyRegistration[]
  locale: string
}

export function AdminRegistrationsComboChart({ rows, locale }: Props) {
  const { t } = useTranslation('admin')
  const comboRows = rows.map((r) => ({
    date: r.date,
    barValue: r.newUsers,
    rollingAverage7d: r.rollingAverage7d,
    deltaVsPreviousDay: r.deltaVsPreviousDay,
  }))
  return (
    <AdminDailyComboChart
      rows={comboRows}
      locale={locale}
      labels={{
        empty: t('totalUsersPage.analytics.chartEmpty'),
        aria: t('totalUsersPage.analytics.chartAria'),
        tipPrimary: t('totalUsersPage.analytics.tipNew'),
        tipAvg: t('totalUsersPage.analytics.tipAvg7'),
        tipDelta: t('totalUsersPage.analytics.tipDelta'),
      }}
    />
  )
}
