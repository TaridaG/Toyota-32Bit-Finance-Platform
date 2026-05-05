/**
 * When the backend exposes a preferred locale (for example on portal profile), implement a single
 * GET under `/api/...` here. Previous probes used paths without the `/api` prefix and did not match
 * the dev proxy, which caused noisy failed requests.
 */
export async function fetchLocaleFromBackend(): Promise<string | null> {
  return null
}
