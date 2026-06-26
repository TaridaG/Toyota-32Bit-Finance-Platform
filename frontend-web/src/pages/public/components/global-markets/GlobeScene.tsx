import { memo, useMemo, useRef } from 'react'
import { useFrame } from '@react-three/fiber'
import { useTexture, Line } from '@react-three/drei'
import * as THREE from 'three'
import {
  EARTH_BUMP_URL,
  EARTH_DAY_TEXTURE_URL,
  EARTH_NIGHT_TEXTURE_URL,
  FLOATING_MARKET_PINS,
  NETWORK_CONNECTIONS,
} from './constants'
import { greatCirclePoints, latLngToVector3 } from './geoUtils'
import type { GlobeTiltRef } from './useGlobePointer'

type GlobeSceneProps = {
  tiltRef: GlobeTiltRef
  reduceMotion: boolean
  isMobile: boolean
  isDark: boolean
}

const EARTH_RADIUS = 1.26

function NetworkLines({ isMobile, isDark }: { isMobile: boolean; isDark: boolean }) {
  const lines = useMemo(() => {
    const pinById = new Map(FLOATING_MARKET_PINS.map((pin) => [pin.id, pin]))
    return NETWORK_CONNECTIONS.map(([fromId, toId], index) => {
      const fromPin = pinById.get(fromId)
      const toPin = pinById.get(toId)
      if (!fromPin || !toPin) {
        return null
      }
      const start = latLngToVector3(fromPin.lat, fromPin.lng, EARTH_RADIUS)
      const end = latLngToVector3(toPin.lat, toPin.lng, EARTH_RADIUS)
      const points = greatCirclePoints(start, end, isMobile ? 12 : 18, 0.08)
      return {
        id: `${fromId}-${toId}-${index}`,
        points,
      }
    }).filter((line): line is NonNullable<typeof line> => line != null)
  }, [isMobile])

  if (isMobile) {
    return null
  }

  return (
    <group>
      {lines.map((line) => (
        <Line
          key={line.id}
          points={line.points}
          color={isDark ? '#6b7280' : '#3b82f6'}
          transparent
          opacity={isDark ? 0.14 : 0.18}
          lineWidth={0.6}
        />
      ))}
    </group>
  )
}

function EarthMesh({ segments, isDark }: { segments: number; isDark: boolean }) {
  const textureUrl = isDark ? EARTH_NIGHT_TEXTURE_URL : EARTH_DAY_TEXTURE_URL
  const [colorMap, bumpMap] = useTexture([textureUrl, EARTH_BUMP_URL])

  colorMap.colorSpace = THREE.SRGBColorSpace

  return (
    <mesh>
      <sphereGeometry args={[EARTH_RADIUS, segments, segments]} />
      <meshStandardMaterial
        map={colorMap}
        bumpMap={bumpMap}
        emissiveMap={isDark ? colorMap : null}
        bumpScale={isDark ? 0.05 : 0.036}
        roughness={isDark ? 0.82 : 0.68}
        metalness={isDark ? 0.03 : 0.02}
        color={isDark ? '#d7dde8' : '#eef8ff'}
        emissive={isDark ? '#f8fafc' : '#60a5fa'}
        emissiveIntensity={isDark ? 0.82 : 0.08}
      />
    </mesh>
  )
}

function AtmosphereShell({ isDark }: { isDark: boolean }) {
  return (
    <mesh scale={[1.025, 1.025, 1.025]}>
      <sphereGeometry args={[EARTH_RADIUS, 32, 32]} />
      <meshBasicMaterial
        color={isDark ? '#a3a3a3' : '#bfdbfe'}
        transparent
        opacity={isDark ? 0.038 : 0.16}
        side={THREE.BackSide}
        depthWrite={false}
      />
    </mesh>
  )
}

export const GlobeScene = memo(function GlobeScene({ tiltRef, reduceMotion, isMobile, isDark }: GlobeSceneProps) {
  const groupRef = useRef<THREE.Group>(null)
  const segments = isMobile ? 28 : 40

  useFrame((_, delta) => {
    const group = groupRef.current
    if (!group) {
      return
    }

    if (!reduceMotion) {
      group.rotation.y += delta * 0.035
    }

    const targetX = tiltRef.current.y
    const targetZ = -tiltRef.current.x
    group.rotation.x += (targetX - group.rotation.x) * 0.06
    group.rotation.z += (targetZ - group.rotation.z) * 0.06
  })

  return (
    <>
      <ambientLight intensity={isDark ? 0.34 : 0.74} />
      {!isDark ? <hemisphereLight args={['#dbeafe', '#d6d3d1', 0.55]} /> : null}
      <directionalLight
        position={isDark ? [3, 2, 4] : [4.5, 1.8, 5]}
        intensity={isDark ? 1.02 : 1.55}
        color={isDark ? '#ffffff' : '#f8fafc'}
      />
      {isDark ? <pointLight position={[-3.5, -1.2, 2.8]} intensity={0.2} color="#94a3b8" /> : null}

      <group ref={groupRef}>
        <EarthMesh key={isDark ? 'night' : 'day'} segments={segments} isDark={isDark} />
        <AtmosphereShell isDark={isDark} />
        <NetworkLines isMobile={isMobile} isDark={isDark} />
      </group>
    </>
  )
})
