import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import {
  addUtcDaysIso,
  utcTodayIsoDate,
  type PortfolioAnalyticsPresetParam,
  type PortfolioAnalyticsQuery,
} from '../../features/admin/api/adminPortfolioAnalyticsApi'
import { useAdminPortfolioAnalytics } from '../../features/admin/hooks/useAdminPortfolioAnalytics'
import { AdminPortfolioAnalyticsEnterprise } from './AdminPortfolioAnalyticsEnterprise'

const MAX_CUSTOM_DAYS = 120

function utcSpanInclusiveDays(fromIso: string, toIso: string): number {
  const t = (s: string) => {
    const [y, m, d] = s.split('-').map(Number)
    return Date.UTC(y, m - 1, d)
  }
  return Math.floor((t(toIso) - t(fromIso)) / 86400000) + 1
}

export function AdminTotalPortfoliosPage() {
  const { t } = useTranslation('admin')
  const todayUtc = utcTodayIsoDate()
  const [query, setQuery] = useState<PortfolioAnalyticsQuery>({ kind: 'preset', preset: '7d' })
  const [customFrom, setCustomFrom] = useState(() => addUtcDaysIso(todayUtc, -29))
  const [customTo, setCustomTo] = useState(todayUtc)
  const [rangeError, setRangeError] = useState<string | null>(null)

  const { state, refetch } = useAdminPortfolioAnalytics(query)

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

  const selectPreset = (p: PortfolioAnalyticsPresetParam) => {
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
      <AdminPortfolioAnalyticsEnterprise
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
    </div>
  )
}
