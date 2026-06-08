import { useCallback, useState, type MouseEvent, type ReactNode } from 'react'
import { PortfolioBreakdownList, type PortfolioBreakdownRow } from './PortfolioBreakdownList'

type Props = {
  enabled: boolean
  title?: string
  rows: PortfolioBreakdownRow[]
  currencyFormat: Intl.NumberFormat
  hideAmounts?: boolean
  children: ReactNode
  className?: string
}

export function PortfolioBreakdownHover({
  enabled,
  title,
  rows,
  currencyFormat,
  hideAmounts = false,
  children,
  className,
}: Props) {
  const [visible, setVisible] = useState(false)
  const [pos, setPos] = useState({ x: 0, y: 0 })

  const handleMove = useCallback((e: MouseEvent) => {
    setPos({ x: e.clientX, y: e.clientY })
  }, [])

  const show = enabled && visible && rows.length > 0

  return (
    <span
      className={['my-portfolio-breakdown-hover', className].filter(Boolean).join(' ')}
      onMouseEnter={(e) => {
        if (!enabled || rows.length === 0) return
        setVisible(true)
        handleMove(e)
      }}
      onMouseMove={handleMove}
      onMouseLeave={() => setVisible(false)}
      onFocus={(e) => {
        if (!enabled || rows.length === 0) return
        setVisible(true)
        const rect = (e.target as HTMLElement).getBoundingClientRect()
        setPos({ x: rect.left + rect.width / 2, y: rect.top })
      }}
      onBlur={() => setVisible(false)}
      tabIndex={enabled && rows.length > 0 ? 0 : undefined}
    >
      {children}
      {show ? (
        <div
          className="my-portfolio-allocation-tooltip my-portfolio-breakdown-tooltip"
          style={{ left: pos.x + 16, top: pos.y + 16 }}
          role="tooltip"
        >
          <PortfolioBreakdownList
            title={title}
            rows={rows}
            currencyFormat={currencyFormat}
            hideAmounts={hideAmounts}
          />
        </div>
      ) : null}
    </span>
  )
}
