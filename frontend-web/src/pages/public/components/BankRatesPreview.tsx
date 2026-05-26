import { useCallback, useEffect, useMemo, useState } from 'react'
import { useTranslation } from 'react-i18next'
import {
  fetchBankRates,
  type BankRatesResponse,
  type BankRatesRow,
} from '../../bank-rates/api/bankRatesApi'
import { BANK_RATES_TABS, type BankRatesTabId, BANK_RATES_TAB_ASSET } from '../../bank-rates/bankRatesAssets'
import { formatBankRate, formatSpreadPercent } from '../../bank-rates/lib/formatBankRate'
import { pickBestBuyRow, pickBestSellRow } from '../../bank-rates/lib/pickBestRows'

type PreviewCache = Partial<Record<BankRatesTabId, BankRatesResponse>>

const FALLBACK_CACHE: PreviewCache = {
  usd: {
    asset: 'USD',
    title: 'Amerikan Doları Banka Kurları',
    fetchedAt: null,
    rows: [
      { slug: 'kapalicarsi', name: 'Kapalıçarşı', buy: 45.78, sell: 45.8, spread: 0.02, spreadPercent: 0.04, bestBuy: true, bestSpreadMin: true, bestSpreadPctMin: true },
      { slug: 'altinkaynak', name: 'Altınkaynak', buy: 45.659, sell: 45.902, spread: 0.243, spreadPercent: 0.53 },
      { slug: 'harem', name: 'Harem', buy: 45.7396, sell: 46.0454, spread: 0.3058, spreadPercent: 0.67 },
      { slug: 'odaci', name: 'Odacı', buy: 45.705, sell: 45.964, spread: 0.259, spreadPercent: 0.57 },
      { slug: 'venus', name: 'Venüs', buy: 45.717, sell: 46.059, spread: 0.342, spreadPercent: 0.75 },
    ],
  },
  eur: {
    asset: 'EUR',
    rows: [
      { slug: 'doviz', name: 'Döviz', buy: 53.15, sell: 53.1824, bestBuy: true, bestSell: true },
    ],
  },
  gbp: {
    asset: 'GBP',
    rows: [
      { slug: 'meksa', name: 'Meksa', buy: 61.4571, sell: 61.3554, bestBuy: true, bestSell: true },
    ],
  },
  gold: {
    asset: 'GOLD',
    rows: [
      { slug: 'doviz', name: 'Döviz', buy: 6703.66, sell: 6609.25, bestBuy: true, bestSell: true },
    ],
  },
}

