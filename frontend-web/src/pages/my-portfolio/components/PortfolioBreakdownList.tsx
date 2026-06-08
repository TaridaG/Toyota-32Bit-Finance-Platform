import { useTranslation } from 'react-i18next'

export type PortfolioBreakdownRow = {
  portfolioId: number
  portfolioName: string
  amount: number
  subRows?: { label: string; amount?: number }[]
}

type Props = {
  title?: string
  rows: PortfolioBreakdownRow[]
  currencyFormat: Intl.NumberFormat
  hideAmounts?: boolean
  className?: string
}

export function PortfolioBreakdownList({
  title,
  rows,
  currencyFormat,
  hideAmounts = false,
  className,
}: Props) {
  const { t } = useTranslation('portfolio')

  if (rows.length === 0) return null

  const formatAmt = (amount: number) => (hideAmounts ? '•••' : currencyFormat.format(amount))

  return (
    <div className={['my-portfolio-breakdown-list', className].filter(Boolean).join(' ')}>
      <p className="my-portfolio-breakdown-list-title">{title ?? t('breakdown.byPortfolio')}</p>
      <ul>
        {rows.map((row) => (
          <li key={row.portfolioId} className="my-portfolio-breakdown-row">
            <div className="my-portfolio-breakdown-row-main">
              <span className="my-portfolio-breakdown-row-name">{row.portfolioName}</span>
              <span className="my-portfolio-breakdown-row-amount">{formatAmt(row.amount)}</span>
            </div>
            {row.subRows && row.subRows.length > 0 ? (
              <ul className="my-portfolio-breakdown-sublist">
                {row.subRows.map((sub) => (
                  <li key={`${row.portfolioId}-${sub.label}`} className="my-portfolio-breakdown-subrow">
                    <span>{sub.label}</span>
                    {sub.amount != null ? (
                      <span className="my-portfolio-breakdown-subrow-amount">{formatAmt(sub.amount)}</span>
                    ) : null}
                  </li>
                ))}
              </ul>
            ) : null}
          </li>
        ))}
      </ul>
    </div>
  )
}
