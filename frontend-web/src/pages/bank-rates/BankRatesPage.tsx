import { useCallback, useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { useDocumentTitle } from '../../shared/hooks/useDocumentTitle'
import { fetchBankRates, type BankRatesResponse } from './api/bankRatesApi'
import {
  BANK_RATES_TAB_ASSET,
  BANK_RATES_TABS,
  type BankRatesTabId,
} from './bankRatesAssets'
import { BankRatesAssetCards } from './components/BankRatesAssetCards'
import { BankRatesTable } from './components/BankRatesTable'

export function BankRatesPage() {
  const { t } = useTranslation('common')
  useDocumentTitle(t('bankRatesPage.titleDoc'))

  const [tab, setTab] = useState<BankRatesTabId>('usd')
  const [cache, setCache] = useState<Partial<Record<BankRatesTabId, BankRatesResponse>>>({})
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const load = useCallback(
    async (id: BankRatesTabId, options?: { silent?: boolean }) => {
      if (!options?.silent) {
        setLoading(true)
        setError(null)
      }
      try {
        const response = await fetchBankRates(BANK_RATES_TAB_ASSET[id])
        setCache((prev) => ({ ...prev, [id]: response }))
        if (!options?.silent) {
          setError(null)
        }
      } catch {
        if (!options?.silent) {
          setCache((prev) => {
            const next = { ...prev }
            delete next[id]
            return next
          })
          setError(t('bankRatesPage.loadError'))
        }
      } finally {
        if (!options?.silent) {
          setLoading(false)
        }
      }
    },
    [t],
  )

  useEffect(() => {
    void load(tab)
  }, [tab, load])

  useEffect(() => {
    for (const id of BANK_RATES_TABS) {
      if (id !== tab && !cache[id]) {
        void load(id, { silent: true })
      }
    }
    // Kart önizlemeleri; yalnızca ilk mount'ta eksik snapshot'ları doldur
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const data = cache[tab] ?? null
  const tableTitle = data?.title ?? t(`bankRatesPage.sectionTitle.${tab}`)

  return (
    <section className="bank-rates-page" aria-labelledby="bank-rates-heading">
      <header className="section-header">
        <h2 id="bank-rates-heading">{t('bankRatesPage.title')}</h2>
        <p className="bank-rates-page-lead">{t('bankRatesPage.lead')}</p>
      </header>

      <BankRatesAssetCards active={tab} cache={cache} onSelect={setTab} />

      <div className="bank-rates-toolbar">
        <button
          type="button"
          className="bank-rates-refresh"
          onClick={() => {
            setCache({})
            void load(tab)
            for (const id of BANK_RATES_TABS) {
              if (id !== tab) {
                void load(id, { silent: true })
              }
            }
          }}
          disabled={loading}
        >
          {t('bankRatesPage.refresh')}
        </button>
        {data?.fetchedAt ? (
          <span className="bank-rates-updated">
            {t('bankRatesPage.updatedAt', {
              time: new Date(data.fetchedAt).toLocaleTimeString('tr-TR', {
                hour: '2-digit',
                minute: '2-digit',
              }),
            })}
          </span>
        ) : null}
      </div>

      {loading ? (
        <div className="bank-rates-page-placeholder" role="status" aria-live="polite">
          <p>{t('bankRatesPage.loading')}</p>
        </div>
      ) : error ? (
        <div className="bank-rates-page-placeholder" role="alert">
          <p>{error}</p>
        </div>
      ) : data?.rows?.length ? (
        <BankRatesTable title={tableTitle} rows={data.rows} />
      ) : (
        <div className="bank-rates-page-placeholder" role="status">
          <p>{t('bankRatesPage.empty')}</p>
        </div>
      )}
    </section>
  )
}
