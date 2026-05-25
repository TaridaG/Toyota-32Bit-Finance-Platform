import * as THREE from 'three'

export function latLngToVector3(lat: number, lng: number, radius: number): THREE.Vector3 {
  const phi = THREE.MathUtils.degToRad(90 - lat)
  const theta = THREE.MathUtils.degToRad(lng + 180)
  const x = -(radius * Math.sin(phi) * Math.cos(theta))
  const z = radius * Math.sin(phi) * Math.sin(theta)
  const y = radius * Math.cos(phi)
  return new THREE.Vector3(x, y, z)
}

export function greatCirclePoints(
  start: THREE.Vector3,
  end: THREE.Vector3,
  segments: number,
  arcHeight = 0.08,
): THREE.Vector3[] {
  const points: THREE.Vector3[] = []
  const startNorm = start.clone().normalize()
  const endNorm = end.clone().normalize()
  for (let i = 0; i <= segments; i += 1) {
    const t = i / segments
    const point = new THREE.Vector3().copy(startNorm).lerp(endNorm, t).normalize()
    const lift = 1 + Math.sin(Math.PI * t) * arcHeight
    points.push(point.multiplyScalar(lift))
  }
  return points
}
