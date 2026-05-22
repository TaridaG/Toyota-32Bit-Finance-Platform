import { useTranslation } from 'react-i18next'
import type { BankRatesResponse, BankRatesRow } from '../api/bankRatesApi'
import { BANK_RATES_TABS, type BankRatesTabId } from '../bankRatesAssets'
import { formatBankRate } from '../lib/formatBankRate'
import { pickBestBuyRow, pickBestSellRow } from '../lib/pickBestRows'

type Props = {
  active: BankRatesTabId
  cache: Partial<Record<BankRatesTabId, BankRatesResponse>>
  onSelect: (id: BankRatesTabId) => void
}

function CardRateRow({
  label,
  row,
  priceKey,
}: {
  label: string
  row: BankRatesRow | null
  priceKey: 'buy' | 'sell'
}) {
  const price = row?.[priceKey]

  return (
    <div className="bank-rates-asset-rate">
      <span className="bank-rates-asset-rate-label">{label}</span>
      <span className="bank-rates-asset-rate-value">
        {row?.logoUrl ? (
          <img
            src={row.logoUrl}
            alt=""
            width={22}
            height={22}
            className="bank-rates-asset-rate-logo"
            loading="lazy"
          />
        ) : row ? (
          <span className="bank-rates-asset-rate-logo bank-rates-asset-rate-logo--placeholder" aria-hidden />
        ) : null}
        <span className="bank-rates-asset-rate-price">{price != null ? formatBankRate(price) : '—'}</span>
      </span>
    </div>
  )
}

export function BankRatesAssetCards({ active, cache, onSelect }: Props) {
  const { t } = useTranslation('common')

  return (
    <div className="bank-rates-asset-grid" role="tablist" aria-label={t('bankRatesPage.tabsAria')}>
      {BANK_RATES_TABS.map((id) => {
        const snapshot = cache[id]
        const rows = snapshot?.rows ?? []
        const bestBuyRow = rows.length ? pickBestBuyRow(rows) : null
        const bestSellRow = rows.length ? pickBestSellRow(rows) : null

        return (
          <button
            key={id}
            type="button"
            role="tab"
            aria-selected={active === id}
            className={`bank-rates-asset-card${active === id ? ' bank-rates-asset-card--active' : ''}`}
            onClick={() => onSelect(id)}
          >
            <span className="bank-rates-asset-card-label">{t(`bankRatesPage.tabs.${id}`)}</span>
            <CardRateRow label={t('bankRatesPage.columns.buy')} row={bestBuyRow} priceKey="buy" />
            <CardRateRow label={t('bankRatesPage.columns.sell')} row={bestSellRow} priceKey="sell" />
          </button>
        )
      })}
    </div>
  )
}
