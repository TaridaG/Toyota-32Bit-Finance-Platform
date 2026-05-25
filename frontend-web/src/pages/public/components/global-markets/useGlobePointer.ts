import { useEffect, useRef, type RefObject } from 'react'

export type GlobeTiltRef = React.MutableRefObject<{ x: number; y: number }>

const MAX_TILT_X = 0.14
const MAX_TILT_Y = 0.1
const EASING = 0.055

export function useGlobePointer(containerRef: RefObject<HTMLElement | null>, enabled: boolean): GlobeTiltRef {
  const tiltRef = useRef({ x: 0, y: 0 })
  const targetRef = useRef({ x: 0, y: 0 })
  const rafRef = useRef<number | null>(null)

  useEffect(() => {
    if (!enabled) {
      targetRef.current = { x: 0, y: 0 }
      return undefined
    }

    const container = containerRef.current
    if (!container) {
      return undefined
    }

    const onPointerMove = (event: PointerEvent) => {
      const rect = container.getBoundingClientRect()
      if (rect.width <= 0 || rect.height <= 0) {
        return
      }
      const nx = ((event.clientX - rect.left) / rect.width) * 2 - 1
      const ny = ((event.clientY - rect.top) / rect.height) * 2 - 1
      targetRef.current = {
        x: nx * MAX_TILT_X,
        y: -ny * MAX_TILT_Y,
      }
    }

    const onPointerLeave = () => {
      targetRef.current = { x: 0, y: 0 }
    }

    const tick = () => {
      tiltRef.current.x += (targetRef.current.x - tiltRef.current.x) * EASING
      tiltRef.current.y += (targetRef.current.y - tiltRef.current.y) * EASING
      rafRef.current = window.requestAnimationFrame(tick)
    }

    container.addEventListener('pointermove', onPointerMove, { passive: true })
    container.addEventListener('pointerleave', onPointerLeave)
    rafRef.current = window.requestAnimationFrame(tick)

    return () => {
      container.removeEventListener('pointermove', onPointerMove)
      container.removeEventListener('pointerleave', onPointerLeave)
      if (rafRef.current != null) {
        window.cancelAnimationFrame(rafRef.current)
      }
    }
  }, [containerRef, enabled])

  return tiltRef
}
