import { memo, useRef } from 'react'
import { Html } from '@react-three/drei'
import { motion } from 'framer-motion'
import { latLngToVector3 } from './geoUtils'
import type { ResolvedFloatingPin } from './resolveFloatingPins'

type FloatingMarketPinsProps = {
  pins: ResolvedFloatingPin[]
  earthRadius: number
  hoveredPinId: string | null
  onPinHover: (pinId: string | null) => void
  reduceMotion: boolean
}

function PinIcon({ icon }: { icon: ResolvedFloatingPin['icon'] }) {
  switch (icon) {
    case 'crypto':
      return (
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <circle cx="12" cy="12" r="9" fill="none" stroke="currentColor" strokeWidth="1.5" />
          <path d="M9 8h4.5a2 2 0 1 1 0 4H9v4" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
          <path d="M12 6v12" fill="none" stroke="currentColor" strokeWidth="1.5" />
        </svg>
      )
    case 'index':
      return (
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M4 18h16M7 14l3-4 3 2 4-6" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
        </svg>
      )
    case 'fx':
      return (
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M7 8h10M7 12h7M7 16h10" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
          <circle cx="5" cy="8" r="1" fill="currentColor" />
          <circle cx="5" cy="12" r="1" fill="currentColor" />
          <circle cx="5" cy="16" r="1" fill="currentColor" />
        </svg>
      )
    case 'metal':
      return (
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <path d="M6 16 12 6l6 10H6z" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinejoin="round" />
        </svg>
      )
    default:
      return (
        <svg viewBox="0 0 24 24" aria-hidden="true">
          <rect x="5" y="5" width="14" height="14" rx="3" fill="none" stroke="currentColor" strokeWidth="1.5" />
          <path d="M9 15V9h3a2 2 0 1 1 0 4H9" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
        </svg>
      )
  }
}

function FloatingPinCard({
  pin,
  isHovered,
  onHover,
  reduceMotion,
}: {
  pin: ResolvedFloatingPin
  isHovered: boolean
  onHover: (active: boolean) => void
  reduceMotion: boolean
}) {
  const isUp = pin.change >= 0

  return (
    <motion.div
      className={`gm-floating-pin${isHovered ? ' is-hovered' : ''}${pin.isLive ? ' is-live' : ''}`}
      initial={false}
      animate={
        reduceMotion
          ? undefined
          : {
              y: isHovered ? -4 : [0, -3, 0],
              scale: isHovered ? 1.05 : 1,
            }
      }
      transition={
        reduceMotion
          ? undefined
          : {
              y: isHovered ? { duration: 0.2 } : { duration: 4.5, repeat: Infinity, ease: 'easeInOut' },
              scale: { duration: 0.2 },
            }
      }
      onPointerEnter={() => onHover(true)}
      onPointerLeave={() => onHover(false)}
    >
      <motion.div
        className="gm-floating-pin-glow"
        animate={{ opacity: isHovered ? 0.95 : 0.45 }}
        transition={{ duration: 0.25 }}
      />
      <motion.div
        className="gm-floating-pin-inner"
        animate={{ boxShadow: isHovered ? '0 0 28px rgba(96,165,250,0.45)' : '0 0 12px rgba(59,130,246,0.18)' }}
      >
        <motion.div className="gm-floating-pin-head">
          <span className="gm-floating-pin-icon">
            <PinIcon icon={pin.icon} />
          </span>
          <strong>{pin.label}</strong>
        </motion.div>
        <span className="gm-floating-pin-price">{pin.price}</span>
        <span className={`gm-floating-pin-change${isUp ? ' is-up' : ' is-down'}`}>
          {isUp ? '+' : ''}
          {pin.change.toFixed(2)}%
        </span>
      </motion.div>
    </motion.div>
  )
}

export const FloatingMarketPins = memo(function FloatingMarketPins({
  pins,
  earthRadius,
  hoveredPinId,
  onPinHover,
  reduceMotion,
}: FloatingMarketPinsProps) {
  const hoverRef = useRef<string | null>(null)

  return (
    <group>
      {pins.map((pin) => {
        const position = latLngToVector3(pin.lat, pin.lng, earthRadius * 1.04)
        const isHovered = hoveredPinId === pin.id
        return (
          <Html
            key={pin.id}
            position={position}
            center
            distanceFactor={6.5}
            zIndexRange={[40, 0]}
            style={{ pointerEvents: 'auto' }}
          >
            <FloatingPinCard
              pin={pin}
              isHovered={isHovered}
              reduceMotion={reduceMotion}
              onHover={(active) => {
                const next = active ? pin.id : null
                if (hoverRef.current === next) {
                  return
                }
                hoverRef.current = next
                onPinHover(next)
              }}
            />
          </Html>
        )
      })}
    </group>
  )
})
