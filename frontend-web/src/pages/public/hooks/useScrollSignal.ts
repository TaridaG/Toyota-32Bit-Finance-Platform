import { useEffect, useState } from 'react'

type ScrollDirection = 'up' | 'down'

export type ScrollSignal = {
  direction: ScrollDirection
  velocity: number
  smoothedVelocity: number
  progress: number
}

const INITIAL_SIGNAL: ScrollSignal = {
  direction: 'down',
  velocity: 0,
  smoothedVelocity: 0,
  progress: 0,
}

export function useScrollSignal(): ScrollSignal {
  const [signal, setSignal] = useState<ScrollSignal>(INITIAL_SIGNAL)

  useEffect(() => {
    let rafId = 0
    let ticking = false
    let lastY = window.scrollY
    let lastTs = performance.now()
    let smoothVelocity = 0

    const updateSignal = () => {
      const now = performance.now()
      const currentY = window.scrollY
      const delta = currentY - lastY
      const dt = Math.max((now - lastTs) / 1000, 1 / 120)
      const velocity = Math.abs(delta) / dt
      smoothVelocity = smoothVelocity * 0.84 + velocity * 0.16

      const maxScroll = Math.max(document.documentElement.scrollHeight - window.innerHeight, 1)
      const progress = Math.min(Math.max(currentY / maxScroll, 0), 1)

      setSignal({
        direction: delta < 0 ? 'up' : 'down',
        velocity,
        smoothedVelocity: smoothVelocity,
        progress,
      })

      lastY = currentY
      lastTs = now
      ticking = false
    }

    const handleScroll = () => {
      if (ticking) return
      ticking = true
      rafId = window.requestAnimationFrame(updateSignal)
    }

    window.addEventListener('scroll', handleScroll, { passive: true })
    handleScroll()

    return () => {
      window.removeEventListener('scroll', handleScroll)
      if (rafId) window.cancelAnimationFrame(rafId)
    }
  }, [])

  return signal
}
