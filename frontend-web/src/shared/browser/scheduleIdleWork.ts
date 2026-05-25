type IdleWindow = Window & {
  requestIdleCallback?: (callback: () => void, options?: { timeout: number }) => number
  cancelIdleCallback?: (handle: number) => void
}

export function scheduleIdleWork(callback: () => void, timeoutMs = 1_000): () => void {
  if (typeof window === 'undefined') {
    return () => {}
  }
  const idleWindow = window as IdleWindow
  if (typeof idleWindow.requestIdleCallback === 'function') {
    const handle = idleWindow.requestIdleCallback(() => callback(), { timeout: timeoutMs })
    return () => {
      idleWindow.cancelIdleCallback?.(handle)
    }
  }
  const handle = window.setTimeout(callback, timeoutMs)
  return () => {
    window.clearTimeout(handle)
  }
}