export function BankRatesPreview() {
  const { t, i18n } = useTranslation('common')
  const [cache, setCache] = useState<PreviewCache>({})
  const [loading, setLoading] = useState(true)

  const loadAll = useCallback(async () => {
    setLoading(true)
    try {
      const responses = await Promise.all(
        BANK_RATES_TABS.map(async (id) => [id, await fetchBankRates(BANK_RATES_TAB_ASSET[id])] as const),
      )
      setCache(Object.fromEntries(responses))
    } catch {
      // Keep fallback snapshot if live fetch fails.
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void loadAll()
  }, [loadAll])

  const resolvedCache = useMemo(() => ({ ...FALLBACK_CACHE, ...cache }), [cache])
  const usdRows = resolvedCache.usd?.rows ?? []
  const tableRows = usdRows.slice(0, 5)

  return (
    <div className="bank-rates-preview" aria-hidden="true">
      <div className="bank-rates-preview-shell">
        <div className="bank-rates-preview-head">
          <h3>{t('bankRatesPage.title')}</h3>
          <p>{t('bankRatesPage.lead')}</p>
        </div>

        <div className="bank-rates-preview-asset-grid">
          {BANK_RATES_TABS.map((id) => {
            const snapshot = resolvedCache[id]
            const rows = snapshot?.rows ?? []
            return (
              <article key={id} className={`bank-rates-preview-asset-card${id === 'usd' ? ' is-active' : ''}`}>
                <strong className="bank-rates-preview-asset-label">{t(`bankRatesPage.tabs.${id}`)}</strong>
                <PreviewRateRow
                  label={t('bankRatesPage.columns.buy')}
                  row={pickBestBuyRow(rows)}
                  price={pickBestBuyRow(rows)?.buy}
                />
                <PreviewRateRow
                  label={t('bankRatesPage.columns.sell')}
                  row={pickBestSellRow(rows)}
                  price={pickBestSellRow(rows)?.sell}
                />
              </article>
            )
          })}
        </div>

        <div className="bank-rates-preview-toolbar">
          <button type="button" className="bank-rates-preview-refresh" onClick={() => void loadAll()}>
            {t('bankRatesPage.refresh')}
          </button>
          <span className="bank-rates-preview-updated">
            {resolvedCache.usd?.fetchedAt
              ? t('bankRatesPage.updatedAt', {
                  time: new Date(resolvedCache.usd.fetchedAt).toLocaleTimeString(resolveLocale(i18n.language), {
                    hour: '2-digit',
                    minute: '2-digit',
                  }),
                })
              : loading
                ? t('bankRatesPage.loading')
                : t('bankRatesPage.updatedAt', { time: '01:03' })}
          </span>
        </div>

        <div className="bank-rates-preview-table-card">
          <h4 className="bank-rates-preview-table-title">
            {resolvedCache.usd?.title ?? t('bankRatesPage.sectionTitle.usd')}
          </h4>
          <div className="bank-rates-preview-table-wrap">
            <table className="bank-rates-preview-table">
              <thead>
                <tr>
                  <th>{t('bankRatesPage.columns.bank')} ↕</th>
                  <th>{t('bankRatesPage.columns.buy')} ↕</th>
                  <th>{t('bankRatesPage.columns.sell')} ↕</th>
                  <th>{t('bankRatesPage.columns.spread')} ↕</th>
                  <th>{t('bankRatesPage.columns.spreadPct')} ↕</th>
                </tr>
              </thead>
              <tbody>
                {tableRows.map((row) => (
                  <tr key={row.slug || row.name}>
                    <td className="bank-rates-preview-bank-cell">
                      <div className="bank-rates-preview-bank-inner">
                        <BankLogo row={row} />
                        <span>{row.name}</span>
                      </div>
                    </td>
                    <td>
                      {formatBankRate(row.buy)}
                      {row.bestBuy ? <span className="bank-rates-preview-star">★</span> : null}
                    </td>
                    <td>
                      {formatBankRate(row.sell)}
                      {row.bestSell ? <span className="bank-rates-preview-star">★</span> : null}
                    </td>
                    <td>
                      {formatBankRate(row.spread)}
                      {row.bestSpreadMin || row.bestSpreadMax ? <span className="bank-rates-preview-star">★</span> : null}
                    </td>
                    <td>
                      {formatSpreadPercent(row.spreadPercent)}
                      {row.bestSpreadPctMin || row.bestSpreadPctMax ? (
                        <span className="bank-rates-preview-star">★</span>
                      ) : null}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  )
}

function PreviewRateRow({
  label,
  row,
  price,
}: {
  label: string
  row: BankRatesRow | null
  price: number | null | undefined
}) {
  return (
    <div className="bank-rates-preview-rate-row">
      <span className="bank-rates-preview-rate-label">{label}</span>
      <span className="bank-rates-preview-rate-value">
        <BankLogo row={row} compact />
        <span>{formatBankRate(price)}</span>
      </span>
    </div>
  )
}

function BankLogo({ row, compact = false }: { row: BankRatesRow | null | undefined; compact?: boolean }) {
  if (row?.logoUrl) {
    return (
      <img
        src={row.logoUrl}
        alt=""
        width={compact ? 18 : 20}
        height={compact ? 18 : 20}
        className={`bank-rates-preview-logo${compact ? ' is-compact' : ''}`}
        loading="lazy"
      />
    )
  }

  const label = (row?.name ?? '?').slice(0, 1).toUpperCase()
  return <span className={`bank-rates-preview-logo bank-rates-preview-logo--placeholder${compact ? ' is-compact' : ''}`}>{label}</span>
}

function resolveLocale(language: string | undefined): string {
  const normalized = (language ?? 'tr').toLowerCase()
  if (normalized.startsWith('de')) return 'de-DE'
  if (normalized.startsWith('en')) return 'en-US'
  return 'tr-TR'
}
