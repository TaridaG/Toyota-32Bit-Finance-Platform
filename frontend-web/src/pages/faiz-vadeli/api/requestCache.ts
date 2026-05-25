type CacheEntry<T> = {
  expiresAt: number
  value?: T
  promise?: Promise<T>
}

const requestCache = new Map<string, CacheEntry<unknown>>()

export function getCachedOrLoad<T>(key: string, ttlMs: number, loader: () => Promise<T>): Promise<T> {
  const now = Date.now()
  const cached = requestCache.get(key) as CacheEntry<T> | undefined

  if (cached && cached.value !== undefined && cached.expiresAt > now) {
    return Promise.resolve(cached.value)
  }
  if (cached?.promise) {
    return cached.promise
  }

  const promise = loader()
    .then((value) => {
      requestCache.set(key, { value, expiresAt: Date.now() + ttlMs })
      return value
    })
    .catch((error) => {
      const current = requestCache.get(key)
      if (current?.promise === promise) {
        requestCache.delete(key)
      }
      throw error
    })

  requestCache.set(key, { promise, expiresAt: now + ttlMs })
  return promise
}
