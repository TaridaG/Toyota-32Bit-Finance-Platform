import { useEffect, useRef, useState } from 'react'
import type { MarketTickerItem } from '../types'

function Sparkline({ values }: { values: number[] }) {
  const width = 92
  const height = 28
  const min = Math.min(...values)
  const max = Math.max(...values)
  const spread = max - min || 1
  const points = values
    .map((value, index) => {
      const x = (index / (values.length - 1)) * width
      const y = height - ((value - min) / spread) * (height - 3)
      return `${x},${y}`
    })
    .join(' ')

  return (
    <svg viewBox={`0 0 ${width} ${height}`} className="fi-sparkline" aria-hidden>
      <polyline points={points} />
    </svg>
  )
}

export function MarketTicker({ item }: { item: MarketTickerItem }) {
  const [displayPrice, setDisplayPrice] = useState(item.price)
  const lastPriceRef = useRef(item.price)
  const isPositive = item.changePercent >= 0

  useEffect(() => {
    let frame = 0
    const start = lastPriceRef.current
    const end = item.price
    const duration = 300
    const startedAt = performance.now()

    const animate = (now: number) => {
      const progress = Math.min((now - startedAt) / duration, 1)
      const next = start + (end - start) * progress
      setDisplayPrice(next)
      if (progress < 1) {
        frame = window.requestAnimationFrame(animate)
      } else {
        lastPriceRef.current = end
      }
    }

    frame = window.requestAnimationFrame(animate)
    return () => window.cancelAnimationFrame(frame)
  }, [item.price])

  return (
    <article className="fi-ticker">
      <div>
        <h4>{item.symbol}</h4>
        <strong>{displayPrice.toLocaleString(undefined, { maximumFractionDigits: 2 })}</strong>
      </div>
      <p className={isPositive ? 'fi-up' : 'fi-down'}>
        {isPositive ? '+' : ''}
        {item.changePercent.toFixed(2)}%
      </p>
      <Sparkline values={item.sparkline} />
    </article>
  )
}
