import { useMemo, useState, type ReactNode } from 'react'
import { useTranslation } from 'react-i18next'
import type { TFunction } from 'i18next'
import type { BankRatesRow } from '../api/bankRatesApi'
import { formatBankRate, formatSpreadPercent } from '../lib/formatBankRate'
import { enrichBankRatesRows } from '../lib/enrichBankRatesRows'
import {
  sortBankRatesRows,
  type BankRatesSortDir,
  type BankRatesSortKey,
} from '../lib/sortBankRatesRows'
type Props = {
  title: string
  rows: BankRatesRow[]
}

type SortState = {
  key: BankRatesSortKey
  dir: BankRatesSortDir
}

function spreadStarTitle(row: BankRatesRow, t: TFunction<'common'>): string {
  const parts: string[] = []
  if (row.bestSpreadMin) {
    parts.push(t('bankRatesPage.bestSpreadMinHint'))
  }
  if (row.bestSpreadMax) {
    parts.push(t('bankRatesPage.bestSpreadMaxHint'))
  }
  return parts.join(' · ')
}

function spreadPctStarTitle(row: BankRatesRow, t: TFunction<'common'>): string {
  const parts: string[] = []
  if (row.bestSpreadPctMin) {
    parts.push(t('bankRatesPage.bestSpreadPctMinHint'))
  }
  if (row.bestSpreadPctMax) {
    parts.push(t('bankRatesPage.bestSpreadPctMaxHint'))
  }
  return parts.join(' · ')
}

function SortCaret({ active, dir }: { active: boolean; dir: BankRatesSortDir }) {
  if (!active) {
    return <span className="bank-rates-sort-ico bank-rates-sort-ico--idle" aria-hidden>↕</span>
  }
  return (
    <span className="bank-rates-sort-ico" aria-hidden>
      {dir === 'asc' ? '↑' : '↓'}
    </span>
  )
}

export function BankRatesTable({ title, rows }: Props) {
  const { t } = useTranslation('common')
  const [sort, setSort] = useState<SortState | null>(null)

  const highlightedRows = useMemo(() => enrichBankRatesRows(rows), [rows])

  const sortedRows = useMemo(() => {
    if (!sort) {
      return highlightedRows
    }
    return sortBankRatesRows(highlightedRows, sort.key, sort.dir)
  }, [highlightedRows, sort])

  const onSort = (key: BankRatesSortKey) => {
    setSort((prev) => {
      if (prev?.key === key) {
        return { key, dir: prev.dir === 'asc' ? 'desc' : 'asc' }
      }
      return { key, dir: key === 'bank' ? 'asc' : 'asc' }
    })
  }

  const sortHeader = (key: BankRatesSortKey, label: ReactNode, numeric: boolean) => {
    const active = sort?.key === key
    const dir = sort?.dir ?? 'asc'
    return (
      <th
        scope="col"
        className={numeric ? 'bank-rates-table-num' : undefined}
        aria-sort={active ? (dir === 'asc' ? 'ascending' : 'descending') : 'none'}
      >
        <button
          type="button"
          className={`bank-rates-sort-btn${numeric ? ' bank-rates-sort-btn--num' : ''}${active ? ' is-active' : ''}`}
          onClick={() => onSort(key)}
        >
          {label}
          <SortCaret active={active} dir={dir} />
        </button>
      </th>
    )
  }

  return (
    <div className="bank-rates-table-card">
      <h3 className="bank-rates-table-title">{title}</h3>
      <div className="bank-rates-table-wrap" role="region" aria-label={title}>
        <table className="bank-rates-table">
          <colgroup>
            <col className="bank-rates-col-bank" />
            <col className="bank-rates-col-num" span={4} />
          </colgroup>
          <thead>
            <tr>
              {sortHeader('bank', t('bankRatesPage.columns.bank'), false)}
              {sortHeader('buy', t('bankRatesPage.columns.buy'), true)}
              {sortHeader('sell', t('bankRatesPage.columns.sell'), true)}
              {sortHeader('spread', t('bankRatesPage.columns.spread'), true)}
              {sortHeader('spreadPercent', t('bankRatesPage.columns.spreadPct'), true)}
            </tr>
          </thead>
          <tbody>
            {sortedRows.map((row) => (
              <tr key={row.slug || row.name}>
                <td className="bank-rates-table-bank">
                  <div className="bank-rates-table-bank-inner">
                    {row.logoUrl ? (
                      <img
                        src={row.logoUrl}
                        alt=""
                        width={23}
                        height={23}
                        className="bank-rates-table-logo"
                        loading="lazy"
                      />
                    ) : (
                      <span className="bank-rates-table-logo bank-rates-table-logo--placeholder" aria-hidden />
                    )}
                    <span className="bank-rates-table-bank-name">{row.name}</span>
                  </div>
                </td>
                <td className="bank-rates-table-num bank-rates-table-value">
                  {formatBankRate(row.buy)}
                  {row.bestBuy ? (
                    <span className="bank-rates-star" title={t('bankRatesPage.bestBuyHint')} aria-label={t('bankRatesPage.bestBuyHint')}>
                      ★
                    </span>
                  ) : null}
                </td>
                <td className="bank-rates-table-num bank-rates-table-value">
                  {formatBankRate(row.sell)}
                  {row.bestSell ? (
                    <span className="bank-rates-star" title={t('bankRatesPage.bestSellHint')} aria-label={t('bankRatesPage.bestSellHint')}>
                      ★
                    </span>
                  ) : null}
                </td>
                <td className="bank-rates-table-num bank-rates-table-value">
                  {formatBankRate(row.spread)}
                  {row.bestSpreadMin || row.bestSpreadMax ? (
                    <span
                      className="bank-rates-star"
                      title={spreadStarTitle(row, t)}
                      aria-label={spreadStarTitle(row, t)}
                    >
                      ★
                    </span>
                  ) : null}
                </td>
                <td className="bank-rates-table-num bank-rates-table-value">
                  {formatSpreadPercent(row.spreadPercent)}
                  {row.bestSpreadPctMin || row.bestSpreadPctMax ? (
                    <span
                      className="bank-rates-star"
                      title={spreadPctStarTitle(row, t)}
                      aria-label={spreadPctStarTitle(row, t)}
                    >
                      ★
                    </span>
                  ) : null}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}
