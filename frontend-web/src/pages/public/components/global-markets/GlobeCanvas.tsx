import { Suspense, lazy, memo, useEffect, useState } from 'react'
import type { GlobeTiltRef } from './useGlobePointer'

const GlobeScene = lazy(() => import('./GlobeScene').then((module) => ({ default: module.GlobeScene })))
const Canvas = lazy(() => import('@react-three/fiber').then((module) => ({ default: module.Canvas })))

type GlobeCanvasProps = {
  tiltRef: GlobeTiltRef
  reduceMotion: boolean
  isMobile: boolean
  isVisible: boolean
  isDark: boolean
}

function GlobeFallback() {
  return (
    <div className="gm-globe-fallback" aria-hidden="true">
      <div className="gm-globe-fallback-sphere" />
    </div>
  )
}

export const GlobeCanvas = memo(function GlobeCanvas({
  tiltRef,
  reduceMotion,
  isMobile,
  isVisible,
  isDark,
}: GlobeCanvasProps) {
  const [shouldMount, setShouldMount] = useState(false)

  useEffect(() => {
    if (isVisible) {
      setShouldMount(true)
    }
  }, [isVisible])

  if (!shouldMount) {
    return <GlobeFallback />
  }

  return (
    <div className="gm-globe-canvas-host">
      <Suspense fallback={<GlobeFallback />}>
        <Canvas
          className="gm-globe-canvas"
          dpr={isMobile ? 1 : [1, 1.35]}
          camera={{ position: [0, 0, 4.75], fov: 33 }}
          gl={{
            antialias: true,
            alpha: true,
            powerPreference: 'high-performance',
          }}
          style={{ display: 'block', overflow: 'visible' }}
          frameloop={isVisible ? 'always' : 'demand'}
        >
          <Suspense fallback={null}>
            <GlobeScene tiltRef={tiltRef} reduceMotion={reduceMotion} isMobile={isMobile} isDark={isDark} />
          </Suspense>
        </Canvas>
      </Suspense>
    </div>
  )
})
