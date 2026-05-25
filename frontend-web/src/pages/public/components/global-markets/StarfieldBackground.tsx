import { memo, useMemo } from 'react'

type Star = {
  id: number
  left: string
  top: string
  size: number
  opacity: number
  delay: number
  tone: 'bright' | 'dim'
}

function seeded(value: number): number {
  const x = Math.sin(value * 12.9898) * 43758.5453
  return x - Math.floor(x)
}

function createStars(count: number): Star[] {
  return Array.from({ length: count }, (_, index) => {
    const leftRoll = seeded(index * 1.17)
    const topRoll = seeded(index * 2.31 + 4.7)
    const sizeRoll = seeded(index * 3.83 + 1.2)
    const isLeftBias = index % 3 === 0

    return {
      id: index,
      left: `${(isLeftBias ? leftRoll * 46 : leftRoll * 100).toFixed(2)}%`,
      top: `${(topRoll * 100).toFixed(2)}%`,
      size: sizeRoll > 0.82 ? 2 : sizeRoll > 0.55 ? 1.5 : 1,
      opacity: 0.12 + seeded(index * 5.11) * 0.55,
      delay: (index % 14) * 0.28,
      tone: sizeRoll > 0.7 ? 'bright' : 'dim',
    }
  })
}

export const StarfieldBackground = memo(function StarfieldBackground() {
  const stars = useMemo(() => createStars(52), [])

  return (
    <div className="gm-starfield" aria-hidden="true">
      {stars.map((star) => (
        <span
          key={star.id}
          className={`gm-star${star.tone === 'bright' ? ' gm-star-bright' : ''}`}
          style={{
            left: star.left,
            top: star.top,
            width: star.size,
            height: star.size,
            opacity: star.opacity,
            animationDelay: `${star.delay}s`,
          }}
        />
      ))}
    </div>
  )
})
