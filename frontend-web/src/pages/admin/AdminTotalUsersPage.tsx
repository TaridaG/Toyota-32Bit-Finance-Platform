import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import {
  addUtcDaysIso,
  utcTodayIsoDate,
  type UserAnalyticsPresetParam,
  type UserAnalyticsQuery,
} from '../../features/admin/api/adminUserAnalyticsApi'
import { useAdminUserAnalytics } from '../../features/admin/hooks/useAdminUserAnalytics'
import { AdminUserAnalyticsEnterprise } from './AdminUserAnalyticsEnterprise'
import { AdminUserDirectorySection } from './AdminUserDirectorySection'

const MAX_CUSTOM_DAYS = 120

function utcSpanInclusiveDays(fromIso: string, toIso: string): number {
  const t = (s: string) => {
    const [y, m, d] = s.split('-').map(Number)
    return Date.UTC(y, m - 1, d)
  }
  return Math.floor((t(toIso) - t(fromIso)) / 86400000) + 1
}

export function AdminTotalUsersPage() {
  const { t } = useTranslation('admin')
  const todayUtc = utcTodayIsoDate()
  const [query, setQuery] = useState<UserAnalyticsQuery>({ kind: 'preset', preset: '7d' })
  const [customFrom, setCustomFrom] = useState(() => addUtcDaysIso(todayUtc, -29))
  const [customTo, setCustomTo] = useState(todayUtc)
  const [rangeError, setRangeError] = useState<string | null>(null)

  const { state, refetch } = useAdminUserAnalytics(query)

  useEffect(() => {
    if (query.kind === 'custom') {
      setCustomFrom(query.from)
      setCustomTo(query.to)
    }
  }, [query])

  useEffect(() => {
    if (state.status === 'ok') {
      setRangeError(null)
    }
  }, [state])

  const selectPreset = (p: UserAnalyticsPresetParam) => {
    setRangeError(null)
    setQuery({ kind: 'preset', preset: p })
  }

  const selectCustomDefault = () => {
    setRangeError(null)
    const to = utcTodayIsoDate()
    const from = addUtcDaysIso(to, -29)
    setCustomFrom(from)
    setCustomTo(to)
    setQuery({ kind: 'custom', from, to })
  }

  const applyCustomRange = () => {
    setRangeError(null)
    if (!customFrom || !customTo) {
      setRangeError(t('totalUsersPage.analytics.customBothRequired'))
      return
    }
    if (customFrom > customTo) {
      setRangeError(t('totalUsersPage.analytics.customEndBeforeStart'))
      return
    }
    const span = utcSpanInclusiveDays(customFrom, customTo)
    if (span > MAX_CUSTOM_DAYS) {
      setRangeError(t('totalUsersPage.analytics.customTooLong', { max: MAX_CUSTOM_DAYS }))
      return
    }
    setQuery({ kind: 'custom', from: customFrom, to: customTo })
  }

  return (
    <div className="fi-admin-page fi-admin-total-users fi-admin-total-users--enterprise">
      <AdminUserAnalyticsEnterprise
        query={query}
        onSelectPreset={selectPreset}
        onSelectCustomDefault={selectCustomDefault}
        customFrom={customFrom}
        customTo={customTo}
        onCustomFromChange={setCustomFrom}
        onCustomToChange={setCustomTo}
        onApplyCustomRange={applyCustomRange}
        rangeError={rangeError}
        analytics={state}
        onRefresh={refetch}
      />
      <AdminUserDirectorySection />
    </div>
  )
}
